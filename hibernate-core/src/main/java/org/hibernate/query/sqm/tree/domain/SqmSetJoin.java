/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Set;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.SetPersistentAttribute;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSetJoin;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.PathException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.SqmCreationProcessingState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmSetJoin<O, E>
		extends AbstractSqmPluralJoin<O,Set<E>, E>
		implements JpaSetJoin<O, E> {
	public SqmSetJoin(
			SqmFrom<?,O> lhs,
			SetPersistentAttribute<O, E> pluralValuedNavigable,
			String alias,
			SqmJoinType sqmJoinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, pluralValuedNavigable, alias, sqmJoinType, fetched, nodeBuilder );
	}

	@Override
	public SetPersistentAttribute<O,E> getReferencedPathSource() {
		//noinspection unchecked
		return (SetPersistentAttribute) super.getReferencedPathSource();
	}

	@Override
	public JavaTypeDescriptor<E> getJavaTypeDescriptor() {
		return getReferencedPathSource().getExpressableJavaTypeDescriptor();
	}

	@Override
	public SetPersistentAttribute<O,E> getModel() {
		return getReferencedPathSource();
	}

	@Override
	public SetPersistentAttribute<O,E> getAttribute() {
		return getReferencedPathSource();
	}

	@Override
	public SqmSetJoin<O, E> on(JpaExpression<Boolean> restriction) {
		return (SqmSetJoin<O, E>) super.on( restriction );
	}

	@Override
	public SqmSetJoin<O, E> on(Expression<Boolean> restriction) {
		return (SqmSetJoin<O, E>) super.on( restriction );
	}

	@Override
	public SqmSetJoin<O, E> on(JpaPredicate... restrictions) {
		return (SqmSetJoin<O, E>) super.on( restrictions );
	}

	@Override
	public SqmSetJoin<O, E> on(Predicate... restrictions) {
		return (SqmSetJoin<O, E>) super.on( restrictions );
	}

	@Override
	public SqmSetJoin<O, E> correlateTo(JpaSubQuery<E> subquery) {
		return (SqmSetJoin<O, E>) super.correlateTo( subquery );
	}

	@Override
	public <S extends E> SqmTreatedSetJoin<O,E,S> treatAs(Class<S> treatAsType) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatAsType ) );
	}

	@Override
	public <S extends E> SqmTreatedSetJoin<O,E,S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		return new SqmTreatedSetJoin<>( this, treatTarget, null );
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
	public SqmAttributeJoin makeCopy(SqmCreationProcessingState creationProcessingState) {
		//noinspection unchecked
		return new SqmSetJoin(
				creationProcessingState.getPathRegistry().findFromByPath( getLhs().getNavigablePath() ),
				getReferencedPathSource(),
				getExplicitAlias(),
				getSqmJoinType(),
				isFetched(),
				nodeBuilder()
		);
	}
}
