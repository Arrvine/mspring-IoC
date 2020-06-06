package gw.mpring.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * @author Arrvine
 * ����List
 * һ���洢����������ķ�����uninvokeMethodList
 * һ���洢�����������ִ�еķ�����invokeableMethodList
 * �洢��ʱ�����ڵĲ������ͣ�dependenceMethodPool 
 */
public class MethodDependence {
	private static final List<MethodDefinition> uninvokeMethodList
			= new ArrayList<MethodDefinition>();
	private static final List<MethodDefinition> invokeableMethodList
			= new LinkedList<MethodDefinition>();
	private static final Map<Class<?>, List<MethodDefinition>>
			dependenceMethodPool = new HashMap<Class<?>, List<MethodDefinition>>();

	MethodDependence() {
	}
	
	/*
	 * ����������������ķ���ʱ��
	 * �����ַ����Ĳ���������ʱ����Ҫ�ѷ����ŵ�uninvokeMethodList��
	 * ����������Ĳ������ͷŵ�dependenceMethodPool
	 * 		���pool�ﲻ���ڸò������ͣ�����һ���Ը�����Ϊkey,value��ʱΪ�յļ�ֵ��
	 * ���ڣ����ݸò������ͻ�ȡ��Ӧ�ķ����б��Ѹ÷��������б���
	 */
	static void addUninvokeMethod(MethodDefinition methodDefinition,
			Map<Class<?>, Integer> paraTypePool) {
		
		uninvokeMethodList.add(methodDefinition);
		
		for (Class<?> paraType : paraTypePool.keySet()) {
			//�жϲ��������Ƿ���Pool�У�
			//�粻����������һ����ֵ��
			if (!dependenceMethodPool.containsKey(paraType)) {
				List<MethodDefinition> methodList = new ArrayList<>();
				dependenceMethodPool.put(paraType, methodList);
			}
			//�������Pool��ȡmethodDefination���ӵ�methodList��
			List<MethodDefinition> methodList = dependenceMethodPool.get(paraType);
			methodList.add(methodDefinition);
		}
	}
	
	/*
	 * ���������
	 *			ÿ��ֻҪ����bean,����beanPool�н���put����Ҫִ�м����������
	 * �������ͻ�ȡmap�е�mdList
	 * �����б� ,�ж��б��з����Ĳ��������Ƿ�Ϊ0
	 * ��Ϊ0����˵���÷�����Ҫ�����в����������㣬�������okMethodList
	 * ��okMethodListΪ�м���ʱ�洢��ִ�з����� �б�
	 * 	��okMethodList��Ϊ�գ����÷�����δ�����б�ɾ���������ִ���б�
	 */
	static void checkDependence(Class<?> beanClass) {
		List<MethodDefinition> mdList = dependenceMethodPool.get(beanClass);
		if (mdList == null) {
			return;
		}
		List<MethodDefinition> okMethodList = new ArrayList<>();
		for (MethodDefinition md : mdList) {
			//�˴�����sub(),����Ϊ֮ǰ�Ĳ���ȷ����beanPool��
			//������һ��֮ǰ�����ڵĲ������ͣ������ҵ��˸ò������ͣ�
			//��ô�ò������Ͷ�Ӧ�ķ����Ĳ�����������Ҫ��1
			if (md.sub() == 0) {
				okMethodList.add(md);
			}
		}
		//��okMethodList��Ϊ�գ�˵���÷�����ִ��
		//���÷�����uninvokeMethodListɾ��������invokeableMethodList
		if (!okMethodList.isEmpty()) {
			for (MethodDefinition method : okMethodList) {
				uninvokeMethodList.remove(method);
				invokeableMethodList.add(method);
			}
		}	
		dependenceMethodPool.remove(beanClass);//ɾ��pool�еļ�
	}
	
	//ִ��invokeableMethodList�б��з���
	static void invokeDependenceMethod() {
		//ÿ��ִ�С���ִ���б��з�����ÿ��ִ��һ����ɾ��һ��������ֱ���б���û�з���
		while (!invokeableMethodList.isEmpty()) {
			MethodDefinition methodDefinition = invokeableMethodList.get(0);
			Object object = methodDefinition.getObject();
			Class<?> klass = object.getClass();
			Method method = methodDefinition.getMethod();
			invokeableMethodList.remove(0);//ֻɾ����һ������
			
			BeanFactory.invokeMethodWithPara(klass, object, method);
		}
	}
	
	//���dependenceMethodPool���������ķ��������������
	static String getUndependence() {
		StringBuffer str = new StringBuffer();
		
		for (Class<?> dependenceClass : dependenceMethodPool.keySet()) {
			List<MethodDefinition> mdList = dependenceMethodPool.get(dependenceClass);
			for (MethodDefinition md : mdList) {
				str.append(md.getMethod())
				.append("  --> ").append(dependenceClass.getName())
				.append('\n');
			}
		}
		
		return str.toString();
	}
	
}
