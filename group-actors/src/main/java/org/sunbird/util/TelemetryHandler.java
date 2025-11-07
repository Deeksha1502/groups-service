package org.sunbird.util;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.request.Request;
import org.sunbird.common.util.JsonKey;
import org.sunbird.models.Group;
import org.sunbird.telemetry.TelemetryEnvKey;
import org.sunbird.telemetry.util.TelemetryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TelemetryHandler {

    /**
     * Converts Scala collections to Java List.
     * Handles both Java List and Scala collections.
     *
     * @param obj The object to convert
     * @return Java List or null if obj is null
     */
    @SuppressWarnings("unchecked")
    private static <T> List<T> convertToJavaList(Object obj) {
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
                List<T> javaList = new java.util.ArrayList<>();
                
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
    private static Map<String, Object> convertToJavaMap(Object obj) {
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
            // If conversion fails, return empty map to prevent ClassCastException
            return new java.util.HashMap<>();
        }
        
        // If not a map, return empty map
        return new java.util.HashMap<>();
    }

    public static void logGroupCreateTelemetry(Request actorMessage, String groupId){
        String source =
                actorMessage.getContext().get(org.sunbird.common.util.JsonKey.REQUEST_SOURCE) != null
                        ? (String) actorMessage.getContext().get(org.sunbird.common.util.JsonKey.REQUEST_SOURCE)
                        : "";

        List<Map<String, Object>> correlatedObject = new ArrayList<>();
        if (StringUtils.isNotBlank(source)) {
            TelemetryUtil.generateCorrelatedObject(
                    source, StringUtils.capitalize(org.sunbird.common.util.JsonKey.REQUEST_SOURCE), null, correlatedObject);
        }
        Map<String, Object> targetObject = null;
        targetObject = groupId != null ?
                TelemetryUtil.generateTargetObject(groupId, TelemetryEnvKey.GROUP_CREATED, org.sunbird.common.util.JsonKey.ACTIVE, null)
                : TelemetryUtil.generateTargetObject(groupId, TelemetryEnvKey.GROUP_CREATE_ERROR,null, null);

        // Add user information to Cdata
        TelemetryUtil.generateCorrelatedObject(
                (String) actorMessage.getContext().get(JsonKey.USER_ID),
                TelemetryEnvKey.USER,
                null,
                correlatedObject);
        // Add group info information to Cdata
        TelemetryUtil.generateCorrelatedObject(
                groupId,
                TelemetryEnvKey.GROUPID,
                null,
                correlatedObject);
        TelemetryUtil.telemetryProcessingCall(
                actorMessage.getRequest(), targetObject, correlatedObject, actorMessage.getContext());
    }

    public static void logGroupDeleteTelemetry(Request actorMessage, String groupId, Map<String, Object> dbResGroup, boolean isDeleted) {
        Map<String, Object> targetObject = null;
        List<Map<String, Object>> correlatedObject = new ArrayList<>();
        if(isDeleted) {
            targetObject =
                    TelemetryUtil.generateTargetObject(
                            groupId,
                            TelemetryEnvKey.DELETE_GROUP,
                            null,
                            (String) dbResGroup.get(JsonKey.STATUS));
        }else{
            targetObject =
                    TelemetryUtil.generateTargetObject(
                            groupId,
                            TelemetryEnvKey.GROUP_DELETE_ERROR,
                            null,
                            null != dbResGroup ? (String) dbResGroup.get(JsonKey.STATUS):null);
        }
        TelemetryUtil.generateCorrelatedObject(
                (String) actorMessage.getContext().get(JsonKey.USER_ID),
                TelemetryEnvKey.USER,
                null,
                correlatedObject);
        // Add group info information to Cdata
        TelemetryUtil.generateCorrelatedObject(
                groupId,
                TelemetryEnvKey.GROUPID,
                null,
                correlatedObject);
        TelemetryUtil.telemetryProcessingCall(
                actorMessage.getRequest(), targetObject, correlatedObject, actorMessage.getContext());
    }

    public static void logGroupUpdateTelemetry(Request actorMessage, Group group, Map<String, Object> dbResGroup, boolean isSuccess) {
        Map<String, Object> targetObject = null;
        List<Map<String, Object>> correlatedObject = new ArrayList<>();
        if (isSuccess) {
            if (null != dbResGroup && null != dbResGroup.get(JsonKey.STATUS)) {
                switch ((String) dbResGroup.get(JsonKey.STATUS)) {
                    case JsonKey.ACTIVE:
                        targetObject = generateTargetForActiveGroup(actorMessage, group, dbResGroup);
                        break;
                    case JsonKey.SUSPENDED:
                        targetObject =
                                TelemetryUtil.generateTargetObject(
                                        group.getId(),
                                        TelemetryEnvKey.ACTIVATE_GROUP,
                                        group.getStatus(),
                                        (String) dbResGroup.get(JsonKey.STATUS));
                        break;

                    default:
                        targetObject =
                                TelemetryUtil.generateTargetObject(
                                        group.getId(), TelemetryEnvKey.GROUP,  null, null);
                }

            }
        }else{
            targetObject =
                    TelemetryUtil.generateTargetObject(
                            group.getId(),
                            TelemetryEnvKey.GROUP_UPDATE_ERROR,
                            null,
                            null != dbResGroup ?(String) dbResGroup.get(JsonKey.STATUS): null);
        }
        TelemetryUtil.generateCorrelatedObject(
                (String) actorMessage.getContext().get(JsonKey.USER_ID),
                TelemetryEnvKey.USER,
                null,
                correlatedObject);
        // Add group info information to Cdata
        TelemetryUtil.generateCorrelatedObject(
                group.getId(),
                TelemetryEnvKey.GROUPID,
                null,
                correlatedObject);
        TelemetryUtil.telemetryProcessingCall(
                actorMessage.getRequest(), targetObject, correlatedObject, actorMessage.getContext());
    }

    public static void logGroupMembershipUpdateTelemetry(Request actorMessage, String userId, boolean isSuccess) {
        String source =
                actorMessage.getContext().get(JsonKey.REQUEST_SOURCE) != null
                        ? (String) actorMessage.getContext().get(JsonKey.REQUEST_SOURCE)
                        : "";

        List<Map<String, Object>> correlatedObject = new ArrayList<>();
        if (StringUtils.isNotBlank(source)) {
            TelemetryUtil.generateCorrelatedObject(
                    source, StringUtils.capitalize(JsonKey.REQUEST_SOURCE), null, correlatedObject);
        }
        List<Object> groups = convertToJavaList(actorMessage.getRequest().get(JsonKey.GROUPS));
        if (groups != null) {
        for (Object groupObj : groups) {
            Map<String,Object> group = convertToJavaMap(groupObj);
            if (group != null) {
                // Add group info information to Cdata
                TelemetryUtil.generateCorrelatedObject(
                        (String) group.get(JsonKey.GROUP_ID),
                        TelemetryEnvKey.GROUPID,
                        null,
                        correlatedObject);
            }
        }
        }
        Map<String, Object> targetObject = null;
        if(isSuccess) {
            targetObject =
                    TelemetryUtil.generateTargetObject(
                            userId, TelemetryEnvKey.MEMBER_UPDATE,  null, null);
        }else{
            targetObject =
                    TelemetryUtil.generateTargetObject(
                            userId, TelemetryEnvKey.GROUP_MEMBER_UPDATE_ERROR, null, null);
        }
        TelemetryUtil.generateCorrelatedObject(
                (String) actorMessage.getContext().get(JsonKey.USER_ID),
                TelemetryEnvKey.USER,
                null,
                correlatedObject);
        TelemetryUtil.telemetryProcessingCall(
                actorMessage.getRequest(), targetObject, correlatedObject, actorMessage.getContext());
    }

    private static Map<String, Object> generateTargetForActiveGroup(Request actorMessage, Group group, Map<String, Object> dbResGroup) {
        Map<String, Object> targetObject;
        if (!CollectionUtils.isEmpty(group.getActivities())) {
            targetObject = TelemetryUtil.generateTargetObject(
                    group.getId(),
                    TelemetryEnvKey.ACTIVITY,
                    JsonKey.ACTIVE,
                    (String) dbResGroup.get(JsonKey.STATUS));
        } else if (!MapUtils.isEmpty((Map<String, Object>) actorMessage.getRequest().get(JsonKey.MEMBERS))) {
            targetObject = TelemetryUtil.generateTargetObject(
                    group.getId(),
                    TelemetryEnvKey.ADD_MEMBER,
                    JsonKey.ACTIVE,
                    (String) dbResGroup.get(JsonKey.STATUS));
        } else if(JsonKey.SUSPENDED.equals(group.getStatus())){
            targetObject = TelemetryUtil.generateTargetObject(
                    group.getId(),
                    TelemetryEnvKey.DEACTIVATE_GROUP,
                    JsonKey.ACTIVE,
                    (String) dbResGroup.get(JsonKey.STATUS));

        } else {
            targetObject = TelemetryUtil.generateTargetObject(
                    group.getId(),
                    TelemetryEnvKey.UPDATE_GROUP,
                    JsonKey.ACTIVE,
                    (String) dbResGroup.get(JsonKey.STATUS));
        }
        return targetObject;
    }
}
