/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
