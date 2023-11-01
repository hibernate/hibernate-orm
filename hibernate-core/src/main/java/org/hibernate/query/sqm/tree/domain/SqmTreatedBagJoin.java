/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.BagPersistentAttribute;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmTreatedAttributeJoin;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedBagJoin<L, R, R1 extends R> extends SqmBagJoin<L, R1> implements SqmTreatedAttributeJoin<L, R, R1> {
	private final SqmBagJoin<L, R> wrappedPath;
	private final EntityDomainType<R1> treatTarget;

	public SqmTreatedBagJoin(
			SqmBagJoin<L, R> wrappedPath,
			EntityDomainType<R1> treatTarget,
			String alias) {
		this( wrappedPath, treatTarget, alias, false );
	}

	public SqmTreatedBagJoin(
			SqmBagJoin<O, T> wrappedPath,
			EntityDomainType<S> treatTarget,
			String alias,
			boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				wrappedPath.getNavigablePath()
						.append( CollectionPart.Nature.ELEMENT.getName() )
						.treatAs( treatTarget.getHibernateEntityName(), alias ),
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
			EntityDomainType<R1> treatTarget,
			String alias,
			boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				wrappedPath.getNavigablePath().treatAs(
						treatTarget.getHibernateEntityName(),
						alias
				),
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
	public EntityDomainType<R1> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<R1> getNodeType() {
		return treatTarget;
	}

	@Override
	public EntityDomainType<R1> getReferencedPathSource() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<?> getResolvedModel() {
		return treatTarget;
	}

	@Override
	public SqmAttributeJoin<L, R1> makeCopy(SqmCreationProcessingState creationProcessingState) {
		return new SqmTreatedBagJoin<>( wrappedPath, treatTarget, getAlias() );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "treat(" );
		wrappedPath.appendHqlString( sb );
		sb.append( " as " );
		sb.append( treatTarget.getName() );
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
