"""
The pipe-based tempfile approach.
this is kind of lame.
"""

from __future__ import division
import subprocess,tempfile,time,os,logging,re
try:
    import ujson as json
except ImportError:
    import json

logging.basicConfig()  # wtf, why we have to call this?
LOG = logging.getLogger("StanfordPipeWrap")
LOG.setLevel("INFO")
# LOG.setLevel("DEBUG")

COMMAND = """
exec {JAVA} -Xmx{XMX_AMOUNT} -cp {CLASSPATH}
    corenlp.PipeCommandRunner {mode} {startup_tmp}"""

JAVA = "java"
XMX_AMOUNT = "4g"

CORENLP_LIBDIR = "/users/brendano/sw/nlp/stanford-corenlp-full-2014-01-04"
LOCAL_LIBDIR = os.path.join(os.path.abspath(os.path.dirname(__file__)), 'lib')

CLASSPATH = ':'.join([
    os.path.join(LOCAL_LIBDIR, "piperunner.jar"),
    # "/Users/brendano/myutil/bin", # for eclipse development only
    os.path.join(LOCAL_LIBDIR, "guava-13.0.1.jar"),
    os.path.join(LOCAL_LIBDIR, "jackson-all-1.9.11.jar"),
    os.path.join(CORENLP_LIBDIR, "stanford-corenlp-3.3.1.jar"),
    os.path.join(CORENLP_LIBDIR, "stanford-corenlp-3.3.1-models.jar"),
])

# If you parallelize a lot on one machine, could get file handle issues if this
# is too small.
BUSY_WAIT_INTERVAL_SEC = 10 * 1e-3
PARSEDOC_TIMEOUT_SEC = 30
STARTUP_TIMEOUT_SEC = 60*5
NUM_RETRIES = 2

TEMP_DIR = None   ## arg for mkstemp(dir=), so if None it defaults to somewhere

def command(mode, startup_tmp):
    d = {}
    d.update(globals())
    d.update(locals())
    return COMMAND.format(**d).replace("\n"," ")

def get_last_byte(fileobj, filename):
    stat = os.stat(filename)
    if stat.st_size==0:
        return None
    fileobj.seek(stat.st_size - 1)
    char = fileobj.read(1)
    if char=='':
        # Something went wrong: the file is smaller than os.stat thinks it is.
        # maybe a weird sync issue.
        # or the file is just empty...
        pass
    return char

def delete_if_necessary(filename):
    if os.path.exists(filename):
        os.unlink(filename)

class SubprocessCrashed(Exception): pass
class TimeoutHappened(Exception): pass

def mktemp(suffix):
    return tempfile.mkstemp(suffix, prefix="tmp.pipewrap.")

class PipeWrap:

    def __init__(self, mode):
        self.mode = mode
        self.proc = None
        self.num_retries = 0
        self.start_pipe()

    def start_pipe(self):
        self.kill_proc_if_running()
        _,startup_tmp = mktemp(".ready")
        cmd = command(mode=self.mode, startup_tmp=startup_tmp)
        LOG.info("Starting pipe subprocess, and waiting for signal it's ready, with command: %s" % cmd)
        self.proc = subprocess.Popen(cmd, shell=True, stdin=subprocess.PIPE)
        self.wait_and_return_result(startup_tmp, STARTUP_TIMEOUT_SEC)
        delete_if_necessary(startup_tmp)
        LOG.info("Subprocess is ready.")

    def ensure_proc_is_running(self):
        if self.proc is None:
            # Has never been started
            self.start_pipe()
        elif self.proc.poll() is not None:
            # Restart
            self.start_pipe()

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

    def parse_doc(self, text, timeout=PARSEDOC_TIMEOUT_SEC):
        tmpfd,tmpfile = mktemp(".parseout")
        cmd = "PARSEDOC\t%s\t%s" % (tmpfile, json.dumps(text))
        return self.send_command_and_wait_for_result(cmd, tmpfile, timeout)

    def send_command_and_wait_for_result(self, cmd, tmpfile, timeout):
        self.ensure_proc_is_running()
        self.proc.stdin.write(cmd)
        self.proc.stdin.write("\n")
        try:
            data = self.wait_and_return_result(tmpfile, timeout)
            decoded = None
            try:
                decoded = json.loads(data)
            except ValueError:
                LOG.warning("Bad JSON returned from subprocess; returning null.")
                return None
            self.num_retries = 0   # set this only when super sure it succeeded
            return decoded
        except SubprocessCrashed:
            if self.increment_current_num_retries():
                LOG.warning("Subprocess creashed: Too many retries. Giving up")
                return None
            LOG.warning("Subprocess crashed. Restarting.")
            self.start_pipe()
            LOG.warning("OK, retrying the command.")
            return self.send_command_and_wait_for_result(cmd, tmpfile, timeout)
        except TimeoutHappened:
            # if self.increment_current_num_retries():
            #     LOG.warning("Timeout: Too many retries. Giving up")
            #     return None
            LOG.warning("Timeout happened. Returning null.")
            # TODO: if many timeouts, should try a restart
            # self.start_pipe()
            return None
        finally:
            delete_if_necessary(tmpfile)

    def increment_current_num_retries(self):
        self.num_retries += 1
        if (self.num_retries >= NUM_RETRIES):
            return True
        return False

    def crash(self):
        self.send_command_and_wait_for_result('CRASH\t/tmp/bogus\t"bogus"', "/tmp/bogus", 1)

    def wait_and_return_result(self, tmpfile, timeout):
        LOG.debug("waiting on %s" % tmpfile)
        fp = self.wait_for_finish(tmpfile, timeout)
        LOG.debug("file wait done.")
        fp.seek(0)
        data = fp.read()
        fp.close()
        assert data[-1]=='\0'
        LOG.debug("file %s had %s size payload" % (tmpfile, len(data)))
        return data[:-1]

    def wait_for_finish(self,tmpfile, timeout):
        """Busy wait..."""
        start = time.time()
        while True:
            if time.time() - start > timeout:
                raise TimeoutHappened()
            retcode = self.proc.poll()
            if retcode is not None:
                LOG.warning("Subprocess seems to have crashed with code: %s" % retcode)
                raise SubprocessCrashed()
            if not os.path.exists(tmpfile):
                pass
            else:
                fp = open(tmpfile)
                last = get_last_byte(fp,tmpfile)
                if last=='\0':
                    return fp
                fp.close()
            time.sleep(BUSY_WAIT_INTERVAL_SEC)

def test_simple():
    assert_no_java("no java when starting")

    p = PipeWrap("ssplit")
    ret = p.parse_doc("Hello world.")
    print ret
    assert len(ret['sentences'])==1
    assert u' '.join(ret['sentences'][0]['tokens']) == u"Hello world ."

    p.kill_proc_if_running()
    assert_no_java()

def assert_no_java(msg=""):
    ps_output = os.popen("ps wux").readlines()
    javalines = [x for x in ps_output if re.search(r'\bbin/java\b', x)]
    print ''.join(javalines)
    assert len(javalines)==0, msg

def test_doctimeout():
    assert_no_java("no java when starting")

    p = PipeWrap("pos")
    ret = p.parse_doc(open("allbrown.txt").read(), 0.5)
    assert ret is None
    p.kill_proc_if_running()
    assert_no_java()

def test_crash():
    assert_no_java("no java when starting")
    p = PipeWrap("ssplit")
    p.crash()
    ret = p.parse_doc("Hello world.")
    assert len(ret['sentences'])==1

    p.kill_proc_if_running()
    assert_no_java()
