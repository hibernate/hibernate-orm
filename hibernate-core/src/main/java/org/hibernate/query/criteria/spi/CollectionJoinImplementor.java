/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.metamodel.model.domain.spi.BagPersistentAttribute;
import org.hibernate.query.criteria.JpaCollectionJoin;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.criteria.PathException;

/**
 * @author Steve Ebersole
 */
public interface CollectionJoinImplementor<O,T> extends JoinImplementor<O,T>, JpaCollectionJoin<O,T> {

	@Override
	BagPersistentAttribute<? super O, ?> getAttribute();

	@Override
	@SuppressWarnings("unchecked")
	default BagPersistentAttribute<? super O, T> getModel() {
		return (BagPersistentAttribute<? super O, T>) getAttribute();
	}

	@Override
	CollectionJoinImplementor<O, T> on(JpaExpression<Boolean> restriction);

	@Override
	CollectionJoinImplementor<O, T> on(Expression<Boolean> restriction);

	@Override
	CollectionJoinImplementor<O, T> on(JpaPredicate... restrictions);

	@Override
	CollectionJoinImplementor<O, T> on(Predicate... restrictions);

	@Override
	CollectionJoinImplementor<O, T> correlateTo(JpaSubQuery<T> subquery);

	@Override
	<S extends T> CollectionJoinImplementor<O, S> treatAs(Class<S> treatJavaType) throws PathException;
}
