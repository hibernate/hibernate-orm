/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.Incubating;

/**
 * Metadata about audit log tables for entities and collections enabled for audit logging.
 *
 * @see org.hibernate.annotations.Audited
 *
 * @author Gavin King
 *
 * @since 7.4
 */
@Incubating
public interface AuditMapping extends AuxiliaryMapping {

	SelectableMapping getTransactionIdMapping();

	SelectableMapping getModificationTypeMapping();

}
