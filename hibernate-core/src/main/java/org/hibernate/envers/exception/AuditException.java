/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.exception;

import org.hibernate.HibernateException;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class AuditException extends HibernateException {
	private static final long serialVersionUID = 4306480965630972168L;

	public AuditException(String message) {
		super( message );
	}

	public AuditException(String message, Throwable cause) {
		super( message, cause );
	}

	public AuditException(Throwable cause) {
		super( cause );
	}
}
