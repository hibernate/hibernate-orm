/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.sql.JoinType;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.produce.spi.TableGroupContext;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionIndex<J> implements CollectionIndex<J> {
	private final PersistentCollectionDescriptor persister;
	private final NavigableRole navigableRole;

	public AbstractCollectionIndex(PersistentCollectionDescriptor persister) {
		this.persister = persister;
		this.navigableRole = persister.getNavigableRole().append( NAVIGABLE_NAME );
	}

	@Override
	public PersistentCollectionDescriptor getContainer() {
		return persister;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String getNavigableName() {
		return NAVIGABLE_NAME;
	}

	@Override
	public String asLoggableText() {
		return "PluralAttributeIndex(" + persister.getNavigableRole() + " [" + getJavaType() + "])";
	}

	@Override
	public void applyTableReferenceJoins(
			JoinType leftOuterJoin,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector collector,
			TableGroupContext tableGroupContext) {
		// only relevant for ONE-TO-MANY and MANY-TO-MANY - noop in the general case
	}
}