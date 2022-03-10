#!/bin/bash
# See https://medium.com/@nthgergo/publishing-gh-pages-with-travis-ci-53a8270e87db

if [ "$TRAVIS_PULL_REQUEST" != "false" ]; then exit 0; fi

set -o errexit

# config
git config --global user.email "nobody@ms-paint-i.de"
git config --global user.name "Travis CI"
git config core.autocrlf true

mkdir pages
cd pages

git clone https://github.com/MSPaintIDE/NewOCR-javadocs .
rm -rf *
cd ../

gradle javadoc --daemon
cd build/docs/
mv javadoc/* ../../pages
cd ../../pages
echo "docs.newocr.dev" > CNAME

# deploy
git add . &> /dev/null
git commit -m "Update docs from https://github.com/MSPaintIDE/NewOCR" &> /dev/null
git push --quiet "https://${GITHUB_TOKEN}@github.com/MSPaintIDE/NewOCR-javadocs.git" master:master &> /dev/null