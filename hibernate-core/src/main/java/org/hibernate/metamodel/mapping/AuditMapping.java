/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

/**
 * Metadata about audit log tables for entities and collections enabled for audit logging.
 *
 * @see org.hibernate.annotations.Audited
 *
 * @author Gavin King
 */
public interface AuditMapping extends AuxiliaryMapping {

	SelectableMapping getTransactionIdMapping();

	SelectableMapping getModificationTypeMapping();

}
