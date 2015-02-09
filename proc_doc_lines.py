"""
Input on stdin.  Every line is one document.
Output on stdout.  Every line is one JSON object.

USAGE
proc_doc_lines.py MODE < inputfile > outputfile

e.g.
echo -e "this is one doc.\nHere is another." | python proc_doc_lines.py pos

a bunch of crap will appear on stderr, but the only two stdout lines will be something like:

{"sentences":[{"pos":["DT","VBZ","CD","NN","."],"lemmas":["this","be","one","doc","."],"tokens":["this","is","one","doc","."],"char_offsets":[[0,4],[5,7],[8,11],[12,15],[15,16]]}]}
{"sentences":[{"pos":["RB","VBZ","DT","."],"lemmas":["here","be","another","."],"tokens":["Here","is","another","."],"char_offsets":[[0,4],[5,7],[8,15],[15,16]]}]}
"""

import sys
mode = sys.argv[1]

import stanford_corenlp_pywrapper.sockwrap as sw
ss = sw.SockWrap(mode)

for line in sys.stdin:
    text = line.rstrip("\n").decode('utf8','replace')
    jdoc = ss.parse_doc(text, raw=True)
    assert "\n" not in jdoc
    print jdoc
