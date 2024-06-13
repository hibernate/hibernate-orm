/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Initializer implementation for initializing entity references.
 *
 * @author Steve Ebersole
 */
public interface EntityInitializer<Data extends InitializerData> extends InitializerParent<Data> {

	/**
	 * Get the descriptor for the type of entity being initialized
	 */
	EntityPersister getEntityDescriptor();

	EntityPersister getConcreteDescriptor(Data data);

	default EntityPersister getConcreteDescriptor(RowProcessingState rowProcessingState) {
		return getConcreteDescriptor( getData( rowProcessingState ) );
	}

	/**
	 * Get the entity instance for the currently processing "row".
	 *
	 * @apiNote Calling this method is only valid from the time
	 * {@link #resolveKey(InitializerData)} has been called until {@link #finishUpRow(InitializerData)}
	 * has been called for the currently processing row
	 */
	Object getEntityInstance(Data data);
	default Object getEntityInstance(RowProcessingState rowProcessingState) {
		return getEntityInstance( getData( rowProcessingState ) );
	}

	default Object getTargetInstance(Data data) {
		return getEntityInstance( data );
	}
	default Object getTargetInstance(RowProcessingState rowProcessingState) {
		return getTargetInstance( getData( rowProcessingState ) );
	}

	default @Nullable EntityKey resolveEntityKeyOnly(RowProcessingState rowProcessingState) {
		final Data data = getData( rowProcessingState );
		resolveKey( data );
		final EntityKey entityKey = new EntityKey(
				getEntityIdentifier( data ),
				getConcreteDescriptor( data )
		);
		finishUpRow( data );
		return entityKey;
	}

	@Nullable Object getEntityIdentifier(Data data);
	default @Nullable Object getEntityIdentifier(RowProcessingState rowProcessingState) {
		return getEntityIdentifier( getData( rowProcessingState ) );
	}

	@Override
	default boolean isEntityInitializer() {
		return true;
	}

	@Override
	default EntityInitializer<?> asEntityInitializer() {
		return this;
	}

}
