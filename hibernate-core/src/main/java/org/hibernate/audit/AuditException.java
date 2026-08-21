/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit;

import org.hibernate.HibernateException;

/**
 * Indicates a problem related to audit functionality.
 *
 * @author Marco Belladelli
 * @since 7.4
 */
public class AuditException extends HibernateException {
	public AuditException(String message) {
		super( message );
	}

	public AuditException(String message, Throwable cause) {
		super( message, cause );
	}
}
