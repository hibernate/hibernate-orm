/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.AbstractPersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.AbstractTableReferenceCollector;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.tree.spi.from.CollectionTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;

/**
 * @author Steve Ebersole
 */
public class RootTableReferenceCollectorImpl extends AbstractTableReferenceCollector {
	private final String uniqueIdentifier;
	private final NavigablePath navigablePath;

	private final AbstractPersistentCollectionDescriptor collectionDescriptor;

	private final TableSpace tableSpace;
	private final LockMode effectiveLockMode;

	public RootTableReferenceCollectorImpl(
			TableSpace tableSpace,
			AbstractPersistentCollectionDescriptor collectionDescriptor,
			NavigablePath navigablePath,
			String uniqueIdentifier,
			LockMode effectiveLockMode) {
		this.tableSpace = tableSpace;
		this.collectionDescriptor = collectionDescriptor;
		this.navigablePath = navigablePath;
		this.uniqueIdentifier = uniqueIdentifier;
		this.effectiveLockMode = effectiveLockMode;
	}

	@SuppressWarnings("WeakerAccess")
	public CollectionTableGroup generateTableGroup() {
		return new CollectionTableGroup(
				uniqueIdentifier,
				tableSpace,
				collectionDescriptor,
				effectiveLockMode,
				navigablePath,
				getPrimaryTableReference(),
				getTableReferenceJoins()
		);
	}
}
