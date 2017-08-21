/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.Collection;
import java.util.Map;
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.query.sqm.produce.spi.criteria.JpaExpression;
import org.hibernate.query.sqm.produce.spi.criteria.path.JpaPath;
import org.hibernate.query.sqm.produce.spi.criteria.path.JpaPathSource;

/**
 * Hibernate ORM specialization of the JPA {@link Path}
 * contract.
 *
 * @implNote This interface extends {@link JpaPathSourceImplementor}.  We do
 * this because that is how JPA models it and, of course, as a JPA-extension,
 * this contract needs to be defined that way.  However, it is important to
 * understand that not all paths can be a path-source - in such cases attempts
 * to dereference them will lead to an exception.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating
public interface JpaPathImplementor<X> extends JpaExpressionImplementor<X>, Path<X> {
	// todo (6.0) : this should be a Navigable instead of attribute.  JPA says that the root "entity type" is a path as well.
	//		another way of looking at this though is that since we make the distinction between
	//		path and its source - path will (make sure always) refer to an attribute

	/**
	 * Retrieve reference to the attribute this path represents.
	 *
	 * @return The metamodel attribute.
	 */
	PersistentAttribute<?, ?> getAttribute();


	Navigable getNavigable();

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
	<T extends X> JpaPathImplementor<T> treatAs(Class<T> treatAsType);


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
