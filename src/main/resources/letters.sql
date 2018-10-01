CREATE TABLE IF NOT EXISTS `letters` (
  letter CHARACTER PRIMARY KEY UNIQUE, -- The letter the data set is for
  avgWidth DECIMAL, -- The average width of all tested character images of this letter
  avgHeight DECIMAL, -- The average height of all tested character images of this letter
  minFontSize INTEGER, -- The minimum font size this data set was trained on
  maxFontSize INTEGER -- The maximum font size this data set was trained on
);