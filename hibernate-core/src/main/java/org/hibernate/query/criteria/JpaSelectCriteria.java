/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import java.util.List;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.EntityType;

/**
 * Commonality between a JPA {@link JpaCriteriaQuery} and {@link JpaSubQuery},
 * mainly in the form of delegation to {@link JpaQueryStructure}
 *
 * @author Steve Ebersole
 */
public interface JpaSelectCriteria<T> extends AbstractQuery<T>, JpaCriteriaBase {
	/**
	 * The query structure.  See {@link JpaQueryStructure} for details
	 */
	JpaQueryStructure<T> getQuerySpec();

	@Override
	JpaSelectCriteria<T> distinct(boolean distinct);

	@Override
	JpaSelection<T> getSelection();

	@Override
	<X> JpaRoot<X> from(Class<X> entityClass);

	@Override
	<X> JpaRoot<X> from(EntityType<X> entity);

	@Override
	JpaPredicate getRestriction();

	@Override
	JpaSelectCriteria<T> where(Expression<Boolean> restriction);

	@Override
	JpaSelectCriteria<T> where(Predicate... restrictions);

	@Override
	JpaSelectCriteria<T> groupBy(Expression<?>... grouping);

	@Override
	JpaSelectCriteria<T> groupBy(List<Expression<?>> grouping);

	@Override
	JpaPredicate getGroupRestriction();

	@Override
	JpaSelectCriteria<T> having(Expression<Boolean> restriction);

	@Override
	JpaSelectCriteria<T> having(Predicate... restrictions);
}
