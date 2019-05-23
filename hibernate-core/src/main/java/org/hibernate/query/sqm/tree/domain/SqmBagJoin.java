/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Collection;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.BagPersistentAttribute;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaCollectionJoin;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.SqmCreationProcessingState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmBagJoin<O, E> extends AbstractSqmPluralJoin<O,Collection<E>, E> implements JpaCollectionJoin<O, E> {
	@SuppressWarnings("WeakerAccess")
	public SqmBagJoin(
			SqmFrom<?,O> lhs,
			BagPersistentAttribute<O,E> attribute,
			String alias,
			SqmJoinType sqmJoinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, attribute, alias, sqmJoinType, fetched, nodeBuilder );
	}

	@Override
	public BagPersistentAttribute<O,E> getReferencedPathSource() {
		//noinspection unchecked
		return (BagPersistentAttribute) super.getReferencedPathSource();
	}

	@Override
	public JavaTypeDescriptor<E> getJavaTypeDescriptor() {
		return getModel().getExpressableJavaTypeDescriptor();
	}

	@Override
	public BagPersistentAttribute<O,E> getModel() {
		return (BagPersistentAttribute<O, E>) getReferencedPathSource();
	}

	@Override
	public BagPersistentAttribute<O,E> getAttribute() {
		//noinspection unchecked
		return (BagPersistentAttribute<O, E>) super.getAttribute();
	}

	@Override
	public SqmBagJoin<O, E> on(JpaExpression<Boolean> restriction) {
		return (SqmBagJoin<O, E>) super.on( restriction );
	}

	@Override
	public SqmBagJoin<O, E> on(Expression<Boolean> restriction) {
		return (SqmBagJoin<O, E>) super.on( restriction );
	}

	@Override
	public SqmBagJoin<O, E> on(JpaPredicate... restrictions) {
		return (SqmBagJoin<O, E>) super.on( restrictions );
	}

	@Override
	public SqmBagJoin<O, E> on(Predicate... restrictions) {
		return (SqmBagJoin<O, E>) super.on( restrictions );
	}

	// todo (6.0) : need to resolve these fetches against the element/index descriptors

	@Override
	public <A> SqmSingularJoin<E, A> fetch(SingularAttribute<? super E, A> attribute) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <A> SqmSingularJoin<E,A> fetch(SingularAttribute<? super E, A> attribute, JoinType jt) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <F> SqmAttributeJoin<E,F> fetch(PluralAttribute<? super E, ?, F> attribute) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <F> SqmAttributeJoin<E,F> fetch(PluralAttribute<? super E, ?, F> attribute, JoinType jt) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <X,Y> SqmAttributeJoin<X,Y> fetch(String attributeName) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <X,Y> SqmAttributeJoin<X,Y> fetch(String attributeName, JoinType jt) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmBagJoin<O, E> correlateTo(JpaSubQuery<E> subquery) {
		return (SqmBagJoin<O, E>) super.correlateTo( subquery );
	}

	@Override
	public <S extends E> SqmTreatedBagJoin<O, E, S> treatAs(Class<S> treatAsType) {
		final EntityDomainType<S> entityTypeDescriptor = nodeBuilder().getDomainModel().entity( treatAsType );
		//noinspection unchecked
		return new SqmTreatedBagJoin( this, entityTypeDescriptor, null );
	}

	@Override
	public SqmAttributeJoin makeCopy(SqmCreationProcessingState creationProcessingState) {
		return new SqmBagJoin(
				creationProcessingState.getPathRegistry().findFromByPath( getLhs().getNavigablePath() ),
				getReferencedPathSource(),
				getExplicitAlias(),
				getSqmJoinType(),
				isFetched(),
				nodeBuilder()
		);
	}
}
