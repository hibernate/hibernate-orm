/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.TreatableDomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.from.SqmTreatedAttributeJoin;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.util.Objects;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedSetJoin<O,T, S extends T> extends SqmSetJoin<O,S> implements SqmTreatedAttributeJoin<O,T,S> {
	private final SqmSetJoin<O,T> wrappedPath;
	private final SqmTreatableDomainType<S> treatTarget;

	public SqmTreatedSetJoin(
			SqmSetJoin<O, T> wrappedPath,
			SqmTreatableDomainType<S> treatTarget,
			String alias) {
		this( wrappedPath, treatTarget, alias, false );
	}

	public SqmTreatedSetJoin(
				SqmSetJoin<O, T> wrappedPath,
				SqmTreatableDomainType<S> treatTarget,
				String alias,
				boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				wrappedPath.getNavigablePath()
						.append( CollectionPart.Nature.ELEMENT.getName() )
						.treatAs( treatTarget.getTypeName(), alias ),
				(SqmSetPersistentAttribute<O, S>) wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				fetched,
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	private SqmTreatedSetJoin(
			NavigablePath navigablePath,
			SqmSetJoin<O, T> wrappedPath,
			SqmTreatableDomainType<S> treatTarget,
			String alias,
			boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				navigablePath,
				(SqmSetPersistentAttribute<O, S>) wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				fetched,
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	public SqmTreatedSetJoin<O, T, S> copy(SqmCopyContext context) {
		final SqmTreatedSetJoin<O, T, S> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTreatedSetJoin<O, T, S> path = context.registerCopy(
				this,
				new SqmTreatedSetJoin<>(
						getNavigablePath(),
						wrappedPath.copy( context ),
						treatTarget,
						getExplicitAlias(),
						isFetched()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public SqmSetJoin<O,T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public TreatableDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<S> getNodeType() {
		return treatTarget;
	}

	@Override
	public SqmTreatableDomainType<S> getReferencedPathSource() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<S> getResolvedModel() {
		return treatTarget;
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "treat(" );
		wrappedPath.appendHqlString( hql, context );
		hql.append( " as " );
		hql.append( treatTarget.getTypeName() );
		hql.append( ')' );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmTreatedSetJoin<?, ?, ?> that
			&& Objects.equals( this.getExplicitAlias(), that.getExplicitAlias() )
			&& Objects.equals( this.treatTarget.getTypeName(), that.treatTarget.getTypeName() )
			&& Objects.equals( this.wrappedPath.getNavigablePath(), that.wrappedPath.getNavigablePath() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( treatTarget.getTypeName(), wrappedPath.getNavigablePath() );
	}

	@Override
	public SqmTreatedSetJoin<O, T, S> on(JpaExpression<Boolean> restriction) {
		return (SqmTreatedSetJoin<O, T, S>) super.on( restriction );
	}

	@Override
	public SqmTreatedSetJoin<O, T, S> on(Expression<Boolean> restriction) {
		return (SqmTreatedSetJoin<O, T, S>) super.on( restriction );
	}

	@Override
	public SqmTreatedSetJoin<O, T, S> on(JpaPredicate... restrictions) {
		return (SqmTreatedSetJoin<O, T, S>) super.on( restrictions );
	}

	@Override
	public SqmTreatedSetJoin<O, T, S> on(Predicate... restrictions) {
		return (SqmTreatedSetJoin<O, T, S>) super.on( restrictions );
	}
}
