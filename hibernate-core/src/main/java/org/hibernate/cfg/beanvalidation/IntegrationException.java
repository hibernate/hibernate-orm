/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.beanvalidation;

import org.hibernate.HibernateException;

/**
 * Indicates a problem integrating Hibernate and the Bean Validation spec.
 *
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
