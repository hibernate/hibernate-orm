/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaJoin;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmTreatedJoin;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

import static org.hibernate.query.sqm.spi.SqmCreationHelper.combinePredicates;

/**
 * @author Steve Ebersole
 */
public interface SqmJoin<L, R> extends SqmFrom<L, R>, JpaJoin<L,R> {
	/**
	 * The type of join - inner, cross, etc
	 */
	SqmJoinType getSqmJoinType();

	/**
	 * When applicable, whether this join should be included in an implicit select clause
	 */
	boolean isImplicitlySelectable();

	/**
	 * Obtain the join predicate
	 *
	 * @return The join predicate
	 */
	SqmPredicate getJoinPredicate();

	/**
	 * Inject the join predicate
	 *
	 * @param predicate The join predicate
	 */
	void setJoinPredicate(SqmPredicate predicate);

	@Override
	<X, Y> SqmAttributeJoin<X, Y> join(String attributeName);

	@Override
	<X, Y> SqmAttributeJoin<X, Y> join(String attributeName, JoinType jt);

	@Override
	SqmJoin<L, R> copy(SqmCopyContext context);

	@Override
	<S extends R> SqmTreatedJoin<L,R,S> treatAs(Class<S> treatAsType);

	@Override
	<S extends R> SqmTreatedJoin<L,R,S> treatAs(EntityDomainType<S> treatAsType);

	@Override
	<S extends R> SqmTreatedJoin<L,R,S> treatAs(Class<S> treatJavaType, String alias);

	@Override
	<S extends R> SqmTreatedJoin<L,R,S> treatAs(EntityDomainType<S> treatTarget, String alias);

	@Override
	default SqmPredicate getOn() {
		return getJoinPredicate();
	}

	@Override
	default SqmJoin<L, R> on(JpaExpression<Boolean> restriction) {
		setJoinPredicate( combinePredicates(
				getJoinPredicate(),
				getJoinPredicate().nodeBuilder().wrap( restriction )
		) );
		return this;
	}

	@Override
	default SqmJoin<L, R> on(Expression<Boolean> restriction) {
		setJoinPredicate( combinePredicates(
				getJoinPredicate(),
				getJoinPredicate().nodeBuilder().wrap( restriction )
		) );
		return this;
	}

	@Override
	default SqmJoin<L, R> on(JpaPredicate... restrictions) {
		setJoinPredicate( combinePredicates(
				getJoinPredicate(),
				restrictions
		) );
		return this;
	}

	@Override
	default SqmJoin<L, R> on(Predicate... restrictions) {
		setJoinPredicate( combinePredicates(
				getJoinPredicate(),
				restrictions
		) );
		return this;
	}
}
