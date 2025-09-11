/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.internal.lock;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ops.spi.LoadedValuesCollector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hibernate.sql.ops.internal.lock.LoadedValuesCollectorImpl.extractPathsToLock;

/**
 * @author Steve Ebersole
 */
public class SingleRootLoadedValuesCollector implements LoadedValuesCollector {
	private final NavigablePath rootPath;
	private final Collection<NavigablePath> pathsToLock;

	private List<LoadedEntityRegistration> loadedRootEntities;
	private List<LoadedEntityRegistration> loadedNonRootEntities;
	private List<LoadedCollectionRegistration> loadedCollections;

	public SingleRootLoadedValuesCollector(NavigablePath rootPath, LockingClauseStrategy lockingClauseStrategy) {
		this.rootPath = rootPath;
		pathsToLock = extractPathsToLock( lockingClauseStrategy );
	}

	@Override
	public void registerEntity(
			NavigablePath navigablePath,
			EntityMappingType entityDescriptor,
			EntityKey entityKey) {
		if ( !pathsToLock.contains( navigablePath ) ) {
			return;
		}

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
		if ( !pathsToLock.contains( navigablePath ) ) {
			return;
		}

		if ( loadedCollections == null ) {
			loadedCollections = new ArrayList<>();
		}
		loadedCollections.add( new LoadedCollectionRegistration( navigablePath, collectionDescriptor, collectionKey ) );
	}

	@Override
	public List<LoadedEntityRegistration> getCollectedRootEntities() {
		return loadedRootEntities == null ? Collections.emptyList() : loadedRootEntities;
	}

	@Override
	public List<LoadedEntityRegistration> getCollectedNonRootEntities() {
		return loadedNonRootEntities == null ? Collections.emptyList() : loadedNonRootEntities;
	}

	@Override
	public List<LoadedCollectionRegistration> getCollectedCollections() {
		return loadedCollections == null ? Collections.emptyList() : loadedCollections;
	}
}
