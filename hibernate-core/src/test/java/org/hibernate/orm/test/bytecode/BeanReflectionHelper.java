/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode;
import java.util.Date;

import org.hibernate.property.access.internal.PropertyAccessStrategyBasicImpl;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.Setter;

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
		final PropertyAccessStrategyBasicImpl propertyAccessStrategy = new PropertyAccessStrategyBasicImpl();

		PropertyAccess propertyAccess = propertyAccessStrategy.buildPropertyAccess( Bean.class, "someString", true );
		Getter getter = propertyAccess.getGetter();
		Setter setter = propertyAccess.getSetter();
		getterNames[0] = getter.getMethodName();
		types[0] = getter.getReturnTypeClass();
		setterNames[0] = setter.getMethodName();

		propertyAccess = propertyAccessStrategy.buildPropertyAccess( Bean.class, "someLong", true );
		getter = propertyAccess.getGetter();
		setter = propertyAccess.getSetter();
		getterNames[1] = getter.getMethodName();
		types[1] = getter.getReturnTypeClass();
		setterNames[1] = setter.getMethodName();

		propertyAccess = propertyAccessStrategy.buildPropertyAccess( Bean.class, "someInteger", true );
		getter = propertyAccess.getGetter();
		setter = propertyAccess.getSetter();
		getterNames[2] = getter.getMethodName();
		types[2] = getter.getReturnTypeClass();
		setterNames[2] = setter.getMethodName();

		propertyAccess = propertyAccessStrategy.buildPropertyAccess( Bean.class, "someDate", true );
		getter = propertyAccess.getGetter();
		setter = propertyAccess.getSetter();
		getterNames[3] = getter.getMethodName();
		types[3] = getter.getReturnTypeClass();
		setterNames[3] = setter.getMethodName();

		propertyAccess = propertyAccessStrategy.buildPropertyAccess( Bean.class, "somelong", true );
		getter = propertyAccess.getGetter();
		setter = propertyAccess.getSetter();
		getterNames[4] = getter.getMethodName();
		types[4] = getter.getReturnTypeClass();
		setterNames[4] = setter.getMethodName();

		propertyAccess = propertyAccessStrategy.buildPropertyAccess( Bean.class, "someint", true );
		getter = propertyAccess.getGetter();
		setter = propertyAccess.getSetter();
		getterNames[5] = getter.getMethodName();
		types[5] = getter.getReturnTypeClass();
		setterNames[5] = setter.getMethodName();

		propertyAccess = propertyAccessStrategy.buildPropertyAccess( Bean.class, "someObject", true );
		getter = propertyAccess.getGetter();
		setter = propertyAccess.getSetter();
		getterNames[6] = getter.getMethodName();
		types[6] = getter.getReturnTypeClass();
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
