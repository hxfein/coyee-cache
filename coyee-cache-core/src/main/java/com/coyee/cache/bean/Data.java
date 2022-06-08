package com.coyee.cache.bean;

import java.io.Serializable;
import java.util.Set;

/**
 * @author hxfein
 * @className: Data
 * @description: 缓存封装数据
 * @date 2022/5/31 10:11
 * @version：1.0
 */
public class Data implements Serializable {
    /**
     * 原始数据
     */
    private Serializable rawData;

    public Data(){}

    public Data(Serializable rawData){
        this.rawData=rawData;
    }

    public Serializable getRawData() {
        return rawData;
    }

    public void setRawData(Serializable rawData) {
        this.rawData = rawData;
    }

}
