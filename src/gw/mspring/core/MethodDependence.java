package gw.mpring.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * @author Arrvine
 * 两个List
 * 一个存储参数不满足的方法：uninvokeMethodList
 * 一个存储满足参数，可执行的方法：invokeableMethodList
 * 存储暂时不存在的参数类型：dependenceMethodPool 
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
	 * 当遇到参数不满足的方法时：
	 * 当发现方法的参数不满足时，需要把方法放到uninvokeMethodList，
	 * 并将不满足的参数类型放到dependenceMethodPool
	 * 		如果pool里不存在该参数类型，创建一个以该类型为key,value暂时为空的键值对
	 * 存在，根据该参数类型获取对应的方法列表，把该方法加入列表中
	 */
	static void addUninvokeMethod(MethodDefinition methodDefinition,
			Map<Class<?>, Integer> paraTypePool) {
		
		uninvokeMethodList.add(methodDefinition);
		
		for (Class<?> paraType : paraTypePool.keySet()) {
			//判断参数类型是否在Pool中，
			//如不包含，增加一个键值对
			if (!dependenceMethodPool.containsKey(paraType)) {
				List<MethodDefinition> methodList = new ArrayList<>();
				dependenceMethodPool.put(paraType, methodList);
			}
			//包含则从Pool获取methodDefination，加到methodList中
			List<MethodDefinition> methodList = dependenceMethodPool.get(paraType);
			methodList.add(methodDefinition);
		}
	}
	
	/*
	 * 检查依赖：
	 *			每次只要产生bean,即向beanPool中进行put，就要执行检查依赖操作
	 * 根据类型获取map中的mdList
	 * 遍历列表 ,判断列表中方法的参数个数是否为0
	 * 若为0，及说明该方法需要的所有参数均已满足，将其加入okMethodList
	 * （okMethodList为中间临时存储可执行方法的 列表）
	 * 	若okMethodList不为空，将该方法从未满足列表删除，加入可执行列表
	 */
	static void checkDependence(Class<?> beanClass) {
		List<MethodDefinition> mdList = dependenceMethodPool.get(beanClass);
		if (mdList == null) {
			return;
		}
		List<MethodDefinition> okMethodList = new ArrayList<>();
		for (MethodDefinition md : mdList) {
			//此处调用sub(),是因为之前的操作确定了beanPool中
			//新增了一个之前不存在的参数类型，现在找到了该参数类型，
			//那么该参数类型对应的方法的参数个数就需要减1
			if (md.sub() == 0) {
				okMethodList.add(md);
			}
		}
		//若okMethodList不为空，说明该方法可执行
		//将该方法从uninvokeMethodList删除，加入invokeableMethodList
		if (!okMethodList.isEmpty()) {
			for (MethodDefinition method : okMethodList) {
				uninvokeMethodList.remove(method);
				invokeableMethodList.add(method);
			}
		}	
		dependenceMethodPool.remove(beanClass);//删除pool中的键
	}
	
	//执行invokeableMethodList列表中方法
	static void invokeDependenceMethod() {
		//每次执行“可执行列表”中方法，每次执行一个，删除一个方法，直到列表中没有方法
		while (!invokeableMethodList.isEmpty()) {
			MethodDefinition methodDefinition = invokeableMethodList.get(0);
			Object object = methodDefinition.getObject();
			Class<?> klass = object.getClass();
			Method method = methodDefinition.getMethod();
			invokeableMethodList.remove(0);//只删除第一个方法
			
			BeanFactory.invokeMethodWithPara(klass, object, method);
		}
	}
	
	//输出dependenceMethodPool中最后残留的方法和其参数类型
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
