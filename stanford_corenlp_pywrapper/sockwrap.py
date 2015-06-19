"""
Client and process monitor for the java socket server.
"""

from __future__ import division
import subprocess, tempfile, time, os, logging, re, struct, socket, atexit, glob, itertools
from copy import copy,deepcopy
from pprint import pprint
try:
    import ujson as json
except ImportError:
    import json

# SUGGESTED: for constituent parsing models, specify shift-reduce parser in
# configdict with:
#  'parse.model': 'edu/stanford/nlp/models/srparser/englishSR.ser.gz'

MODES_items = [

('ssplit',  {'annotators': "tokenize, ssplit",
    'description': "tokenization and sentence splitting (included in all subsequent ones)", }),
('pos',     {'annotators':"tokenize, ssplit, pos, lemma",
    'description':"POS (and lemmas)",}),
('ner',     {'annotators':"tokenize, ssplit, pos, lemma, ner, entitymentions",
    'description':"POS and NER (and lemmas)",}),
('parse',    {'annotators':"tokenize, ssplit, pos, lemma, parse",
    'description':"fairly basic parsing with POS, lemmas, trees, dependencies",}),
('nerparse', {'annotators':"tokenize, ssplit, pos, lemma, ner, entitymentions, parse",
    'description':"parsing with NER, POS, lemmas, depenencies."}),
('coref', {'annotators':"tokenize, ssplit, pos, lemma, ner, entitymentions, parse, dcoref",
    'description':"Coreference, including constituent parsing."})
]

MODES = dict(MODES_items)

logging.basicConfig()  # wtf, why we have to call this?
LOG = logging.getLogger("CoreNLP_PyWrapper")
LOG.setLevel("INFO")
# LOG.setLevel("DEBUG")

PARSEDOC_TIMEOUT_SEC = 60 * 5
STARTUP_BUSY_WAIT_INTERVAL_SEC = 1.0

def command(mode=None, configfile=None, configdict=None, comm_mode=None,
        java_command="java",
        java_options="-Xmx4g -XX:ParallelGCThreads=1",
        **kwargs):
    d = {}
    d.update(**locals())
    d.update(**kwargs)

    more_config = ""
    if mode is None and configfile is None and configdict is None:
        assert False, "Need to set mode, or the annotators directly, for this wrapper to work."
    if mode:
        if configdict is not None:
            assert 'annotators' not in configdict, "mode was given but annotators are set in the configdict.  use only one please."
        elif configdict is None:
            configdict = {}
        LOG.info("mode given as '%s' so setting annotators: %s" % (mode, MODES[mode]['annotators']))
        configdict['annotators'] = MODES[mode]['annotators']
    if configfile:
        more_config += " --configfile {}".format(configfile)
    if configdict:
        j = json.dumps(configdict)
        assert "'" not in j, "can't handle single quote in config values"
        more_config += " --configdict '{}'".format(j)
    d['more_config'] = more_config

    if comm_mode=='SOCKET':
        d['comm_info'] = "--server {server_port}".format(**d)
    elif comm_mode=='PIPE':
        d['comm_info'] = "--outpipe {outpipe}".format(**d)
    else: assert False, "need comm_mode to be SOCKET or PIPE but got " + repr(comm_mode)


    cmd = """exec {java_command} {java_options} -cp '{classpath}' 
    corenlp.SocketServer {comm_info} {more_config}"""
    return cmd.format(**d).replace("\n", " ")


class SubprocessCrashed(Exception):
    pass


