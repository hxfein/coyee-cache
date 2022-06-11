package test.service;

import com.coyee.cache.annotation.Flush;

import java.util.List;
import java.util.Map;

/**
 * @author hxfein
 * @className: TestService
 * @description:
 * @date 2022/4/26 15:53
 * @version：1.0
 */
public interface TestService {
    List<Map<String,Object>> getUserAndRoleList(String orderBy,String keyword,int limit);
    List<Map<String,Object>> getRoleList(String keyword);
    void updateUser(Map<String, Object> bean);
    void updateRole(Map<String, Object> bean);
}
