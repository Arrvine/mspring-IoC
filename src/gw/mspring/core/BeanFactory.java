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
				//klass����Ϊ�˴�������͡�ע�⡢���顢�ӿڡ�String�࣬��ֱ�ӷ���
				//Component���͵�ע���Ƿ���klass���ϡ�
				if(klass.isPrimitive()
						|| klass.isAnnotation()
						|| klass.isArray()
						|| klass.isInterface()
						|| klass == String.class
						|| !klass.isAnnotationPresent(Component.class)) {
					return;
				}
				
				//����˶���@Componnetע������ʵ������������ŵ�BeanPool
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
		//����δ��ɴ���Bean����
		MethodDependence.invokeDependenceMethod();
	}
	
	/*
	 * 
	 * ��ȡ�������͡�
	 * 1.��ȡ�����Ĳ������ͣ��Բ������ͽ���ȥ�ز�����װ��paraPool
	 * 2.����һ��ArrayList����BeanPool�в����Ƿ���ڸò������ͣ�
	 * 3.�����ڣ����paraPool��ɾ��
	 * */
	private static Map<Class<?>,Integer> getMethodPara(Object object,Method method) {
		Map<Class<?>, Integer> paraPool = new HashMap<>();//�洢BeanPool�в����ڵĲ���
		
		Class<?>[] parameterTypes = method.getParameterTypes();
		for(int index = 0;index <parameterTypes.length;index++) {
			//HashMap�в�������ͬ��keyֵ��put�������Զ�����equals������hashCode
			//ȥ�أ�ȥ�������ظ��Ĳ�������
			paraPool.put(parameterTypes[index], 0);//ֵĿǰ����ν
		}
		//Map��List���ܱ߱�����ɾ�����Ƚ���洢��klassList
		List<Class<?>> klassList = new ArrayList<Class<?>>();
		for(Class<?> type:paraPool.keySet()) {
			//�˴�ֻ��Ҫ�ж�bean�Ƿ���ڣ�ʹ��get()
			BeanDefinition beanDefinition = BeanPool.get(type.getName());
			if(beanDefinition != null) {
				klassList.add(type);
			}
		}
		for(Class<?>type:klassList) {
			paraPool.remove(type);
		}
		//����߱�����ɾ��
		
		return paraPool;
	}
	
	static void invokeMethodWithPara(Class<?> klass,Object object,Method method) {
		Class<?>[] paraTypes = method.getParameterTypes();//��ȡ�������в���������
		Object[] paraValues = new Object[paraTypes.length];//���ɶ�Ӧ����������ʵ���б�
		
		for(int index=0;index<paraTypes.length;index++) {
			Class<?> paraklass = paraTypes[index];
			//�����getBeanObject()��һ������ȡ���ģ�
			//�ڸ÷�������Ѿ��жϹ���������Ҫ�Ĳ������洢��BeanPool
			BeanDefinition beanDefinition = getBeanObject(paraklass.getName());
			paraValues[index] = beanDefinition.getObject();
		}

		try {
			//ִ�и÷������Ѹ÷���ִ��֮�������beanҲ�Ŵ洢��BeanPool��
			Class<?> beanClass = method.getReturnType();
			Object bean = method.invoke(object, paraValues);
			BeanDefinition beanDefinition = new BeanDefinition();
			
			beanDefinition.setKlass(beanClass);
			beanDefinition.setObject(bean);
			//ע��͵�������Ĭ��
			
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
		
		//��Ϊ�գ���˵�����в������Ի�ȡ����ִ�з���
		if(paraTypeMap.isEmpty()) {
			invokeMethodWithPara(klass,object, method);
			return;
		}
		
		//����Ϊ�գ�˵���÷���ִ����Ҫ�Ĳ���������
		//���÷�����ִ�ж��󣬷�������Ͳ����������з�װ
		//���뵽δִ�з����б�
		MethodDefinition methodDefinition = new MethodDefinition();
		methodDefinition.setObject(object);
		methodDefinition.setMethod(method);
		methodDefinition.setParaCount(paraTypeMap.size());
		
		MethodDependence.addUninvokeMethod(methodDefinition, paraTypeMap);
	}
	
	/*
	 * �����beanע��ķ���,�в��޲ζ�����
	 */
	private static void dealBean(Object object,Class<?> klass) {
		Method[] methods = klass.getDeclaredMethods();
		for(Method method:methods) {
			//�Ƿ�ΪBeanע��ķ���
			if(!method.isAnnotationPresent(Bean.class)) {
				continue;
			}			
			//�����в�����Beanע��ķ���
			if(method.getParameterCount() >0) {
				//����ֱ�Ӵ����������Beanע��ķ���
				//��Ϊ�޷�ȷ����ز����Ƿ���BeanFactory��
				dealMethodWithPara(klass, object, method);
				continue;
			}
			
			//�����޲�����beanע��
			Class<?> returnType = method.getReturnType();//��ȡ����ֵ����
			try {
				Object	beanObject = method.invoke(object);//ͨ��ִ�з�����ȡ����ֵ
				
				BeanDefinition beanDefinition = new BeanDefinition();
				beanDefinition.setKlass(klass);
				beanDefinition.setObject(beanObject);
				//Ĭ�ϣ����Բ�����
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
		//���ݷ�����ƻ�ȡ������г�Ա
		Field[] fields = klass.getDeclaredFields();
		for(Field field: fields) {
			if(!field.isAnnotationPresent(Autowired.class)) {
				continue;
			}
			//��ע�����
			//�Ȼ�ȡ��Ա�����ͣ��ٸ������ʹ�beanFactory�л�ȡ��ŵĸ����͵Ķ���
			Class<?> fieldClass = field.getType();
			Object value = getBean(fieldClass);
			field.setAccessible(true);
			try {
				//����Ҫע�������(value)��set������Ա����(object)
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
			//��"set"��ͷ��Ȩ��Ϊpublic,ֻ��1����������AutowiredΪע��
			if(!methodName.startsWith("set") 
					|| !Modifier.isPublic(modify)
					|| parameterCount != 1
					|| !method.isAnnotationPresent(Autowired.class)){
				continue;
			}			
			//�Է������з������
			//�����ķ�����ã���Ҫ����Ͳ���			
			//��Ϊ����ֻ��һ������������ֱ��ȡ�±�Ϊ0�ļ���
			Class<?> paraType =  method.getParameterTypes()[0];
			Object value = getBean(paraType);
			
			try {
				//��Ȼֻ��һ��������new Object[]{}Ϊ�˳����ʽ
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
	 * ע�����
	 */	
	private void inject(BeanDefinition bean) {
		//��ȡ��ض������
		Object object = bean.getObject();
		Class<?> klass = bean.getKlass();
		
		injectField(klass,object);
		injectMethod(klass,object);
	}
	
	
	/*
	 * ��ȡBean
	 */
	private static BeanDefinition getBeanObject(String className) {
		BeanDefinition bean = BeanPool.get(className);
		if (bean == null) {
			return null;
		}
		Object object = null;
		//�������ͷǵ��������
		//�ǵ�������������������������µĶ���object
		//���������ֱ�ӷ���bean
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
	 * ����ע��
	 */	
	@SuppressWarnings("unchecked")
	public <T> T getBean(String className) {
		BeanDefinition bean = getBeanObject(className);
		if (bean == null) {
			showCircleDependency();
			System.out.println("Bean[" + className + "]�����ڣ�");
			return null;
		}
		Object object = bean.getObject();
		
		//�ǵ����������������ɵģ���δע����������Ҫ����ע��
		if(!bean.isInject() || !bean.isSingleton()) {
			//�������object����Ҫע��ĳ�Ա�ĳ�ʼ��������
			bean.setInject(true);
			inject(bean);
		}
		
		return (T) object;
	}
	
	public <T> T getBean(Class<?> klass) {
		return getBean(klass.getName());
	}
	  
	//���ѭ�����������û��ж�
	private void showCircleDependency() {
		System.out.println(MethodDependence.getUndependence());
	}
}
