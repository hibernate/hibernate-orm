/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.metamodel.model.domain.spi.SetPersistentAttribute;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSetJoin;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.criteria.PathException;

/**
 * @author Steve Ebersole
 */
public interface SetJoinImplementor<O,T> extends JoinImplementor<O,T>, JpaSetJoin<O,T> {

	@Override
	SetPersistentAttribute<? super O, ?> getAttribute();

	@Override
	@SuppressWarnings("unchecked")
	default SetPersistentAttribute<? super O, T> getModel() {
		return (SetPersistentAttribute<? super O, T>) getAttribute();
	}

	@Override
	SetJoinImplementor<O, T> on(JpaExpression<Boolean> restriction);

	@Override
	SetJoinImplementor<O, T> on(Expression<Boolean> restriction);

	@Override
	SetJoinImplementor<O, T> on(JpaPredicate... restrictions);

	@Override
	SetJoinImplementor<O, T> on(Predicate... restrictions);

	@Override
	SetJoinImplementor<O, T> correlateTo(JpaSubQuery<T> subquery);

	@Override
	<S extends T> SetJoinImplementor<O, S> treatAs(Class<S> treatJavaType) throws PathException;
}
