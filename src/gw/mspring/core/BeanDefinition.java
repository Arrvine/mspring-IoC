package gw.mpring.core;

//封装类和对象
public class BeanDefinition {
	private Class<?> klass;
	private Object object;
	private boolean singleton;//判断是否单例
	private boolean inject;//判断是否被注入
	
	BeanDefinition() {
		this.inject = false;
		this.singleton = true;
	}

	Class<?> getKlass() {
		return klass;
	}

	void setKlass(Class<?> klass) {
		this.klass = klass;
	}

	Object getObject() {
		return object;
	}

	void setObject(Object object) {
		this.object = object;
	}

	boolean isSingleton() {
		return singleton;
	}

	void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	boolean isInject() {
		return inject;
	}

	void setInject(boolean inject) {
		this.inject = inject;
	}
	
	
	
}
