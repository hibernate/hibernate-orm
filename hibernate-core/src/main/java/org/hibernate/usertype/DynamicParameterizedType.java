/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.usertype;

import java.lang.annotation.Annotation;

/**
 * Types who implements this interface will have in the setParameterValues an
 * instance of the class DynamicParameterizedType$ParameterType instead of
 * the key PARAMETER_TYPE = "org.hibernate.type.ParameterType"
 * 
 * The interface ParameterType provides some methods to read information
 * dynamically for build the type
 * 
 * @author Janario Oliveira
 */
public interface DynamicParameterizedType extends ParameterizedType {
	public static final String PARAMETER_TYPE = "org.hibernate.type.ParameterType";

	public static final String IS_DYNAMIC = "org.hibernate.type.ParameterType.dynamic";

	public static final String RETURNED_CLASS = "org.hibernate.type.ParameterType.returnedClass";
	public static final String IS_PRIMARY_KEY = "org.hibernate.type.ParameterType.primaryKey";
	public static final String ENTITY = "org.hibernate.type.ParameterType.entityClass";
	public static final String PROPERTY = "org.hibernate.type.ParameterType.propertyName";
	public static final String ACCESS_TYPE = "org.hibernate.type.ParameterType.accessType";
	public static final String XPROPERTY = "org.hibernate.type.ParameterType.xproperty";

	public static interface ParameterType {

		public Class getReturnedClass();

		public Annotation[] getAnnotationsMethod();

		public String getCatalog();

		public String getSchema();

		public String getTable();

		public boolean isPrimaryKey();

		public String[] getColumns();

	}
}
