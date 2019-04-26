package com.uddernetworks.newocr.recognition.mergence;

/**
 * The priority of {@link MergeRule}s, {@link MergePriority#HIGHEST} going first, {@link MergePriority#LOWEST} going
 * last.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
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
