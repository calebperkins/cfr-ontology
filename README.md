Vocabulary Extraction
=====================

This tool extracts RDF triples from the Code of Federal Regulations. It was developed for the [Cornell Legal Information Institute](http://www.law.cornell.edu).

Compiling
---------


Running
-------

Hadoop usage
------------

This program can also be ran as a Hadoop job. Note that you will almost definitely want to use the OpenNLP parser; the CoreNLP parser requires 3 GB on each node and you will probably run out of memory.

1. Export a runnable JAR file (instructions and Ant script coming soon)
2. Upload the JAR and input files to Elastic MapReduce, or run locally like so:

```bash
export HADOOP_OPTS="-XX:+UseParallelGC -mx8g"
hadoop fs -copyFromLocal /path/to/stanford-corenlp-1.3.4-models.jar /tmp/cfr/preprocessor/models.jar
hadoop fs -copyFromLocal /path/to/agencies.txt /tmp/cfr/preprocessor/agencies.txt
hadoop jar preprocessor.jar /path/to/input/files /path/to/output/files -agencies /tmp/cfr/preprocessor/agencies.txt -resolvePronouns
```
