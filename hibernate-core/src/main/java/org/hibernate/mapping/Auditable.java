/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.Incubating;

/**
 * Part of the boot model which is audited.
 */
@Incubating
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
