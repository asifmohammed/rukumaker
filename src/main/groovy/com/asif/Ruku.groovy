package com.asif

import groovy.xml.MarkupBuilder

import java.nio.file.Files
import java.nio.file.Paths


class RukuMaker {
    ClassLoader classLoader = getClass().getClassLoader()
    File xmlFile = new File(classLoader.getResource("quran-data.xml").getFile())
    def mp3Dir = "/Users/asifmohammed/Quran/Hifdh/MinshawyMujawwad/"
    def rukus = new XmlParser().parse(xmlFile).rukus[0].value()
    def suras = new XmlParser().parse(xmlFile).suras[0].value()

    def suraNumberOfAyasMap = suras.collectEntries {
        [(it.attributes().get("index") as int): it.attributes().get("ayas") as int]
    }

    def suraNameMap = suras.collectEntries {
        [(it.attributes().get("index") as int): it.attributes().get("name") + "_" + it.attributes().get("tname")]
    }

    private String startAyaNumber(ruku) {
        ruku.get("aya") as int
    }

    private String endAyaNumber(ruku) {
        def ayaNumber = (ruku.get("aya") as int)
        int suraNumber = suraNumber(ruku)
        if (ayaNumber != 1)
            ayaNumber - 1
        else
            suraNumberOfAyasMap.get(suraNumber - 1)
    }

    private int suraNumber(ruku) {
        ruku.get("sura") as int
    }

    private String threeDigitNumber(Integer number) {
        String.format("%03d", number)
    }

    void makeRukus() {
        println "(Surah) (Ruku) (#ofAyaat): AyahAudio"
        for (int rukuNumber = 0; rukuNumber < rukus.size(); rukuNumber++) {
            List<String> ayaFiles = this.ayaFiles(rukuNumber)
            def fullLinks = ayaFiles.collect { "http://everyayah.com/data/Husary_128kbps/" + it }
            def surahNumber = ayaFiles[0].substring(0, 3) as int
            println "(Surah ${suraNameMap.get(surahNumber)}) (${threeDigitNumber(rukuNumber + 1)}) (${threeDigitNumber(ayaFiles.size())}): $fullLinks"
            //moveFiles(rukuNumber, ayaFiles)
        }
    }

    void makeRukusHtml(String qariUrl) {
        for (int rukuNumber = 0; rukuNumber < rukus.size(); rukuNumber++) {
            def writer = new StringWriter()
            def builder = new MarkupBuilder(writer)
            builder.html {
                List<String> ayaFiles = this.ayaFiles(rukuNumber)
                def surahNumber = ayaFiles[0].substring(0, 3) as int
                if (rukuNumber < 555) {
                    def firstAyahNextRuku = this.ayaFiles(rukuNumber + 1).first()
                    def surahNumberNextRuku = firstAyahNextRuku.substring(0, 3) as int
                    if (surahNumber == surahNumberNextRuku)
                        ayaFiles << firstAyahNextRuku
                }
                def heading = "Surah ${suraNameMap.get(surahNumber)} Ruku# ${threeDigitNumber(rukuNumber + 1)} with # of ayas (${threeDigitNumber(ayaFiles.size())})"
                def fullLinks = ayaFiles.collect { qariUrl + it }
                head {
                    title "Ruku Files for : $heading"
                }
                body {
                    h1 "Ruku Files for : $heading"
                    p ""
                    p() {
                        ol {
                            for (String link : fullLinks) {
                                li {
                                    p() {
                                        label(link.split("/").last())
                                        br()
                                        audio(controls: "") {
                                            source(src: link, type: "audio/mp3")
                                        }
                                    }

                                }
                            }
                        }

                    }
                }

            }
            new File("ruku$rukuNumber" + ".html").write(writer.toString())
        }
    }

    void makeRukusShell(String qariUrl) {
        def outputDir = new File("${System.getProperty("user.dir")}/shellFiles/")
        outputDir.deleteDir()
        outputDir.mkdirs()
        for (int rukuNumber = 0; rukuNumber < rukus.size(); rukuNumber++) {
            List<String> fullLinks = fullAyahLinks(rukuNumber, qariUrl)
            if (fullLinks.size() > 0) {
                String shellText = shellText(rukuNumber, fullLinks)
                new File(outputDir.getAbsolutePath() + "/ruku${threeDigitNumber(rukuNumber + 1)}.sh").write(shellText)
            }
        }
    }

