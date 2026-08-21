/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import java.util.IdentityHashMap;
import jakarta.annotation.Nonnull;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * A {@link PersistEvent} represents a {@linkplain org.hibernate.Session#persist(Object) persist operation}
 * applied to a single entity. A {@code PersistContext} is propagated across all cascaded persist operations,
 * and keeps track of all the entities we've already visited.
 *
 * @author Gavin King
 */
public interface PersistContext extends ManagedOperationContext {

	boolean add(@Nonnull Object entity);

	static @Nonnull PersistContext create() {
		// use extension to avoid creating
		// a useless wrapper object
		class Impl extends IdentityHashMap<Object,Object>
				implements PersistContext {

			private BatchGenerationContext batchGenerationContext;

			Impl() {
				super(10);
			}

			@Override
			public boolean add(@Nonnull Object entity) {
				return put(entity,entity)==null;
			}

			@Override
			public @Nonnull BatchGenerationContext getBatchGenerationContext() {
				if ( batchGenerationContext == null ) {
					batchGenerationContext = new BatchGenerationContext();
				}
				return batchGenerationContext;
			}

			@Override
			public void resolveBatchGenerators(@Nonnull SharedSessionContractImplementor session) {
				if ( batchGenerationContext != null ) {
					batchGenerationContext.resolve( session );
				}
			}
		}
		return new Impl();
	}
}
