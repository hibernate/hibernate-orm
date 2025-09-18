/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal.lock;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.exec.spi.LoadedValuesCollector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Standard implementation of LoadedValuesCollector, intended for use with follow-on locking support.
 *
 * @author Steve Ebersole
 */
public class LoadedValuesCollectorImpl implements LoadedValuesCollector {
	private final List<NavigablePath> rootPaths;
	private final Collection<NavigablePath> pathsToLock;

	private List<LoadedEntityRegistration> rootEntitiesToLock;
	private List<LoadedEntityRegistration> nonRootEntitiesToLock;
	private List<LoadedCollectionRegistration> collectionsToLock;

	public LoadedValuesCollectorImpl(List<NavigablePath> rootPaths, LockingClauseStrategy lockingClauseStrategy) {
		this.rootPaths = rootPaths;
		pathsToLock = FollowOnLockingHelper.extractPathsToLock( lockingClauseStrategy );
	}

	@Override
	public void registerEntity(NavigablePath navigablePath, EntityMappingType entityDescriptor, EntityKey entityKey) {
		if ( !pathsToLock.contains( navigablePath ) ) {
			return;
		}

		if ( rootPaths.contains( navigablePath ) ) {
			if ( rootEntitiesToLock == null ) {
				rootEntitiesToLock = new ArrayList<>();
			}
			rootEntitiesToLock.add( new LoadedEntityRegistration( navigablePath, entityDescriptor, entityKey ) );
		}
		else {
			if ( nonRootEntitiesToLock == null ) {
				nonRootEntitiesToLock = new ArrayList<>();
			}
			nonRootEntitiesToLock.add( new LoadedEntityRegistration( navigablePath, entityDescriptor, entityKey ) );
		}
	}

	@Override
	public void registerCollection(NavigablePath navigablePath, PluralAttributeMapping collectionDescriptor, CollectionKey collectionKey) {
		if ( !pathsToLock.contains( navigablePath ) ) {
			return;
		}

		if ( collectionsToLock == null ) {
			collectionsToLock = new ArrayList<>();
		}
		collectionsToLock.add( new LoadedCollectionRegistration( navigablePath, collectionDescriptor, collectionKey ) );
	}

	@Override
	public void clear() {
		if ( rootEntitiesToLock != null ) {
			rootEntitiesToLock.clear();
		}
		if ( nonRootEntitiesToLock != null ) {
			nonRootEntitiesToLock.clear();
		}
		if ( collectionsToLock != null ) {
			collectionsToLock.clear();
		}
	}

	@Override
	public List<LoadedEntityRegistration> getCollectedRootEntities() {
		return rootEntitiesToLock;
	}

	@Override
	public List<LoadedEntityRegistration> getCollectedNonRootEntities() {
		return nonRootEntitiesToLock;
	}

	@Override
	public List<LoadedCollectionRegistration> getCollectedCollections() {
		return collectionsToLock;
	}
}
