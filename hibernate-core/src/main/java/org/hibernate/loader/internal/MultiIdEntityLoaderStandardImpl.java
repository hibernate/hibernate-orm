/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.internal;

import java.io.Serializable;
import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.entity.DynamicBatchingEntityLoaderBuilder;
import org.hibernate.loader.spi.MultiIdEntityLoader;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.MultiLoadOptions;
import org.hibernate.persister.entity.OuterJoinLoadable;

/**
 * @author Steve Ebersole
 */
public class MultiIdEntityLoaderStandardImpl<T> implements MultiIdEntityLoader<T> {
	private final EntityPersister entityDescriptor;

	public MultiIdEntityLoaderStandardImpl(EntityPersister entityDescriptor) {
		this.entityDescriptor = entityDescriptor;
	}

	@Override
	public EntityPersister getLoadable() {
		return entityDescriptor;
	}

	@Override
	public List<T> load(Object[] ids, MultiLoadOptions loadOptions, SharedSessionContractImplementor session) {
		//noinspection unchecked
		return DynamicBatchingEntityLoaderBuilder.INSTANCE.multiLoad(
				(OuterJoinLoadable) entityDescriptor,
				(Serializable[]) ids,
				session,
				loadOptions
		);

	}
}
