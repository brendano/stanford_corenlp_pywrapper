"""
Input is multiple text files.  Each text file represents one document.
Output is stdout, a stream of 2-column TSV
  DocID  \t  JsonAnnotations
where the DocID is based on the filename.

USAGE
proc_text_files.py MODE  [files...]

e.g.
python proc_text_files_to_stdout.py pos *.txt > allpos.anno
"""

import sys, re, os, json
mode = sys.argv[1]

import stanford_corenlp_pywrapper.sockwrap as sw
ss = sw.SockWrap(mode)

for filename in sys.argv[2:]:
    docid = os.path.basename(filename)
    docid = re.sub(r'\.txt$', "", docid)

    text = open(filename).read().decode('utf8', 'replace')
    jdoc = ss.parse_doc(text, raw=True)
    print "%s\t%s" % (docid, jdoc)
