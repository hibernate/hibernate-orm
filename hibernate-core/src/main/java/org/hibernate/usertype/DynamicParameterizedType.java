/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.usertype;

import org.hibernate.Incubating;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Properties;

/**
 * Types which implement this interface will have
 * {@link ParameterizedType#setParameterValues(Properties)} called with an
 * instance of the class {@link DynamicParameterizedType.ParameterType}
 * instead of the key {@value PARAMETER_TYPE}.
 *
 * @author Janario Oliveira
 * @author Yanming Zhou
 */
public interface DynamicParameterizedType extends ParameterizedType {
	String PARAMETER_TYPE = "org.hibernate.type.ParameterType";

	String IS_DYNAMIC = "org.hibernate.type.ParameterType.dynamic";

	String RETURNED_CLASS = "org.hibernate.type.ParameterType.returnedClass";
	String IS_PRIMARY_KEY = "org.hibernate.type.ParameterType.primaryKey";
	String ENTITY = "org.hibernate.type.ParameterType.entityClass";
	String PROPERTY = "org.hibernate.type.ParameterType.propertyName";
	String ACCESS_TYPE = "org.hibernate.type.ParameterType.accessType";
	String XPROPERTY = "org.hibernate.type.ParameterType.xproperty";

	interface ParameterType {

		Class<?> getReturnedClass();

		@Incubating
		default Type getReturnedJavaType() {
			return getReturnedClass();
		}

		Annotation[] getAnnotationsMethod();

		String getCatalog();

		String getSchema();

		String getTable();

		boolean isPrimaryKey();

		String[] getColumns();

		Long[] getColumnLengths();
	}
}
