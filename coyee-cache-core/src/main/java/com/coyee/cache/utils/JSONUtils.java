package com.coyee.cache.utils;


import com.alibaba.fastjson.JSONObject;
import com.coyee.cache.exception.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hxfein
 * @className: JSONUtils
 * @description: json工具类
 * @date 2022/4/27 16:09
 * @version：1.0
 */
public class JSONUtils {
	
	private static Logger logger = LoggerFactory.getLogger(JSONUtils.class);
 
	/**
	 * 对象转为JSON字符串
	 * @param object
	 * @return
	 */
	public static String objectToString(Object object) {
		try {
			String jsonString = JSONObject.toJSONString(object);
			return jsonString;
		} catch (Exception e) {
			throw new CacheException("转换json出错",e);
		}
	}

	/**
	 * Json字符串解析为Json对象
	 * @param object
	 * @return
	 */
	public static String stringToObject(Object object) {
		try {
			String jsonString = JSONObject.toJSONString(object);
			return jsonString;
		} catch (Exception e) {
			throw new CacheException("转换json出错",e);
		}
	}
 
}