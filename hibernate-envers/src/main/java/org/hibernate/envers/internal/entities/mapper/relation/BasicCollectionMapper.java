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
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

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
					if ( isCollectionElementSame( session, collectionPersister, oldEntry, newEntry ) ) {
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
					if ( isCollectionElementSame( session, collectionPersister, deletedEntry, newEntry ) ) {
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

	private boolean isCollectionElementSame(
			SessionImplementor session,
			CollectionPersister collectionPersister,
			Object lhs,
			Object rhs) {
		final Type elementType = collectionPersister.getElementType();

		// If the collection element is an Entity association but the collection does not include the
		// REVTYPE column as a part of the primary key, special care must be taken in order to assess
		// whether the element actually changed.
		//
		// Previously we delegated to the element type, which for entity-based collections would be
		// EntityType.  The EntityType#isSame method results in only a reference equality check.  This
		// would result in both an ADD/DEL entry trying to be saved for the same entity identifier
		// under certain circumstances.  While we generally agree with this ORM assessment, this
		// leads to HHH-13080 which ultimately is because REVTYPE is not part of the middle entity
		// table's primary key.
		//
		// For 5.x, rather than impose schema changes mid-major release, we're going to explore this
		// compromise for now where we're going to treat EntityType-based collections in a slightly
		// different way by delegating the equality check to the entity identifier instead.  This
		// ultimately means that the equality check will leverage both reference and value equality
		// since identifiers can be basic or composite types.
		//
		// In the end for 5.x, this means if an entity is removed from the collection and added
		// back with the same identifier, we will treat it as a no-change for now to avoid the
		// problem presented in HHH-13080.
		//
		// todo (6.0) - support REVTYPE as part of the primary key.
		//		What we actually want to do here is to introduce a legacy compat flag that we check
		//		when we generate the mapper that influences whether the revisionTypeInId value is
		//		true or false.  When its set to true, we actually will treat all element types,
		//		regardless if they're entity, embeddables, or basic types equally.
		//
		//		As an example, if a collection is cleared and instances are added back and it just
		//		so happens that those instances ahve the same entity identifier but aren't reference
		//		equal to the original collection elements, Envers will then actually treat that as
		//		a series of DEL followed by ADD operations for those elements, which ultimately is
		//		the right behavior.  But that only works if REVTYPE is part of the primary key so
		//		that the tuple { owner_id, entity_id, rev, rev_type } differ for the two types of
		//		revision type operations.
		//
		//		Currently the tuple is { owner_id, entity_id, rev } and so having this special
		//		treatment is critical to avoid HHH-13080.
		//
		if ( elementType.isEntityType() && !revisionTypeInId ) {

			// This is a short-circuit to check for reference equality only.
			// There is no need to delegate to the identifier if the objects are reference equal.
			if ( elementType.isSame( lhs, rhs ) ) {
				return true;
			}

			final EntityPersister entityPersister = session.getFactory()
					.getMetamodel()
					.locateEntityPersister( ( (EntityType) elementType ).getAssociatedEntityName() );

			final Object lhsId = entityPersister.getIdentifier( lhs, session );
			final Object rhsId = entityPersister.getIdentifier( rhs, session );

			// Since the two instances aren't reference equal, delegate to identifier now.
			return entityPersister.getIdentifierType().isSame( lhsId, rhsId );
		}

		// for element types that aren't entities (aka embeddables/basic types), use legacy behavior.
		return elementType.isSame( lhs, rhs );
	}
}
