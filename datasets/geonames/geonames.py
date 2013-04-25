u"""
This Python script loads city and country datasets from GeoNames and parses the
entity name and GeoID. The purpose is to preprocess these files so we can
build a Java mapping from name to GeoID easily.
"""

import csv
import json
import sys


sys.stdout = open("geoids.txt", "w")

def toURI(geoID):
    return u"http://sws.geonames.org/%s/" % geoID

with open("cities15000.txt", "r") as csv_file:
    reader = csv.reader(csv_file, delimiter='\t')
    for row in reader:
        geoid = row[0]
        ascii_name = row[2]
        if not ascii_name:
            continue
        s = "%s|%s" % (toURI(geoid), ascii_name)
        print s.encode('utf-8')


with open("countryInfo.json") as json_file:
    raw_content = json_file.readlines()[0]
    data = json.loads(raw_content)
    for country in data[u"geonames"]:
        s = "%s|%s" % (toURI(country[u"geonameId"]), country[u"countryName"])
        print s.encode('utf-8')
