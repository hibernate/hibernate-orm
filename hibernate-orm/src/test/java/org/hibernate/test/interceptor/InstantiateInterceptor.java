/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * @author Gail Badner
 */
package org.hibernate.test.interceptor;
import java.io.Serializable;

import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityMode;

public class InstantiateInterceptor extends EmptyInterceptor {
	private String injectedString;

	public InstantiateInterceptor(String injectedString) {
		this.injectedString = injectedString;		
	}

	public Object instantiate(String entityName, EntityMode entityMode, Serializable id) throws CallbackException {
		if ( ! "org.hibernate.test.interceptor.User".equals( entityName ) ) {
			return null;
		}
		// Simply inject a sample string into new instances
		User instance = new User();
		instance.setName( ( String ) id );
		instance.setInjectedString( injectedString );
		return instance;
	}
}
