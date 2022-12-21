package com.coyee.cache.support;

public class ExpressionRootObject {
    private final Object object;
    private final Object[] args;

    public ExpressionRootObject(Object object, Object[] args) {
        this.object = object;
        //转换空值
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                args[i] = "null";
            }
        }
        this.args = args;
    }

    public Object getObject() {
        return object;
    }

    public Object[] getArgs() {
        return args;
    }
}