/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.PathException;

/**
 * @author Steve Ebersole
 */
public interface RootImplementor<T> extends FromImplementor<T,T>, JpaRoot<T> {
	@Override
	default boolean isFetched() {
		return true;
	}

	@Override
	EntityTypeDescriptor<T> getModel();

	@Override
	EntityTypeDescriptor<T> getManagedType();

	@Override
	<S extends T> RootImplementor<S> treatAs(Class<S> treatJavaType) throws PathException;
}
