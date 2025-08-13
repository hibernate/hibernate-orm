/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;

import java.util.List;

/**
 * Used to collect entity and collection values which are loaded as part of
 * {@linkplain org.hibernate.sql.results.jdbc.spi.JdbcValues} processing.
 * Kept as part of {@linkplain org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState}
 *
 * @author Steve Ebersole
 */
public interface LoadedValuesCollector {
	/**
	 * Register a loading entity.
	 *
	 * @param navigablePath The NavigablePath relative to the SQL AST used to load the entity
	 * @param entityDescriptor The descriptor for the entity being loaded.
	 * @param entityKey The EntityKey for the entity being loaded
	 */
	void registerEntity(
			NavigablePath navigablePath,
			EntityMappingType entityDescriptor,
			EntityKey entityKey);

	/**
	 * Register a loading collection.
	 *
	 * @param navigablePath The NavigablePath relative to the SQL AST used to load the entity
	 * @param collectionDescriptor The descriptor for the collection being loaded.
	 * @param collectionKey The CollectionKey for the collection being loaded
	 */
	void registerCollection(
			NavigablePath navigablePath,
			PluralAttributeMapping collectionDescriptor,
			CollectionKey collectionKey);

	/**
	 * Clears the state of the collector.
	 *
	 * @implSpec In some cases, the collector may be cached as part of a
	 * JdbcSelect being cached (see {@linkplain JdbcSelect#getLoadedValuesCollector()}.
	 * This method allows clearing of the internal state after execution of the JdbcSelect.
	 */
	void clear();

	/**
	 * Access to all root entities loaded.
	 */
	List<LoadedEntityRegistration> getCollectedRootEntities();

	/**
	 * Access to all non-root entities (join fetches e.g.) loaded.
	 */
	List<LoadedEntityRegistration> getCollectedNonRootEntities();

	/**
	 * Access to all collection loaded.
	 */
	List<LoadedCollectionRegistration> getCollectedCollections();

	interface LoadedPartRegistration {
		NavigablePath navigablePath();
		ModelPart modelPart();
	}

	/**
	 * Details about a loaded entity.
	 */
	record LoadedEntityRegistration(
			NavigablePath navigablePath,
			EntityMappingType entityDescriptor,
			EntityKey entityKey) implements LoadedPartRegistration {
		@Override
		public EntityMappingType modelPart() {
			return entityDescriptor();
		}
	}

	/**
	 * Details about a loaded collection.
	 */
	record LoadedCollectionRegistration(
			NavigablePath navigablePath,
			PluralAttributeMapping collectionDescriptor,
			CollectionKey collectionKey) implements LoadedPartRegistration {
		@Override
		public PluralAttributeMapping modelPart() {
			return collectionDescriptor();
		}
	}
}
