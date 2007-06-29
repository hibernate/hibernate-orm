package org.hibernate.intercept.javassist;

import org.hibernate.intercept.FieldInterceptor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.bytecode.javassist.FieldHandled;

import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class JavassistHelper {
	private JavassistHelper() {
	}

	public static FieldInterceptor extractFieldInterceptor(Object entity) {
		return ( FieldInterceptor ) ( ( FieldHandled ) entity ).getFieldHandler();
	}

	public static FieldInterceptor injectFieldInterceptor(
			Object entity,
	        String entityName,
	        Set uninitializedFieldNames,
	        SessionImplementor session) {
		FieldInterceptorImpl fieldInterceptor = new FieldInterceptorImpl( session, uninitializedFieldNames, entityName );
		( ( FieldHandled ) entity ).setFieldHandler( fieldInterceptor );
		return fieldInterceptor;
	}
}
