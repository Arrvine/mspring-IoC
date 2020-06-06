package gw.mpring.core;

//��װ��Ͷ���
public class BeanDefinition {
	private Class<?> klass;
	private Object object;
	private boolean singleton;//�ж��Ƿ���
	private boolean inject;//�ж��Ƿ�ע��
	
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
