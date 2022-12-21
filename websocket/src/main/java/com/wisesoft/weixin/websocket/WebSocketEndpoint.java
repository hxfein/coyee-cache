package com.wisesoft.weixin.websocket;

import org.springframework.stereotype.Component;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
@ServerEndpoint("/websocket/{token}")
@Component
public class WebSocketController {
    private static int onlineCount=0;//在线人数
    private static CopyOnWriteArrayList<WebSocketController> webSocketSet=new CopyOnWriteArrayList<WebSocketController>();//在线用户集合
    private Session session;//与某个客户端的连接会话
    private String currentUser;

    @OnOpen
    public void onOpen(@PathParam("token") String token, Session session){
        this.currentUser = token;
        this.session=session;
        webSocketSet.add(this);//加入set中
        addOnlineCount();
        System.out.println("有新连接加入！当前在线人数为"+getOnlineCount());
        allCurrentOnline();
    }

    @OnClose
    public void onClose(){
        webSocketSet.remove(this);
        subOnlineCount();
        System.out.println("有一连接关闭！当前在线人数为" + getOnlineCount());
        allCurrentOnline();
    }

    @OnMessage
    public void onMessage(String message,Session session){
        System.out.println("来自客户端的消息："+message);
        for (WebSocketController item:webSocketSet){
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable){
        System.out.println("发生错误！");
        throwable.printStackTrace();
    }

    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    /**
     * 获取当前所有在线用户名
     */
    public static void allCurrentOnline(){
        for (WebSocketController item : webSocketSet) {
            System.out.println(item.currentUser);
        }
    }

    /**
     * 发送给指定用户
     */
    public static void sendMessageTo(String message,String token) throws IOException {
        for (WebSocketController item : webSocketSet) {
            if(item.currentUser.equals(token)){
                item.session.getBasicRemote().sendText(message);
            }
        }
    }

    /**
     * 群发自定义消息
     */
    public static void sendInfo(String message) throws IOException {
        System.out.println(message);
        for (WebSocketController item : webSocketSet) {
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                continue;
            }
        }
    }

    public static synchronized int getOnlineCount(){
        return onlineCount;
    }
    public static synchronized void addOnlineCount(){
        WebSocketController.onlineCount++;
    }
    public static synchronized void subOnlineCount(){
        WebSocketController.onlineCount--;
    }

}

