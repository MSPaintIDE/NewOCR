package com.uddernetworks.newocr;

import com.uddernetworks.newocr.utils.MuteItList;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;

public class MuteItListTest {

    private String[] genArray(int... values) {
        return Arrays.stream(values).mapToObj(String::valueOf).toArray(String[]::new);
    }

    @Test
    public void noChange() {
        var list = new MuteItList<String>();
        for (int i = 0; i < 10; i++) list.add("" + i);
        assertArrayEquals(list.toArray(), genArray(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

    @Test
    public void insertionNoLoop() {
        var list = new MuteItList<String>();
        for (int i = 0; i < 10; i++) list.add("" + i);
        list.add(0, "100");
        list.add(1, "101");
        list.add(2, "102");
        assertArrayEquals(list.toArray(), genArray(100, 101, 102, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

    @Test
    public void removingNoLoop() {
        var list = new MuteItList<String>();
        for (int i = 0; i < 10; i++) list.add("" + i);
        list.remove("3");
        list.remove("4");
        list.remove("5");
        assertArrayEquals(list.toArray(), genArray(0, 1, 2, 6, 7, 8, 9));
    }

    @Test
    public void afterAdditionInLoop() {
        var list = new MuteItList<String>();
        for (int i = 0; i < 10; i++) list.add("" + i);
        list.forEach(element -> {
            if (element.equals("5")) {
                list.add("100");
                list.add("101");
                list.add("102");
            }
        });
        assertArrayEquals(list.toArray(), genArray(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 100, 101, 102));
    }

    @Test
    public void beforeAdditionInLoop() {
        var list = new MuteItList<String>();
        for (int i = 0; i < 10; i++) list.add("" + i);
        var orderIterated = new ArrayList<>();
        list.forEach(element -> {
            orderIterated.add(element);
            if (element.equals("5")) {
                list.add(0, "100");
                list.add(1, "101");
                list.add(2, "102");
            }
        });
        assertArrayEquals(list.toArray(), genArray(100, 101, 102, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        assertArrayEquals(orderIterated.toArray(), genArray(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

    @Test
    public void afterRemovalInLoop() {
        var list = new MuteItList<String>();
        for (int i = 0; i < 10; i++) list.add("" + i);
        var orderIterated = new ArrayList<>();
        list.forEach(element -> {
            orderIterated.add(element);
            if (element.equals("5")) {
                list.remove("7");
                list.remove("8");
                list.remove("9");
            }
        });
        assertArrayEquals(list.toArray(), genArray(0, 1, 2, 3, 4, 5, 6));
        assertArrayEquals(orderIterated.toArray(), genArray(0, 1, 2, 3, 4, 5, 6));
    }

    @Test
    public void beforeRemovalInLoop() {
        var list = new MuteItList<String>();
        for (int i = 0; i < 10; i++) list.add("" + i);
        var orderIterated = new ArrayList<>();
        list.forEach(element -> {
            orderIterated.add(element);
            if (element.equals("5")) {
                list.remove("1");
                list.remove("2");
                list.remove("3");
            }
        });
        assertArrayEquals(list.toArray(), genArray(0, 4, 5, 6, 7, 8, 9));
        assertArrayEquals(orderIterated.toArray(), genArray(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

}
