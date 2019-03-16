package com.uddernetworks.newocr.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Mutable & Iterable List
 * Allows for basic insertion and deletion (With {@link MuteItList#add(Object)}, {@link MuteItList#add(int, Object)},
 * {@link MuteItList#remove(int)}, and {@link MuteItList#remove(Object)}) during a {@link MuteItList#forEach(Consumer)}
 * iteration. This class is currently a WIP and may be unstable for applications outside of NewOCR, so use with caution.
 *
 * @param <E> The type of elements in this list
 */
public class MuteItList<E> extends ArrayList<E> {

    private Object[] elementData = null;

    private int sizeMod = 0;
    private int activelyLooping = -1;
    private boolean modified = false;

    private static Field reflected;

    static {
        try {
            reflected = ArrayList.class.getDeclaredField("elementData");
            reflected.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the current class' elementData field from its super elementData field via cached reflection. This is a
     * heavy operation (In comparison) as all reflection, so it should be used sparingly.
     */
    private void refreshElementData() {
        try {
            elementData = (Object[]) reflected.get(this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Iterated over the elements in the current list the same as {@link ArrayList#forEach(Consumer)}, however in the
     * Consumer, you can invoke the methods {@link MuteItList#add(Object)}, {@link MuteItList#add(int, Object)},
     * {@link MuteItList#remove(int)}, and {@link MuteItList#remove(Object)} upon the current list. Unlike a normal
     * {@link ArrayList}, this will not cause any {@link java.util.ConcurrentModificationException}; if anything bad
     * happens it will just break, but in testing has appeared to be fairly stable.
     *
     * @param action The consumer to take each element of the current list
     */
    @Override
    public void forEach(Consumer<? super E> action) {
        sizeMod = 0;
        refreshElementData();
        modified = false;

        Objects.requireNonNull(action);
        int expectedModCount = modCount;
        Object[] es = elementData;
        int size = size();

        for (int i = 0; modCount == expectedModCount && i < size + sizeMod; i++) {
            activelyLooping = i;
            action.accept(elementAt(es, i));
            i = activelyLooping;

            if (modified) {
                refreshElementData();
                es = elementData;
                expectedModCount = modCount;
                sizeMod = 0;
                size = size();
                modified = false;
            }
        }

        sizeMod = 0;
        activelyLooping = -1;
    }

    private static <E> E elementAt(Object[] es, int index) {
        return (E) es[index];
    }

    @Override
    public boolean add(E e) {
        var res = super.add(e);
        modified = true;
        return res;
    }

    @Override
    public void add(int index, E element) {
        super.add(index, element);
        modCount--;
        if (activelyLooping <= index) modified = true;
    }

    @Override
    public E remove(int index) {
        var ret = super.remove(index);
        modCount--;

        if (activelyLooping > index) {
            activelyLooping--;
        } else {
            modified = true;
        }

        sizeMod--;

        return ret;
    }

    @Override
    public boolean remove(Object o) {
        var index = indexOf(o);
        super.remove(index);
        modCount--;

        if (activelyLooping > index) {
            activelyLooping--;
        } else {
            modified = true;
        }

        sizeMod--;
        return true;
    }
}
