package com.iamnotme.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * 快速创建map并放入数据
 * @author wangjianbo
 *
 */
public class Map2 extends HashMap<String, Object> implements Map<String, Object>{
	private static String EXP=".+\\[[0-9]\\]";
	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		if(m!=null){
			super.putAll(m);
		}
	}

	private static final long serialVersionUID = 1L;
	private Map2(){}
	public static final Map2 instance(){
		return new Map2();
	}
	@SuppressWarnings("unchecked")
	public static final Map2 instance(Object obj) throws Exception{
		Map2 p=instance();
		if(obj instanceof Map){
			p.putAll((Map<String, Object>)obj);
		}
		return p;
	}
	
	public Map2 put(String key,Object value){
		super.put(key, value);
		return this;
	}
	/* (non-Javadoc)
	 * @see java.util.HashMap#get(java.lang.Object)
	 */
	@Override
	public Object get(Object key) {
		if(key instanceof String){
			String[] arr=((String)key).split(".");
			for(int i=0;i<arr.length;i++){
				String aKey=arr[i];
				List<Map<String,Object>> list;
				Map<String,Object> map;
				if(i<arr.length-1){
					if(aKey.matches(EXP)){
					}else{
						
					}
				}
			}
		}
		return super.get(key);
	}
	
}
