This is a Python wrapper for the [Stanford CoreNLP][c] library for Unix (Mac,
Linux), allowing sentence splitting, POS/NER, temporal expression, constituent
and dependency parsing, and coreference annotations. It runs the Java software
as a subprocess and communicates with named pipes or sockets.  It can also be
used to get a JSON-formatted version of the NLP annotations.

Alternatives you may want to consider:

  * https://bitbucket.org/torotoki/corenlp-python
  * Many others listed [here][c]

[c]: http://nlp.stanford.edu/software/corenlp.shtml

## Install

You need to have CoreNLP already downloaded.
If you want to, you can install this software with something like:

```
git clone https://github.com/brendano/stanford_corenlp_pywrapper
cd stanford_corenlp_pywrapper
pip install .
```

Or you can just put the `stanford_corenlp_pywrapper` subdirectory into your
project (or use virtualenv, etc.).  For example:

```
git clone https://github.com/brendano/stanford_corenlp_pywrapper scp_repo
ln -s scp_repo/stanford_corenlp_pywrapper .
```

Java needs to be a version that CoreNLP is happy with; perhaps version 8.

## Commandline usage

See `proc_text_files.py` for an example of processing text files.
Note that you'll have to edit it to specify the jar paths as described below.

## Usage from Python

The basic arguments to open a server are 
    (1) the pipeline mode (or alternatively, the annotator pipeline), and
    (2) the path to the CoreNLP jar files (passed on to the Java classpath)

The pipeline modes are just quick shortcuts for some pipeline configurations we
commonly use.  They are defined near the top of
`stanford_corenlp_pywrapper/sockwrap.py` and include

  * `ssplit`: tokenization and sentence splitting (included in all subsequent ones)
  * `pos`: POS (and lemmas)
  * `ner`: POS and NER (and lemmas)
  * `parse`: fairly basic parsing with POS, lemmas, trees, dependencies
  * `nerparse`: parsing with NER, POS, lemmas, depenencies.
  * `coref`: Coreference, including constituent parsing.

Here we assume the program has been installed using `pip install`.  You will
have to change `corenlp_jars` to where you have them on your system.
Here's how to initialize the pipeline with the `pos` mode:

```
>>> from stanford_corenlp_pywrapper import CoreNLP
>>> proc = CoreNLP("pos", corenlp_jars=["/home/sw/corenlp/stanford-corenlp-full-2015-04-20/*"])
```

If things are working there will be lots of messages looking something like:

```
INFO:CoreNLP_PyWrapper:mode given as 'pos' so setting annotators: tokenize, ssplit, pos, lemma
INFO:CoreNLP_PyWrapper:Starting java subprocess, and waiting for signal it's ready, with command: exec java -Xmx4g -XX:ParallelGCThreads=1 -cp '/Users/brendano/sw/nlp/stanford_corenlp_pywrapper/stanford_corenlp_pywrapper/lib/*:/home/sw/corenlp/stanford-corenlp-full-2015-04-20/*:/home/sw/stanford-srparser-2014-10-23-models.jar'      corenlp.SocketServer --outpipe /tmp/corenlp_pywrap_pipe_pypid=140_time=1435943221.14  --configdict '{"annotators":"tokenize, ssplit, pos, lemma"}'
Adding annotator tokenize
TokenizerAnnotator: No tokenizer type provided. Defaulting to PTBTokenizer.
Adding annotator ssplit
Adding annotator pos
Reading POS tagger model from edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger ... done [1.7 sec].
Adding annotator lemma
INFO:CoreNLP_JavaServer: CoreNLP pipeline initialized.
INFO:CoreNLP_JavaServer: Waiting for commands on stdin
INFO:CoreNLP_PyWrapper:Successful ping. The server has started.
INFO:CoreNLP_PyWrapper:Subprocess is ready.
```

Now it's ready to parse documents.  You give it a string and it returns
JSON-safe data structures: 

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
say we want to parse but don't want lemmas. This can be done
with the `configdict` option:

