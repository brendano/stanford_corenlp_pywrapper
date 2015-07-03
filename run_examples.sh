#!/bin/bash

set -x
rm examples/*.anno examples/*.xml

set -euo pipefail

python proc_text_files_to_stdout.py pos examples/*.txt

python proc_text_files.py pos examples/*.txt

ls -l examples/*.anno
