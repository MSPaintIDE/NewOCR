CREATE TABLE IF NOT EXISTS sizing (
  letter INTEGER,
  size INTEGER,
  height INTEGER,
  UNIQUE(letter, height)
);