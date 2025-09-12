/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.spi;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;

import java.util.List;

/**
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
