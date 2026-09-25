package org.hibernate.loader.ast.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Loader for loading an entity by a single identifier value.
 *
 * @author Steve Ebersole
 */
public interface SingleIdEntityLoader<T> extends SingleEntityLoader<T> {
	/**
	 * Load by primary key value
	 */
	@Nullable
	@Override
	T load(@Nonnull Object pkValue, @Nonnull LockOptions lockOptions, @Nullable Boolean readOnly, @Nonnull SharedSessionContractImplementor session);


	@Nullable
	T load(@Nonnull Object pkValue, @Nullable Object entityInstance, @Nonnull LockOptions lockOptions, @Nullable Boolean readOnly, @Nonnull SharedSessionContractImplementor session);

	/**
	 * Load by primary key value, populating the passed entity instance.  Used to initialize an uninitialized
	 * bytecode-proxy or {@link org.hibernate.event.spi.LoadEvent} handling.
	 * The passed instance is the enhanced proxy or the entity to be loaded.
	 */
	@Nullable
	default T load(@Nonnull Object pkValue, @Nullable Object entityInstance, @Nonnull LockOptions lockOptions, @Nonnull SharedSessionContractImplementor session) {
		return load( pkValue, entityInstance, lockOptions, null, session );
	}

	/**
	 * Load database snapshot by primary key value
	 */
	@Nullable
	Object[] loadDatabaseSnapshot(@Nonnull Object id, @Nonnull SharedSessionContractImplementor session);
}
