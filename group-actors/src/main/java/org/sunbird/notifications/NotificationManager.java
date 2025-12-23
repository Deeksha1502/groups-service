package org.sunbird.notifications;

import org.apache.pekko.actor.ActorRef;
import org.sunbird.Application;
import org.sunbird.common.request.Request;
import org.sunbird.models.ActorOperations;
import org.sunbird.models.MemberResponse;
import org.sunbird.util.JsonKey;
import org.sunbird.util.LoggerUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationManager {
    private static LoggerUtil logger = new LoggerUtil(NotificationManager.class);

    /**
     * This function will take the request object and list of notifications to be sent
     * @param request
     * @param notifications List of notifications
     */
    public static void sendNotifications(Request request, List<String> notifications, Map<String,Object> dbResGroup,
                                         List<MemberResponse> groupMembers){
        //Send list of notifications
        logger.info(request.getContext(),"NotificationManager: Send Notifications"+ notifications.size());

        for (String notification: notifications ) {
            Request reqObj = new Request();
            Map<String,Object> reqMap = new HashMap<>();
            reqMap.put(JsonKey.REQUEST,request.getRequest());
            reqMap.put(JsonKey.GROUP,dbResGroup);
            reqMap.put(JsonKey.MEMBERS,groupMembers);
            reqObj.setOperation(notification);
            reqObj.setRequest(reqMap);
            reqObj.setContext(request.getContext());

            Application.getInstance().getActorRef(ActorOperations.SEND_NOTIFICATION.getValue())
                    .tell(reqObj, ActorRef.noSender());
        }


    }
}
