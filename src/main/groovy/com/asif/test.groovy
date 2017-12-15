package com.asif


def xmlFile ="/Users/asifmohammed/IdeaProjects/rukuproj/src/main/resources/quran-data.xml"
def xml = new XmlParser().parse(xmlFile)
xml.foo[0].each {
    it.@id = "test2"
    it.value = "test2"
}