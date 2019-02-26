<a href="https://discord.gg/RXmPkPJ">
        <img src="https://img.shields.io/discord/528423806453415972.svg?logo=discord"
            alt="NewOCR and MS Paint IDE's Discord server">
</a>

# NewOCR
NewOCR is an OCR library made to suit [MS Paint IDE](https://github.com/RubbaBoy/MSPaintIDE)'s needs, though can be used in any project, as nothing is made specific to the IDE. The OCR can be trained with many fonts, though is geared towards fonts like **Verdana** and similar fonts. Other fonts _may_ require some tweaking of the character detector, but the main detection will work with no matter how different the characters are from Verdana (Hell you could modify it to work with emojis).

With upcoming versions in the `dev` branch, the OCR is meant to detect and read both natural (With more filtering being worked on) and computer-generated (Not low resolution) images containing text.

## Branch info

The branch you are currently on is `dev`. This branch is exclusively for new features and testing with the OCR, and will probably not be stable depending when the last release was. All completed features/additions will be pushed to `master`. If you contribute, please make any PRs to `dev`.

## How it works
### Summary
NewOCR uses a super sketchy method of detecting characters, which in short breaks up each character into different subsections, then gets the percentage of filled in pixels each section contains, and puts them into an array. It then gets the closest matching array, which is decided as the closest pixel.

### Preprocessing

The first step in reading the image is what happens in nearly all OCRs, and it is to remove any noise and other distortions to the input image. This also includes binarization, which is making the input image purely black and white. All these things make character recognition and identification.

What NewOCR uses for preprocessing is primarily [leptonica](http://www.leptonica.com/), with the amount of preprocessing dependent on if the setting for natural images is on or off, due to the extra processing required for natural images. Something like a screenshot of just text doesn't require nearly as much processing as a photo of your handwriting.

### Character Recognition

After the image went through any necessary preprocessing phases and is black and white, it needs to detect the bounds of the lines and characters. It does this by finding horizontal separations to find the general character lines, then finding vertical spaces to find each individual character. Once character-dependent vertical padding is removed, the character bounds are found. More details on this method can be found on page 55 of [this paper](https://www.researchgate.net/publication/260405352_OPTICAL_CHARACTER_RECOGNITION_OCR_SYSTEM_FOR_MULTIFONT_ENGLISH_TEXTS_USING_DCT_WAVELET_TRANSFORM).

### Sectioning
Each letter is broken up into 16 sections. These aren't pixel-based, but percentage based. This allows them to be created on all sized letters with the same proportions.

First, the letter is horizontally broken up into top and bottom sections. Then, each of those two sections are broken up vertically into another two sections. The remaining sections are broken up into diagonal sections, with their diagonals angling towards the center of the character. A visual of what the sections look like and their index of the value array (Will be used later) can be found here: 
![Section examples 1](/images/E1.png)

After that process has occurred, the second sectioning process starts. This one is more simple, in that it first horizontally separates it into thirds, then those sections into vertical thirds. The sections and their indices look like the following:  
![Section examples 2](/images/E2.png)

### Applying the sections

After the sections and their indices have been established, the system gets the percentage the pixels are black (Rather than white, as it's effectively binary image). Applied to our sections, this is what the values for sections of the letter **E** would look like (Depending on the size, these values may vary from your results):  
![Section values 1](/images/Eval1.png)  
![Section values 2](/images/Eval2.png)

With the indices applied, the value array would be:
```
[0.86, 0.51, 0.46, 0.48, 0.46, 0.67, 0.43, 0.09, 0.77, 0.37, 0.37, 0.77, 0.36, 0.36, 0.77, 0.37, 0.37]
```

These values are then compared to the averaged out trained characters' data, and the closest match is given. Other things that affect its similarity to the trained database character are the width/height ratio, which helps distinguish characters like `_` and `-`. Some type meta can also be attached to the database character, but still has the percentage values stored. These meta values are things like if it had to append chunks of pixels together in such a way it has to be a percentage sign, if it appended pixels to the top of a base character (`!`, `i`, `j`), to a bottom of a character (`!`), and some others. The enum containing these values may be found here: [LetterMeta.java](/src/main/java/com/uddernetworks/newocr/LetterMeta.java).

### Training
A vital part in the OCR is its training. Though many OCRs require training for their Neural Networks, NewOCR uses a simple, fast method of training involving essentially averaging values form charcaters.

The OCR starts off with a generated image of all the characters it can take advantage of through the [TrainGenerator](/src/main/java/com/uddernetworks/newocr/TrainGenerator.java) class, taking up fonts from an upper to lower bound. The system gets the character bounds for every character, then incrementally goes through the characters, putting the segmented percentages described above into a database, after averaging all the font sizes together. This is also done with the width and height of the character, for increased accuracy. The accuracy of the character segmentation is crucial in this step, as if one character is detected as say 2, it will throw off the entire line, resulting in a useless training data set. 

With scaling fonts to smaller sizes where they get deformed by their pixelation, their percentages may be significantly different than the higher resolution variants. To circumvent this, the database is broken up into different sections of font bounds, e.g. from font size 0-12 values will be places together, 13-20, and 20+ will be grouped together. The bounds' values and count may be changed in the program.

Example of a training image:  
![Training image](/images/training.png)

## Using It
NewOCR is on Central, so it's insanely easy to get on both Maven and Gradle.

Gradle:
```Groovy
compile 'com.uddernetworks.newocr:NewOCR:1.2.1'
```

Maven:
```XML
<dependency>
    <groupId>com.uddernetworks.newocr</groupId>
    <artifactId>NewOCR</artifactId>
    <version>1.2.1</version>
</dependency>
```

### Creating the training image
The OCR needs an image to base all its font data off of, so a training image is required. The class `TrainGenerator.java` has the ability to create such images, and you can just change `UPPER_FONT_BOUND` and `LOWER_FONT_BOUND` to the maximum and minimum fonts to be created in the image. After running the program, you should have an image similar to the one displayed above in [Training](#Training).

Currently the font `Verdana` is the only font tested to work with the character recogniser, though if character detection was modified/improved (Planned for the future) it could easily detect many more fonts with high accuracy.

### Setting up the database
To use NewOCR, a MySQL database is required. This is to store all the section data of each character. To run by the example usage in `Main.java`, you will need to put the database's URL, username, and password as the program arguments in their respective orders. An example of this would be:
```java -jar NewOCR-1.2.1.java "jdbc:mysql://127.0.0.1:3306/OCR" "my_user" "my_pass"```
You will _not_ be required to run any queries manually once you have created a table for the OCR; the program will do that for you.

Before you do anything with detecting characters you must train the OCR. It does not use any Neural Networks as shown in the explanation above, but it needs to register how the font works. In order to get this working in `Main.java`, make sure in the main method you have `new Main().run(args)` uncommented, and that more down the file that `new File("training.png")` and  `new File("HWTest.png")` points to valid paths, the first one being the training image as described above, and then your input image. When you run the program, type `yes` when it asks if you want to train, and then wait a minute or so. When the program exits, you should be able to run it again, answer `no` to that question, and after a few seconds it should give its output.  

### System properties used
NewOCR uses a few system properties for some extra options for debugging and other things. Here is a list of them (More may be added in the future):
- **newocr.rewrite** [Boolean] - Rewrites the image to a new BufferedImage before it's scanned. This could fix some weird encoding issues happening in the past
- **newocr.error** [Boolean] - If the system should output certain problems it thinks may have occurred (NOT stacktraces, those are always shown)
- **newocr.debug** [Boolean] - If the system should display some certain debug messages used in the program

## Resources
The following papers were used as inspiration, ideas, knowledge gathering, whatever it may be towards the advancement of this OCR. I could have forgotten a few research papers, I read a lot of them. They might just be stuff I thought was really cool related to the subject, I'm generalizing this description to hell so I won't have to change it later.

- https://www.researchgate.net/publication/260405352_OPTICAL_CHARACTER_RECOGNITION_OCR_SYSTEM_FOR_MULTIFONT_ENGLISH_TEXTS_USING_DCT_WAVELET_TRANSFORM
- https://core.ac.uk/download/pdf/20643247.pdf
- https://www.researchgate.net/publication/321761298_Generalized_Haar-like_filters_for_document_analysis_application_to_word_spotting_and_text_extraction_from_comics
- https://pdfs.semanticscholar.org/c8b7/804abc030ee93eff2f5baa306b8b95361c57.pdf
- http://www.frc.ri.cmu.edu/~akeipour/downloads/Conferences/ICIT13.pdf
- https://support.dce.felk.cvut.cz/mediawiki/images/2/24/Bp_2017_troller_milan.pdf
- http://www.cs.toronto.edu/~scottl/research/msc_thesis.pdf
- https://www.researchgate.net/publication/258651794_Novel_Approach_for_Baseline_Detection_and_Text_Line_Segmentation
- https://www.researchgate.net/publication/2954700_Neural_and_fuzzy_methods_in_handwriting_recognition
- https://cyber.felk.cvut.cz/theses/papers/444.pdf
