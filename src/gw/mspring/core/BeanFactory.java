package gw.mpring.core;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mec.gw.util.PackageScanner;

/*
 * @author Arrvine
 */
public class BeanFactory {
	
	public BeanFactory() {
	}
	
	public static final void scanPackage(String packageName) {
		new PackageScanner() {
			@Override
			public void dealClass(Class<?> klass) {
				//klass类若为八大基本类型、注解、数组、接口、String类，则直接返回
				//Component类型的注解是否在klass类上。
				if(klass.isPrimitive()
						|| klass.isAnnotation()
						|| klass.isArray()
						|| klass.isInterface()
						|| klass == String.class
						|| !klass.isAnnotationPresent(Component.class)) {
					return;
				}
				
				//完成了对有@Componnet注解的类的实例化，并将其放到BeanPool
				try {
					Object object = null;
					Component component = klass.getAnnotation(Component.class);
					boolean singleton = component.singleton();
					
					BeanDefinition beanDefinition = new BeanDefinition();
					if(singleton) {
						object = klass.newInstance();
					}
					
					beanDefinition.setSingleton(singleton);
					beanDefinition.setKlass(klass);
					beanDefinition.setObject(object);
										
					BeanPool.put(klass.getName(), beanDefinition);
					MethodDependence.checkDependence(klass);
					
				
					dealBean(object, klass);
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				
			}
		}.scanPackage(packageName);
		//处理未完成带参Bean方法
		MethodDependence.invokeDependenceMethod();
	}
	
	/*
	 * 
	 * 获取参数类型。
	 * 1.获取方法的参数类型，对参数类型进行去重操作再装入paraPool
	 * 2.产生一个ArrayList，在BeanPool中查找是否存在该参数类型，
	 * 3.若存在，则从paraPool中删掉
	 * */
	private static Map<Class<?>,Integer> getMethodPara(Object object,Method method) {
		Map<Class<?>, Integer> paraPool = new HashMap<>();//存储BeanPool中不存在的参数
		
		Class<?>[] parameterTypes = method.getParameterTypes();
		for(int index = 0;index <parameterTypes.length;index++) {
			//HashMap中不存在相同的key值，put方法会自动调用equals方法和hashCode
			//去重；去掉类型重复的参数类型
			paraPool.put(parameterTypes[index], 0);//值目前无所谓
		}
		//Map和List不能边遍历边删除，先将其存储到klassList
		List<Class<?>> klassList = new ArrayList<Class<?>>();
		for(Class<?> type:paraPool.keySet()) {
			//此处只需要判断bean是否存在，使用get()
			BeanDefinition beanDefinition = BeanPool.get(type.getName());
			if(beanDefinition != null) {
				klassList.add(type);
			}
		}
		for(Class<?>type:klassList) {
			paraPool.remove(type);
		}
		//避免边遍历边删除
		
		return paraPool;
	}
	
	static void invokeMethodWithPara(Class<?> klass,Object object,Method method) {
		Class<?>[] paraTypes = method.getParameterTypes();//获取方法所有参数的类型
		Object[] paraValues = new Object[paraTypes.length];//生成对应参数个数的实参列表
		
		for(int index=0;index<paraTypes.length;index++) {
			Class<?> paraklass = paraTypes[index];
			//这里的getBeanObject()是一定可以取到的，
			//在该方法外层已经判断过，所有需要的参数均存储在BeanPool
			BeanDefinition beanDefinition = getBeanObject(paraklass.getName());
			paraValues[index] = beanDefinition.getObject();
		}

		try {
			//执行该方法，把该方法执行之后产生的bean也才存储在BeanPool中
			Class<?> beanClass = method.getReturnType();
			Object bean = method.invoke(object, paraValues);
			BeanDefinition beanDefinition = new BeanDefinition();
			
			beanDefinition.setKlass(beanClass);
			beanDefinition.setObject(bean);
			//注入和单例保持默认
			
			BeanPool.put(beanClass.getName(), beanDefinition);
			MethodDependence.checkDependence(beanClass);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
	}
	
	private static void dealMethodWithPara(Class<?> klass,Object object,Method method) {
		Map<Class<?>, Integer> paraTypeMap = getMethodPara(object, method);
		
		//若为空，即说明所有参数均以获取，可执行方法
		if(paraTypeMap.isEmpty()) {
			invokeMethodWithPara(klass,object, method);
			return;
		}
		
		//若不为空，说明该方法执行需要的参数不满足
		//将该方法的执行对象，方法本身和参数个数进行封装
		//加入到未执行方法列表。
		MethodDefinition methodDefinition = new MethodDefinition();
		methodDefinition.setObject(object);
		methodDefinition.setMethod(method);
		methodDefinition.setParaCount(paraTypeMap.size());
		
		MethodDependence.addUninvokeMethod(methodDefinition, paraTypeMap);
	}
	
	/*
	 * 处理带bean注解的方法,有参无参都处理
	 */
	private static void dealBean(Object object,Class<?> klass) {
		Method[] methods = klass.getDeclaredMethods();
		for(Method method:methods) {
			//是否为Bean注解的方法
			if(!method.isAnnotationPresent(Bean.class)) {
				continue;
			}			
			//处理有参数的Bean注解的方法
			if(method.getParameterCount() >0) {
				//不能直接处理带参数的Bean注解的方法
				//因为无法确定相关参数是否在BeanFactory中
				dealMethodWithPara(klass, object, method);
				continue;
			}
			
			//处理无参数的bean注解
			Class<?> returnType = method.getReturnType();//获取返回值类型
			try {
				Object	beanObject = method.invoke(object);//通过执行方法获取返回值
				
				BeanDefinition beanDefinition = new BeanDefinition();
				beanDefinition.setKlass(klass);
				beanDefinition.setObject(beanObject);
				//默认，可以不设置
				//beanDefinition.setSingleton(true);
				//beanDefinition.setInject(false);
				
				BeanPool.put(returnType.getName(), beanDefinition);
				MethodDependence.checkDependence(returnType);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			
		}
	}

	private void injectField(Class<?> klass,Object object) {
		//根据反射机制获取类的所有成员
		Field[] fields = klass.getDeclaredFields();
		for(Field field: fields) {
			if(!field.isAnnotationPresent(Autowired.class)) {
				continue;
			}
			//有注解的类
			//先获取成员的类型，再根据类型从beanFactory中获取存放的该类型的对象
			Class<?> fieldClass = field.getType();
			Object value = getBean(fieldClass);
			field.setAccessible(true);
			try {
				//把需要注入的内容(value)“set”到成员对象(object)
				field.set(object, value);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
		
	private void injectMethod(Class<?> klass,Object object) {
		Method[] methods = klass.getDeclaredMethods();
		for(Method method:methods) {
			String methodName = method.getName();
			int parameterCount = method.getParameterCount();
			int modify =  method.getModifiers();
			//以"set"开头，权限为public,只有1个参数，以Autowired为注解
			if(!methodName.startsWith("set") 
					|| !Modifier.isPublic(modify)
					|| parameterCount != 1
					|| !method.isAnnotationPresent(Autowired.class)){
				continue;
			}			
			//对方法进行反射调用
			//方法的反射调用，需要对象和参数			
			//因为方法只有一个参数，所以直接取下标为0的即可
			Class<?> paraType =  method.getParameterTypes()[0];
			Object value = getBean(paraType);
			
			try {
				//虽然只有一个参数，new Object[]{}为了程序格式
				method.invoke(object, new Object[] {value});
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
	

	/*
	 * 注入操作
	 */	
	private void inject(BeanDefinition bean) {
		//获取相关对象和类
		Object object = bean.getObject();
		Class<?> klass = bean.getKlass();
		
		injectField(klass,object);
		injectMethod(klass,object);
	}
	
	
	/*
	 * 获取Bean
	 */
	private static BeanDefinition getBeanObject(String className) {
		BeanDefinition bean = BeanPool.get(className);
		if (bean == null) {
			return null;
		}
		Object object = null;
		//处理单例和非单例的情况
		//非单例的情况，根据类重新生成新的对象object
		//单例情况，直接返回bean
		if (!bean.isSingleton()) {
			Class<?> klass = bean.getKlass();
			try {
				object = klass.newInstance();
				bean.setObject(object);
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		
		return bean;
	}
	
	/*
	 * 进行注入
	 */	
	@SuppressWarnings("unchecked")
	public <T> T getBean(String className) {
		BeanDefinition bean = getBeanObject(className);
		if (bean == null) {
			showCircleDependency();
			System.out.println("Bean[" + className + "]不存在！");
			return null;
		}
		Object object = bean.getObject();
		
		//非单例（对象是新生成的）和未注入的情况都需要进行注入
		if(!bean.isInject() || !bean.isSingleton()) {
			//这里完成object中需要注入的成员的初始化工作！
			bean.setInject(true);
			inject(bean);
		}
		
		return (T) object;
	}
	
	public <T> T getBean(Class<?> klass) {
		return getBean(klass.getName());
	}
	  
	//输出循环依赖，由用户判断
	private void showCircleDependency() {
		System.out.println(MethodDependence.getUndependence());
	}
}
