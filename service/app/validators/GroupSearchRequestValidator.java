package validators;

import com.google.common.collect.Lists;
import java.text.MessageFormat;
import java.util.Map;
import org.sunbird.common.exception.BaseException;
import org.sunbird.common.message.ResponseCode;
import org.sunbird.common.request.Request;
import org.sunbird.common.util.JsonKey;
import org.sunbird.util.LoggerUtil;

public class GroupSearchRequestValidator implements validators.IRequestValidator {
  private static LoggerUtil logger =
      new LoggerUtil(validators.GroupSearchRequestValidator.class);

  @Override
  public boolean validate(Request request) throws BaseException {
    logger.info(request.getContext(),"Validating the search group request "+ request.getRequest());
    try {
      Map<String, Object> requestMap = request.getRequest();
      Object filters = requestMap.get(JsonKey.FILTERS);
      if (filters != null && (filters instanceof scala.collection.Map || filters instanceof scala.collection.Seq)) {
        requestMap.put(JsonKey.FILTERS, ValidationUtil.convertScalaCollectionToJavaCollection(filters));
      }

      validators.ValidationUtil.validateRequestObject(request);
      validators.ValidationUtil.validateMandatoryParamsWithType(
              request.getRequest(),
              Lists.newArrayList(JsonKey.FILTERS),
              Map.class,
              false,
              JsonKey.REQUEST,request.getContext());
      return true;
    }catch (BaseException ex){
      BaseException baseException = new BaseException(ResponseCode.GS_LST02.getErrorCode(),ResponseCode.GS_LST02.getErrorMessage(),ex.getResponseCode());
      logger.error(request.getContext(), MessageFormat.format("GroupSearchRequestValidator: Error Code: {0}, ErrMsg {1}",ResponseCode.GS_LST02.getErrorCode(),ex.getMessage()),baseException);
      throw baseException;
    }
  }
}
