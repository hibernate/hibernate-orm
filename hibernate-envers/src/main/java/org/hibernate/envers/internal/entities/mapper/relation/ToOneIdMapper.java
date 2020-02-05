/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.EntityTools;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Chris Cranford
 */
public class ToOneIdMapper extends AbstractToOneMapper {
	private final IdMapper delegate;
	private final String referencedEntityName;
	private final boolean nonInsertableFake;
	private final boolean lazyMapping;

	public ToOneIdMapper(
			IdMapper delegate,
			PropertyData propertyData,
			String referencedEntityName,
			boolean nonInsertableFake,
			boolean lazyMapping) {
		super( delegate.getServiceRegistry(), propertyData );
		this.delegate = delegate;
		this.referencedEntityName = referencedEntityName;
		this.nonInsertableFake = nonInsertableFake;
		this.lazyMapping = lazyMapping;
	}

	@Override
	public boolean mapToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		final HashMap<String, Object> newData = new HashMap<>();

		// If this property is originally non-insertable, but made insertable because it is in a many-to-one "fake"
		// bi-directional relation, we always store the "old", unchanged data, to prevent storing changes made
		// to this field. It is the responsibility of the collection to properly update it if it really changed.
		Object entity = nonInsertableFake ? oldObj : newObj;
		if ( lazyMapping && entity instanceof HibernateProxy ) {
			entity = ( (HibernateProxy) entity ).getHibernateLazyInitializer().getImplementation();
		}

		delegate.mapToMapFromEntity( newData, entity );

		for ( Map.Entry<String, Object> entry : newData.entrySet() ) {
			data.put( entry.getKey(), entry.getValue() );
		}

		return checkModified( session, newObj, oldObj );
	}

	@Override
	public void mapModifiedFlagsToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		if ( getPropertyData().isUsingModifiedFlag() ) {
			data.put( getPropertyData().getModifiedFlagPropertyName(), checkModified( session, newObj, oldObj ) );
		}
	}

	@Override
	public void mapModifiedFlagsToMapForCollectionChange(String collectionPropertyName, Map<String, Object> data) {
		if ( getPropertyData().isUsingModifiedFlag() ) {
			data.put(
					getPropertyData().getModifiedFlagPropertyName(),
					collectionPropertyName.equals( getPropertyData().getName() )
			);
		}
	}

	protected boolean checkModified(SessionImplementor session, Object newObj, Object oldObj) {
		if ( nonInsertableFake ) {
			return false;
		}

		if ( newObj == null || oldObj == null || newObj.getClass().equals( oldObj.getClass() ) ) {
			return !EntityTools.entitiesEqual( session, referencedEntityName, newObj, oldObj );
		}

		// There is a chance that oldObj may reference the identifier of the old entity rather
		// than the entity instance itself.  This happens under Session#update with a detached
		// entity because the database snapshot that is used to derive the prior state doesn't
		// return the entity instances of the to-one associations but only the identifier.
		//
		// So here we assume the method was supplied the id and we ask the persister to verify
		// if the value is the identifier type.  If not, we assume its the entity type and
		// therefore resolve the identifier from the entity directly prior to simply then
		// doing the identifier comparison.

		final EntityPersister persister = session.getFactory().getMetamodel().entityPersister( referencedEntityName );

		Object resolvedNewObjectId = newObj;
		if ( !persister.getIdentifierType().getReturnedClass().isInstance( newObj ) ) {
			resolvedNewObjectId = EntityTools.getIdentifier( session, referencedEntityName, newObj );
		}

		Object resolvedOldObjectId = oldObj;
		if ( !persister.getIdentifierType().getReturnedClass().isInstance( oldObj ) ) {
			resolvedOldObjectId = EntityTools.getIdentifier( session, referencedEntityName, oldObj );
		}

		return !Objects.deepEquals( resolvedNewObjectId, resolvedOldObjectId );
	}

	@Override
	public void nullSafeMapToEntityFromMap(
			EnversService enversService,
			Object obj,
			Map data,
			Object primaryKey,
			AuditReaderImplementor versionsReader,
			Number revision) {
		final Object entityId = delegate.mapToIdFromMap( data );
		Object value = null;
		if ( entityId != null ) {
			if ( versionsReader.getFirstLevelCache().contains( referencedEntityName, revision, entityId ) ) {
				value = versionsReader.getFirstLevelCache().get( referencedEntityName, revision, entityId );
			}
			else {
				final EntityInfo referencedEntity = getEntityInfo( enversService, referencedEntityName );
				boolean ignoreNotFound = false;
				if ( !referencedEntity.isAudited() ) {
					final String referencingEntityName = enversService.getEntitiesConfigurations().getEntityNameForVersionsEntityName( (String) data.get( "$type$" ) );
					if ( referencingEntityName == null && primaryKey == null ) {
						// HHH-11215 - Fix for NPE when Embeddable with ManyToOne inside ElementCollection
						// an embeddable in an element-collection
						// todo: perhaps the mapper should account for this instead?
						ignoreNotFound = true;
					}
					else {
						ignoreNotFound = enversService.getEntitiesConfigurations().getRelationDescription( referencingEntityName, getPropertyData().getName() ).isIgnoreNotFound();
					}
				}
				if ( ignoreNotFound ) {
					// Eagerly loading referenced entity to silence potential (in case of proxy)
					// EntityNotFoundException or ObjectNotFoundException. Assigning null reference.
					value = ToOneEntityLoader.loadImmediate(
							versionsReader,
							referencedEntity.getEntityClass(),
							referencedEntityName,
							entityId,
							revision,
							RevisionType.DEL.equals( data.get( enversService.getAuditEntitiesConfiguration().getRevisionTypePropName() ) ),
							enversService
					);
				}
				else {
					value = ToOneEntityLoader.createProxyOrLoadImmediate(
							versionsReader,
							referencedEntity.getEntityClass(),
							referencedEntityName,
							entityId,
							revision,
							RevisionType.DEL.equals( data.get( enversService.getAuditEntitiesConfiguration().getRevisionTypePropName() ) ),
							enversService
					);
				}
			}
		}

		setPropertyValue( obj, value );
	}

	public void addMiddleEqualToQuery(
			Parameters parameters,
			String idPrefix1,
			String prefix1,
			String idPrefix2,
			String prefix2) {
		delegate.addIdsEqualToQuery( parameters, prefix1, delegate, prefix2 );
	}
}
