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
public interface JpaEntityJoin<L,R> extends JpaJoinedFrom<L,R> {
	@Override
	EntityDomainType<R> getModel();

	@Override
	JpaEntityJoin<L,R> on(JpaExpression<Boolean> restriction);

	@Override
	JpaEntityJoin<L,R> on(Expression<Boolean> restriction);

	@Override
	JpaEntityJoin<L,R> on(JpaPredicate... restrictions);

	@Override
	JpaEntityJoin<L,R> on(Predicate... restrictions);
}
