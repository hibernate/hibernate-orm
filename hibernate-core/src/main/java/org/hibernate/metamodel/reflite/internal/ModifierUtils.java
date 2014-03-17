/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.reflite.internal;

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;

/**
 * Fills-in non-public aspects of the {@link java.lang.reflect.Modifier} class
 *
 * @author Steve Ebersole
 */
public class ModifierUtils {

	private static final int BRIDGE    = 0x00000040;
	private static final int ENUM      = 0x00004000;
	private static final int SYNTHETIC = 0x00001000;

	/**
	 * Disallow instantiation.  This is a utility class, use statically.
	 */
	private ModifierUtils() {
	}

	/**
	 * Determine if the given method is a bridge.
	 *
	 * @param methodDescriptor The method descriptor to check
	 *
	 * @return {@code true} if the method is a bridge , {@code false} otherwise.
	 */
	public static boolean isBridge(MethodDescriptor methodDescriptor) {
		return (methodDescriptor.getModifiers() & BRIDGE) != 0;
	}

	/**
	 * Determine if the given Java type is an enum.
	 *
	 * @param javaType The descriptor of the Java type to check
	 *
	 * @return {@code true} if the Java type is an enum, {@code false} otherwise.
	 */
	public static boolean isEnum(JavaTypeDescriptor javaType) {
		return (javaType.getModifiers() & ENUM) != 0;
	}

	/**
	 * Determine is the given member (field/method) is synthetic
	 *
	 * @param memberDescriptor The descriptor for the member to check
	 *
	 * @return {@code true} if the member is synthetic, {@code false} otherwise.
	 */
	public static boolean isSynthetic(MemberDescriptor memberDescriptor) {
		return (memberDescriptor.getModifiers() & SYNTHETIC) != 0;
	}
}
