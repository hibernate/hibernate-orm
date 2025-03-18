/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.synchronization.work;


/**
 * Visitor pattern visitor. All methods should be invoked on the first work unit.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public interface WorkUnitMergeVisitor {
	AuditWorkUnit merge(AddWorkUnit second);

	AuditWorkUnit merge(ModWorkUnit second);

	AuditWorkUnit merge(DelWorkUnit second);

	AuditWorkUnit merge(CollectionChangeWorkUnit second);

	AuditWorkUnit merge(FakeBidirectionalRelationWorkUnit second);
}
