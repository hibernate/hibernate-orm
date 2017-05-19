/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.sql.JoinType;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionElement<J> implements CollectionElement<J> {
	private final PersistentCollectionMetadata persister;
	private final NavigableRole navigableRole;

	public AbstractCollectionElement(PersistentCollectionMetadata persister) {
		this.persister = persister;
		this.navigableRole = persister.getNavigableRole().append( NAVIGABLE_NAME );
	}

	@Override
	public PersistentCollectionMetadata getContainer() {
		return persister;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public Class<J> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public String asLoggableText() {
		return NAVIGABLE_NAME;
	}

	@Override
	public void applyTableReferenceJoins(
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector collector) {
		// only relevant for ONE-TO-MANY and MANY-TO-MANY - noop in the general case
	}
}
