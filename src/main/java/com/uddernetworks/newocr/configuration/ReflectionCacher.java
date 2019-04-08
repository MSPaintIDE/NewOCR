package com.uddernetworks.newocr.configuration;

import java.lang.reflect.Constructor;
import java.util.Optional;

public interface ReflectionCacher {

    // TODO: Docs
    <T> Optional<Constructor<T>> getOrLookupConstructor(Class<T> clazz, ReflexiveSupplier<Constructor<T>> constructorGenerator);

}
