package com.wisesoft.weixin.websocket;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;

@RestController
public class HelloController {
	//给指定用户发
    @GetMapping("/sendone/{message}/{token}")
    public String sendmessage(@PathVariable("message") String message,@PathVariable("token") String token) throws IOException {
        WebSocketController.sendMessageTo(message,token);
        return "ok";
    }

    //这个可以后台群发，所有用户都能看到
    @GetMapping("/sendall/{message}")
    public String sendmessageall(@PathVariable("message") String message) throws IOException {
        WebSocketController.sendInfo(message);
        return "ok";
    }

}
