package com.uddernetworks.newocr.configuration;

@FunctionalInterface
public interface ReflexiveSupplier<T> {
    T get() throws ReflectiveOperationException;
}
