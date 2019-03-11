package com.uddernetworks.newocr.recognition.mergence;

public enum MergePriority {
    LOWEST(-2),
    LOW(-1),
    NORMAL(0),
    HIGH(1),
    HIGHEST(-2);

    private int priority;

    MergePriority(int priority) {
        this.priority = priority;
    }

    public int getPriorityIndex() {
        return priority;
    }
}
