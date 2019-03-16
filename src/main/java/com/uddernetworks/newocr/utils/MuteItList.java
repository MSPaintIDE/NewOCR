package com.uddernetworks.newocr.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
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

    // TODO: Clean and add tests
    public static void main(String[] args) throws Exception {
        System.out.println("Starting");
        var test = new MuteItList<String>();
        for (int i = 0; i < 7; i++) {
            test.add("" + i);
        }
//        test.add("one");
//        test.add("two");
//        test.add("three");
//        test.add("four");
//        test.add("five");
        System.out.println("Made");

        test.forEach(element -> {
            System.out.println("element = " + element);

            if (element.equals("3")) {
                test.add(0, "7");
            }
        });

        System.out.println("test = " + test);
    }

    private Object[] elementData = null;

    private boolean copyInternalElements = true;
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

    private void refreshElementData() {
        if (!this.copyInternalElements) return;

        try {
            elementData = (Object[]) reflected.get(this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        sizeMod = 0;
        refreshElementData();
        copyInternalElements = true;
        System.out.println("elementData = " + Arrays.toString(elementData));
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
//        if (modCount != expectedModCount)
//            throw new ConcurrentModificationException();

        sizeMod = 0;
        activelyLooping = -1;
        copyInternalElements = false;
    }

    private static <E> E elementAt(Object[] es, int index) {
        return (E) es[index];
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public boolean isCopyInternalElements() {
        return copyInternalElements;
    }

    public void setCopyInternalElements(boolean copyInternalElements) {
        this.copyInternalElements = copyInternalElements;
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
