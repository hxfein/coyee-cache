package test.service;

import com.coyee.cache.annotation.Cache;
import com.coyee.cache.annotation.Flush;
import org.springframework.stereotype.Service;

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
    @Cache(channels = {"user","role"},key="''.concat(#orderBy).concat(#keyword).concat(#limit)")
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

    @Cache(channels = {"role"})
    public List<Map<String, Object>> getRoleList() {
        List<Map<String,Object>> list=new ArrayList<>();
        Map<String,Object> bean=new HashMap<>();
        bean.put("roleName","开发人员");
        bean.put("roleId","1");
        list.add(bean);
        System.err.println("从数据库获取角色数据:"+System.currentTimeMillis());
        return list;
    }


    @Override
    @Flush(channels={"user"})
    public void updateUser(Map<String, Object> bean) {
        System.out.println("更新用户数据:"+System.currentTimeMillis());
    }

    @Override
    @Flush(channels={"role"})
    public void updateRole(Map<String, Object> bean) {
        System.out.println("更新角色数据:"+System.currentTimeMillis());
    }
}