class CoreNLP:

    def __init__(self, mode=None, 
            configfile=None, configdict=None,
            corenlp_jars=(
                "/home/sw/corenlp/stanford-corenlp-full-2015-04-20/*",
                "/home/sw/stanford-srparser-2014-10-23-models.jar",
                ),
            comm_mode='PIPE',  # SOCKET or PIPE
            server_port=12340, outpipe_filename_prefix="/tmp/corenlp_pywrap_pipe",
            **more_configdict_args
            ):
        """
        mode: if you supply this as a single string, we'll use a prebaked set
        of annotators.  if you don't want this, specify either (configfile or
        configdict) and set 'annotators' there.

        corenlp_jars: jars for the classpath.  tuple or list of strings.
        this is just passed on to java's "-cp" flag; note that it accepts
        limited use of wildcards.

        configfile, configdict: can give a corenlp configuration, either as an
        external file (like the .ini or java properties format, whatever it
        is), or else as a python dictionary.  we just pass it on, though we
        will look at the annotators setting.

        Extra keyword arguments are added to the configdict.
        Note you can't pass options with dots in them this way.

        server_port: have to specify this if you want to run multple instances
        in separate processes.  todo we should use some other communication
        mechanism that doesnt have to worry about this
        """
        self.mode = mode
        self.proc = None
        self.server_port = server_port
        self.configfile = configfile
        self.comm_mode = comm_mode
        self.outpipe = None

        self.configdict = deepcopy(configdict)
        if not self.configdict: self.configdict = {}
        self.configdict.update(more_configdict_args)
        if not self.configdict: self.configdict = None

        if self.comm_mode=='PIPE':
            tag = "pypid=%d_time=%s" % (os.getpid(), time.time())
            self.outpipe = "%s_%s" % (outpipe_filename_prefix, tag)
            assert not os.path.exists(self.outpipe)

        assert isinstance(corenlp_jars, (list,tuple))

        deglobbed = itertools.chain(*[glob.glob(f) for f in corenlp_jars])
        assert any(os.path.exists(f) for f in deglobbed), "CoreNLP jar files don't seem to exist; are the paths correct?  Searched files: %s" % repr(deglobbed)

        local_libdir = os.path.join(os.path.abspath(os.path.dirname(__file__)),
                                    'lib')

        jars = [os.path.join(local_libdir, "*")]
        jars += corenlp_jars
        self.classpath = ':'.join(jars)
        # self.classpath += ":../bin:bin"  ## for eclipse java dev

        # LOG.info("CLASSPATH: " + self.classpath)

        self.start_server()
        # This probably is only half-reliable, but worth a shot.
        atexit.register(self.cleanup)

    def cleanup(self):
        self.kill_proc_if_running()
        if self.outpipe and os.path.exists(self.outpipe):
            os.unlink(self.outpipe)

    def __del__(self):
        # This is also an unreliable way to ensure the subproc is gone, but
        # might as well try
        self.cleanup()

    def start_server(self):
        self.kill_proc_if_running()

        if self.comm_mode=='PIPE':
            if not os.path.exists(self.outpipe):
                os.mkfifo(self.outpipe)
        
        cmd = command(**self.__dict__)
        LOG.info("Starting java subprocess, and waiting for signal it's ready, with command: %s" % cmd)
        self.proc = subprocess.Popen(cmd, shell=True, stdin=subprocess.PIPE)
        time.sleep(STARTUP_BUSY_WAIT_INTERVAL_SEC)

        if self.comm_mode=='SOCKET':
            sock = self.get_socket(num_retries=100, retry_interval=STARTUP_BUSY_WAIT_INTERVAL_SEC)
            sock.close()
        elif self.comm_mode=='PIPE':
            self.outpipe_fp = open(self.outpipe, 'r')

        while True:
            # This loop is for if you have timeouts for the socket connection
            # The pipe system doesn't have timeouts, so this should run only
            # once in that case.
            try:
                ret = self.send_command_and_parse_result('PING\t""', 2)
                if ret is None:
                    continue
                assert ret == "PONG", "Bad return data on startup ping: " + ret
                LOG.info("Successful ping. The server has started.")
                break
            except socket.error, e:
                LOG.info("Waiting for startup: ping got exception: %s %s" % (type(e), e))
                LOG.info("pausing before retry")
                time.sleep(STARTUP_BUSY_WAIT_INTERVAL_SEC)

        LOG.info("Subprocess is ready.")

    def ensure_proc_is_running(self):
        if self.proc is None:
            # Has never been started
            self.start_server()
        elif self.proc.poll() is not None:
            # Restart
            self.start_server()

    def kill_proc_if_running(self):
        if self.proc is None:
            # it's never been started yet
            return
        retcode = self.proc.poll()
        if retcode is not None:
            LOG.info("Subprocess seems to be stopped, exit code %s" % retcode)
        elif retcode is None:
            LOG.warning("Killing subprocess %s" % self.proc.pid)
            os.kill(self.proc.pid, 9)

    def parse_doc(self, text, timeout=PARSEDOC_TIMEOUT_SEC, raw=False):
        cmd = "PARSEDOC\t%s" % json.dumps(text)
        return self.send_command_and_parse_result(cmd, timeout, raw=raw)

    def get_socket(self, num_retries=1, retry_interval=1):
        # could be smarter here about reusing the same socket?
        for trial in range(num_retries):
            try:
                sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                # sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1) # not sure if this is needed?
                sock.connect(('localhost', self.server_port))
                return sock
            except (socket.error, socket.timeout) as e:
                LOG.info("socket error when making connection (%s)" % e)
                if trial < num_retries-1:
                    LOG.info("pausing before retry")
                    time.sleep(retry_interval)
        assert False, "couldnt connect socket"

    def send_command_and_parse_result(self, cmd, timeout, raw=False):
        try:
            self.ensure_proc_is_running()
            data = self.send_command_and_get_string_result(cmd, timeout)
            if data is None: return None
            decoded = None
            if raw:
                return data
            try:
                decoded = json.loads(data)
            except ValueError:
                LOG.warning("Bad JSON returned from subprocess; returning null.")
                LOG.warning("Bad JSON length %d, starts with: %s" % (len(data), repr(data[:1000])))
                return None
            return decoded
        except socket.timeout, e:
            LOG.info("Socket timeout happened, returning None: %s %s" % (type(e), e))
            return None
            # This is tricky. maybe the process is running smoothly but just
            # taking longer than we like.  if it's in thie state, and we try to
            # send another command, what happens?  Should we forcibly restart
            # the process now just in case?

    def send_command_and_get_string_result(self, cmd, timeout):
        if self.comm_mode == 'SOCKET':
            sock = self.get_socket(num_retries=100)
            sock.settimeout(timeout)
            sock.sendall(cmd + "\n")
            size_info_str = sock.recv(8)
        elif self.comm_mode == 'PIPE':
            self.proc.stdin.write(cmd + "\n")
            self.proc.stdin.flush()
            size_info_str = self.outpipe_fp.read(8)

        # java "long" is 8 bytes, which python struct calls "long long".
        # java default byte ordering is big-endian.
        size_info = struct.unpack('>Q', size_info_str)[0]
        # print "size expected", size_info

        chunks = []
        curlen = lambda: sum(len(x) for x in chunks)
        while True:
            remaining_size = size_info - curlen()
            if self.comm_mode == 'SOCKET':
                data = sock.recv(remaining_size)
            elif self.comm_mode == 'PIPE':
                data = self.outpipe_fp.read(remaining_size)
            chunks.append(data)
            if curlen() >= size_info: break
            if len(chunks) > 1000:
                LOG.warning("Incomplete value from server")
                return None
            time.sleep(0.01)
        return ''.join(chunks)


