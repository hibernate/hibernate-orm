package org.hibernate.intercept.cglib;

import org.hibernate.intercept.FieldInterceptor;
import org.hibernate.engine.SessionImplementor;
import net.sf.cglib.transform.impl.InterceptFieldEnabled;

import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class CGLIBHelper {
	private CGLIBHelper() {
	}

	public static FieldInterceptor extractFieldInterceptor(Object entity) {
		return ( FieldInterceptor ) ( ( InterceptFieldEnabled ) entity ).getInterceptFieldCallback();
	}

	public static FieldInterceptor injectFieldInterceptor(
			Object entity,
	        String entityName,
	        Set uninitializedFieldNames,
	        SessionImplementor session) {
		FieldInterceptorImpl fieldInterceptor = new FieldInterceptorImpl(
				session, uninitializedFieldNames, entityName
		);
		( ( InterceptFieldEnabled ) entity ).setInterceptFieldCallback( fieldInterceptor );
		return fieldInterceptor;

	}
}
