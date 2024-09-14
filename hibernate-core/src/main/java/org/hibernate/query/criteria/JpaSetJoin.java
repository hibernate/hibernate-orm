/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria;

import java.util.Set;

import org.hibernate.metamodel.model.domain.EntityDomainType;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.SetJoin;

/**
 * Specialization of {@link JpaJoin} for {@link java.util.Set} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface JpaSetJoin<O, T> extends JpaPluralJoin<O, Set<T>, T>, SetJoin<O, T> {

	@Override
	JpaSetJoin<O, T> on(JpaExpression<Boolean> restriction);

	JpaSetJoin<O, T> on(Expression<Boolean> restriction);

	@Override
	JpaSetJoin<O, T> on(JpaPredicate... restrictions);

	@Override
	JpaSetJoin<O, T> on(Predicate... restrictions);

	@Override
	<S extends T> JpaTreatedJoin<O,T,S> treatAs(Class<S> treatAsType);

	@Override
	<S extends T> JpaTreatedJoin<O,T,S> treatAs(EntityDomainType<S> treatAsType);
}
