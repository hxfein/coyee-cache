package test.controller;

import org.apache.commons.codec.binary.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import test.service.TestService;

import javax.annotation.Resource;
import java.util.Collections;

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

    @RequestMapping("/updateUserAndRoleList")
    @ResponseBody
    public String updateUserAndRoleList(){
        testService.getUserAndRoleList("id","黄",999);
        return "success";
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

    @RequestMapping("/getUserAndRoleList")
    @ResponseBody
    public String getUserAndRoleList(String keyword){
        testService.getUserAndRoleList("",keyword,10);
        return "success";
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
}
