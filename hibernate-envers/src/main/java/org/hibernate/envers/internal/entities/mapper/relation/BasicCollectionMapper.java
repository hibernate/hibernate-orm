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
import org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.BasicCollectionInitializor;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.Initializor;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class BasicCollectionMapper<T extends Collection> extends AbstractCollectionMapper<T> implements PropertyMapper {
	protected final MiddleComponentData elementComponentData;

	public BasicCollectionMapper(
			CommonCollectionMapperData commonCollectionMapperData,
			Class<? extends T> collectionClass,
			Class<? extends T> proxyClass,
			MiddleComponentData elementComponentData,
			boolean ordinalInId,
			boolean revisionTypeInId) {
		super( commonCollectionMapperData, collectionClass, proxyClass, ordinalInId, revisionTypeInId );
		this.elementComponentData = elementComponentData;
	}

	@Override
	protected Initializor<T> getInitializor(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Object primaryKey,
			Number revision,
			boolean removed) {
		return new BasicCollectionInitializor<>(
				enversService,
				versionsReader,
				commonCollectionMapperData.getQueryGenerator(),
				primaryKey,
				revision,
				removed,
				collectionClass,
				elementComponentData
		);
	}

	@Override
	protected Collection getNewCollectionContent(PersistentCollection newCollection) {
		return (Collection) newCollection;
	}

	@Override
	protected Collection getOldCollectionContent(Serializable oldCollection) {
		if ( oldCollection == null ) {
			return null;
		}
		else if ( oldCollection instanceof Map ) {
			return ( (Map) oldCollection ).keySet();
		}
		else {
			return (Collection) oldCollection;
		}
	}

	@Override
	protected void mapToMapFromObject(
			SessionImplementor session,
			Map<String, Object> idData,
			Map<String, Object> data,
			Object changed) {
		elementComponentData.getComponentMapper().mapToMapFromObject( session, idData, data, changed );
	}

	@Override
	protected Set<Object> buildCollectionChangeSet(Object eventCollection, Collection collection) {
		final Set<Object> changeSet = new HashSet<>();
		if ( eventCollection != null ) {
			for ( Object entry : collection ) {
				if ( entry != null ) {
					changeSet.add( entry );
				}
			}
		}
		return changeSet;
	}

	@Override
	protected List<PersistentCollectionChangeData> mapCollectionChanges(
			SessionImplementor session,
			PersistentCollection newColl,
			Serializable oldColl,
			Serializable id) {

		final List<PersistentCollectionChangeData> collectionChanges = new ArrayList<>();

		final CollectionPersister collectionPersister = resolveCollectionPersister( session, newColl );

		// Comparing new and old collection content.
		final Collection newCollection = getNewCollectionContent( newColl );
		final Collection oldCollection = getOldCollectionContent( oldColl );

		final Set<Object> addedElements = buildCollectionChangeSet( newColl, newCollection );
		if ( oldColl != null ) {
			for ( Object oldEntry : oldCollection ) {
				for ( Iterator itor = addedElements.iterator(); itor.hasNext(); ) {
					Object newEntry = itor.next();
					if ( collectionPersister.getElementType().isSame( oldEntry, newEntry ) ) {
						itor.remove();
						break;
					}
				}
			}
		}

		final Set<Object> deleteElements = buildCollectionChangeSet( oldColl, oldCollection );
		if ( newColl != null ) {
			for ( Object newEntry : newCollection ) {
				for ( Iterator itor = deleteElements.iterator(); itor.hasNext(); ) {
					Object deletedEntry = itor.next();
					if ( collectionPersister.getElementType().isSame( deletedEntry, newEntry ) ) {
						itor.remove();
						break;
					}
				}
			}
		}

		addCollectionChanges( session, collectionChanges, addedElements, RevisionType.ADD, id );
		addCollectionChanges( session, collectionChanges, deleteElements, RevisionType.DEL, id );

		return collectionChanges;
	}
}
