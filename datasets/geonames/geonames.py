u"""
This Python script loads city and country datasets from GeoNames and parses the
entity name and GeoID. The purpose is to preprocess these files so we can
build a Java mapping from name to GeoID easily.
"""

import csv
import json
import sys


sys.stdout = open("geoids.txt", "w")

with open("cities15000.txt", "r") as csv_file:
    reader = csv.reader(csv_file, delimiter='\t')
    for row in reader:
        geoid = row[0]
        ascii_name = row[2]
        print "%s %s" % (geoid, ascii_name)


with open("countryInfo.json") as json_file:
    raw_content = json_file.readlines()[0]
    data = json.loads(raw_content)
    for country in data[u"geonames"]:
        s = "%s %s" % (country[u"geonameId"], country[u"countryName"])
        print s.encode('utf-8')
