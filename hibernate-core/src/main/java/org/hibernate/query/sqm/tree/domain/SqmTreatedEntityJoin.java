/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmJoin;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedEntityJoin<T, S extends T> extends SqmEntityJoin<S> implements SqmTreatedPath<T,S> {
	private final SqmEntityJoin<T> wrappedPath;
	private final EntityDomainType<S> treatTarget;

	public SqmTreatedEntityJoin(
			SqmEntityJoin<T> wrappedPath,
			EntityDomainType<S> treatTarget,
			String alias,
			SqmJoinType joinType) {
		super(
				treatTarget,
				alias,
				joinType,
				wrappedPath.getRoot()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@Override
	public void addSqmJoin(SqmJoin<S, ?> join) {
		super.addSqmJoin( join );
		//noinspection unchecked
		wrappedPath.addSqmJoin( (SqmJoin<T, ?>) join );
	}

	@Override
	public EntityDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmPath<T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public SqmPathSource<S> getNodeType() {
		return treatTarget;
	}

	@Override
	public EntityDomainType<S> getReferencedPathSource() {
		//noinspection unchecked
		return (EntityDomainType<S>) wrappedPath.getReferencedPathSource();
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "treat(" );
		wrappedPath.appendHqlString( sb );
		sb.append( " as " );
		sb.append( treatTarget.getName() );
		sb.append( ')' );
	}
}