def test_modes():
    import pytest
    gosimple(comm_mode='SOCKET')
    gosimple(comm_mode='PIPE')
    with pytest.raises(AssertionError):
        gosimple(comm_mode=None)
    with pytest.raises(AssertionError):
        gosimple(comm_mode='asdfasdf')

def test_coref():
    assert_no_java("no java when starting")
    p = CoreNLP("coref")
    ret = p.parse_doc("I saw Fred. He saw me.")
    pprint(ret)
    assert 'entities' in ret
    assert isinstance(ret['entities'], list)

def gosimple(**kwargs):
    assert_no_java("no java when starting")

    p = CoreNLP("ssplit", **kwargs)
    ret = p.parse_doc("Hello world.")
    # pprint(ret)
    assert len(ret['sentences']) == 1
    assert u' '.join(ret['sentences'][0]['tokens']) == u"Hello world ."

    p.kill_proc_if_running()
    assert_no_java()

def test_paths():
    import pytest
    with pytest.raises(AssertionError):
        CoreNLP("ssplit", corenlp_jars=["/asdfadsf/asdfasdf"])

def assert_no_java(msg=""):
    ps_output = os.popen("ps wux").readlines()
    javalines = [x for x in ps_output if re.search(r'\bbin/java\b', x)]
    print ''.join(javalines)
    assert len(javalines) == 0, msg

# def test_doctimeout():
#     assert_no_java("no java when starting")
#
#     p = CoreNLP("pos")
#     ret = p.parse_doc(open("allbrown.txt").read(), 0.5)
#     assert ret is None
#     p.kill_proc_if_running()
#     assert_no_java()

if __name__=='__main__':
    import sys
    if sys.argv[1]=='modes':
        for mode,d in MODES_items:
            print "  * `%s`: %s" % (mode, d['description'])
    if sys.argv[1]=='modes_json':
        # import json as stdjson
        # print stdjson.dumps(MODES, indent=4)
        print '"%s"' % json.dumps(MODES).replace('"', r'\"')
