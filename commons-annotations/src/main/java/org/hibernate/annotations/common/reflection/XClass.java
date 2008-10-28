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
package org.hibernate.annotations.common.reflection;

import java.util.List;

/**
 * @author Paolo Perrotta
 * @author Davide Marchignoli
 */
public interface XClass extends XAnnotatedElement {

	public static final String ACCESS_PROPERTY = "property";
	public static final String ACCESS_FIELD = "field";

	static final Filter DEFAULT_FILTER = new Filter() {

		public boolean returnStatic() {
			return false;
		}

		public boolean returnTransient() {
			return false;
		}
	};

	String getName();

	/**
	 * @see Class#getSuperclass()
	 */
	XClass getSuperclass();

	/**
	 * @see Class#getInterfaces()
	 */
	XClass[] getInterfaces();

	/**
	 * see Class#isInterface()
	 */
	boolean isInterface();

	boolean isAbstract();

	boolean isPrimitive();

	boolean isEnum();

	boolean isAssignableFrom(XClass c);

	List<XProperty> getDeclaredProperties(String accessType);

	List<XProperty> getDeclaredProperties(String accessType, Filter filter);

	/**
	 * Returns the <tt>Method</tt>s defined by this class.
	 */
	List<XMethod> getDeclaredMethods();
}
