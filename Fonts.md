# Fonts

NewOCR can train almost any arbitrary font from a training file in the correct format (Some may require configuration file modification), though there are some fonts that are already configured and trained to work. Fonts can be added/removed from this list as long as they work and pass all tests. Any OCR changes are tested against these fonts, so the more fonts the less problems the OCR will have in the end.

## Why is [font] not supported?

There are several reasons a font may not be supported. At the current NewOCR version, fonts must contain all characters in the training image, and have **no kerning. ** Kerning is the biggest reason some fonts are not supported.

## Supported Fonts

Just because a font is not on this list, does **not** mean it will not work! These are just the fonts that the OCR is tested against, if you have a font that works then make a PR and add its config to the repo and add it here!

+ Comic Sans MS
+ Monospaced
+ Verdana
+ Calibri
## Unsupported Fonts

+ Arial **Reason: Kerning**
+ Terminal **Reason: Kerning**