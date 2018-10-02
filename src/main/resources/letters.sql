CREATE TABLE IF NOT EXISTS `letters` (
  letter INTEGER PRIMARY KEY UNIQUE, -- The letter the data set is for
  avgWidth DOUBLE, -- The average width of all tested character images of this letter
  avgHeight DOUBLE, -- The average height of all tested character images of this letter
  minFontSize INTEGER, -- The minimum font size this data set was trained on
  maxFontSize INTEGER -- The maximum font size this data set was trained on
);