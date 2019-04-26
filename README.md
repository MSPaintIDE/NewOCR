<div>
    <a href="https://search.maven.org/artifact/com.uddernetworks.newocr/NewOCR/">
    	<img alt="Maven Central" src="https://maven-badges.herokuapp.com/maven-central/com.uddernetworks.newocr/NewOCR/badge.svg">
    </a>
    <a href="https://discord.gg/RXmPkPJ">
            <img src="https://img.shields.io/discord/528423806453415972.svg?logo=discord"
                alt="NewOCR and MS Paint IDE's Discord server">
    </a>
    <a href="https://travis-ci.org/MSPaintIDE/NewOCR/">
        <img alt="Travis (.org) branch" src="https://img.shields.io/travis/MSPaintIDE/NewOCR/dev.svg">
    </a>
</div>

# NewOCR
NewOCR is an OCR library made to suit [MS Paint IDE](https://github.com/MSPaintIDE/MSPaintIDE)'s needs, though can be used in any project, as nothing is made specific to the IDE. The OCR can be trained with many fonts, though is geared towards fonts like **Verdana** and similar fonts. Other fonts _may_ require some tweaking of the character detector, but the main detection will work with no matter how different the characters are from Verdana (It could even modify it to work with emojis).

Currently, NewOCR is being tested against the following fonts:

- Comic Sans MS
- Monospaced
- Verdana
- Calibri
- Consolas
- Courier New

Though the OCR can be trained on many, many other fonts. For more information on fonts used and how they are chosen, see the [fonts page of the wiki](https://wiki.newocr.dev/fonts).

Instructions to get started with using NewOCR and a detailed description of how every piece of the OCR works from start to finish, is available on the wiki: [https://wiki.newocr.dev/](https://wiki.newocr.dev/)

The javadocs for the project are available here: [https://docs.newocr.dev/](https://docs.newocr.dev/)