/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.Collection;
import java.util.Map;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.criteria.PathException;

/**
 * SPI-level contract for {@link org.hibernate.query.criteria.JpaPath}
 * implementors
 *
 * @author Steve Ebersole
 */
public interface PathImplementor<T> extends ExpressionImplementor<T>, JpaPath<T> {
	@Override
	<X> PathSourceImplementor<X> getSource();

	Navigable<T> getNavigable();

	@Override
	default PathSourceImplementor<?> getParentPath() {
		return getSource();
	}

	/**
	 * Get the PathSource form of this Path (to create a sub-path)
	 *
	 * @param subPathName The sub-path name being de-referenced
	 *
	 * @throws PathException If this path cannot be the source for a sub-path
	 */
	PathSourceImplementor<T> asPathSource(String subPathName) throws PathException;

	@Override
	<Y> PathImplementor<Y> get(String attributeName);

	@Override
	<Y> PathImplementor<Y> get(SingularAttribute<? super T, Y> attribute);

	@Override
	<E, C extends Collection<E>> ExpressionImplementor<C> get(PluralAttribute<T, C, E> collection);

	@Override
	<K, V, M extends Map<K, V>> ExpressionImplementor<M> get(MapAttribute<T, K, V> map);

	@Override
	<S extends T> PathImplementor<S> treatAs(Class<S> treatJavaType) throws PathException;
}
