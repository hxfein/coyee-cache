package test.service;

import com.coyee.cache.annotation.Cache;
import com.coyee.cache.annotation.Flush;
import com.coyee.cache.bean.Data;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hxfein
 * @className: TestServiceImpl
 * @description:
 * @date 2022/4/26 15:53
 * @version：1.0
 */
@Service
public class TestServiceImpl implements TestService{
    @Override
    @Cache(channels = {"user","role","a","b","c","d","e","f","g","h","i","j","k","m"},key="''.concat(#orderBy).concat(#keyword).concat(#limit)")
    public List<Map<String, Object>> getUserAndRoleList(String orderBy,String keyword,int limit) {
        List<Map<String,Object>> list=new ArrayList<>();
        Map<String,Object> bean=new HashMap<>();
        bean.put("username","huangfei");
        bean.put("nickname","黄飞");
        bean.put("roleName","开发人员");
        list.add(bean);
        System.err.println("从数据库获取用户数据和角色数据:"+System.currentTimeMillis());
        return list;
    }

    public void getRoleList$Before(Object[] params){
        System.out.println("执行前置方法，更新浏览量数据");
    }

    public Serializable getRoleList$After(Object[] params, Serializable result){
        List<Map<String,Object>> roleList=(List<Map<String,Object>>)result;
        roleList.stream().forEach((item)->{
            item.put("timestamp",System.currentTimeMillis());
        });
        return result;
    }

    @Cache(channels = {"user"},beforeExec = "getRoleList$Before",afterExec = "getRoleList$After")
    public List<Map<String, Object>> getRoleList(String keyword) {
        List<Map<String,Object>> list=new ArrayList<>();
        Map<String,Object> bean=new HashMap<>();
        bean.put("roleName","开发人员");
        bean.put("roleId","1");
        list.add(bean);
        System.err.println("从数据库获取角色数据:"+System.currentTimeMillis());
        return list;
    }


    @Override
    @Flush(channels={"user","role","a","b","c","d","e","f","g","h","i","j","k","m"})
    public void updateUser(Map<String, Object> bean) {
        System.out.println("更新用户数据:"+System.currentTimeMillis());
    }

    @Override
    @Flush(channels={"user","role","a","b","c","d","e","f","g","h","i","j","k","m"})
    public void updateRole(Map<String, Object> bean) {
        System.out.println("更新角色数据:"+System.currentTimeMillis());
    }


}
