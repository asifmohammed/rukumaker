package com.asif

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
        for (int rukuNumber = 0; rukuNumber < rukus.size(); rukuNumber++) {
            List<String> ayaFiles = this.ayaFiles(rukuNumber)
            println "${threeDigitNumber(rukuNumber + 1)} (${threeDigitNumber(ayaFiles.size())}): $ayaFiles"
            //moveFiles(rukuNumber, ayaFiles)
        }
    }

    private List<String> ayaFiles(int rukuNumber) {
        def ruku = rukus[rukuNumber].attributes()
        def surahNumber = suraNumber(ruku)
        def startAyaNumber = this.startAyaNumber(ruku) as int
        def endAyaNumber = this.endAyaNumber(rukus[rukuNumber + 1].attributes()) as int

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

new RukuMaker().makeRukus()