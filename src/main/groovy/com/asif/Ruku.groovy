package com.asif

class RukuMaker {

    ClassLoader classLoader = getClass().getClassLoader()
    File xmlFile = new File(classLoader.getResource("quran-data.xml").getFile())
    def rukus = new XmlParser().parse(xmlFile).rukus[0].value()
    def suras = new XmlParser().parse(xmlFile).suras[0].value()

    def rukusBySuraMap = rukus.groupBy { it -> it.attributes().get('sura') }

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

    void makeRukusShellScript(int beginRukuNumber, int endRukuNumber) {
        println "Creating shell script from ruku#$beginRukuNumber to ruku#$endRukuNumber"
        def outputDir = new File("${System.getProperty("user.dir")}/shellFiles/")
        outputDir.deleteDir()
        outputDir.mkdirs()
        String shellText = createShellScript(beginRukuNumber, endRukuNumber)
        new File(outputDir.getAbsolutePath() + "/rukus.sh").write(shellText)
    }

    void makeRukusShellScript(def surahNumber) {
        println "Creating shell script for surah#$surahNumber ${suraNameMap.get(surahNumber)}"
        def rukus = rukusBySuraMap.get(surahNumber as String)
        def rukuNos = rukus.collect { it.attributes().get('index') as Integer }
        makeRukusShellScript(rukuNos.min(), rukuNos.max())
    }

    private String createShellScript(int beginRukuNumber, int endRukuNumber) {
        def shellTextString = "#!/bin/bash\n\nmkdir -p \$2\n\nstart=`date +%s` \n\n"
        def end = endRukuNumber >= 556 ? 556 : endRukuNumber
        if (beginRukuNumber < endRukuNumber) {
            for (int rukuNumber = beginRukuNumber - 1; rukuNumber < end; rukuNumber++) {
                def (fullLinks, isFullSurahInRuku) = rukuAyahsPlusOne(rukuNumber)
                if (fullLinks.size() > 0) {
                    shellTextString = shellTextString + shellText(rukuNumber, fullLinks, isFullSurahInRuku) + "\necho Ruku#${rukuNumber + 1} complete\n\n"
                }
            }
        }
        shellTextString + "\nend=`date +%s`\nruntime=\${end-start}/60\necho \${runtime} seconds"
    }

    private def rukuAyahsPlusOne(int rukuNumber) {
        boolean isFullSurahInRuku = false
        List<String> ayaFiles = this.ayaFiles(rukuNumber)
        def surahNumber = ayaFiles[0].substring(0, 3) as int
        if (rukuNumber < 555) {
            def firstAyahNextRuku = this.ayaFiles(rukuNumber + 1).first()
            def surahNumberNextRuku = firstAyahNextRuku.substring(0, 3) as int
            if (surahNumber == surahNumberNextRuku)
                ayaFiles << firstAyahNextRuku
            else
                isFullSurahInRuku = true
        }
        def fullLinks = ayaFiles.collect { it }

        [fullLinks, isFullSurahInRuku]
    }

    private String shellText(int rukuNumber, List<String> fullLinks, boolean isFullSurahInRuku) {
        def shellText = ''

        fullLinks.each {
            shellText = shellText + "curl -Os \$1/${it.split("/").last()}\n"
        }
        shellText = shellText + "\ncat "
        def firstAyah = fullLinks.first().split("/").last().split("\\.")[0].reverse().take(3).reverse()
        def surah = fullLinks.first().split("/").last().split("\\.")[0].take(3)
        def lastAyahLink = fullLinks.size() > 1 ? (isFullSurahInRuku ? fullLinks.last() : fullLinks.take(fullLinks.size() - 1).last()) : fullLinks.first()
        def lastAyah = lastAyahLink.split("/").last().split("\\.")[0].reverse().take(3).reverse()
        fullLinks.each {
            shellText = shellText + it.split("/").last() + " "
        }
        shellText = shellText + "> \$2/ruku#${threeDigitNumber(rukuNumber + 1)}_${surah}_${firstAyah}-${lastAyah}.mp3\n\n"

        fullLinks.each {
            shellText = shellText + "rm -rf  ${it.split("/").last()}\n"
        }

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
}

//new RukuMaker().makeRukusShellScript(1, 3)
new RukuMaker().makeRukusShellScript(59)
