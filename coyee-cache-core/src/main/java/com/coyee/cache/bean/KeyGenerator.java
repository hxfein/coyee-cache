package com.coyee.cache.bean;

/**
 * @author hxfein
 * @className: KeyGenerator
 * @description: 缓存key生成器枚举
 * @date 2022/5/6 16:13
 * @version：1.0
 */
public enum KeyGenerator {

    Signature(1, "已删除");

    private Integer code;
    private String desc;

    KeyGenerator(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
