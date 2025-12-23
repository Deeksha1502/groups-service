package org.sunbird.actors;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.apache.pekko.testkit.javadsl.TestKit;
import jnr.x86asm.Mem;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.Application;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.message.Localizer;
import org.sunbird.common.request.Request;
import org.sunbird.common.response.Response;
import org.sunbird.common.util.JsonKey;
import org.sunbird.common.util.NotificationType;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.models.Member;
import org.sunbird.models.MemberResponse;
import org.sunbird.util.HttpClientUtil;
import org.sunbird.util.SystemConfigUtil;
import org.sunbird.util.helper.PropertiesCache;

import java.time.Duration;
import java.util.*;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ServiceFactory.class,
        Localizer.class,
        Application.class,
        SystemConfigUtil.class,
        PropertiesCache.class,
        HttpClientUtil.class
})
@PowerMockIgnore({"javax.management.*", "jdk.internal.reflect.*"})
public class GroupNotificationActorTest extends BaseActorTest {
    private final Props props = Props.create(GroupNotificationActor.class);
    public static PropertiesCache propertiesCache;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Localizer.class);
        Localizer localizer = mock(Localizer.class);
        when(Localizer.getInstance()).thenReturn(localizer);
        when(localizer.getMessage(Mockito.any(), Mockito.any())).thenReturn("");
        PowerMockito.mockStatic(SystemConfigUtil.class);
        PowerMockito.mockStatic(PropertiesCache.class);
        propertiesCache = mock(PropertiesCache.class);
        when(PropertiesCache.getInstance()).thenReturn(propertiesCache);
        when(PropertiesCache.getInstance().getProperty(JsonKey.MAX_BATCH_LIMIT))
                .thenReturn("4");
        when(PropertiesCache.getInstance().getProperty(JsonKey.ENABLE_TENANT_CONFIGURATION))
                .thenReturn("custchannel,tc");

        PowerMockito.mockStatic(HttpClientUtil.class);
        when(HttpClientUtil.post(Mockito.anyString(), Mockito.any(), Mockito.anyMap(), Mockito.anyMap())).thenReturn(getUserDetails());
    }


    @Test
    public void testMemberAddNotification() {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);
        PowerMockito.mockStatic(ServiceFactory.class);
        Request reqObj = memberAddNotificationReq();
        subject.tell(reqObj, probe.getRef());
        Response res = probe.expectMsgClass(Duration.ofSeconds(30), Response.class);
        Assert.assertTrue(null != res && res.getResponseCode() == 200);
    }

    @Test
    public void testMemberRemoveNotification() {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);
        PowerMockito.mockStatic(ServiceFactory.class);
        Request reqObj = memberRemoveNotificationReq();
        subject.tell(reqObj, probe.getRef());
        Response res = probe.expectMsgClass(Duration.ofSeconds(50), Response.class);
        Assert.assertTrue(null != res && res.getResponseCode() == 200);
    }

    @Test
    public void testMemberExitNotification() {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);
        PowerMockito.mockStatic(ServiceFactory.class);
        Request reqObj = memberExitNotificationReq();
        subject.tell(reqObj, probe.getRef());
        Response res = probe.expectMsgClass(Duration.ofSeconds(50), Response.class);
        Assert.assertTrue(null != res && res.getResponseCode() == 200);
    }


    @Test
    public void testActivityAddedNotification() {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);
        PowerMockito.mockStatic(ServiceFactory.class);
        Request reqObj = activityAddNotificationReq();
        subject.tell(reqObj, probe.getRef());
        Response res = probe.expectMsgClass(Duration.ofSeconds(50), Response.class);
        Assert.assertTrue(null != res && res.getResponseCode() == 200);
    }

    @Test
    public void testActivityRemoveNotification() {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);
        PowerMockito.mockStatic(ServiceFactory.class);
        Request reqObj = activityRemoveNotificationReq();
        subject.tell(reqObj, probe.getRef());
        Response res = probe.expectMsgClass(Duration.ofSeconds(50), Response.class);
        Assert.assertTrue(null != res && res.getResponseCode() == 200);
    }

    @Test
    public void testGroupDeleteNotification() {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);
        PowerMockito.mockStatic(ServiceFactory.class);
        Request reqObj = groupDeleteNotificationReq();
        subject.tell(reqObj, probe.getRef());
        Response res = probe.expectMsgClass(Duration.ofSeconds(50), Response.class);
        Assert.assertTrue(null != res && res.getResponseCode() == 200);
    }

    private Map<String, Object> memberAddReq() {
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put(JsonKey.GROUP_ID, "1234");
        Map<String, Object> memberOp = new HashMap<>();
        List<Map<String, Object>> members = new ArrayList<>();
        Map<String, Object> member = new HashMap<>();
        member.put(JsonKey.USER_ID, "12");
        member.put(JsonKey.ROLE, JsonKey.MEMBER);
        members.add(member);
        memberOp.put(JsonKey.ADD, members);
        reqMap.put(JsonKey.MEMBERS, memberOp);
        return reqMap;
    }

    private Request memberAddNotificationReq() {
        Request request = new Request();
        Map<String, Object> reqMap = memberAddReq();
        Map<String, Object> groupDetails = getGroupDetails();
        List<MemberResponse> members = getGroupMembers();
        Map<String, Object> reqObj = new HashMap<>();
        reqObj.put(JsonKey.REQUEST, reqMap);
        reqObj.put(JsonKey.GROUP, groupDetails);
        reqObj.put(JsonKey.MEMBERS, members);
        request.setRequest(reqObj);
        Map<String, Object> context = new HashMap<>();
        context.put(JsonKey.USER_ID, "12");
        context.put(JsonKey.MANAGED_FOR, "12");
        request.setContext(context);
        request.setOperation(NotificationType.MEMBER_UPDATE);
        return request;
    }

    private Request groupDeleteNotificationReq() {
        Request request = new Request();
        Map<String, Object> reqMap = groupDeleteReq();
        Map<String, Object> groupDetails = getGroupDetails();
        List<MemberResponse> members = getGroupMembers();
        Map<String, Object> reqObj = new HashMap<>();
        reqObj.put(JsonKey.REQUEST, reqMap);
        reqObj.put(JsonKey.GROUP, groupDetails);
        reqObj.put(JsonKey.MEMBERS, members);
        request.setRequest(reqObj);
        Map<String, Object> context = new HashMap<>();
        context.put(JsonKey.USER_ID, "12");
        context.put(JsonKey.MANAGED_FOR, "12");
        request.setContext(context);
        request.setOperation(NotificationType.GROUP_DELETE);
        return request;
    }

    private Request memberRemoveNotificationReq() {
        Request request = new Request();
        Map<String, Object> reqMap = memberRemoveReq();
        Map<String, Object> groupDetails = getGroupDetails();
        List<MemberResponse> members = getGroupMembers();
        Map<String, Object> reqObj = new HashMap<>();
        reqObj.put(JsonKey.REQUEST, reqMap);
        reqObj.put(JsonKey.GROUP, groupDetails);
        reqObj.put(JsonKey.MEMBERS, members);
        request.setRequest(reqObj);

        Map<String, Object> context = new HashMap<>();
        context.put(JsonKey.USER_ID, "12");
        context.put(JsonKey.MANAGED_FOR, "12");
        request.setContext(context);
        request.setOperation(NotificationType.MEMBER_UPDATE);
        return request;
    }

    private Request memberExitNotificationReq() {
        Request request = new Request();
        Map<String, Object> reqMap = memberExitReq();
        Map<String, Object> groupDetails = getGroupDetails();
        List<MemberResponse> members = getGroupMembers();
        Map<String, Object> reqObj = new HashMap<>();
        reqObj.put(JsonKey.REQUEST, reqMap);
        reqObj.put(JsonKey.GROUP, groupDetails);
        reqObj.put(JsonKey.MEMBERS, members);
        request.setRequest(reqObj);

        Map<String, Object> context = new HashMap<>();
        context.put(JsonKey.USER_ID, "12");
        context.put(JsonKey.MANAGED_FOR, "12");
        request.setContext(context);
        request.setOperation(NotificationType.MEMBER_UPDATE);
        return request;
    }

    private Map<String, Object> memberRemoveReq() {
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put(JsonKey.GROUP_ID, "1234");
        Map<String, Object> memberOp = new HashMap<>();
        List<String> members = new ArrayList<>();
        members.add("2341");
        memberOp.put(JsonKey.REMOVE, members);
        reqMap.put(JsonKey.MEMBERS, memberOp);
        return reqMap;
    }

    private Map<String, Object> memberExitReq() {
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put(JsonKey.GROUP_ID, "1234");
        Map<String, Object> memberOp = new HashMap<>();
        List<String> members = new ArrayList<>();
        members.add("12");
        memberOp.put(JsonKey.REMOVE, members);
        reqMap.put(JsonKey.MEMBERS, memberOp);
        return reqMap;
    }

    private Map<String, Object> groupDeleteReq() {
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put(JsonKey.GROUP_ID, "1234");
        return reqMap;
    }

    private Map<String, Object> getGroupDetails() {
        Map<String, Object> dbResGroup = new HashMap<>();
        dbResGroup.put(JsonKey.ID, "1234");
        dbResGroup.put(JsonKey.NAME, "Test");
        List<Map<String, Object>> activities = new ArrayList<>();
        Map<String, Object> activity = new HashMap<>();
        activity.put(JsonKey.ID, "do_12345");
        activity.put(JsonKey.TYPE, "Course");
        activities.add(activity);
        dbResGroup.put(JsonKey.ACTIVITIES, activities);
        return dbResGroup;
    }

    private List<MemberResponse> getGroupMembers() {
        MemberResponse member1 = new MemberResponse();
        member1.setGroupId("1234");
        member1.setUserId("234");
        member1.setRole(JsonKey.ADMIN);
        MemberResponse member2 = new MemberResponse();
        member2.setGroupId("1234");
        member2.setUserId("2341");
        member2.setRole(JsonKey.MEMBER);
        List<MemberResponse> members = new ArrayList<>();
        members.add(member1);
        members.add(member2);
        return members;
    }

    private Request activityAddNotificationReq() {
        Request request = new Request();
        Map<String, Object> reqMap = activityAddReq();
        Map<String, Object> groupDetails = getGroupDetails();
        List<MemberResponse> members = getGroupMembers();
        Map<String, Object> reqObj = new HashMap<>();
        reqObj.put(JsonKey.REQUEST, reqMap);
        reqObj.put(JsonKey.GROUP, groupDetails);
        reqObj.put(JsonKey.MEMBERS, members);
        request.setRequest(reqObj);
        Map<String, Object> context = new HashMap<>();
        context.put(JsonKey.USER_ID, "12");
        context.put(JsonKey.MANAGED_FOR, "12");
        request.setContext(context);
        request.setOperation(NotificationType.ACTIVITY_UPDATE);
        return request;
    }

    private Request activityRemoveNotificationReq() {
        Request request = new Request();
        Map<String, Object> reqMap = activityRemoveReq();
        Map<String, Object> groupDetails = getGroupDetails();
        List<MemberResponse> members = getGroupMembers();
        Map<String, Object> reqObj = new HashMap<>();
        reqObj.put(JsonKey.REQUEST, reqMap);
        reqObj.put(JsonKey.GROUP, groupDetails);
        reqObj.put(JsonKey.MEMBERS, members);
        request.setRequest(reqObj);
        Map<String, Object> context = new HashMap<>();
        context.put(JsonKey.USER_ID, "12");
        context.put(JsonKey.MANAGED_FOR, "12");
        request.setContext(context);
        request.setOperation(NotificationType.ACTIVITY_UPDATE);
        return request;
    }


    private Map<String, Object> activityAddReq() {
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put(JsonKey.GROUP_ID, "1234");
        Map<String, Object> activityOp = new HashMap<>();
        List<Map<String, Object>> activities = new ArrayList<>();
        Map<String, Object> activity = new HashMap<>();
        activity.put(JsonKey.ID, "do_123457");
        activity.put(JsonKey.TYPE, "Course");
        activities.add(activity);
        activityOp.put(JsonKey.ADD, activities);
        reqMap.put(JsonKey.ACTIVITIES, activityOp);
        return reqMap;
    }

    private Map<String, Object> activityRemoveReq() {
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put(JsonKey.GROUP_ID, "1234");
        Map<String, Object> activityOp = new HashMap<>();
        List<String> activity = new ArrayList<>();
        activity.add("do_1234");
        activityOp.put(JsonKey.REMOVE, activity);
        reqMap.put(JsonKey.ACTIVITIES, activityOp);
        return reqMap;
    }


    private String getUserDetails() {
        return "{\"id\":\"api.user.search\",\"ver\":\"v1\",\"ts\":\"2021-10-14 08:07:47:366+0000\",\"params\":{\"resmsgid\":null,\"msgid\":\"7e6815596011bf8f8908ec79c827d244\",\"err\":null,\"status\":\"success\",\"errmsg\":null},\"responseCode\":\"OK\",\"result\":{\"response\":{\"count\":1,\"content\":[{\"channel\":\"custchannel\",\"id\":\"12\",\"recoveryEmail\":\"\",\"identifier\":\"12\",\"firstName\":\"satish\",\"provider\":null,\"userName\":\"satish_x6cc\",\"userId\":\"12\"},{\"channel\":\"custchannel\",\"id\":\"a10d5216-6b96-404c-8d1c-cc1f720d910a\",\"recoveryEmail\":\"\",\"identifier\":\"a10d5216-6b96-404c-8d1c-cc1f720d910a\",\"firstName\":\"satish\",\"provider\":null,\"userName\":\"satish_x6cc\",\"userId\":\"a10d5216-6b96-404c-8d1c-cc1f720d910a\"}]}}}";
    }
}
