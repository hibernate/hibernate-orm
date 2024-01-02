/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.EntityType;

/**
 * @author Steve Ebersole
 */
public interface JpaCriteriaDelete<T> extends JpaManipulationCriteria<T>, CriteriaDelete<T> {

	@Override
	JpaRoot<T> from(Class<T> entityClass);

	@Override
	JpaRoot<T> from(EntityType<T> entity);

	@Override
	JpaRoot<T> getRoot();

	@Override
	JpaCriteriaDelete<T> where(Expression<Boolean> restriction);

	@Override
	JpaCriteriaDelete<T> where(Predicate... restrictions);
}
