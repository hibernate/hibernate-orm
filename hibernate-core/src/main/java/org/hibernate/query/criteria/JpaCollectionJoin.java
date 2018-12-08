/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

/**
 * Specialization of {@link JpaJoin} for {@link java.util.Collection} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface JpaCollectionJoin<O, T> extends JpaJoin<O, T>, CollectionJoin<O, T> {
	@Override
	JpaCollectionJoin<O, T> correlateTo(JpaSubQuery<T> subquery);

	@Override
	JpaCollectionJoin<O, T> on(JpaExpression<Boolean> restriction);

	@Override
	JpaCollectionJoin<O, T> on(Expression<Boolean> restriction);

	@Override
	JpaCollectionJoin<O, T> on(JpaPredicate... restrictions);

	@Override
	JpaCollectionJoin<O, T> on(Predicate... restrictions);

	@Override
	<S extends T> JpaCollectionJoin<O, S> treatAs(Class<S> treatAsType);
}
