/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.spi.NavigablePath;

import java.util.Objects;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedEntityJoin<L,R,S extends R> extends SqmEntityJoin<L,S> implements SqmTreatedJoin<L,R,S> {
	private final SqmEntityJoin<L,R> wrappedPath;
	private final SqmEntityDomainType<S> treatTarget;

	public SqmTreatedEntityJoin(
			SqmEntityJoin<L,R> wrappedPath,
			SqmEntityDomainType<S> treatTarget,
			String alias) {
		super(
				wrappedPath.getNavigablePath().treatAs(
						treatTarget.getHibernateEntityName(),
						alias
				),
				treatTarget,
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.getRoot()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	private SqmTreatedEntityJoin(
			NavigablePath navigablePath,
			SqmEntityJoin<L,R> wrappedPath,
			SqmEntityDomainType<S> treatTarget,
			String alias) {
		super(
				navigablePath,
				treatTarget,
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.getRoot()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@Override
	public SqmTreatedEntityJoin<L,R,S> copy(SqmCopyContext context) {
		final SqmTreatedEntityJoin<L,R,S> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTreatedEntityJoin<L,R,S> path = context.registerCopy(
				this,
				new SqmTreatedEntityJoin<>(
						getNavigablePath(),
						wrappedPath.copy( context ),
						treatTarget,
						getExplicitAlias()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public EntityDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmEntityDomainType<S> getModel() {
		return treatTarget;
	}

	@Override
	public SqmPath<R> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public SqmPathSource<S> getNodeType() {
		return treatTarget;
	}

	@Override
	public SqmEntityDomainType<S> getReferencedPathSource() {
		return treatTarget;
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "treat(" );
		wrappedPath.appendHqlString( hql, context );
		hql.append( " as " );
		hql.append( treatTarget.getName() );
		hql.append( ')' );
	}


	@Override
	public boolean equals(Object object) {
		return object instanceof SqmTreatedEntityJoin<?,?,?> that
			&& Objects.equals( this.getExplicitAlias(), that.getExplicitAlias() )
			&& Objects.equals( this.treatTarget.getName(), that.treatTarget.getName() )
			&& Objects.equals( this.wrappedPath.getNavigablePath(), that.wrappedPath.getNavigablePath() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( treatTarget.getName(), wrappedPath.getNavigablePath() );
	}
}
