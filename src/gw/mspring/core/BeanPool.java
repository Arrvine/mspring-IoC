package gw.mpring.core;

import java.util.HashMap;
import java.util.Map;

public class BeanPool {
	private static final Map<String, BeanDefinition> classBeanPool
			= new HashMap<>();
	private static final Map<String, BeanDefinition> aliasBeanPool
	= new HashMap<>();
	
	static void put(String key, BeanDefinition bean) {
		Class<?> beanClass = bean.getKlass();
		String beanClassName = beanClass.getName();
		if (beanClassName.equals(key)) {
			classBeanPool.put(key, bean);
		} else {
			aliasBeanPool.put(key, bean);
		}
	}
	
	static BeanDefinition get(String key) {
		BeanDefinition bean = classBeanPool.get(key);
		if (bean == null) {
			bean = aliasBeanPool.get(key);
		}
		
		return bean;
	}
}
