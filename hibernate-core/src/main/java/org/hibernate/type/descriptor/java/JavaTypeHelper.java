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
				"Unknown unwrap conversion requested: " + sourceType.getName() + " to " + targetType.getName() + " : `" + jtd.getClass().getName() + "` (" + jtd.getJavaTypeClass().getName() + ")"
		);
	}

	protected static <T extends JavaType<?>> HibernateException unknownWrap(Class<?> valueType, Class<?> sourceType, T jtd) {
		throw new HibernateException(
				"Unknown wrap conversion requested: " + valueType.getName() + " to " + sourceType.getName() + " : `" + jtd.getClass().getName() + "` (" + jtd.getJavaTypeClass().getName() + ")"
		);
	}

	public static boolean isTemporal(JavaType<?> javaType) {
		return javaType != null && javaType.isTemporalType();
	}

	public static boolean isUnknown(JavaType<?> javaType) {
		return javaType.getClass() == UnknownBasicJavaType.class;
	}
}
