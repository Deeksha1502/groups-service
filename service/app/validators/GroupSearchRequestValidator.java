package validators;

import com.google.common.collect.Lists;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.common.exception.BaseException;
import org.sunbird.common.message.ResponseCode;
import org.sunbird.common.request.Request;
import org.sunbird.common.util.JsonKey;
import org.sunbird.util.LoggerUtil;
import scala.collection.Iterator;
import scala.collection.Seq;
import scala.Tuple2;

public class GroupSearchRequestValidator implements validators.IRequestValidator {
  private static LoggerUtil logger =
      new LoggerUtil(validators.GroupSearchRequestValidator.class);

  @Override
  public boolean validate(Request request) throws BaseException {
    logger.info(request.getContext(),"Validating the search group request "+ request.getRequest());
    try {
      Map<String, Object> requestMap = request.getRequest();
      Object filters = requestMap.get(JsonKey.FILTERS);
      if (filters != null && filters.getClass().getName().startsWith("scala.collection")) {
        requestMap.put(JsonKey.FILTERS, convertScalaCollectionToJava(filters));
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

  private Object convertScalaCollectionToJava(Object obj) {
    if (obj instanceof scala.collection.Map) {
        scala.collection.Map<String, Object> scalaMap = (scala.collection.Map<String, Object>) obj;
        Map<String, Object> javaMap = new HashMap<>();
        Iterator<Tuple2<String, Object>> iterator = scalaMap.iterator();
        while (iterator.hasNext()) {
            Tuple2<String, Object> tuple = iterator.next();
            javaMap.put(tuple._1(), convertScalaCollectionToJava(tuple._2()));
        }
        return javaMap;
    } else if (obj instanceof scala.collection.Seq) {
        scala.collection.Seq<Object> scalaSeq = (scala.collection.Seq<Object>) obj;
        List<Object> javaList = new ArrayList<>();
        Iterator<Object> iterator = scalaSeq.iterator();
        while (iterator.hasNext()) {
            javaList.add(convertScalaCollectionToJava(iterator.next()));
        }
        return javaList;
    }
    return obj;
  }
}
