package com.uddernetworks.newocr.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A class to cache constructors from the {@link HOCONFontConfiguration}.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class ConfigReflectionCacher implements ReflectionCacher {

    private static Logger LOGGER = LoggerFactory.getLogger(ConfigReflectionCacher.class);

    private Map<Class<?>, Constructor<?>> constructors = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<Constructor<T>> getOrLookupConstructor(Class<T> clazz, ReflexiveSupplier<Constructor<T>> constructorGenerator) {
        return Optional.ofNullable((Constructor<T>) constructors.computeIfAbsent(clazz, x -> {
            try {
                return constructorGenerator.get();
            } catch (ReflectiveOperationException e) {
                LOGGER.error("Error while creating constructor", e);
                return null;
            }
        }));
    }
}
