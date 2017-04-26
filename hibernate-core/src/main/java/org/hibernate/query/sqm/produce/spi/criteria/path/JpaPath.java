/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi.criteria.path;

import java.util.Collection;
import java.util.Map;
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.query.sqm.domain.SqmNavigable;
import org.hibernate.query.sqm.produce.spi.criteria.JpaExpression;

/**
 * @author Steve Ebersole
 */
public interface JpaPath<X> extends Path<X>, JpaExpression<X> {
	SqmNavigable getNavigable();

	@Override
	JpaPathSource<?> getParentPath();

	/**
	 * Defines handling for the JPA 2.1 TREAT down-casting feature.
	 *
	 * @param treatAsType The type to treat the path as.
	 * @param <T> The parameterized type representation of treatAsType.
	 *
	 * @return The properly typed view of this path.
	 */
	<T extends X> JpaPath<T> treatAs(Class<T> treatAsType);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Co-variant returns


	@Override
	<Y> JpaPath<Y> get(SingularAttribute<? super X, Y> attribute);

	@Override
	<E, C extends Collection<E>> JpaExpression<C> get(PluralAttribute<X, C, E> collection);

	@Override
	<K, V, M extends Map<K, V>> JpaExpression<M> get(MapAttribute<X, K, V> map);

	@Override
	JpaExpression<Class<? extends X>> type();

	@Override
	<Y> JpaPath<Y> get(String attributeName);
}
