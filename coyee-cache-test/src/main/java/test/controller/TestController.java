package test.controller;

import com.alibaba.fastjson.JSON;
import com.coyee.cache.template.AbstractRedisCacheTemplate;
import com.coyee.cache.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.scripting.support.StaticScriptSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import test.service.TestService;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author hxfein
 * @className: TestController
 * @description:
 * @date 2022/6/2 17:21
 * @version：1.0
 */
@Controller
public class TestController {
    @Resource
    private TestService testService;
    @Resource
    private RedisTemplate<String,Object> redisTemplate;
    @Resource
    private AbstractRedisCacheTemplate template;


    @RequestMapping("/updateUserAndRoleList")
    @ResponseBody
    public String updateUserAndRoleList(){
        testService.getUserAndRoleList("id","黄",999);
        return "success";
    }

    @RequestMapping("/getDbIndex")
    @ResponseBody
    public String getDbIndex(){
        long dbIndex = template.getDbIndex();
        return "success:"+dbIndex;
    }

    @RequestMapping("/updateRole")
    @ResponseBody
    public String updateRole(){
        testService.updateRole(Collections.emptyMap());
        return "success";
    }

    @RequestMapping("/updateUser")
    @ResponseBody
    public String updateUser(){
        testService.updateUser(Collections.emptyMap());
        return "success";
    }


    @RequestMapping("/getRoleList")
    @ResponseBody
    public List<Map<String,Object>> getRoleList(String keyword){
        return testService.getRoleList(keyword);
    }

    @RequestMapping("/getUserAndRoleList")
    @ResponseBody
    public List<Map<String,Object>> getUserAndRoleList(String keyword){
        return testService.getUserAndRoleList("",keyword,10);
    }





    @RequestMapping("/test2")
    @ResponseBody
    public String test2(String type){
        try {
            redisTemplate.execute(new SessionCallback<Object>() {
                @Override
                public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                    redisTemplate.setEnableTransactionSupport(true);
                    redisTemplate.multi();
                    try {
                        redisTemplate.opsForValue().set("test:test1", "111");
                        redisTemplate.opsForValue().set("test:test2", "222");
                        if (StringUtils.equals(type,"error")) {
                            throw new RuntimeException("111111");
                        }
                        redisTemplate.opsForValue().set("test:test3", "3333");
                        redisTemplate.exec();
                    }catch(Exception er){
                        er.printStackTrace();
                        redisTemplate.discard();
                    }
                    return null;
                }
            });
            return "success";
        }catch(Exception er){
            er.printStackTrace();
            return "error";
        }
    }

    @RequestMapping("/test3")
    @ResponseBody
    public String test3(){
        for(int i=0;i<10000;i++){
            testService.getUserAndRoleList("",Math.random()+"",10);
            if(Math.random()*100%2==0){
                testService.updateRole(Collections.emptyMap());
            }else{
                testService.updateUser(Collections.emptyMap());
            }
        }
        return "success";
    }

    @RequestMapping("/testLua")
    @ResponseBody
    public String testLua(){
        DefaultRedisScript<String> script=new DefaultRedisScript<>();
        script.setResultType(String.class);
        ResourceScriptSource scriptSource=new ResourceScriptSource(new ClassPathResource("test.lua"));
        script.setScriptSource(scriptSource);
        List<String> keys=new ArrayList<>();
        keys.add("coyee_cache");
        keys.add("role");
        String result=redisTemplate.execute(script,keys);
        return "result:"+result;
    }

    @RequestMapping("/testLua2")
    @ResponseBody
    public String testLua2(){
        DefaultRedisScript<String> script=new DefaultRedisScript<>();
        script.setResultType(String.class);
        StaticScriptSource scriptSource=new StaticScriptSource("local result=redis.call('smembers','testKey');local flag='';for k1,v1 in ipairs(result) do flag=flag..k1..'='..v1..','; end return '\"'..flag..'\"';");
        script.setScriptSource(scriptSource);
        List<String> keys=new ArrayList<>();
        String result=redisTemplate.execute(script,keys);
        return "result:"+result;
    }

    @RequestMapping("/testAdd2")
    @ResponseBody
    public String testAdd(){
        DefaultRedisScript<String> script=new DefaultRedisScript<>();
        script.setResultType(String.class);
        StaticScriptSource scriptSource=new StaticScriptSource("redis.call('sadd','testKey',KEYS[1],KEYS[2]);");
        script.setScriptSource(scriptSource);
        List<String> keys=new ArrayList<>();
        keys.add("test1");
        keys.add("test2");
        String result=redisTemplate.execute(script,keys);
        return "result:"+result;
    }

    @RequestMapping("/testLua3")
    @ResponseBody
    public String testLua3(){
        redisTemplate.opsForValue().set("testKey","testValue");
        DefaultRedisScript<String> script=new DefaultRedisScript<>();
        script.setResultType(String.class);
        StaticScriptSource scriptSource=new StaticScriptSource("local result=redis.call('get','testKey');return result;");
        script.setScriptSource(scriptSource);
        List<String> keys=new ArrayList<>();
        String result=redisTemplate.execute(script,keys);
        return "result:"+result;
    }

    @Resource
    private AmqpTemplate amqpTemplate;

    @RequestMapping("/send")
    @ResponseBody
    public String send(String exchange,String routingKey,String msg){
//        Message message=new Message(msg.getBytes(StandardCharsets.UTF_8));
//        message.getMessageProperties().setExpiration("30000");
//        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
//        amqpTemplate.send("testExchange",routingKey,message);
//        amqpTemplate.convertAndSend("testExchange","test",msg);
//        amqpTemplate.convertAndSend(routingKey,msg);
        exchange= StringUtils.defaultIfBlank(exchange,"testExchange");
        amqpTemplate.convertAndSend(exchange, routingKey, msg, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().setHeader("x-delay", 1000*10);
                return message;
            }
        });
        return "success";
    }

}
