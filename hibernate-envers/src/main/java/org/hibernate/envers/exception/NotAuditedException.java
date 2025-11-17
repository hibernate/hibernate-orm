/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.exception;


/**
 * @author Adam Warski (adam at warski dot org)
 */
public class NotAuditedException extends AuditException {
	private static final long serialVersionUID = 4809674577449455510L;

	private final String entityName;

	public NotAuditedException(String entityName, String message) {
		super( message );
		this.entityName = entityName;
	}

	public String getEntityName() {
		return entityName;
	}
}
