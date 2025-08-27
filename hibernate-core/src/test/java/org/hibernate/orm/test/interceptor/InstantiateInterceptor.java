/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor;

import org.hibernate.CallbackException;
import org.hibernate.Interceptor;
import org.hibernate.metamodel.RepresentationMode;

public class InstantiateInterceptor implements Interceptor {
	private String injectedString;

	public InstantiateInterceptor(String injectedString) {
		this.injectedString = injectedString;
	}

	@Override
	public Object instantiate(String entityName, RepresentationMode entityMode, Object id) throws CallbackException {
		if ( ! "org.hibernate.orm.test.interceptor.User".equals( entityName ) ) {
			return null;
		}
		// Simply inject a sample string into new instances
		User instance = new User();
		instance.setName( ( String ) id );
		instance.setInjectedString( injectedString );
		return instance;
	}
}
