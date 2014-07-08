EXPERIMENTAL DO NOT USE WITHOUT LOTS OF TESTING
java files copied from github.com/brendano/myutil
License GPL version 2 or later
see also
https://bitbucket.org/torotoki/corenlp-python
https://github.com/dasmith/stanford-corenlp-python


=== Usage ===

The return values are JSON-safe data structures (in fact, the python<->java
communication is a JSON protocol).
see javasrc/corenlp/Parse.java for the allowable pipeline types.
TODO allow full CoreNLP configuration

>>> import sockwrap
>>> p=sockwrap.SockWrap("pos")

INFO:StanfordSocketWrap:Starting pipe subprocess, and waiting for signal it's ready, with command:  exec java -Xmx4g -cp /Users/brendano/sw/nlp/stanford-pywrapper/lib/piperunner.jar:/Users/brendano/sw/nlp/stanford-pywrapper/lib/guava-13.0.1.jar:/Users/brendano/sw/nlp/stanford-pywrapper/lib/jackson-all-1.9.11.jar:/users/brendano/sw/nlp/stanford-corenlp-full-2014-01-04/stanford-corenlp-3.3.1.jar:/users/brendano/sw/nlp/stanford-corenlp-full-2014-01-04/stanford-corenlp-3.3.1-models.jar     corenlp.PipeCommandRunner --server 12340 pos /var/folders/_6/wn2lzlvn7glfw4zp24vbh72r0000gn/T/tmp.pipewrap.GgjwfK.ready
Adding annotator tokenize
Adding annotator ssplit
Adding annotator pos
Reading POS tagger model from edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger ... done [0.9 sec].
Adding annotator lemma
[Server] Started socket server on port 12340
INFO:StanfordSocketWrap:Subprocess is ready.

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

>>> p=sockwrap.SockWrap("justparse")
{u'sentences':
    [
        {u'tokens': [u'hello', u'world', u'.'],
         u'char_offsets': [[0, 5], [6, 11], [11, 12]],
         u'pos': [u'UH', u'NN', u'.'],
         u'parse': u'(ROOT (S (VP (NP (INTJ (UH hello)) (NP (NN world)))) (. .)))'
        },
        {u'tokens': [u'how', u'are', u'you', u'?'],
         u'char_offsets': [[13, 16], [17, 20], [21, 24], [24, 25]],
         u'pos': [u'WRB', u'VBP', u'PRP', u'.'],
         u'parse': u'(ROOT (SBARQ (WHADVP (WRB how)) (SQ (VBP are) (NP (PRP you))) (. ?)))'
        }
    ]
}

