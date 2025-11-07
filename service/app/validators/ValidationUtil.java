package validators;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.BaseException;
import org.sunbird.common.exception.ValidationException;
import org.sunbird.common.request.Request;
import org.sunbird.util.LoggerUtil;
import scala.Tuple2;
import scala.collection.Iterator;

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
        } catch (ReflectiveOperationException e) {
          // If reflection fails, treat as unable to validate emptiness
          logger.error(reqContext, "Could not validate Scala collection emptiness via reflection: " + e.getMessage());
          throw new ValidationException.InvalidRequestData();
        }
        // Let other exceptions propagate (runtime exceptions, etc.)
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
   * Delegates to CollectionConverterUtil for centralized implementation.
   *
   * @param obj The object to convert
   * @return Java List or null if obj is null
   */
  public static <T> List<T> convertToJavaList(Object obj) {
    return org.sunbird.util.CollectionConverterUtil.convertToJavaList(obj);
  }

  /**
   * Converts Scala Map to Java Map.
   * Delegates to CollectionConverterUtil for centralized implementation.
   *
   * @param obj The object to convert
   * @return Java Map or null if obj is null
   */
  public static Map<String, Object> convertToJavaMap(Object obj) {
    return org.sunbird.util.CollectionConverterUtil.convertToJavaMap(obj);
  }

  /**
   * Recursively converts Scala collections (Map and Seq) to Java collections (Map and List).
   * This method handles nested Scala collections and converts them to their Java equivalents.
   *
   * @param obj The object to convert (can be Scala Map, Scala Seq, or any other object)
   * @return Converted Java collection or the original object if not a Scala collection
   */
  public static Object convertScalaCollectionToJavaCollection(Object obj) {
    if (obj instanceof scala.collection.Map) {
      scala.collection.Map<String, Object> scalaMap = (scala.collection.Map<String, Object>) obj;
      Map<String, Object> javaMap = new HashMap<>(scalaMap.size());
      Iterator<Tuple2<String, Object>> iterator = scalaMap.iterator();
      while (iterator.hasNext()) {
        Tuple2<String, Object> tuple = iterator.next();
        javaMap.put(tuple._1(), convertScalaCollectionToJavaCollection(tuple._2()));
      }
      return javaMap;
    } else if (obj instanceof scala.collection.Seq) {
      scala.collection.Seq<Object> scalaSeq = (scala.collection.Seq<Object>) obj;
      List<Object> javaList = new ArrayList<>(scalaSeq.size());
      Iterator<Object> iterator = scalaSeq.iterator();
      while (iterator.hasNext()) {
        javaList.add(convertScalaCollectionToJavaCollection(iterator.next()));
      }
      return javaList;
    }
    return obj;
  }
}
