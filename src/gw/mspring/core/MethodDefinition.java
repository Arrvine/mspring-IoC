package gw.mpring.core;

import java.lang.reflect.Method;

/*
 * ��������
 *
 * */
public class MethodDefinition {
	private Method method;//��������
	private Object object;//ִ�з����Ķ���
	private int paraCount;//�����Ĳ�������
	
	public MethodDefinition() {
	}

	Method getMethod() {
		return method;
	}

	void setMethod(Method method) {
		this.method = method;
	}

	Object getObject() {
		return object;
	}

	void setObject(Object object) {
		this.object = object;
	}
	
	void setParaCount(int paraCount) {
		this.paraCount = paraCount;
	}

	//������������
	void add() {
		++this.paraCount;
	}
	//������������
	int sub() {
		return --this.paraCount;
	}
}
