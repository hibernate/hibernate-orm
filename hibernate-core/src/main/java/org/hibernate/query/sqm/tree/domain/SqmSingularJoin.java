/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Locale;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.criteria.PathException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmJoinable;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class SqmSingularJoin<O,T> extends AbstractSqmAttributeJoin<O,T> {
	public SqmSingularJoin(
			SqmFrom<?,O> lhs,
			SqmJoinable<O,T,T> joinedNavigable,
			String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, joinedNavigable, alias, joinType, fetched, nodeBuilder );
	}

	@Override
	public <S extends T> SqmTreatedSingularJoin<O,T,S> treatAs(Class<S> treatJavaType) throws PathException {
		final EntityTypeDescriptor<S> targetDescriptor = nodeBuilder().getDomainModel().entity( treatJavaType );
		return new SqmTreatedSingularJoin<>( this, targetDescriptor, null );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"SqmSingularJoin(%s : %s)",
				getNavigablePath().getFullPath(),
				getReferencedPathSource().getPathName()
		);
	}
}
