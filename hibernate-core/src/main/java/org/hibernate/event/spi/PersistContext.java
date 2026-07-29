package org.hibernate.event.spi;

import java.util.IdentityHashMap;
import jakarta.annotation.Nonnull;

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

			private transient BatchGenerationContext batchGenerationContext;

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
			public void resolveBatchGenerators(@Nonnull EventSource session) {
				if ( batchGenerationContext != null ) {
					if ( session.getActionQueue().hasUnresolvedEntityInsertActions() ) {
						// There is no need to run batch generation, since the flush will fail
						return;
					}
					batchGenerationContext.resolve( session );
				}
			}
		}
		return new Impl();
	}
}