```
>>> p = CoreNLP(configdict={'annotators':'tokenize, ssplit, pos, parse'}, output_types=['pos','parse'])
```

Or use an external configuration file (of the same sort the original CoreNLP commandline uses):

```
>>> p = CoreNLP(configfile='sample.ini')
>>> p.parse_doc("hello world. how are you?")
...
```

The `annotators` configuration option is explained more on the CoreNLP webpage.

Another example: using the [shift-reduce][sr] constituent parser.  The jar
paths will have to be changed for your system.

```
>>> p = CoreNLP(configdict={
    'annotators': "tokenize,ssplit,pos,lemma,parse",
    'parse.model': 'edu/stanford/nlp/models/srparser/englishSR.ser.gz'},  
    corenlp_jars=["/path/to/stanford-corenlp-full-2015-04-20/*", "/path/to/stanford-srparser-2014-10-23-models.jar"])
```

[sr]: http://nlp.stanford.edu/software/srparser.shtml

Another example: coreference. This tool does not annotate
[coreference](http://nlp.stanford.edu/projects/coref.shtml "coreference") in
the same way that it annotates other linguistic features. Where the other kinds
of annotation (for instance, part of speech tagging) are collected in the
top-level `sentences` attribute of the json output, coreference annotations get
collected in the attribute, `entities`.  

In the example below, there are 3 entities in the two sentences. The first is
"Fred", the second is "her", and the third is the telescope. The telescope is
mentioned twice, ("telescope" and "It"), so there are two mention objects in
the json. "It" and "telescope" are said to co-refer.

```
>>> proc = CoreNLP("coref")
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

* You can get the raw unserialized JSON with the option `raw=True`: e.g.,
    `parse_doc("Hello world.", raw=True)`.  The python<->java communication is
    based on JSON and this just hands it back without deserializing it.  In
    fact you can run the Java code as a standalone commandline program to just
    produce the JSON format. This can be helpful for storing parses from large
    corpora. (Even though this format is pretty repetitive, it is much more
    compact than CoreNLP's XML format. Though of course protobuf or something
    should be better.)

* To use a different CoreNLP version, just update `corenlp_jars` 
    to what you want. If a future CoreNLP breaks binary (Java API)
    compatibility, you'll have to edit the Java server code and re-compile with
    `./build.sh`.

* To change the Java settings, see the `java_command` and `java_options`
    arguments.

* Output messages (on standard error) that start with `INFO:CoreNLP_PyWrapper`,
    `INFO:CoreNLP_JavaServer`, or `INFO:CoreNLP_RWrapper` are from our code.
    Other output is probably from CoreNLP.

* Only works on Unix (Linux and Mac).  Does not currently work on Windows.

* If you want to know the latest version of CoreNLP this has been tested with,
    look at the paths in the default options in the Python source code.

* `SOCKET` mode: By default, the inter-process communication is
    through named pipes, established with Unix calls. As an alternative, there is
    also a socket server mode (`comm_mode='SOCKET'`) which is sometimes more robust,
    but requires using a port number, which you have to ensure does not
    conflict with any other processes running at the same time.  (It's not much
    of a server since the python code assumes it's the only process
    communicating with it.)  One advantage of `SOCKET' mode is that it has
    a timeout, in case CoreNLP is taking a very long time to return an answer.

* Question: do [JPype](http://jpype.sourceforge.net/) or
    [Py4J](http://py4j.sourceforge.net/) work well?  They seemed complex which
    is why we wrote our own IPC mechanism.  But if there's a better
    alternative, no need.

## Testing

There's a tiny amount of pytest-style tests.

    py.test -v sockwrap.py

## Changelog

Major changes include

  * 2015-07-03: add pipe mode and make it default (the *namedpipe* branch), plus an R wrapper.
  * 2015-05-15: no longer need to specify `output_types` (the outputs to include are inferred from the `annotators` setting).

For details see the commit log.

## License etc.

Copyright Brendan O'Connor (http://brenocon.com).  
License GPL version 2 or later.
