/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

/**
 * Part of the boot model which is audited.
 */
public interface Auditable {
	void enableAudit(Table auditTable, Column transactionIdColumn, Column modificationTypeColumn);

	Table getAuditTable();

	Table getMainTable();

	Column getAuditTransactionIdColumn();

	Column getAuditModificationTypeColumn();

	default boolean isAudited() {
		return getAuditTable() != null;
	}
}
