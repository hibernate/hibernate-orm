/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.AbstractTableReferenceCollector;
import org.hibernate.metamodel.model.domain.spi.PluralValuedNavigable;
import org.hibernate.query.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class RootTableReferenceCollectorImpl extends AbstractTableReferenceCollector {
	private final String uniqueIdentifier;
	private final NavigablePath navigablePath;

	private final PluralValuedNavigable navigable;

	private final String explicitSourceAlias;
	private final LockMode effectiveLockMode;

	public RootTableReferenceCollectorImpl(
			String uniqueIdentifier,
			NavigablePath navigablePath,
			PluralValuedNavigable navigable,
			String explicitSourceAlias,
			LockMode effectiveLockMode) {
		this.uniqueIdentifier = uniqueIdentifier;
		this.navigable = navigable;
		this.navigablePath = navigablePath;
		this.explicitSourceAlias = explicitSourceAlias;
		this.effectiveLockMode = effectiveLockMode;
	}

	public CollectionTableGroup generateTableGroup() {
		return new CollectionTableGroup(
				uniqueIdentifier,
				navigablePath,
				navigable,
				explicitSourceAlias,
				effectiveLockMode,
				getPrimaryTableReference(),
				getTableReferenceJoins()
		);
	}
}
