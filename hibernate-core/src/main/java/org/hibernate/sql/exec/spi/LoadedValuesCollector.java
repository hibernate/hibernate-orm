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
	void registerEntity(
			NavigablePath navigablePath,
			EntityMappingType entityDescriptor,
			EntityKey entityKey);

	void registerCollection(
			NavigablePath navigablePath,
			PluralAttributeMapping collectionDescriptor,
			CollectionKey collectionKey);

	interface LoadedPartRegistration {
		NavigablePath navigablePath();
		ModelPart modelPart();
	}

	List<LoadedEntityRegistration> getCollectedRootEntities();

	List<LoadedEntityRegistration> getCollectedNonRootEntities();

	List<LoadedCollectionRegistration> getCollectedCollections();

	record LoadedEntityRegistration(
			NavigablePath navigablePath,
			EntityMappingType entityDescriptor,
			EntityKey entityKey) implements LoadedPartRegistration {
		@Override
		public EntityMappingType modelPart() {
			return entityDescriptor();
		}
	}

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
