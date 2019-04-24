#!/bin/bash
# See https://medium.com/@nthgergo/publishing-gh-pages-with-travis-ci-53a8270e87db

if [ "$TRAVIS_PULL_REQUEST" != "false" ]; then exit 0; fi

set -o errexit

# config
git config --global user.email "nobody@ms-paint-i.de"
git config --global user.name "Travis CI"
git config core.autocrlf true

gradle javadoc --no-daemon
cd build/docs/javadoc

# deploy
git init
git add . &> /dev/null
git commit -m "Update docs from https://github.com/MSPaintIDE/NewOCR"
git push --force --quiet "https://${GITHUB_TOKEN}@github.com/MSPaintIDE/NewOCR-javadocs.git" master:master &> /dev/null