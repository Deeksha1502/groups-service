package validators;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.BaseException;
import org.sunbird.common.exception.ValidationException;
import org.sunbird.common.request.Request;
import org.sunbird.util.LoggerUtil;

public class ValidationUtil {

  private static LoggerUtil logger = new LoggerUtil(ValidationUtil.class);

  public static void validateRequestObject(Request request) throws BaseException {
    if (request.getRequest().isEmpty()) {
      logger.error(request.getContext(),"validateMandatoryParamsOfStringType:incorrect request provided");
      throw new ValidationException.InvalidRequestData();
    }
  }

  public static void validateMandatoryParamsWithType(
      Map<String, Object> reqMap,
      List<String> mandatoryParamsList,
      Class<?> type,
      boolean validatePresence,
      String parentKey,
      Map<String,Object> reqContext)
      throws BaseException {
    for (String param : mandatoryParamsList) {
      if (!reqMap.containsKey(param)) {
        throw new ValidationException.MandatoryParamMissing(param, parentKey);
      }

      if (!(isInstanceOf(reqMap.get(param).getClass(), type))) {
        logger.error(reqContext,"validateMandatoryParamsOfStringType:incorrect request provided");
        throw new ValidationException.ParamDataTypeError(parentKey + "." + param, type.getName());
      }

      if (validatePresence) {
        validatePresence(param, reqMap.get(param), type, parentKey,reqContext);
      }
    }
  }

    public static void validateParamsWithType(Map<String, Object> reqMap,
            List<String> paramList,
            Class<?> type,
            String parentKey,
            Map<String,Object> reqContext)
      throws BaseException {
      for (String param : paramList) {
        if(reqMap.containsKey(param)) {
          if (!(isInstanceOf(reqMap.get(param).getClass(), type))) {
            logger.error(reqContext,"validateMandatoryParamsType:incorrect request provided");
            throw new ValidationException.ParamDataTypeError(parentKey + "." + param, type.getName());
          }
        }
      }
  }

  private static void validatePresence(String key, Object value, Class<?> type, String parentKey,Map<String,Object> reqContext)
      throws BaseException {
    if (type == String.class) {
      if (StringUtils.isBlank((String) value)) {
        logger.error(reqContext,"validatePresence:incorrect request provided");
        throw new ValidationException.MandatoryParamMissing(key, parentKey);
      }
    } else if (type == Map.class) {
      Map<String, Object> map = (Map<String, Object>) value;
      if (map.isEmpty()) {
        logger.error(reqContext,"validatePresence:incorrect request provided");
        throw new ValidationException.MandatoryParamMissing(key, parentKey);
      }
    } else if (type == List.class) {
      // Handle both Java and Scala collections
      if (value instanceof List) {
        List<?> list = (List<?>) value;
        if (list.isEmpty()) {
          logger.error(reqContext,"validatePresence:incorrect request provided");
          throw new ValidationException.MandatoryParamMissing(key, parentKey);
        }
      } else {
        // Handle Scala collections
        try {
          Class scalaIterableClass = Class.forName("scala.collection.Iterable");
          if (scalaIterableClass.isInstance(value)) {
            // Use reflection to check if Scala collection is empty
            Object isEmpty = value.getClass().getMethod("isEmpty").invoke(value);
            if (Boolean.TRUE.equals(isEmpty)) {
              logger.error(reqContext,"validatePresence:incorrect request provided");
              throw new ValidationException.MandatoryParamMissing(key, parentKey);
            }
          }
        } catch (Exception e) {
          // If we can't check Scala collection emptiness, throw the mandatory param missing exception
          logger.error(reqContext, "Could not validate Scala collection emptiness: " + e.getMessage());
          throw new ValidationException.MandatoryParamMissing(key, parentKey);
        }
      }
    }
  }
  /**
   * @param reqMap
   * @param params list of params to validate values it contains
   * @param paramsValue for each params provided , add a values in the map key should be the
   *     paramName , value should be list of paramValue it should be for example key=status
   *     value=[active, inactive]
   * @throws BaseException
   */
  public static void validateParamValue(
      Map<String, Object> reqMap,
      List<String> params,
      Map<String, List<String>> paramsValue,
      String parentKey,
      Map<String,Object> reqContext)
      throws BaseException {
    logger.info(reqContext, MessageFormat.format(
        "validateParamValue: validating Param Value for the params {0} values {1}",
        params,
        paramsValue));
    for (String param : params) {
      if (reqMap.containsKey(param) && StringUtils.isNotEmpty((String) reqMap.get(param))) {
        List<String> values = paramsValue.get(param);
        String paramValue = (String) reqMap.get(param);
        if (!values.contains(paramValue)) {
          throw new ValidationException.InvalidParamValue(paramValue, parentKey + param);
        }
      }
    }
  }

  public static boolean isInstanceOf(Class objClass, Class targetClass) {
    // Handle both Java and Scala collections
    if (targetClass == List.class) {
      // Check for Java List
      if (targetClass.isAssignableFrom(objClass)) {
        return true;
      }
      // Check for Scala List (which is scala.collection.immutable.List)
      try {
        Class scalaListClass = Class.forName("scala.collection.immutable.List");
        if (scalaListClass.isAssignableFrom(objClass)) {
          return true;
        }
      } catch (ClassNotFoundException e) {
        // Scala classes not available, ignore
      }
      // Check for Scala collections that extend Iterable
      try {
        Class scalaIterableClass = Class.forName("scala.collection.Iterable");
        if (scalaIterableClass.isAssignableFrom(objClass)) {
          return true;
        }
      } catch (ClassNotFoundException e) {
        // Scala classes not available, ignore
      }
    }
    return targetClass.isAssignableFrom(objClass);
  }

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
      Class scalaIterableClass = Class.forName("scala.collection.Iterable");
      if (scalaIterableClass.isInstance(obj)) {
        // Convert Scala collection to Java List using reflection
        Object iterator = obj.getClass().getMethod("iterator").invoke(obj);
        List<T> javaList = new java.util.ArrayList<>();
        
        // Use reflection to iterate through Scala iterator
        while ((Boolean) iterator.getClass().getMethod("hasNext").invoke(iterator)) {
          T element = (T) iterator.getClass().getMethod("next").invoke(iterator);
          javaList.add(element);
        }
        return javaList;
      }
    } catch (Exception e) {
      // If conversion fails, try to return as is (might cause ClassCastException later)
      logger.error(null, "Failed to convert Scala collection to Java List: " + e.getMessage());
    }
    
    // If not a collection, return null
    return null;
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
      Class scalaMapClass = Class.forName("scala.collection.Map");
      if (scalaMapClass.isInstance(obj)) {
        // Convert Scala Map to Java Map using reflection
        Object iterator = obj.getClass().getMethod("iterator").invoke(obj);
        Map<String, Object> javaMap = new java.util.HashMap<>();
        
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
      // If conversion fails, try to return as is (might cause ClassCastException later)
      logger.error(null, "Failed to convert Scala Map to Java Map: " + e.getMessage());
    }
    
    // If not a map, return null
    return null;
  }
}
