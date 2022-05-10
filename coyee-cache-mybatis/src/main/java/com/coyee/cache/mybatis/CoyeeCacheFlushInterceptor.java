package com.coyee.cache.mybatis;

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

    public CoyeeCacheFlushInterceptor(CoyeeCacheSupport coyeeCacheSupport) {
        this.coyeeCacheSupport = coyeeCacheSupport;
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
            this.flushCacheOfTable(table);
        } else if (StringUtils.equalsIgnoreCase(commandName, "UPDATE")) {
            Table table = this.getUpdateTable(sql);
            this.flushCacheOfTable(table);
        } else if (StringUtils.equalsIgnoreCase(commandName, "DELETE")) {
            Table table = this.getDeleteTable(sql);
            this.flushCacheOfTable(table);
        }
        return result;
    }

    /**
     * 刷新缓存数据
     * @param table
     */
    private void flushCacheOfTable(Table table){
        if(table!=null){
            String tableName=StringUtils.lowerCase(table.getName());
            coyeeCacheSupport.flushChannelKeysAndCache(new String[]{tableName});
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

//    public static void main(String args[]) throws Exception {
//        CoyeeCacheFlushInterceptor bean = new CoyeeCacheFlushInterceptor(null);
//        String sql = "UPDATE pay.pay_order a\n" +
//                "  JOIN (\n" +
//                "         SELECT\n" +
//                "           t.id                                                     order_id,\n" +
//                "           b.id                                                     house_id,\n" +
//                "           ifnull(c.company_id, 'a0000000000000000000000000000000') company_id\n" +
//                "         FROM pay_order t\n" +
//                "           LEFT JOIN room b\n" +
//                "             ON t.room_id = b.id\n" +
//                "           LEFT JOIN house_company_mapping c\n" +
//                "             ON b.house_id = c.house_id\n" +
//                "\n" +
//                "         WHERE t.company_id = 'a0000000000000000000000000000000'\n" +
//                "               ) temp\n" +
//                "    ON a.id = temp.order_id\n" +
//                "SET a.company_id = temp.company_id;";
//        Table table = bean.getUpdateTable(sql);
//        System.out.println(table);
//    }
}