/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import org.hibernate.HibernateException;

/**
 * Indicates problems accessing TransactionManager or UserTransaction through the JtaPlatform
 *
 * @author Steve Ebersole
 */
public class JtaPlatformInaccessibleException extends HibernateException {
	public JtaPlatformInaccessibleException(String message) {
		super( message );
	}

	public JtaPlatformInaccessibleException(String message, Throwable cause) {
		super( message, cause );
	}
}
