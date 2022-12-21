package com.coyee.cache.mybatis;

import com.coyee.cache.exception.CacheException;
import com.coyee.cache.support.CoyeeCacheSupport;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 拦截mybatis底层执行的SQL，刷新相应栏目的缓存
 * 遗留问题：存在刷新缓存后数据库事务发生回滚，导致缓存意外失效问题
 */
@Intercepts(value = {
        @Signature(type = Executor.class,
                method = "update",
                args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class,
                method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                        CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class,
                method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
public class CoyeeCacheFlushInterceptor implements Interceptor {
    private static Log log = LogFactory.getLog(CoyeeCacheFlushInterceptor.class);

    private CoyeeCacheSupport coyeeCacheSupport;
    /**
     * 关注的数据表<表名，相关栏目>,其中表名全部小写,栏目名区分大小写
     */
    private Map<String, Set<String>> tableAndChannels = new HashMap<>();

    /**
     * 初始化拦截器
     *
     * @param coyeeCacheSupport 缓存操作类
     * @param channelAndTables  关注变化的数据表,格式为<栏目，相关表名>
     */
    public CoyeeCacheFlushInterceptor(CoyeeCacheSupport coyeeCacheSupport, Map<String, Set<String>> channelAndTables) {
        this.coyeeCacheSupport = coyeeCacheSupport;
        if (channelAndTables != null) {
            channelAndTables.forEach((channel, tables) -> {
                tables.forEach((table) -> {
                    String tableName = StringUtils.lowerCase(table);
                    Set<String> channels = tableAndChannels.get(tableName);
                    if (channels == null) {
                        channels = new HashSet<>();
                        tableAndChannels.put(tableName, channels);
                    }
                    channels.add(channel);
                });
            });
        }
    }

    /**
     * 初始化拦截器
     *
     * @param coyeeCacheSupport 缓存操作类
     * @param channelAndTables  关注变化的数据表,格式为: 栏目:表名1,表名2,表名3。。。
     */
    public CoyeeCacheFlushInterceptor(CoyeeCacheSupport coyeeCacheSupport, List<String> channelAndTables) {
        this.coyeeCacheSupport = coyeeCacheSupport;
        if (channelAndTables != null) {
            channelAndTables.forEach((configString) -> {
                String configArr[] = StringUtils.split(configString, ":");
                if (configArr.length != 2) {
                    throw new CacheException("配置:[" + configString + "不符合配置规范!]");
                }
                String channel = configArr[0];
                String tableString = configArr[1];
                String tables[] = StringUtils.split(tableString, ",");
                Arrays.stream(tables).forEach((table) -> {
                    String tableName = StringUtils.lowerCase(table);
                    Set<String> channels = tableAndChannels.get(tableName);
                    if (channels == null) {
                        channels = new HashSet<>();
                        tableAndChannels.put(tableName, channels);
                    }
                    channels.add(channel);
                });
            });
        }
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object target = invocation.getTarget();
        Object result = invocation.proceed();
        if (target instanceof Executor == false) {
            return result;
        }
        final Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameterObject = args[1];
        BoundSql boundSql = ms.getBoundSql(parameterObject);
        String commandName = ms.getSqlCommandType().name();
        String sql = boundSql.getSql();
        if (StringUtils.equalsIgnoreCase(commandName, "INSERT")) {
            Table table = this.getInsertTable(sql);
            this.flushCacheOfTable(invocation, table);
        } else if (StringUtils.equalsIgnoreCase(commandName, "UPDATE")) {
            Table table = this.getUpdateTable(sql);
            this.flushCacheOfTable(invocation, table);
        } else if (StringUtils.equalsIgnoreCase(commandName, "DELETE")) {
            Table table = this.getDeleteTable(sql);
            this.flushCacheOfTable(invocation, table);
        }
        return result;
    }

    /**
     * 刷新缓存数据
     *
     * @param table
     */
    private void flushCacheOfTable(Invocation invocation, Table table) {
        if (table == null || invocation == null) {
            return;
        }
        final Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        String methodId = ms.getId();
        String tableName = StringUtils.lowerCase(table.getName());
        if (tableAndChannels.containsKey(tableName)) {
            if (log.isDebugEnabled()) {
                log.debug("准备刷新[" + tableName + "]相关的缓存");
            }
            Set<String> channels = tableAndChannels.get(tableName);
            if (channels != null) {
                String[] channelArray = channels.stream().toArray(String[]::new);
                com.coyee.cache.annotation.Update update = this.getUpdateAnnotation(invocation);
                if (update != null && update.cache() == false) {//不更新缓存
                    log.info("mapper方法[" + methodId + "]标记了不更新缓存，忽略。。。");
                } else {
                    coyeeCacheSupport.flushChannelKeysAndCache(channelArray);
                }
            }
        }
    }

    /**
     * 获取mapper方法上的update注解,这里假设mapper中不存在重复的方法
     *
     * @param invocation
     * @return
     */
    private com.coyee.cache.annotation.Update getUpdateAnnotation(Invocation invocation) {
        final Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        String methodId = ms.getId();
        try {
            String className = StringUtils.substringBeforeLast(methodId, ".");
            String methodName = StringUtils.substringAfterLast(methodId, ".");
            Class clazz = Class.forName(className);
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                String name = method.getName();
                if (StringUtils.equals(name, methodName)) {
                    com.coyee.cache.annotation.Update update = method.getDeclaredAnnotation(com.coyee.cache.annotation.Update.class);
                    return update;
                }
            }
            return null;
        } catch (Exception er) {
            throw new CacheException("未知的方法:" + methodId, er);
        }
    }

    /**
     * 获取 update 语句的表名
     *
     * @param sql
     * @return
     * @throws JSQLParserException
     */
    public Table getUpdateTable(String sql) throws JSQLParserException {
        Update model = (Update) CCJSqlParserUtil.parse(sql);
        Table table = model.getTable();
        return table;
    }

    /**
     * 获取 delete 语句的表名
     *
     * @param sql
     * @return
     * @throws JSQLParserException
     */
    public Table getDeleteTable(String sql) throws JSQLParserException {
        Delete model = (Delete) CCJSqlParserUtil.parse(sql);
        Table table = model.getTable();
        return table;
    }

    /**
     * 获取insert 语句的表名
     *
     * @param sql
     * @return
     * @throws JSQLParserException
     */
    protected Table getInsertTable(String sql) throws JSQLParserException {
        Insert model = (Insert) CCJSqlParserUtil.parse(sql);
        Table table = model.getTable();
        return table;
    }


    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }

}