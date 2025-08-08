/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.java.spi.UnknownBasicJavaType;

/**
 * @author Steve Ebersole
 */
public class JavaTypeHelper {
	protected static <T extends JavaType<?>> HibernateException unknownUnwrap(Class<?> sourceType, Class<?> targetType, T jtd) {
		throw new HibernateException(
				"Could not convert '" + sourceType.getName()
						+ "' to '" + targetType.getName()
						+ "' using '" + jtd.getClass().getName() + "' to unwrap"
		);
	}

	protected static <T extends JavaType<?>> HibernateException unknownWrap(Class<?> valueType, Class<?> sourceType, T jtd) {
		throw new HibernateException(
				"Could not convert '" + valueType.getName()
						+ "' to '" + sourceType.getName()
						+ "' using '" + jtd.getClass().getName() + "' to wrap"
		);
	}

	public static boolean isTemporal(JavaType<?> javaType) {
		return javaType != null && javaType.isTemporalType();
	}

	public static boolean isUnknown(JavaType<?> javaType) {
		return javaType == null || javaType.getClass() == UnknownBasicJavaType.class;
	}
}
