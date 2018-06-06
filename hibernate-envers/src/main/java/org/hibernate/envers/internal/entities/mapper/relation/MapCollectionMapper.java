/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.internal.entities.mapper.PropertyMapper;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.Initializor;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.MapCollectionInitializor;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class MapCollectionMapper<T extends Map> extends AbstractCollectionMapper<T> implements PropertyMapper {
	protected final MiddleComponentData elementComponentData;
	protected final MiddleComponentData indexComponentData;

	public MapCollectionMapper(
			CommonCollectionMapperData commonCollectionMapperData,
			Class<? extends T> collectionClass, Class<? extends T> proxyClass,
			MiddleComponentData elementComponentData, MiddleComponentData indexComponentData,
			boolean revisionTypeInId) {
		super( commonCollectionMapperData, collectionClass, proxyClass, false, revisionTypeInId );
		this.elementComponentData = elementComponentData;
		this.indexComponentData = indexComponentData;
	}

	@Override
	protected Initializor<T> getInitializor(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Object primaryKey,
			Number revision,
			boolean removed) {
		return new MapCollectionInitializor<>(
				enversService,
				versionsReader,
				commonCollectionMapperData.getQueryGenerator(),
				primaryKey,
				revision,
				removed,
				collectionClass,
				elementComponentData,
				indexComponentData
		);
	}

	@Override
	protected Collection getNewCollectionContent(PersistentCollection newCollection) {
		if ( newCollection == null ) {
			return null;
		}
		else {
			return ( (Map) newCollection ).entrySet();
		}
	}

	@Override
	protected Collection getOldCollectionContent(Serializable oldCollection) {
		if ( oldCollection == null ) {
			return null;
		}
		else {
			return ( (Map) oldCollection ).entrySet();
		}
	}

	@Override
	protected void mapToMapFromObject(
			SessionImplementor session,
			Map<String, Object> idData,
			Map<String, Object> data,
			Object changed) {
		elementComponentData.getComponentMapper().mapToMapFromObject(
				session,
				idData,
				data,
				( (Map.Entry) changed ).getValue()
		);
		indexComponentData.getComponentMapper().mapToMapFromObject(
				session,
				idData,
				data,
				( (Map.Entry) changed ).getKey()
		);
	}

	@Override
	protected Set<Object> buildCollectionChangeSet(Object eventCollection, Collection collection) {
		final Set<Object> changeSet = new HashSet<>();
		if ( eventCollection != null ) {
			for ( Object entry : collection ) {
				if ( entry != null ) {
					final Map.Entry element = Map.Entry.class.cast( entry );
					if ( element.getValue() == null ) {
						continue;
					}
					changeSet.add( entry );
				}
			}
		}
		return changeSet;
	}

	@Override
	protected boolean isSame(CollectionPersister collectionPersister, Object oldObject, Object newObject) {
		final Map.Entry oldEntry = Map.Entry.class.cast( oldObject );
		final Map.Entry newEntry = Map.Entry.class.cast( newObject );
		if ( collectionPersister.getKeyType().isSame( oldEntry.getKey(), newEntry.getKey() ) ) {
			if ( collectionPersister.getElementType().isSame( oldEntry.getValue(), newEntry.getValue() ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<PersistentCollectionChangeData> mapCollectionChanges(
			SessionImplementor session,
			PersistentCollection newColl,
			Serializable oldColl,
			Serializable id) {
		final List<PersistentCollectionChangeData> collectionChanges = new ArrayList<>();
		final CollectionPersister collectionPersister = resolveCollectionPersister( session, newColl );

		// Comparing new and old collection content
		final Collection newCollection = getNewCollectionContent( newColl );
		final Collection oldCollection = getOldCollectionContent( oldColl );

		// take the new collection and remove any that exist in the old collection.
		// take the resulting Set<> and generate ADD changes
		final Set<Object> added = buildCollectionChangeSet( newColl, newCollection );
		if ( oldColl != null ) {
			for ( Object oldObject : oldCollection ) {
				for ( Iterator itor = added.iterator(); itor.hasNext(); ) {
					Object newObject = itor.next();
					if ( isSame( collectionPersister, oldObject, newObject ) ) {
						itor.remove();
						break;
					}
				}
			}
		}

		// take the old collection and remove any that exist in the new collection.
		// take the resulting Set<> and generate DEL changes.
		final Set<Object> deleted = buildCollectionChangeSet( oldColl, oldCollection );
		if ( newColl != null ) {
			for ( Object newObject : newCollection ) {
				for ( Iterator itor = deleted.iterator(); itor.hasNext(); ) {
					Object oldObject = itor.next();
					if ( isSame( collectionPersister, newObject, oldObject ) ) {
						itor.remove();
						break;
					}
				}
			}
		}

		addCollectionChanges( session, collectionChanges, added, RevisionType.ADD, id );
		addCollectionChanges( session, collectionChanges, deleted, RevisionType.DEL, id );

		return collectionChanges;
	}
}
