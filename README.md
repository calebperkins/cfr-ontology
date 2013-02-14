Cornell-LII
===========

NLP preprocessing and RDF triplet generation for the Cornell LII. This is organized as a Hadoop job.

Usage
-----

1. Export a runnable JAR file (instructions and Ant script coming soon)
2. Upload the JAR and input files to Elastic MapReduce, or run locally like so:

```bash
export HADOOP_OPTS="-XX:+UseParallelGC -mx8g"
hadoop fs -copyFromLocal /path/to/stanford-corenlp-1.3.4-models.jar /tmp/cfr/preprocessor/models.jar  
hadoop fs -copyFromLocal /path/to/agencies.txt /tmp/cfr/preprocessor/agencies.txt  
hadoop jar preprocessor.jar /path/to/input/files /path/to/output/files -agencies /tmp/cfr/preprocessor/agencies.txt -resolvePronouns
```
