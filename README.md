# NewOCR
NewOCR is an OCR library made to suit [MS Paint IDE](https://github.com/RubbaBoy/MSPaintIDE)'s needs, though can be used in any project, as nothing is made specific to the IDE. The OCR can be trained with many fonts, though is geared towards fonts like **Verdana** and similar fonts. Other fonts _may_ require some tweaking of the character detector, but the main detection will work with no matter how different the characters are from Verdana (Hell you could modify it to work with emojis).

## How it works
### Summary
NewOCR uses a super sketchy method of detecting characters, which in short breaks up each character into different subsections, then gets the percentage of filled in pixels each section contains, and puts them into an array. It then gets the closest matching array, which is decided as the closest pixel.

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

## Resources
The following papers were used as inspiration, ideas, knowledge gathering, whatever it may be towards the advancement of this OCR. I could have forgotten a few research papers, I read a lot of them. They might just be stuff I thought was really cool related to the subject, I'm generalizing this description to hell so I won't have to change it later.

- https://www.researchgate.net/publication/260405352_OPTICAL_CHARACTER_RECOGNITION_OCR_SYSTEM_FOR_MULTIFONT_ENGLISH_TEXTS_USING_DCT_WAVELET_TRANSFORM
- https://core.ac.uk/download/pdf/20643247.pdf
- https://www.researchgate.net/publication/321761298_Generalized_Haar-like_filters_for_document_analysis_application_to_word_spotting_and_text_extraction_from_comics
- https://pdfs.semanticscholar.org/c8b7/804abc030ee93eff2f5baa306b8b95361c57.pdf
- http://www.frc.ri.cmu.edu/~akeipour/downloads/Conferences/ICIT13.pdf
- https://support.dce.felk.cvut.cz/mediawiki/images/2/24/Bp_2017_troller_milan.pdf
- http://www.cs.toronto.edu/~scottl/research/msc_thesis.pdf