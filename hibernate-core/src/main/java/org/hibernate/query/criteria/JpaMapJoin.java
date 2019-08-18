/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Predicate;

/**
 * Specialization of {@link JpaJoin} for {@link java.util.Map} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface JpaMapJoin<O,K,V> extends JpaJoin<O,V>, MapJoin<O,K,V> {
	@Override
	JpaMapJoin<O, K, V> on(JpaExpression<Boolean> restriction);

	@Override
	JpaMapJoin<O, K, V> on(Expression<Boolean> restriction);

	@Override
	JpaMapJoin<O, K, V> on(JpaPredicate... restrictions);

	@Override
	JpaMapJoin<O, K, V> on(Predicate... restrictions);

	@Override
	JpaMapJoin<O, K, V> correlateTo(JpaSubQuery<V> subquery);

	@Override
	<S extends V> JpaMapJoin<O, K, S> treatAs(Class<S> treatAsType);
}