    void makeRukusShellBig(String qariUrl) {
        def outputDir = new File("${System.getProperty("user.dir")}/shellFiles/")
        outputDir.deleteDir()
        outputDir.mkdirs()
        def shellTextString = '#!/bin/bash\n\n'
        for (int rukuNumber = 0; rukuNumber < rukus.size(); rukuNumber++) {
            List<String> fullLinks = fullAyahLinks(rukuNumber, qariUrl)
            if (fullLinks.size() > 0) {
                 shellTextString = shellTextString + shellText(rukuNumber, fullLinks)
            }
        }
        new File(outputDir.getAbsolutePath() + "/rukus_qari${qariUrl.split("/").last()}.sh").write(shellTextString)

    }

    private List<String> fullAyahLinks(int rukuNumber, String qariUrl) {
        List<String> ayaFiles = this.ayaFiles(rukuNumber)
        def surahNumber = ayaFiles[0].substring(0, 3) as int
        if (rukuNumber < 555) {
            def firstAyahNextRuku = this.ayaFiles(rukuNumber + 1).first()
            def surahNumberNextRuku = firstAyahNextRuku.substring(0, 3) as int
            if (surahNumber == surahNumberNextRuku)
                ayaFiles << firstAyahNextRuku
        }
        def fullLinks = ayaFiles.collect { qariUrl + it }
        fullLinks
    }

    private String shellText(int rukuNumber, List<String> fullLinks) {
        def shellText = '#!/bin/bash\n\n'
        fullLinks.each {
            shellText = shellText + "curl -O $it\n"
        }
        shellText = shellText + "\ncat "
        def firstAyah = fullLinks.first().split("/").last().split("\\.")[0].reverse().take(3).reverse()
        def surah = fullLinks.first().split("/").last().split("\\.")[0].take(3)
        def lastAyahLink = fullLinks.size() > 1 ? fullLinks.take(fullLinks.size() - 1).last() : fullLinks.first()
        def lastAyah = lastAyahLink.split("/").last().split("\\.")[0].reverse().take(3).reverse()
        fullLinks.each {
            shellText = shellText + it.split("/").last() + " "
        }
        shellText = shellText + "> ruku#${threeDigitNumber(rukuNumber + 1)}_${surah}_${firstAyah}-${lastAyah}.mp3\n\n"

        fullLinks.each {
            shellText = shellText + "rm -rf  ${it.split("/").last()}\n"
        }

        println "shellText = $shellText"
        shellText
    }


    private List<String> ayaFiles(int rukuNumber) {
        def ruku = rukus[rukuNumber].attributes()
        def surahNumber = suraNumber(ruku)
        def startAyaNumber = this.startAyaNumber(ruku) as int
        def endAyaNumber = rukuNumber == 555 ? 6 : this.endAyaNumber(rukus[rukuNumber + 1].attributes()) as int

        def files = (startAyaNumber..endAyaNumber).collect() {
            threeDigitNumber(surahNumber) + threeDigitNumber(it) + ".mp3"
        }
        files
    }

    private void moveFiles(int rukuNumber, List<String> files) {
        def rukuString = "Ruku" + threeDigitNumber(rukuNumber + 1)
        def rukuDir = mp3Dir + rukuString + File.separator
        new File(rukuDir).mkdirs()
        for (int j = 0; j < files.size(); j++) {
            def fileName = files[j].split("\\.")[0]
            def surahNumber = fileName.substring(0, 3) as int
            def ayaNumber = fileName.substring(3) as int
            def sourceFile = Paths.get(mp3Dir + "raw" + File.separator + files[j])
            def targetFile = Paths.get(rukuDir + rukuString + "_" + suraNameMap.get(surahNumber) + "(" + threeDigitNumber(surahNumber) + ")-" + threeDigitNumber(ayaNumber) + ".mp3")
            println "$sourceFile -> $targetFile"
            Files.copy(sourceFile, targetFile)
        }
    }
}

new RukuMaker().makeRukusShellBig("http://everyayah.com/data/Husary_128kbps/")
