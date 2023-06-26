/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.EntityBatchLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;

public abstract class AbstractEntityBatchLoader<T>
		extends SingleIdEntityLoaderSupport<T>
		implements EntityBatchLoader<T>{

	protected final SingleIdEntityLoaderStandardImpl<T> singleIdLoader;

	public AbstractEntityBatchLoader(EntityMappingType entityDescriptor, SessionFactoryImplementor sessionFactory) {
		super( entityDescriptor, sessionFactory );
		singleIdLoader = new SingleIdEntityLoaderStandardImpl<>( entityDescriptor, sessionFactory );
	}

	@Override
	public T load(
			Object pkValue,
			Object entityInstance,
			LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		final T entity = load( pkValue, entityInstance, lockOptions, null, session );
		if ( Hibernate.isInitialized( entity ) ) {
			return entity;
		}
		else {
			return null;
		}
	}

}
