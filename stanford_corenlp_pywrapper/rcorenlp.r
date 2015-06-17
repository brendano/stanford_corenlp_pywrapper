# R wrapper for the Java JSON pipe server for CoreNLP
library(rjson)
library(stringr)

# paste from python sockwrap.py modes_json
MODES = rjson::fromJSON(
"{\"ssplit\":{\"annotators\":\"tokenize, ssplit\",\"description\":\"tokenization and sentence splitting (included in all subsequent ones)\"},\"coref\":{\"annotators\":\"tokenize, ssplit, pos, lemma, ner, entitymentions, parse, dcoref\",\"description\":\"Coreference, including constituent parsing.\"},\"pos\":{\"annotators\":\"tokenize, ssplit, pos, lemma\",\"description\":\"POS (and lemmas)\"},\"parse\":{\"annotators\":\"tokenize, ssplit, pos, lemma, parse\",\"description\":\"fairly basic parsing with POS, lemmas, trees, dependencies\"},\"nerparse\":{\"annotators\":\"tokenize, ssplit, pos, lemma, ner, entitymentions, parse\",\"description\":\"parsing with NER, POS, lemmas, depenencies.\"},\"ner\":{\"annotators\":\"tokenize, ssplit, pos, lemma, ner, entitymentions\",\"description\":\"POS and NER (and lemmas)\"}}"
)

CoreNLP = function(
            mode=NULL,
            configdict=list(annotators="tokenize, ssplit"),
            corenlp_jars=c(
                "/home/sw/corenlp/stanford-corenlp-full-2015-04-20/*",
                "/home/sw/stanford-srparser-2014-10-23-models.jar"),
            java_command="java",
            java_options="-Xmx4g -XX:ParallelGCThreads=1",
            outpipe_filename_prefix="/tmp/corenlp_rwrap_pipe",
            ...
) {

    # If a mode is specified, set the annotators on the configdict.
    if (!is.null(mode)) {
        stopifnot(mode %in% names(MODES))
        configdict[['annotators']] = MODES[[mode]][['annotators']]
    }

    # Extra arguments are put into the configdict.

    moreargs = list(...)
    for (k in names(moreargs)) {
        configdict[[k]] = moreargs[[k]]
    }

    corenlp = list()
    corenlp$outpipe_filename = sprintf("%s_rpid=%s_time=%s", outpipe_filename_prefix, Sys.getpid(), as.numeric(Sys.time()))

    cmd = "exec JAVA_COMMAND JAVA_OPTIONS -cp 'CLASSPATH' \
            corenlp.SocketServer COMM_INFO MORE_CONFIG"
    cmd = str_replace(cmd, "JAVA_COMMAND", java_command)
    cmd = str_replace(cmd, "JAVA_OPTIONS", java_options)
    # How to specify location of resources in R? there's no __FILE__ equivalent
    # Packages are the only way?  Too bad.
    jars = c("lib/corenlpwrapper.jar", "lib/*")
    jars = c(jars, corenlp_jars)
    cmd = str_replace(cmd, "CLASSPATH", str_join(jars, collapse=":"))
    cmd = str_replace(cmd, "COMM_INFO", sprintf("--outpipe %s", corenlp$outpipe_filename))
    cmd = str_replace(cmd, "MORE_CONFIG", sprintf(" --configdict '%s'", rjson::toJSON(configdict)))

    cmd = str_replace_all(cmd, "\n", " ")
    logmessage(sprintf("Starting with command: %s\n", cmd))

    # - I'm not sure how R encodings work
    # - pipe() in write mode seems to block until the subprocess tries to read
    #   from stdin.  Perfect, so we don't need to check for that.
    corenlp$pipe = pipe(cmd, "wb", encoding="UTF-8")
    system(sprintf("mkfifo %s", corenlp$outpipe_filename))
    corenlp$outpipe = file(corenlp$outpipe_filename, "rb", encoding="UTF-8", raw=TRUE)

    class(corenlp) = "corenlp_wrapper"
    corenlp
}

logmessage = function(msg) cat(sprintf("INFO:CoreNLP_RWrapper:%s", msg), file=stderr())

readresult = function(outpipe) {
# TESTING
# readresult(file("return.bin","rb", raw=TRUE))
    size = readBin(outpipe, 'integer', n=1, endian='big', size=8)
    cat(sprintf("Returned size %s\n", size))
    stopifnot(size > 0)
    # does useBytes=TRUE circumvent the encoding declaration earlier?
    result = readChar(outpipe, size, useBytes=TRUE)
    result = rjson::fromJSON(result)
    result
}


parsedoc = function(corenlp, string) {
    command = sprintf("PARSEDOC\t%s", rjson::toJSON(string))
    writeLines(command, corenlp$pipe)
    flush(corenlp$pipe)
    readresult(corenlp$outpipe)
}

close.corenlp_wrapper = function(corenlp) {
    close(corenlp$outpipe)
    close(corenlp$pipe)
    system(sprintf("rm -f %s", corenlp$outpipe_filename))
}
