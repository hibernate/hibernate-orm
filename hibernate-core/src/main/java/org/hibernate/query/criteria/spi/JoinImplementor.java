/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaJoin;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.PathException;

/**
 * SPI-level contract for {@link org.hibernate.query.criteria.JpaJoin}
 * implementors
 *
 * @author Steve Ebersole
 */
public interface JoinImplementor<O,T> extends FromImplementor<O,T>, JpaJoin<O,T> {
	@Override
	JpaJoin<O, T> on(JpaExpression<Boolean> restriction);

	@Override
	JpaJoin<O, T> on(Expression<Boolean> restriction);

	@Override
	JpaJoin<O, T> on(JpaPredicate... restrictions);

	@Override
	JpaJoin<O, T> on(Predicate... restrictions);

	@Override
	<S extends T> JoinImplementor<O, S> treatAs(Class<S> treatJavaType) throws PathException;
}
