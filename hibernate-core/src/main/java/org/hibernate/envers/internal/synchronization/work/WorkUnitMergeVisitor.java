/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
