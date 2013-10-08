package com.wsf_lp.utils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class MethodUtil {

	/**
	 * クラスからメソッドを列挙し、メソッド名からのマップを作成する
	 * 同じ名前のメソッドが複数あってはいけない
	 * @param clazz クラス
	 * @return メソッド名をキー、メソッドインスタンスを値とするマップ
	 */
	public static Map<String, Method> createMethodMap(Class<?> clazz) throws IllegalArgumentException {
		final Method[] methods = clazz.getMethods();
		Map<String, Method> map = new HashMap<String, Method>(methods.length);
		for(Method method : methods) {
			final String name = method.getName();
			if(!method.getDeclaringClass().equals(clazz)) {
				continue;
			}
			if(map.containsKey(name)) {
				throw new IllegalArgumentException(name);
			}
			map.put(name, method);
		}
		return map;
	}

}
