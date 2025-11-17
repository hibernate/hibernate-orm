/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing;

import java.lang.reflect.Method;

//TODO -- fold this into ReflectionUtil
public class HSReflectionUtil  {

	/**
	 * Get target method
	 *
	 * @param target target class
	 * @param methodName method name
	 * @param parameterTypes method parameter types
	 *
	 * @return return value
	 */
	public static Method getStaticMethod(Class target, String methodName, Class... parameterTypes) {
		try {
			return target.getMethod( methodName, parameterTypes );
		}
		catch (NoSuchMethodException e) {
			throw new IllegalArgumentException( e );
		}
	}

}
