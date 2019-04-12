/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.type.ForeignKeyDirection;

/**
 * Specialization for Navigable that can be used in creating TableGroupJoins
 *
 * @author Steve Ebersole
 */
public interface Joinable<O,T> extends PersistentAttributeDescriptor<O,T>, NavigableContainer<T> {
	@Override
	default Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	default ForeignKeyDirection getForeignKeyDirection() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	default boolean isCascadeDeleteEnabled() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	SqmAttributeJoin createSqmJoin(
			SqmFrom lhs,
			SqmJoinType joinType,
			String alias,
			boolean fetched,
			SqmCreationState creationState);
}
