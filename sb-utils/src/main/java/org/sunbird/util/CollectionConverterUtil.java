package org.sunbird.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for converting Scala collections to Java collections.
 * This handles the interoperability between Scala and Java collection types.
 */
public class CollectionConverterUtil {

    /**
     * Converts Scala collections to Java List.
     * Handles both Java List and Scala collections.
     *
     * @param obj The object to convert
     * @return Java List or null if obj is null
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> convertToJavaList(Object obj) {
        if (obj == null) {
            return null;
        }
        
        // If it's already a Java List, return it
        if (obj instanceof List) {
            return (List<T>) obj;
        }
        
        // Handle Scala collections
        try {
            Class<?> scalaIterableClass = Class.forName("scala.collection.Iterable");
            if (scalaIterableClass.isInstance(obj)) {
                // Convert Scala collection to Java List using reflection
                Object iterator = obj.getClass().getMethod("iterator").invoke(obj);
                List<T> javaList = new ArrayList<>();
                
                // Use reflection to iterate through Scala iterator
                while ((Boolean) iterator.getClass().getMethod("hasNext").invoke(iterator)) {
                    T element = (T) iterator.getClass().getMethod("next").invoke(iterator);
                    javaList.add(element);
                }
                return javaList;
            }
        } catch (Exception e) {
            // If conversion fails, return empty list to prevent ClassCastException
            return new ArrayList<>();
        }
        
        // If not a collection, return empty list
        return new ArrayList<>();
    }

    /**
     * Converts Scala Map to Java Map.
     * Handles both Java Map and Scala Map.
     *
     * @param obj The object to convert
     * @return Java Map or null if obj is null
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> convertToJavaMap(Object obj) {
        if (obj == null) {
            return null;
        }
        
        // If it's already a Java Map, return it
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        
        // Handle Scala Map
        try {
            Class<?> scalaMapClass = Class.forName("scala.collection.Map");
            if (scalaMapClass.isInstance(obj)) {
                // Convert Scala Map to Java Map using reflection
                Object iterator = obj.getClass().getMethod("iterator").invoke(obj);
                Map<String, Object> javaMap = new HashMap<>();
                
                // Use reflection to iterate through Scala iterator
                while ((Boolean) iterator.getClass().getMethod("hasNext").invoke(iterator)) {
                    Object tuple = iterator.getClass().getMethod("next").invoke(iterator);
                    // Scala tuple has _1() for key and _2() for value
                    Object key = tuple.getClass().getMethod("_1").invoke(tuple);
                    Object value = tuple.getClass().getMethod("_2").invoke(tuple);
                    javaMap.put((String) key, value);
                }
                return javaMap;
            }
        } catch (Exception e) {
            // If conversion fails, return empty map to prevent ClassCastException
            return new HashMap<>();
        }
        
        // If not a map, return empty map
        return new HashMap<>();
    }
}