/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.collection.internal.PersistentMap;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.internal.entities.mapper.PropertyMapper;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.Initializor;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.property.access.spi.Setter;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Chris Cranford
 */
public abstract class AbstractCollectionMapper<T> implements PropertyMapper {
	protected final CommonCollectionMapperData commonCollectionMapperData;
	protected final Class<? extends T> collectionClass;
	protected final boolean ordinalInId;
	protected final boolean revisionTypeInId;

	private final Constructor<? extends T> proxyConstructor;

	protected AbstractCollectionMapper(
			CommonCollectionMapperData commonCollectionMapperData,
			Class<? extends T> collectionClass, Class<? extends T> proxyClass, boolean ordinalInId,
			boolean revisionTypeInId) {
		this.commonCollectionMapperData = commonCollectionMapperData;
		this.collectionClass = collectionClass;
		this.ordinalInId = ordinalInId;
		this.revisionTypeInId = revisionTypeInId;

		try {
			proxyConstructor = proxyClass.getConstructor( Initializor.class );
		}
		catch (NoSuchMethodException e) {
			throw new AuditException( e );
		}
	}

	protected abstract Collection getNewCollectionContent(PersistentCollection newCollection);

	protected abstract Collection getOldCollectionContent(Serializable oldCollection);

	/**
	 * Maps the changed collection element to the given map.
	 *
	 * @param idData Map to which composite-id data should be added.
	 * @param data Where to map the data.
	 * @param changed The changed collection element to map.
	 */
	protected abstract void mapToMapFromObject(
			SessionImplementor session,
			Map<String, Object> idData,
			Map<String, Object> data,
			Object changed);

	/**
	 * Creates map for storing identifier data. Ordinal parameter guarantees uniqueness of primary key.
	 * Composite primary key cannot contain embeddable properties since they might be nullable.
	 *
	 * @param ordinal Iteration ordinal.
	 *
	 * @return Map for holding identifier data.
	 */
	protected Map<String, Object> createIdMap(int ordinal) {
		final Map<String, Object> idMap = new HashMap<>();
		if ( ordinalInId ) {
			idMap.put( commonCollectionMapperData.getVerEntCfg().getEmbeddableSetOrdinalPropertyName(), ordinal );
		}
		return idMap;
	}

	private void addCollectionChanges(
			SessionImplementor session, List<PersistentCollectionChangeData> collectionChanges,
			Set<Object> changed, RevisionType revisionType, Serializable id) {
		int ordinal = 0;

		for ( Object changedObj : changed ) {
			final Map<String, Object> entityData = new HashMap<>();
			final Map<String, Object> originalId = createIdMap( ordinal++ );
			entityData.put( commonCollectionMapperData.getVerEntCfg().getOriginalIdPropName(), originalId );

			collectionChanges.add(
					new PersistentCollectionChangeData(
							commonCollectionMapperData.getVersionsMiddleEntityName(), entityData, changedObj
					)
			);
			// Mapping the collection owner's id.
			commonCollectionMapperData.getReferencingIdData().getPrefixedMapper().mapToMapFromId( originalId, id );

			// Mapping collection element and index (if present).
			mapToMapFromObject( session, originalId, entityData, changedObj );

			( revisionTypeInId ? originalId : entityData ).put(
					commonCollectionMapperData.getVerEntCfg()
							.getRevisionTypePropName(), revisionType
			);
		}
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<PersistentCollectionChangeData> mapCollectionChanges(
			SessionImplementor session,
			String referencingPropertyName,
			PersistentCollection newColl,
			Serializable oldColl, Serializable id) {
		if ( !commonCollectionMapperData.getCollectionReferencingPropertyData().getName().equals(
				referencingPropertyName
		) ) {
			return null;
		}

		// HHH-11063
		final CollectionEntry collectionEntry = session.getPersistenceContext().getCollectionEntry( newColl );
		if ( collectionEntry != null ) {
			// This next block delegates only to the persiter-based collection change code if
			// the following are true:
			//	1. New collection is not a PersistentMap.
			//	2. The collection has a persister.
			//	3. The collection is not indexed, e.g. @IndexColumn
			//
			// In the case of 1 and 3, the collection is transformed into a set of Pair<> elements where the
			// pair's left element is either the map key or the index.  In these cases, the key/index do
			// affect the change code; hence why they're skipped here and handled at the end.
			//
			// For all others, the persister based method uses the collection's ElementType#isSame to calculate
			// equality between the newColl and oldColl.  This enforces the same equality check that core uses
			// for element types such as @Entity in cases where the hash code does not use the id field but has
			// the same value in both collections.  Using #isSame, these will be seen as differing elements and
			// changes to the collection will be returned.
			if ( !( newColl instanceof PersistentMap ) ) {
				final CollectionPersister collectionPersister = collectionEntry.getCurrentPersister();
				if ( collectionPersister != null && !collectionPersister.hasIndex() ) {
					return mapCollectionChanges( session, newColl, oldColl, id, collectionPersister );
				}
			}
		}

		return mapCollectionChanges( session, newColl, oldColl, id );
	}

	@Override
	public boolean mapToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		// Changes are mapped in the "mapCollectionChanges" method.
		return false;
	}

