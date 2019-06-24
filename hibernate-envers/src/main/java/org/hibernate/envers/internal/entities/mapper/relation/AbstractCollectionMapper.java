/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.AbstractPropertyMapper;
import org.hibernate.envers.internal.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.Initializor;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.property.access.spi.Setter;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Chris Cranford
 */
public abstract class AbstractCollectionMapper<T> extends AbstractPropertyMapper {
	protected final CommonCollectionMapperData commonCollectionMapperData;
	protected final Class<? extends T> collectionClass;
	protected final boolean ordinalInId;
	protected final boolean revisionTypeInId;

	private final Constructor<? extends T> proxyConstructor;

	protected AbstractCollectionMapper(
			CommonCollectionMapperData commonCollectionMapperData,
			Class<? extends T> collectionClass,
			Class<? extends T> proxyClass,
			boolean ordinalInId,
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

	protected abstract Set<Object> buildCollectionChangeSet(Object eventCollection, Collection collection);

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

	protected void addCollectionChanges(
			SessionImplementor session,
			List<PersistentCollectionChangeData> collectionChanges,
			Set<Object> changed,
			RevisionType revisionType,
			Serializable id) {
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
			Serializable oldColl,
			Serializable id) {
		final PropertyData propertyData = commonCollectionMapperData.getCollectionReferencingPropertyData();
		if ( !propertyData.getName().equals( referencingPropertyName ) ) {
			return null;
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
				data.put( propertyData.getModifiedFlagPropertyName(), !Objects.deepEquals( newObj, oldObj ) );
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

	protected CollectionPersister resolveCollectionPersister(
			SessionImplementor session,
			PersistentCollection collection) {
		// First attempt to resolve the persister from the collection entry
		if ( collection != null ) {
			CollectionEntry collectionEntry = session.getPersistenceContextInternal().getCollectionEntry( collection );
			if ( collectionEntry != null ) {
				CollectionPersister collectionPersister = collectionEntry.getCurrentPersister();
				if ( collectionPersister != null ) {
					return collectionPersister;
				}
			}
		}

		// Fallback to resolving the persister from the collection role
		final CollectionPersister collectionPersister = session.getFactory()
				.getMetamodel()
				.collectionPersister( commonCollectionMapperData.getRole() );

		if ( collectionPersister == null ) {
			throw new AuditException(
					String.format(
							Locale.ROOT,
							"Failed to locate CollectionPersister for collection [%s]",
							commonCollectionMapperData.getRole()
					)
			);
		}

		return collectionPersister;
	}

	/**
	 * Checks whether the old collection element and new collection element are the same.
	 * By default, this delegates to the collection persister's {@link CollectionPersister#getElementType()}.
	 *
	 * @param collectionPersister The collection persister.
	 * @param oldObject The collection element from the old persistent collection.
	 * @param newObject The collection element from the new persistent collection.
	 *
	 * @return true if the two objects are the same, false otherwise.
	 */
	protected boolean isSame(CollectionPersister collectionPersister, Object oldObject, Object newObject) {
		return collectionPersister.getElementType().isSame( oldObject, newObject );
	}

	@Override
	public void mapToEntityFromMap(
			final EnversService enversService,
			final Object obj,
			final Map data,
			final Object primaryKey,
			final AuditReaderImplementor versionsReader,
			final Number revision) {
		final String revisionTypePropertyName = enversService.getAuditEntitiesConfiguration().getRevisionTypePropName();

		// construct the collection proxy
		final Object collectionProxy;
		try {
			collectionProxy = proxyConstructor.newInstance(
					getInitializor(
							enversService,
							versionsReader,
							primaryKey,
							revision,
							RevisionType.DEL.equals( data.get( revisionTypePropertyName ) )
					)
			);
		}
		catch ( Exception e ) {
			throw new AuditException( "Failed to construct collection proxy", e );
		}

		final PropertyData collectionPropertyData = commonCollectionMapperData.getCollectionReferencingPropertyData();

		if ( isDynamicComponentMap() ) {
			final Map<String, Object> map = (Map<String, Object>) obj;
			map.put( collectionPropertyData.getBeanName(), collectionProxy );
		}
		else {
			AccessController.doPrivileged(
					new PrivilegedAction<Object>() {
						@Override
						public Object run() {
							final Setter setter = ReflectionTools.getSetter(
									obj.getClass(),
									collectionPropertyData,
									enversService.getServiceRegistry()
							);

							setter.set( obj, collectionProxy, null );

							return null;
						}
					}
			);
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
	protected abstract List<PersistentCollectionChangeData> mapCollectionChanges(
			SessionImplementor session,
			PersistentCollection newColl,
			Serializable oldColl,
			Serializable id);

	@Override
	public boolean hasPropertiesWithModifiedFlag() {
		if ( commonCollectionMapperData != null ) {
			final PropertyData propertyData = commonCollectionMapperData.getCollectionReferencingPropertyData();
			return propertyData != null && propertyData.isUsingModifiedFlag();
		}
		return false;
	}
}
