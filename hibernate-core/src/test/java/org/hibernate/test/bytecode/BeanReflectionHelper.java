package org.hibernate.test.bytecode;
import java.util.Date;

import org.hibernate.property.BasicPropertyAccessor;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;

/**
 * @author Steve Ebersole
 */
public class BeanReflectionHelper {

	public static final Object[] TEST_VALUES = new Object[] {
			"hello", new Long(1), new Integer(1), new Date(), new Long(1), new Integer(1), new Object()
	};

	private static final String[] getterNames = new String[7];
	private static final String[] setterNames = new String[7];
	private static final Class[] types = new Class[7];

	static {
		BasicPropertyAccessor propertyAccessor = new BasicPropertyAccessor();
		Getter getter = propertyAccessor.getGetter( Bean.class, "someString" );
		Setter setter = propertyAccessor.getSetter( Bean.class, "someString" );
		getterNames[0] = getter.getMethodName();
		types[0] = getter.getReturnType();
		setterNames[0] = setter.getMethodName();

		getter = propertyAccessor.getGetter( Bean.class, "someLong" );
		setter = propertyAccessor.getSetter( Bean.class, "someLong" );
		getterNames[1] = getter.getMethodName();
		types[1] = getter.getReturnType();
		setterNames[1] = setter.getMethodName();

		getter = propertyAccessor.getGetter( Bean.class, "someInteger" );
		setter = propertyAccessor.getSetter( Bean.class, "someInteger" );
		getterNames[2] = getter.getMethodName();
		types[2] = getter.getReturnType();
		setterNames[2] = setter.getMethodName();

		getter = propertyAccessor.getGetter( Bean.class, "someDate" );
		setter = propertyAccessor.getSetter( Bean.class, "someDate" );
		getterNames[3] = getter.getMethodName();
		types[3] = getter.getReturnType();
		setterNames[3] = setter.getMethodName();

		getter = propertyAccessor.getGetter( Bean.class, "somelong" );
		setter = propertyAccessor.getSetter( Bean.class, "somelong" );
		getterNames[4] = getter.getMethodName();
		types[4] = getter.getReturnType();
		setterNames[4] = setter.getMethodName();

		getter = propertyAccessor.getGetter( Bean.class, "someint" );
		setter = propertyAccessor.getSetter( Bean.class, "someint" );
		getterNames[5] = getter.getMethodName();
		types[5] = getter.getReturnType();
		setterNames[5] = setter.getMethodName();

		getter = propertyAccessor.getGetter( Bean.class, "someObject" );
		setter = propertyAccessor.getSetter( Bean.class, "someObject" );
		getterNames[6] = getter.getMethodName();
		types[6] = getter.getReturnType();
		setterNames[6] = setter.getMethodName();
	}

	public static String[] getGetterNames() {
		return getterNames;
	}

	public static String[] getSetterNames() {
		return setterNames;
	}

	public static Class[] getTypes() {
		return types;
	}
}
