package com.uddernetworks.newocr.recognition;

import com.uddernetworks.newocr.character.SearchCharacter;

import java.util.List;

public class TrainLine implements CharacterLine {

    private List<SearchCharacter> letters;
    private int topY;
    private int bottomY;

    public TrainLine(List<SearchCharacter> letters, int topY, int bottomY) {
        this.letters = letters;
        this.topY = topY;
        this.bottomY = bottomY;
    }

    @Override
    public List<SearchCharacter> getLetters() {
        return this.letters;
    }

    @Override
    public int topY() {
        return this.topY;
    }

    @Override
    public int bottomY() {
        return this.bottomY;
    }
}
