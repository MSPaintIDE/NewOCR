CREATE TABLE IF NOT EXISTS letters (
  letter INTEGER, -- The letter the data set is for
  modifier INTEGER, -- The modifier number of the letter. E.g. different parts of a "
  avgWidth DOUBLE, -- The average width of all tested character images of this letter
  avgHeight DOUBLE, -- The average height of all tested character images of this letter
  minCenter DOUBLE,
  maxCenter DOUBLE,
  hasDot BOOLEAN,
  letterMeta INTEGER,
  isSpace BOOLEAN,
  UNIQUE(letter, modifier)
);