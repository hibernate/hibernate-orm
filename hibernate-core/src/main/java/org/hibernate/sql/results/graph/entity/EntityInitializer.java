/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Initializer implementation for initializing entity references.
 *
 * @author Steve Ebersole
 */
public interface EntityInitializer extends FetchParentAccess {

	/**
	 * Get the descriptor for the type of entity being initialized
	 */
	EntityPersister getEntityDescriptor();

	EntityPersister getConcreteDescriptor();

	@Override
	default @Nullable EntityInitializer findFirstEntityDescriptorAccess() {
		// Keep this method around for binary backwards compatibility
		return this;
	}

	@Override
	default EntityInitializer findFirstEntityInitializer() {
		return this;
	}

	/**
	 * Get the entity instance for the currently processing "row".
	 *
	 * @apiNote Calling this method is only valid from the time
	 * {@link #resolveKey} has been called until {@link #finishUpRow}
	 * has been called for the currently processing row
	 */
	Object getEntityInstance();

	default Object getManagedInstance() {
		return getEntityInstance();
	}

	default Object getTargetInstance() {
		return getEntityInstance();
	}

	@Override
	default Object getInitializedInstance() {
		return getEntityInstance();
	}

	default @Nullable EntityKey resolveEntityKeyOnly(RowProcessingState rowProcessingState) {
		resolveKey();
		final EntityKey entityKey = getEntityKey();
		finishUpRow();
		return entityKey;
	}

	/**
	 * @deprecated Use {@link #resolveEntityKeyOnly(RowProcessingState)} instead.
	 */
	@Deprecated(forRemoval = true)
	@Nullable EntityKey getEntityKey();

	default @Nullable Object getEntityIdentifier() {
		final EntityKey entityKey = getEntityKey();
		return entityKey == null ? null : entityKey.getIdentifier();
	}

	@Override
	default boolean isEntityInitializer() {
		return true;
	}

	@Override
	default EntityInitializer asEntityInitializer() {
		return this;
	}

	/**
	 * @return true if the current entity associated to this EntityInitializer has been initialized
	 */
	boolean isEntityInitialized();

}
