/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import org.hibernate.metamodel.model.domain.EntityDomainType;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
public interface JpaEntityJoin<T> extends JpaJoinedFrom<T,T> {
	@Override
	EntityDomainType<T> getModel();

	@Override
	JpaEntityJoin<T> on(JpaExpression<Boolean> restriction);

	@Override
	JpaEntityJoin<T> on(Expression<Boolean> restriction);

	@Override
	JpaEntityJoin<T> on(JpaPredicate... restrictions);

	@Override
	JpaEntityJoin<T> on(Predicate... restrictions);
}
