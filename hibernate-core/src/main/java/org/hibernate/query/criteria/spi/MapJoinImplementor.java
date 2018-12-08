/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.metamodel.model.domain.spi.MapPersistentAttribute;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaMapJoin;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.criteria.PathException;

/**
 * @author Steve Ebersole
 */
public interface MapJoinImplementor<O, K, V> extends JoinImplementor<O, V>, JpaMapJoin<O, K, V> {

	@Override
	MapPersistentAttribute<? super O, K, V> getAttribute();

	@Override
	default MapPersistentAttribute<? super O, K, V> getModel() {
		return getAttribute();
	}

	@Override
	MapJoinImplementor<O, K, V> on(JpaExpression<Boolean> restriction);

	@Override
	MapJoinImplementor<O, K, V> on(Expression<Boolean> restriction);

	@Override
	MapJoinImplementor<O, K, V> on(JpaPredicate... restrictions);

	@Override
	MapJoinImplementor<O, K, V> on(Predicate... restrictions);

	@Override
	MapJoinImplementor<O, K, V> correlateTo(JpaSubQuery<V> subquery);

	@Override
	<S extends V> MapJoinImplementor<O, K, S> treatAs(Class<S> treatJavaType) throws PathException;
}
