package gw.mpring.core;

import java.lang.reflect.Method;

/*
 * 方法定义
 *
 * */
public class MethodDefinition {
	private Method method;//方法本身
	private Object object;//执行方法的对象
	private int paraCount;//方法的参数个数
	
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

	//参数个数增加
	void add() {
		++this.paraCount;
	}
	//参数个数减少
	int sub() {
		return --this.paraCount;
	}
}
