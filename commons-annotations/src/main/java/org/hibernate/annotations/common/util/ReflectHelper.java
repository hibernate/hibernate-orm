/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.annotations.common.util;

import java.lang.reflect.Method;
import java.lang.reflect.Member;

/**
 * Complete duplication of {@link org.hibernate.util.ReflectHelper}.
 *
 * @author Emmanuel Bernard
 * @deprecated Use {@link org.hibernate.util.ReflectHelper} instead.
 */
public final class ReflectHelper {
	public static boolean overridesEquals(Class clazz) {
		return org.hibernate.util.ReflectHelper.overridesEquals( clazz );
	}

	public static boolean overridesHashCode(Class clazz) {
		return org.hibernate.util.ReflectHelper.overridesHashCode( clazz );
	}

	public static Class classForName(String name) throws ClassNotFoundException {
		return org.hibernate.util.ReflectHelper.classForName( name );
	}

	public static Class classForName(String name, Class caller) throws ClassNotFoundException {
		return org.hibernate.util.ReflectHelper.classForName( name, caller );
	}

	public static boolean isPublic(Class clazz, Member member) {
		return org.hibernate.util.ReflectHelper.isPublic( clazz, member );
	}

	public static Object getConstantValue(String name) {
		return org.hibernate.util.ReflectHelper.getConstantValue( name );
	}

	public static boolean isAbstractClass(Class clazz) {
		return org.hibernate.util.ReflectHelper.isAbstractClass( clazz );
	}

	public static boolean isFinalClass(Class clazz) {
		return org.hibernate.util.ReflectHelper.isFinalClass( clazz );
	}

	public static Method getMethod(Class clazz, Method method) {
		return org.hibernate.util.ReflectHelper.getMethod( clazz, method );
	}

	/**
	 * Direct instantiation of ReflectHelper disallowed.
	 */
	private ReflectHelper() {
	}

}