	@Override
	public void mapModifiedFlagsToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		final PropertyData propertyData = commonCollectionMapperData.getCollectionReferencingPropertyData();
		if ( propertyData.isUsingModifiedFlag() ) {
			if ( isNotPersistentCollection( newObj ) || isNotPersistentCollection( oldObj ) ) {
				// Compare POJOs.
				data.put( propertyData.getModifiedFlagPropertyName(), !EqualsHelper.areEqual( newObj, oldObj ) );
			}
			else if ( isFromNullToEmptyOrFromEmptyToNull( (PersistentCollection) newObj, (Serializable) oldObj ) ) {
				data.put( propertyData.getModifiedFlagPropertyName(), true );
			}
			else {
				// HHH-7949 - Performance optimization to avoid lazy-fetching collections that have
				// not been changed for deriving the modified flags value.
				final PersistentCollection pc = (PersistentCollection) newObj;
				if ( ( pc != null && !pc.isDirty() ) || ( newObj == null && oldObj == null ) ) {
					data.put( propertyData.getModifiedFlagPropertyName(), false );
					return;
				}

				final List<PersistentCollectionChangeData> changes = mapCollectionChanges(
						session,
						commonCollectionMapperData.getCollectionReferencingPropertyData().getName(),
						pc,
						(Serializable) oldObj,
						null
				);
				data.put( propertyData.getModifiedFlagPropertyName(), !changes.isEmpty() );
			}
		}
	}

	private boolean isNotPersistentCollection(Object obj) {
		return obj != null && !(obj instanceof PersistentCollection);
	}

	private boolean isFromNullToEmptyOrFromEmptyToNull(PersistentCollection newColl, Serializable oldColl) {
		// Comparing new and old collection content.
		final Collection newCollection = getNewCollectionContent( newColl );
		final Collection oldCollection = getOldCollectionContent( oldColl );

		return oldCollection == null && newCollection != null && newCollection.isEmpty()
				|| newCollection == null && oldCollection != null && oldCollection.isEmpty();
	}

	@Override
	public void mapModifiedFlagsToMapForCollectionChange(String collectionPropertyName, Map<String, Object> data) {
		final PropertyData propertyData = commonCollectionMapperData.getCollectionReferencingPropertyData();
		if ( propertyData.isUsingModifiedFlag() ) {
			data.put(
					propertyData.getModifiedFlagPropertyName(),
					propertyData.getName().equals( collectionPropertyName )
			);
		}
	}

	protected abstract Initializor<T> getInitializor(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Object primaryKey,
			Number revision,
			boolean removed);

	@Override
	public void mapToEntityFromMap(
			EnversService enversService,
			Object obj,
			Map data,
			Object primaryKey,
			AuditReaderImplementor versionsReader,
			Number revision) {
		final Setter setter = ReflectionTools.getSetter(
				obj.getClass(),
				commonCollectionMapperData.getCollectionReferencingPropertyData(),
				enversService.getServiceRegistry()
		);
		try {
			setter.set(
					obj,
					proxyConstructor.newInstance(
							getInitializor(
									enversService,
									versionsReader,
									primaryKey,
									revision,
									RevisionType.DEL.equals(
											data.get(
													enversService.getAuditEntitiesConfiguration().getRevisionTypePropName()
											)
									)
							)
					),
					null
			);
		}
		catch (InstantiationException e) {
			throw new AuditException( e );
		}
		catch (IllegalAccessException e) {
			throw new AuditException( e );
		}
		catch (InvocationTargetException e) {
			throw new AuditException( e );
		}
	}

	/**
	 * Map collection changes using hash identity.
	 *
	 * @param session The session.
	 * @param newColl The new persistent collection.
	 * @param oldColl The old collection.
	 * @param id The owning entity identifier.
	 * @return the persistent collection changes.
	 */
	@SuppressWarnings("unchecked")
	private List<PersistentCollectionChangeData> mapCollectionChanges(
			SessionImplementor session,
			PersistentCollection newColl,
			Serializable oldColl,
			Serializable id) {
		final List<PersistentCollectionChangeData> collectionChanges = new ArrayList<PersistentCollectionChangeData>();

		// Comparing new and old collection content.
		final Collection newCollection = getNewCollectionContent( newColl );
		final Collection oldCollection = getOldCollectionContent( oldColl );

		final Set<Object> added = new HashSet<>();
		if ( newColl != null ) {
			added.addAll( newCollection );
		}
		// Re-hashing the old collection as the hash codes of the elements there may have changed, and the
		// removeAll in AbstractSet has an implementation that is hashcode-change sensitive (as opposed to addAll).
		if ( oldColl != null ) {
			added.removeAll( new HashSet( oldCollection ) );
		}
		addCollectionChanges( session, collectionChanges, added, RevisionType.ADD, id );

		final Set<Object> deleted = new HashSet<>();
		if ( oldColl != null ) {
			deleted.addAll( oldCollection );
		}
		// The same as above - re-hashing new collection.
		if ( newColl != null ) {
			deleted.removeAll( new HashSet( newCollection ) );
		}
		addCollectionChanges( session, collectionChanges, deleted, RevisionType.DEL, id );

		return collectionChanges;
	}

	/**
	 * Map collection changes using the collection element type equality functionality.
	 *
	 * @param session The session.
	 * @param newColl The new persistent collection.
	 * @param oldColl The old collection.
	 * @param id The owning entity identifier.
	 * @param collectionPersister The collection persister.
	 * @return the persistent collection changes.
	 */
	private List<PersistentCollectionChangeData> mapCollectionChanges(
			SessionImplementor session,
			PersistentCollection newColl,
			Serializable oldColl,
			Serializable id,
			CollectionPersister collectionPersister) {

		final List<PersistentCollectionChangeData> collectionChanges = new ArrayList<PersistentCollectionChangeData>();

		// Comparing new and old collection content.
		final Collection newCollection = getNewCollectionContent( newColl );
		final Collection oldCollection = getOldCollectionContent( oldColl );

		// take the new collection and remove any that exist in the old collection.
		// take the resulting Set<> and generate ADD changes.
		final Set<Object> added = new HashSet<>();
		if ( newColl != null ) {
			added.addAll( newCollection );
		}
		if ( oldColl != null && collectionPersister != null ) {
			for ( Object object : oldCollection ) {
				for ( Iterator addedIt = added.iterator(); addedIt.hasNext(); ) {
					Object object2 = addedIt.next();
					if ( collectionPersister.getElementType().isSame( object, object2 ) ) {
						addedIt.remove();
						break;
					}
				}
			}
		}
		addCollectionChanges( session, collectionChanges, added, RevisionType.ADD, id );

		// take the old collection and remove any that exist in the new collection.
		// take the resulting Set<> and generate DEL changes.
		final Set<Object> deleted = new HashSet<>();
		if ( oldColl != null ) {
			deleted.addAll( oldCollection );
		}
		if ( newColl != null && collectionPersister != null ) {
			for ( Object object : newCollection ) {
				for ( Iterator deletedIt = deleted.iterator(); deletedIt.hasNext(); ) {
					Object object2 = deletedIt.next();
					if ( collectionPersister.getElementType().isSame( object, object2 ) ) {
						deletedIt.remove();
						break;
					}
				}
			}
		}
		addCollectionChanges( session, collectionChanges, deleted, RevisionType.DEL, id );

		return collectionChanges;
	}

	@Override
	public boolean hasPropertiesWithModifiedFlag() {
		if ( commonCollectionMapperData != null ) {
			final PropertyData propertyData = commonCollectionMapperData.getCollectionReferencingPropertyData();
			return propertyData != null && propertyData.isUsingModifiedFlag();
		}
		return false;
	}
}
