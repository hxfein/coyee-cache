package test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import test.config.MainConfig;
import test.service.TestService;

import java.util.Collections;

public class DemoApplication {
    static public void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfig.class);
        TestService testService = context.getBean(TestService.class);
        testService.getUserAndRoleList("id","黄",999);
        testService.getRoleList();
        testService.updateUser(Collections.emptyMap());
        testService.getRoleList();
        testService.getRoleList();
        testService.getRoleList();
        testService.getRoleList();
        testService.getRoleList();
        testService.getRoleList();
        testService.updateUser(Collections.emptyMap());
        testService.getRoleList();
        testService.getRoleList();
        testService.getRoleList();
        testService.getUserAndRoleList("name","三",888);
        testService.updateRole(Collections.emptyMap());
        testService.getUserAndRoleList("age","4",777);
//        testService.getUserAndRoleList();
    }
}