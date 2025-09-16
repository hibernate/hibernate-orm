/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.exec.spi.LoadedValuesCollector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Standard implementation of LoadedValuesCollector, used mainly for follow-on locking support.
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
		pathsToLock = extractPathsToLock( lockingClauseStrategy );
	}

	static Collection<NavigablePath> extractPathsToLock(LockingClauseStrategy lockingClauseStrategy) {
		final LinkedHashSet<NavigablePath> paths = new LinkedHashSet<>();

		final Collection<TableGroup> rootsToLock = lockingClauseStrategy.getRootsToLock();
		if ( rootsToLock != null ) {
			rootsToLock.forEach( (tableGroup) -> paths.add( tableGroup.getNavigablePath() ) );
		}

		final Collection<TableGroupJoin> joinsToLock = lockingClauseStrategy.getJoinsToLock();
		if ( joinsToLock != null ) {
			joinsToLock.forEach( (tableGroupJoin) -> {
				paths.add( tableGroupJoin.getNavigablePath() );

				final ModelPartContainer modelPart = tableGroupJoin.getJoinedGroup().getModelPart();
				if ( modelPart instanceof PluralAttributeMapping pluralAttributeMapping ) {
					final NavigablePath elementPath = tableGroupJoin.getNavigablePath().append( pluralAttributeMapping.getElementDescriptor().getPartName() );
					paths.add( elementPath );

					if ( pluralAttributeMapping.getIndexDescriptor() != null ) {
						final NavigablePath indexPath = tableGroupJoin.getNavigablePath().append( pluralAttributeMapping.getIndexDescriptor().getPartName() );
						paths.add( indexPath );
					}
				}
			} );
		}
		return paths;
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
