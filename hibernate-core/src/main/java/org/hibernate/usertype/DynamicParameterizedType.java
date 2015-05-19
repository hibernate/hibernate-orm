/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
