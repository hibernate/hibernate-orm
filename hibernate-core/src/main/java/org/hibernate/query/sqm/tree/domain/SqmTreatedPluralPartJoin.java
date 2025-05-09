/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.spi.NavigablePath;

import java.util.Objects;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("rawtypes")
public class SqmTreatedPluralPartJoin extends SqmPluralPartJoin implements SqmTreatedJoin {
	private final SqmPluralPartJoin wrappedPath;
	private final SqmEntityDomainType treatTarget;

	public SqmTreatedPluralPartJoin(
			SqmPluralPartJoin wrappedPath,
			SqmEntityDomainType treatTarget,
			String alias) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				wrappedPath.getNavigablePath()
						.treatAs( treatTarget.getHibernateEntityName(), alias ),
				wrappedPath.getReferencedPathSource(),
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	private SqmTreatedPluralPartJoin(
			NavigablePath navigablePath,
			SqmPluralPartJoin wrappedPath,
			SqmEntityDomainType treatTarget,
			String alias) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				navigablePath,
				wrappedPath.getReferencedPathSource(),
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	public SqmTreatedPluralPartJoin copy(SqmCopyContext context) {
		final SqmTreatedPluralPartJoin existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTreatedPluralPartJoin path = context.registerCopy(
				this,
				new SqmTreatedPluralPartJoin(
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
	public SqmPluralPartJoin getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public EntityDomainType getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmPathSource getNodeType() {
		return treatTarget;
	}

	@Override
	public SqmPathSource getReferencedPathSource() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<?> getResolvedModel() {
		return treatTarget;
	}

	@Override
	public SqmTreatedPluralPartJoin treatAs(Class treatJavaType) {
		//noinspection unchecked
		return super.treatAs( treatJavaType );
	}

	@Override
	public SqmTreatedPluralPartJoin treatAs(EntityDomainType treatTarget) {
		//noinspection unchecked
		return super.treatAs( treatTarget );
	}

	@Override
	public SqmTreatedPluralPartJoin treatAs(Class treatJavaType, String alias) {
		//noinspection unchecked
		return super.treatAs( treatJavaType, alias );
	}

	@Override
	public SqmTreatedPluralPartJoin treatAs(EntityDomainType treatTarget, String alias) {
		//noinspection unchecked
		return super.treatAs( treatTarget, alias );
	}

	@Override
	public SqmTreatedPluralPartJoin treatAs(Class treatJavaType, String alias, boolean fetch) {
		//noinspection unchecked
		return super.treatAs( treatJavaType, alias, fetch );
	}

	@Override
	public SqmTreatedPluralPartJoin treatAs(EntityDomainType treatTarget, String alias, boolean fetch) {
		//noinspection unchecked
		return super.treatAs( treatTarget, alias, fetch );
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
		return object instanceof SqmTreatedPluralPartJoin that
			&& Objects.equals( this.getExplicitAlias(), that.getExplicitAlias() )
			&& Objects.equals( this.treatTarget.getName(), that.treatTarget.getName() )
			&& Objects.equals( this.wrappedPath.getNavigablePath(), that.wrappedPath.getNavigablePath() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( treatTarget.getName(), wrappedPath.getNavigablePath() );
	}
}
