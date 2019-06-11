/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.List;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ListPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaListJoin;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.PathException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmListJoin<O,E> extends AbstractSqmPluralJoin<O,List<E>, E> implements JpaListJoin<O, E> {
	public SqmListJoin(
			SqmFrom<?,O> lhs,
			ListPersistentAttribute<O, E> listAttribute,
			String alias,
			SqmJoinType sqmJoinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, listAttribute, alias, sqmJoinType, fetched, nodeBuilder );
	}

	@Override
	public ListPersistentAttribute<O, E> getModel() {
		return (ListPersistentAttribute<O, E>) super.getModel();
	}

	@Override
	public ListPersistentAttribute<O,E> getReferencedPathSource() {
		//noinspection unchecked
		return (ListPersistentAttribute) super.getReferencedPathSource();
	}

	@Override
	public JavaTypeDescriptor<E> getJavaTypeDescriptor() {
		return getNodeJavaTypeDescriptor();
	}

	@Override
	public SqmPath<Integer> index() {
		final String navigableName = "{index}";
		final NavigablePath navigablePath = getNavigablePath().append( navigableName );

		//noinspection unchecked
		return new SqmBasicValuedSimplePath<>(
				navigablePath,
				( (ListPersistentAttribute) getReferencedPathSource() ).getIndexPathSource(),
				this,
				nodeBuilder()
		);
	}

	@Override
	public SqmListJoin<O, E> on(JpaExpression<Boolean> restriction) {
		return (SqmListJoin<O, E>) super.on( restriction );
	}

	@Override
	public SqmListJoin<O, E> on(Expression<Boolean> restriction) {
		return (SqmListJoin<O, E>) super.on( restriction );
	}

	@Override
	public SqmListJoin<O, E> on(JpaPredicate... restrictions) {
		return (SqmListJoin<O, E>) super.on( restrictions );
	}

	@Override
	public SqmListJoin<O, E> on(Predicate... restrictions) {
		return (SqmListJoin<O, E>) super.on( restrictions );
	}

	@Override
	public SqmListJoin<O, E> correlateTo(JpaSubQuery<E> subquery) {
		return (SqmListJoin<O, E>) super.correlateTo( subquery );
	}

	@Override
	public <S extends E> SqmTreatedListJoin<O,E,S> treatAs(Class<S> treatAsType) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatAsType ) );
	}

	@Override
	public <S extends E> SqmTreatedListJoin<O,E,S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		//noinspection unchecked
		return new SqmTreatedListJoin( this, treatTarget, null );
	}

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
		return new SqmListJoin(
				creationProcessingState.getPathRegistry().findFromByPath( getLhs().getNavigablePath() ),
				getReferencedPathSource(),
				getExplicitAlias(),
				getSqmJoinType(),
				isFetched(),
				nodeBuilder()
		);
	}
}
