/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
