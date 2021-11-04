/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;

/**
 * Consolidates the {@link Join} and {@link Fetch} hierarchies since that is how we implement them.
 * This allows us to treat them polymorphically.
*
* @author Steve Ebersole
*/
public interface JpaJoin<O, T> extends JpaJoinedFrom<O, T>, Join<O, T> {
	@Override
	PersistentAttribute<? super O, ?> getAttribute();

	JpaJoin<O, T> on(JpaExpression<Boolean> restriction);

	@Override
	JpaJoin<O, T> on(Expression<Boolean> restriction);

	JpaJoin<O, T> on(JpaPredicate... restrictions);

	@Override
	JpaJoin<O, T> on(Predicate... restrictions);

	@Override
	<S extends T> JpaJoin<O, S> treatAs(Class<S> treatAsType);

	@Override
	<S extends T> JpaJoin<O, S> treatAs(EntityDomainType<S> treatAsType);
}
