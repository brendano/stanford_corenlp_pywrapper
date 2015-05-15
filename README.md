This is a Python wrapper for the [Stanford CoreNLP][1] library, allowing
sentence splitting, POS/NER, temporal expression, constituent and dependency
parsing, and coreference annotations. It runs the Java software as a subprocess
and communicates over sockets.  This library handles timeouts and some process
management (restarting the server if it seems to have crashed).

Alternatives you may want to consider:

  * https://bitbucket.org/torotoki/corenlp-python
  * https://github.com/dasmith/stanford-corenlp-python
  * http://py4j.sourceforge.net/

This wrapper uses whatever the CoreNLP default settings are, but they can be
overriden with configuration (from a file or a dictionary).
The included `sample.ini` configuration file, for example, runs with the
[shift-reduce][2] parser (and requires the appropriate model file to be
downloaded).

[1]: http://nlp.stanford.edu/software/corenlp.shtml
[2]: http://nlp.stanford.edu/software/srparser.shtml

## Install

If you like, you can install it with something like this:

```
git clone https://github.com/brendano/stanford_corenlp_pywrapper
cd stanford_corenlp_pywrapper
pip install .
```

Java version 8 is required to be installed (version 7 may work with older
CoreNLP versions).  We have used the wrapper most recently with CoreNLP 3.5.2,
and have used it with older versions as well.

## Commandline usage

See `proc_text_files.py` for an example of processing text files,
or `proc_doc_lines.py` for an alternative input/output format.
Note that you'll have to edit them to specify the jar paths as described below.

## Usage from Python

The basic arguments to open a server are 
    (1) the pipeline mode, or the annotator pipeline, and
    (2) the paths to the CoreNLP jar files, for the java classpath.
    
Here we assume the program has been installed using `pip install`.  You will
have to change `corenlp_jars` to where you have them on your system.
Here's how to initialize the pipeline.

```
>>> from stanford_corenlp_pywrapper import sockwrap
>>> proc = sockwrap.SockWrap("pos", corenlp_jars=["/home/sw/corenlp/stanford-corenlp-full-2015-04-20/*"])

INFO:StanfordSocketWrap:mode given as 'pos' so setting annotators: tokenize, ssplit, pos, lemma
INFO:StanfordSocketWrap:Starting pipe subprocess, and waiting for signal it's ready, with command:  exec java -Xmx4g -cp '/Users/brendano/sw/nlp/stanford_corenlp_pywrapper/stanford_corenlp_pywrapper/lib/*:/home/sw/corenlp/stanford-corenlp-full-2015-04-20/*'     corenlp.SocketServer --server 12340  --configdict '{"annotators":"tokenize, ssplit, pos, lemma"}'
Adding annotator tokenize
TokenizerAnnotator: No tokenizer type provided. Defaulting to PTBTokenizer.
Adding annotator ssplit
Adding annotator pos
Reading POS tagger model from edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger ... done [1.2 sec].
Adding annotator lemma
[Server] Started socket server on port 12340
INFO:StanfordSocketWrap:Successful ping. The server has started.
INFO:StanfordSocketWrap:Subprocess is ready.
```

Now it's ready to parse documents.  You give it a string and it returns
JSON-safe data structures (in fact, the python<->java communication is a JSON
protocol, and you can pass `raw=True` to get it without deserializing it).

```
>>> proc.parse_doc("hello world. how are you?")
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

You can also specify the annotators directly. For example,
say we want to parse but don't want lemmas or dependencies. This can be done
with the `configdict` option:

```
>>> p = sockwrap.SockWrap(configdict={'annotators':'tokenize, ssplit, pos, parse'}, output_types=['pos','parse'])
```

Or use an external configuration file (of the same sort the original CoreNLP commandline uses):

```
>>> p = sockwrap.SockWrap(configfile='sample.ini')
>>> p.parse_doc("hello world. how are you?")
...
```

The `annotators` configuration option is explained more on the CoreNLP webpage.

Another example: using the shift-reduce constituent parser.  The jar paths will
have to be changed for your system.

```
>>> p = sockwrap.SockWrap(configdict={
    'annotators':"tokenize,ssplit,pos,lemma,parse",
    'parse.model': 'edu/stanford/nlp/models/srparser/englishSR.ser.gz'},  
    corenlp_jars=["/path/to/stanford-corenlp-full-2015-04-20/*", "/path/to/stanford-srparser-2014-10-23-models.jar"])
```

Another example: coreference.  All the other annotators only put things into the top-level `sentences` attribute for the document.  But for coreference, you'll get data in `entities`.  Here, there are 3 entities. The first is "Fred", the second is "her", and the third is the telescope with two mentions ("telescope" and "It").

```
>>> proc = sockwrap.SockWrap("coref")
>>> proc.parse_doc("Fred saw her through a telescope. It was broken.")['entities']
[{u'entityid': 1,
  u'mentions': [{u'animacy': u'ANIMATE',
                 u'gender': u'MALE',
                 u'head': 0,
                 u'mentionid': 1,
                 u'mentiontype': u'PROPER',
                 u'number': u'SINGULAR',
                 u'representative': True,
                 u'sentence': 0,
                 u'tokspan_in_sentence': [0, 1]}]},
 {u'entityid': 2,
  u'mentions': [{u'animacy': u'ANIMATE',
                 u'gender': u'FEMALE',
                 u'head': 2,
                 u'mentionid': 2,
                 u'mentiontype': u'PRONOMINAL',
                 u'number': u'SINGULAR',
                 u'representative': True,
                 u'sentence': 0,
                 u'tokspan_in_sentence': [2, 3]}]},
 {u'entityid': 3,
  u'mentions': [{u'animacy': u'INANIMATE',
                 u'gender': u'NEUTRAL',
                 u'head': 5,
                 u'mentionid': 3,
                 u'mentiontype': u'NOMINAL',
                 u'number': u'SINGULAR',
                 u'representative': True,
                 u'sentence': 0,
                 u'tokspan_in_sentence': [4, 6]},
                {u'animacy': u'INANIMATE',
                 u'gender': u'NEUTRAL',
                 u'head': 0,
                 u'mentionid': 4,
                 u'mentiontype': u'PRONOMINAL',
                 u'number': u'SINGULAR',
                 u'sentence': 1,
                 u'tokspan_in_sentence': [0, 1]}]}]
```


## Notes

* We always use 0-indexed numbering conventions for token, sentence, and
  character indexes.  Spans are always inclusive-exclusive pairs, just like
  Python slicing.

* Some of the output messages are stderr from the CoreNLP subprocess.
  Everything starting with `INFO:` or `WARNING:` is from the Python logging
  system, in the parent process.  Messages starting with `[Server]` are from the
  Java subprocess, in our server code (but not from Stanford CoreNLP).

* To use a different CoreNLP version, make sure the `corenlp_jars` 
    parameter is correct. If a future CoreNLP breaks binary (Java API)
    compatibility, you'll have to edit the Java server code and re-compile
    with `./build.sh`.

* If you want to run multiple instances on the same machine, make sure each
  SockWrap instance has a unique port number.  (TOCONSIDER: use a different
  mechanism that doesn't require port numbers.)

## Testing

There are some pytest-style tests, though they're incomplete. Run:

    py.test -v sockwrap.py

## License etc.

Copyright Brendan O'Connor (http://brenocon.com).  
License GPL version 2 or later.
