/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.spi.NaturalIdLoader;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public class NaturalIdLoaderStandardImpl<T> implements NaturalIdLoader<T> {
	private final EntityPersister entityDescriptor;

	public NaturalIdLoaderStandardImpl(EntityPersister entityDescriptor) {
		this.entityDescriptor = entityDescriptor;
	}

	@Override
	public EntityPersister getLoadable() {
		return entityDescriptor;
	}

	@Override
	public T load(Object naturalIdToLoad, LoadOptions options, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
