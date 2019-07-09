/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.internal;

import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.spi.SingleIdEntityLoader;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Implementation of SingleIdEntityLoader for cases where the application has
 * provided the select load query
 *
 * @author Steve Ebersole
 */
public class SingleIdEntityLoaderProvidedQueryImpl<T> implements SingleIdEntityLoader<T> {
	private final EntityPersister entityDescriptor;
	private final String loadQueryName;

	public SingleIdEntityLoaderProvidedQueryImpl(EntityPersister entityDescriptor, String loadQueryName) {
		this.entityDescriptor = entityDescriptor;
		this.loadQueryName = loadQueryName;
	}

	@Override
	public EntityPersister getLoadable() {
		return entityDescriptor;
	}

	@Override
	public T load(Object pkValue, LockOptions lockOptions, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Object[] loadDatabaseSnapshot(Object id, SharedSessionContractImplementor session) {
		return new Object[0];
	}
}
