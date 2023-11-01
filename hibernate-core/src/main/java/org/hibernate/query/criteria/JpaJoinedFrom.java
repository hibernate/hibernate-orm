/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * Exists within the hierarchy mainly to support "entity joins".
 *
 * @see JpaEntityJoin
 * @see org.hibernate.query.sqm.tree.from.SqmEntityJoin
 * @see JpaDerivedJoin
 * @see org.hibernate.query.sqm.tree.from.SqmDerivedJoin
 *
 * @author Steve Ebersole
 */
public interface JpaJoinedFrom<L, R> extends JpaFrom<L,R>, JpaJoin<L,R> {

	JpaJoinedFrom<L, R> on(JpaExpression<Boolean> restriction);

	JpaJoinedFrom<L, R> on(Expression<Boolean> restriction);

	JpaJoinedFrom<L, R> on(JpaPredicate... restrictions);

	JpaJoinedFrom<L, R> on(Predicate... restrictions);

	JpaPredicate getOn();

}
