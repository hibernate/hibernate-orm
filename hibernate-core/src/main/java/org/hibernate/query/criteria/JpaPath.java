/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import java.util.Collection;
import java.util.Map;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.spi.NavigablePath;

/**
 * API extension to the JPA {@link Path} contract
 *
 * @author Steve Ebersole
 */
public interface JpaPath<T> extends JpaExpression<T>, Path<T> {
	/**
	 * Get this path's NavigablePath
	 */
	NavigablePath getNavigablePath();

	/**
	 * The source (think "left hand side") of this path
	 */
	JpaPath<?> getLhs();

	/**
	 * Support for JPA's explicit (TREAT) down-casting.
	 */
	<S extends T> JpaPath<S> treatAs(Class<S> treatJavaType);

	/**
	 * Support for JPA's explicit (TREAT) down-casting.
	 */
	<S extends T> JpaPath<S> treatAs(EntityDomainType<S> treatJavaType);

	/**
	 * Get this path's actual resolved model, i.e. the concrete type for generic attributes.
	 */
	SqmPathSource<?> getResolvedModel();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant overrides

	@Override
	default JpaPath<?> getParentPath() {
		return getLhs();
	}

	@Override
	<Y> JpaPath<Y> get(SingularAttribute<? super T, Y> attribute);

	@Override
	<E, C extends Collection<E>> JpaExpression<C> get(PluralAttribute<T, C, E> collection);

	@Override
	<K, V, M extends Map<K, V>> JpaExpression<M> get(MapAttribute<T, K, V> map);

	@Override
	JpaExpression<Class<? extends T>> type();

	@Override
	<Y> JpaPath<Y> get(String attributeName);
}
