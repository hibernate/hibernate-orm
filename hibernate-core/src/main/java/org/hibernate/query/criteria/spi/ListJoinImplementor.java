/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.metamodel.model.domain.spi.ListPersistentAttribute;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaListJoin;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.criteria.PathException;

/**
 * @author Steve Ebersole
 */
public interface ListJoinImplementor<O,T> extends JoinImplementor<O,T>, JpaListJoin<O,T> {

	@Override
	ListPersistentAttribute<? super O, ?> getAttribute();

	@Override
	@SuppressWarnings("unchecked")
	default ListPersistentAttribute<? super O, T> getModel() {
		return (ListPersistentAttribute<? super O, T>) getAttribute();
	}

	@Override
	ListJoinImplementor<O, T> on(JpaExpression<Boolean> restriction);

	@Override
	ListJoinImplementor<O, T> on(Expression<Boolean> restriction);

	@Override
	ListJoinImplementor<O, T> on(JpaPredicate... restrictions);

	@Override
	ListJoinImplementor<O, T> on(Predicate... restrictions);

	@Override
	ListJoinImplementor<O, T> correlateTo(JpaSubQuery<T> subquery);

	@Override
	<S extends T> ListJoinImplementor<O, S> treatAs(Class<S> treatJavaType) throws PathException;
}
