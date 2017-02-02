/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service.internal;

import org.hibernate.HibernateException;

/**
 * Indicates a problem generating a service proxy
 * 
 * @author Steve Ebersole
 */
public class ServiceProxyGenerationException extends HibernateException {
	public ServiceProxyGenerationException(String string, Throwable root) {
		super( string, root );
	}

	public ServiceProxyGenerationException(Throwable root) {
		super( root );
	}
}
