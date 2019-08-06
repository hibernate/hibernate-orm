/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Set;

import org.hibernate.metamodel.model.domain.SetPersistentAttribute;
import org.hibernate.query.sqm.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmSetJoin;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class SetAttributeImpl<X, E> extends AbstractPluralAttribute<X, Set<E>, E>
		implements SetPersistentAttribute<X, E> {
	public SetAttributeImpl(PluralAttributeBuilder<X, Set<E>, E, ?> xceBuilder) {
		super( xceBuilder );
	}

	@Override
	public CollectionType getCollectionType() {
		return CollectionType.SET;
	}

	@Override
	public SqmAttributeJoin createSqmJoin(
			SqmFrom lhs, SqmJoinType joinType, String alias, boolean fetched, SqmCreationState creationState) {
		return new SqmSetJoin(
				lhs,
				this,
				alias,
				joinType,
				fetched,
				creationState.getCreationContext().getNodeBuilder()
		);
	}
}
