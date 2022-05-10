package com.coyee.cache.bean;

import java.io.Serializable;

/**
 * @author hxfein
 * @className: Partition
 * @description: 分区信息
 * @date 2022/5/6 16:40
 * @version：1.0
 */
public class Partition implements Serializable {
    public static final String MASTER="master";
    /**
     * 分区类型
     */
    private PartitionType partitionType;
    /**
     * 分区关键字
     */
    private String partitionKey=MASTER;


    public PartitionType getPartitionType() {
        return partitionType;
    }

    public void setPartitionType(PartitionType partitionType) {
        this.partitionType = partitionType;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }
}
