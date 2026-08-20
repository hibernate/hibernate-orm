/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ValueClassHelper {
	private static final Method IS_VALUE;

	static {
		Method isValue = null;
		try {
			isValue = Class.class.getMethod( "isValue" );
		}
		catch (NoSuchMethodException e) {
			// No-op
		}
		IS_VALUE = isValue;
	}

	public static boolean isValue(Class<?> clazz) {
		try {
			return IS_VALUE != null && (boolean) IS_VALUE.invoke( clazz );
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			return false;
		}
	}
}
