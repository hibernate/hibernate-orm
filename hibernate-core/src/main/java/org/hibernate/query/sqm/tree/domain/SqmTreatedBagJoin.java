/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.BagPersistentAttribute;
import org.hibernate.metamodel.model.domain.TreatableDomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.from.SqmTreatedAttributeJoin;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedBagJoin<L, R, R1 extends R> extends SqmBagJoin<L, R1> implements SqmTreatedAttributeJoin<L, R, R1> {
	private final SqmBagJoin<L, R> wrappedPath;
	private final TreatableDomainType<R1> treatTarget;

	public SqmTreatedBagJoin(
			SqmBagJoin<L, R> wrappedPath,
			TreatableDomainType<R1> treatTarget,
			String alias) {
		this( wrappedPath, treatTarget, alias, false );
	}

	public SqmTreatedBagJoin(
			SqmBagJoin<L, R> wrappedPath,
			TreatableDomainType<R1> treatTarget,
			String alias,
			boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				wrappedPath.getNavigablePath()
						.append( CollectionPart.Nature.ELEMENT.getName() )
						.treatAs( treatTarget.getTypeName(), alias ),
				(BagPersistentAttribute<L, R1>) wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				fetched,
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	private SqmTreatedBagJoin(
			NavigablePath navigablePath,
			SqmBagJoin<L, R> wrappedPath,
			TreatableDomainType<R1> treatTarget,
			String alias,
			boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				navigablePath,
				(BagPersistentAttribute<L, R1>) wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.isFetched(),
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	public SqmTreatedBagJoin<L, R, R1> copy(SqmCopyContext context) {
		final SqmTreatedBagJoin<L, R, R1> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTreatedBagJoin<L, R, R1> path = context.registerCopy(
				this,
				new SqmTreatedBagJoin<>(
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
	public SqmBagJoin<L, R> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public TreatableDomainType<R1> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<R1> getNodeType() {
		return treatTarget;
	}

	@Override
	public TreatableDomainType<R1> getReferencedPathSource() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<?> getResolvedModel() {
		return treatTarget;
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "treat(" );
		wrappedPath.appendHqlString( sb );
		sb.append( " as " );
		sb.append( treatTarget.getTypeName() );
		sb.append( ')' );
	}

	@Override
	public SqmTreatedBagJoin<L,R,R1> on(JpaExpression<Boolean> restriction) {
		return (SqmTreatedBagJoin<L, R, R1>) super.on( restriction );
	}

	@Override
	public SqmTreatedBagJoin<L,R,R1> on(Expression<Boolean> restriction) {
		return (SqmTreatedBagJoin<L, R, R1>) super.on( restriction );
	}

	@Override
	public SqmTreatedBagJoin<L,R,R1> on(JpaPredicate... restrictions) {
		return (SqmTreatedBagJoin<L, R, R1>) super.on( restrictions );
	}

	@Override
	public SqmTreatedBagJoin<L,R,R1> on(Predicate... restrictions) {
		return (SqmTreatedBagJoin<L, R, R1>) super.on( restrictions );
	}
}
