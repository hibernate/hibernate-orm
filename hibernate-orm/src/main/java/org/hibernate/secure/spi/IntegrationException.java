/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.secure.spi;

import org.hibernate.HibernateException;

/**
 * @author Steve Ebersole
 */
public class IntegrationException extends HibernateException {
	public IntegrationException(String message) {
		super( message );
	}

	public IntegrationException(String message, Throwable root) {
		super( message, root );
	}
}
