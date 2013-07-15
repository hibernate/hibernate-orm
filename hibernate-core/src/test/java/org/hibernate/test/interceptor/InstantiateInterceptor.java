/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
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
 *
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
