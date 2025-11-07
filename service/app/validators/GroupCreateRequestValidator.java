package validators;

import com.google.common.collect.Lists;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.sunbird.common.exception.BaseException;
import org.sunbird.common.message.ResponseCode;
import org.sunbird.common.request.Request;
import org.sunbird.common.util.JsonKey;
import org.sunbird.util.LoggerUtil;

public class GroupCreateRequestValidator implements IRequestValidator {

  private static LoggerUtil logger = new LoggerUtil(GroupCreateRequestValidator.class);

  @Override
  public boolean validate(Request request) throws BaseException {
    logger.info(request.getContext(), MessageFormat.format("Validating the create group request {0}", request.getRequest()));
    try {
      ValidationUtil.validateRequestObject(request);
      ValidationUtil.validateMandatoryParamsWithType(
              request.getRequest(),
              Lists.newArrayList(JsonKey.NAME),
              String.class,
              true,
              JsonKey.REQUEST,request.getContext());
      ValidationUtil.validateParamsWithType(request.getRequest(),Lists.newArrayList(JsonKey.MEMBERS,JsonKey.ACTIVITIES),
              List.class,JsonKey.REQUEST,request.getContext());
      // validate value of status and role of members and userId if provided in request
      validateRoleAndStatus(request);
      validateActivityList(request);

    }catch (BaseException ex){
      BaseException baseException = new BaseException(ResponseCode.GS_CRT02.getErrorCode(),ResponseCode.GS_CRT02.getErrorMessage(),ex.getResponseCode());
      logger.error(request.getContext(),MessageFormat.format("GroupCreateRequestValidator: Error Code: {0}, ErrMsg {1}",ResponseCode.GS_CRT02.getErrorCode(),ex.getMessage()),baseException);
      throw baseException;
    }
    return true;
  }

  /**
   * validates UserId, role, status
   *
   * @param request
   * @throws BaseException
   */
  private void validateRoleAndStatus(Request request) throws BaseException {
    Map<String, List<String>> paramValue = new HashMap<>();
    paramValue.put(JsonKey.STATUS, Lists.newArrayList(JsonKey.ACTIVE, JsonKey.INACTIVE));
    paramValue.put(JsonKey.ROLE, Lists.newArrayList(JsonKey.ADMIN, JsonKey.MEMBER));
    Object membersObj = request.getRequest().get(JsonKey.MEMBERS);
    List<Object> memberObjectList = ValidationUtil.convertToJavaList(membersObj);
    if (CollectionUtils.isNotEmpty(memberObjectList)) {
      for (Object memberObj : memberObjectList) {
        Map<String, Object> member = ValidationUtil.convertToJavaMap(memberObj);
        int index = memberObjectList.indexOf(memberObj);
        ValidationUtil.validateMandatoryParamsWithType(
            member,
            Lists.newArrayList(JsonKey.USER_ID),
            String.class,
            true,
            JsonKey.MEMBERS + "[" + index + "]",request.getContext());
        ValidationUtil.validateParamValue(
            member,
            Lists.newArrayList(JsonKey.STATUS, JsonKey.ROLE),
            paramValue,
            JsonKey.MEMBERS + "[" + index + "]",request.getContext());
      }
    }
  }

  /**
   * checks mandatory param id and type of activity
   *
   * @param request
   * @throws BaseException
   */
  private void validateActivityList(Request request) throws BaseException {
    Object activitiesObj = request.getRequest().get(JsonKey.ACTIVITIES);
    List<Object> activityObjectList = ValidationUtil.convertToJavaList(activitiesObj);
    if (CollectionUtils.isNotEmpty(activityObjectList)) {
      for (Object activityObj : activityObjectList) {
        Map<String, Object> activity = ValidationUtil.convertToJavaMap(activityObj);
        int index = activityObjectList.indexOf(activityObj);
        ValidationUtil.validateMandatoryParamsWithType(
            activity,
            Lists.newArrayList(JsonKey.ID, JsonKey.TYPE),
            String.class,
            true,
            JsonKey.ACTIVITIES + "[" + index + "]", request.getContext());
      }
    }
  }
}
