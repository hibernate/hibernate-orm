/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Predicate;

/**
 * Specialization of {@link JpaJoin} for {@link java.util.List} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface JpaListJoin<O, T> extends JpaJoin<O, T>, ListJoin<O, T> {
	@Override
	JpaListJoin<O, T> on(JpaExpression<Boolean> restriction);

	@Override
	JpaListJoin<O, T> on(Expression<Boolean> restriction);

	@Override
	JpaListJoin<O, T> on(JpaPredicate... restrictions);

	@Override
	JpaListJoin<O, T> on(Predicate... restrictions);

	@Override
	JpaListJoin<O, T> correlateTo(JpaSubQuery<T> subquery);

	@Override
	<S extends T> JpaListJoin<O, S> treatAs(Class<S> treatAsType);
}
