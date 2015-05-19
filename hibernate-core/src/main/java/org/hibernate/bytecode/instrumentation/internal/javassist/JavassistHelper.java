/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.instrumentation.internal.javassist;

import java.util.Set;

import org.hibernate.bytecode.instrumentation.spi.FieldInterceptor;
import org.hibernate.bytecode.internal.javassist.FieldHandled;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Javassist specific helper
 *
 * @author Steve Ebersole
 */
public class JavassistHelper {
	private JavassistHelper() {
	}

	/**
	 * Perform the Javassist-specific field interceptor extraction
	 *
	 * @param entity The entity from which to extract the interceptor
	 *
	 * @return The extracted interceptor
	 */
	public static FieldInterceptor extractFieldInterceptor(Object entity) {
		return (FieldInterceptor) ( (FieldHandled) entity ).getFieldHandler();
	}

	/**
	 * Perform the Javassist-specific field interceptor injection
	 *
	 * @param entity The entity instance
	 * @param entityName The entity name
	 * @param uninitializedFieldNames The names of any uninitialized fields
	 * @param session The session
	 *
	 * @return The generated and injected interceptor
	 */
	public static FieldInterceptor injectFieldInterceptor(
			Object entity,
			String entityName,
			Set uninitializedFieldNames,
			SessionImplementor session) {
		final FieldInterceptorImpl fieldInterceptor = new FieldInterceptorImpl( session, uninitializedFieldNames, entityName );
		( (FieldHandled) entity ).setFieldHandler( fieldInterceptor );
		return fieldInterceptor;
	}
}
