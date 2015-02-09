This is a Python wrapper for the [Stanford CoreNLP][1] library, allowing
sentence splitting, POS/NER, and parse annotations.  (Coreference is not
supported currently.)  It runs the Java software as a subprocess and
communicates over sockets.  This library handles timeouts and some process
management (restarting the server if it seems to have crashed).

Alternatives you may want to consider:

  * https://bitbucket.org/torotoki/corenlp-python
  * https://github.com/dasmith/stanford-corenlp-python
  * http://py4j.sourceforge.net/

This wrapper's defaults assume CoreNLP 3.5.  It uses whatever the CoreNLP
default settings are, but they can be overriden with configuration (from a file or a dictionary).
The included `sample.ini` configuration file, for example, runs with the
[shift-reduce][2] parser (and requires the appropriate model file to be
downloaded).

[1]: http://nlp.stanford.edu/software/corenlp.shtml
[2]: http://nlp.stanford.edu/software/srparser.shtml

## Install

You can install the program using something like:

```
git clone https://github.com/brendano/stanford_corenlp_pywrapper
cd stanford_corenlp_pywrapper
pip install .
```

Java version 7 or higher is required to be installed.
(CoreNLP 3.5 might requires Jave 8 actually?)

## Commandline usage

See `proc_text_files.py` for an example of processing text files,
or `proc_doc_lines.py` for an alternative input/output format.
For more sophisticated usage, you'll want to write your own Python code
as described below.

## Usage from Python

The basic arguments to open a server are (1) the pipeline type (see
`javasrc/corenlp/Parse.java` for the list of possible ones), and (2) the
full paths to the CoreNLP jar files. Here we assume the program has been
installed using `pip install`.

```
>>> from stanford_corenlp_pywrapper import sockwrap
>>> p=sockwrap.SockWrap("pos",corenlp_jars=["stanford-corenlp-full-2015-01-30/stanford-corenlp-3.5.1.jar","stanford-corenlp-full-2015-01-30/stanford-corenlp-3.5.1-models.jar"])

INFO:StanfordSocketWrap:Starting pipe subprocess, and waiting for signal it's ready, with command:  exec java -Xmx4g -cp /Users/brendano/sw/nlp/stanford-pywrapper/lib/piperunner.jar:/Users/brendano/sw/nlp/stanford-pywrapper/lib/guava-13.0.1.jar:/Users/brendano/sw/nlp/stanford-pywrapper/lib/jackson-all-1.9.11.jar:stanford-corenlp-full-2014-06-16/stanford-corenlp-3.4.jar:stanford-corenlp-full-2014-06-16/stanford-corenlp-3.4-models.jar:stanford-corenlp-full-2014-06-16/stanford-srparser-2014-07-01-models.jar     corenlp.PipeCommandRunner --server 12340  --mode pos
[Server] Using mode type: pos
Adding annotator tokenize
Adding annotator ssplit
Adding annotator pos
Reading POS tagger model from edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger ... INFO:StanfordSocketWrap:Waiting for startup: ping got exception: <class 'socket.error'> [Errno 61] Connection refused
done [1.6 sec].
Adding annotator lemma
[Server] Started socket server on port 12340
INFO:StanfordSocketWrap:Successful ping. The server has started.
INFO:StanfordSocketWrap:Subprocess is ready.
```

The return values are JSON-safe data structures (in fact, the python<->java
communication is a JSON protocol, and you can pass `raw=True` to get it
without deserializing it).

```
>>> p.parse_doc("hello world. how are you?")
{u'sentences': 
    [
        {u'tokens': [u'hello', u'world', u'.'],
         u'lemmas': [u'hello', u'world', u'.'],
         u'pos': [u'UH', u'NN', u'.'],
         u'char_offsets': [[0, 5], [6, 11], [11, 12]]
        },
        {u'tokens': [u'how', u'are', u'you', u'?'],
         u'lemmas': [u'how', u'be', u'you', u'?'],
         u'pos': [u'WRB', u'VBP', u'PRP', u'.'],
         u'char_offsets': [[13, 16], [17, 20], [21, 24], [24, 25]]
        }
    ]
}
```

Here is how to specify a configuration file:

```
>>> p=sockwrap.SockWrap("parse", configfile='sample.ini')
>>> p.parse_doc("hello world. how are you?")
...
```

You can also give a configuration with a dictionary.  Furthermore, you can
explicitly give it the output types, and not even use a mode at all. For
example, say we want to parse but don't want lemmas or dependencies. This can
be done with:

```
>>> p=sockwrap.SockWrap(configdict={'annotators':'tokenize, ssplit, pos, parse'}, output_types=['pos','parse'])
```

The `annotators` configuration setting is internal to CoreNLP; see their
documentation how that works.  But `output_types` is specific to the wrapper
code.  Parse.java defines what they are.  You're responsible for choosing
`output_types` that are compatible with the annotators.  It's somewhat
complicated because it's a weird many-to-many relationship within CoreNLP, so
you have to read their documentation which sometimes may have the answer.


## Notes

* Some of the output messages are stderr from the CoreNLP subprocess.
  Everything starting with `INFO:` or `WARNING:` is from the Python logging
  system, in the parent process.  Messages starting with `[Server]` are from the
  Java subprocess, in our server code (but not from Stanford CoreNLP).

* To use a different CoreNLP version, make sure the `corenlp_jars` 
    parameter is correct. If a future CoreNLP breaks binary (Java API)
    compatibility, you'll have to edit the Java server code and re-compile
    `piperunner.jar` via `./build.sh`.

* If you want to run multiple instances on the same machine, make sure each
  SockWrap instance has a unique port number.  (TOCONSIDER: use a different
  mechanism that doesn't require port numbers.)

* An important to-do is to test this code's robustness in a variety of
  situations.  Bugs will probably occur when processing larger and larger
  datasets, and I don't know the right policies to have for timeouts, when to
  give up and restart after a timeout, and whether to re-try analyzing a
  document or give up and move on (because state dependence and "killer
  documents" screw all this up in different ways).  Thanks to John Beieler for
  testing on the PETRARCH news analysis pipeline.

## Testing

There are some pytest-style tests, though they're incomplete. Run:

    py.test -v sockwrap.py

## License etc.

Copyright Brendan O'Connor (http://brenocon.com).  
License GPL version 2 or later.

Some Java files were copied from [github.com/brendano/myutil](github.com/brendano/myutil).
