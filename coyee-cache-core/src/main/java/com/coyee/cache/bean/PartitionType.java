package com.coyee.cache.bean;

/**
 * @author hxfein
 * @className: PartitionType
 * @description: 分区类型枚举
 * @date 2022/5/6 16:13
 * @version：1.0
 */
public enum PartitionType {

    MASTER(0, "主分区"),
    NORMAL(1, "普通分区"),
    ;

    private Integer code;
    private String desc;

    PartitionType(Integer code, String desc) {
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
