/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.SetJoin;

/**
 * Specialization of {@link JpaJoin} for {@link java.util.Set} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface JpaSetJoin<O, T> extends JpaJoin<O, T>, SetJoin<O, T> {
	@Override
	JpaSetJoin<O, T> correlateTo(JpaSubQuery<T> subquery);

	@Override
	JpaSetJoin<O, T> on(JpaExpression<Boolean> restriction);

	@Override
	JpaSetJoin<O, T> on(Expression<Boolean> restriction);

	@Override
	JpaSetJoin<O, T> on(JpaPredicate... restrictions);

	@Override
	JpaSetJoin<O, T> on(Predicate... restrictions);

	@Override
	<S extends T> JpaSetJoin<O, S> treatAs(Class<S> treatAsType);
}
