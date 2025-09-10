/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.internal;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ops.spi.LoadedValuesCollector;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class SingleRootLoadedValuesCollector implements LoadedValuesCollector {
	private final NavigablePath rootPath;

	private List<LoadedEntityRegistration> loadedRootEntities;
	private List<LoadedEntityRegistration> loadedNonRootEntities;
	private List<LoadedCollectionRegistration> loadedCollections;

	public SingleRootLoadedValuesCollector(NavigablePath rootPath) {
		this.rootPath = rootPath;
	}

	@Override
	public void registerEntity(
			NavigablePath navigablePath,
			EntityMappingType entityDescriptor,
			EntityKey entityKey) {
		if ( rootPath.pathsMatch( navigablePath ) ) {
			if ( loadedRootEntities == null ) {
				loadedRootEntities = new ArrayList<>();
			}
			loadedRootEntities.add( new LoadedEntityRegistration( navigablePath, entityDescriptor, entityKey ) );
		}
		else {
			if ( loadedNonRootEntities == null ) {
				loadedNonRootEntities = new ArrayList<>();
			}
			loadedNonRootEntities.add( new LoadedEntityRegistration( navigablePath, entityDescriptor, entityKey ) );
		}
	}

	@Override
	public void registerCollection(
			NavigablePath navigablePath,
			PluralAttributeMapping collectionDescriptor,
			CollectionKey collectionKey) {
		if ( loadedCollections == null ) {
			loadedCollections = new ArrayList<>();
		}
		loadedCollections.add( new LoadedCollectionRegistration( navigablePath, collectionDescriptor, collectionKey ) );
	}

	@Override
	public List<LoadedEntityRegistration> getCollectedRootEntities() {
		return loadedRootEntities;
	}

	@Override
	public List<LoadedEntityRegistration> getCollectedNonRootEntities() {
		return loadedNonRootEntities;
	}

	@Override
	public List<LoadedCollectionRegistration> getCollectedCollections() {
		return loadedCollections;
	}
}
