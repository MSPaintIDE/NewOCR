<div>
    <a href="https://search.maven.org/artifact/com.uddernetworks.newocr/NewOCR/">
    	<img alt="Maven Central" src="https://maven-badges.herokuapp.com/maven-central/com.uddernetworks.newocr/NewOCR/badge.svg">
    </a>
    <a href="https://discord.gg/RXmPkPJ">
            <img src="https://img.shields.io/discord/528423806453415972.svg?logo=discord"
                alt="NewOCR and MS Paint IDE's Discord server">
    </a>
    <a href="https://travis-ci.org/MSPaintIDE/NewOCR/">
        <img alt="Travis (.org) branch" src="https://img.shields.io/travis/RubbaBoy/NewOCR/dev.svg">
    </a>
</div>

# NewOCR
NewOCR is an OCR library made to suit [MS Paint IDE](https://github.com/MSPaintIDE/MSPaintIDE)'s needs, though can be used in any project, as nothing is made specific to the IDE. The OCR can be trained with many fonts, though is geared towards fonts like **Verdana** and similar fonts. Other fonts _may_ require some tweaking of the character detector, but the main detection will work with no matter how different the characters are from Verdana (You could even modify it to work with emojis).

Currently, NewOCR is being tested against the following fonts:

- Comic Sans MS
- Monospaced
- Verdana
- Calibri
- Consolas
- Courier New

Though you can train the OCR on many, many other fonts. For more information on fonts used and how they are chosen, see the [fonts page](Fonts.md).

To get started with using NewOCR or get a detailed description of how every piece of the OCR works from start to finish, you can visit the wiki here: [https://wiki.newocr.dev/](https://wiki.newocr.dev/)

To view javadocs on the project, you can go here: [https://docs.newocr.dev/](https://docs.newocr.dev/)