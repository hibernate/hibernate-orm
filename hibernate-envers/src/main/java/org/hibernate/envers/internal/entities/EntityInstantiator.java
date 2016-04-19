/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.entities.mapper.id.MultipleIdMapper;
import org.hibernate.envers.internal.entities.mapper.id.SingleIdMapper;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.ToOneDelegateSessionImplementor;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Hern&aacute;n Chanfreau
 */
public class EntityInstantiator {
	private final EnversService enversService;
	private final AuditReaderImplementor versionsReader;

	public EntityInstantiator(EnversService enversService, AuditReaderImplementor versionsReader) {
		this.enversService = enversService;
		this.versionsReader = versionsReader;
	}

	/**
	 * Creates an entity instance based on an entry from the versions table.
	 *
	 * @param entityName Name of the entity, which instances should be read
	 * @param versionsEntity An entry in the versions table, from which data should be mapped.
	 * @param revision Revision at which this entity was read.
	 *
	 * @return An entity instance, with versioned properties set as in the versionsEntity map, and proxies
	 *         created for collections.
	 */
	public Object createInstanceFromVersionsEntity(String entityName, Map versionsEntity, Number revision) {
		if ( versionsEntity == null ) {
			return null;
		}

		// The $type$ property holds the name of the (versions) entity
		final String type = enversService.getEntitiesConfigurations()
				.getEntityNameForVersionsEntityName( (String) versionsEntity.get( "$type$" ) );

		if ( type != null ) {
			entityName = type;
		}

		// First mapping the primary key
		final IdMapper idMapper = enversService.getEntitiesConfigurations().get( entityName ).getIdMapper();
		final Map originalId = (Map) versionsEntity.get( enversService.getAuditEntitiesConfiguration().getOriginalIdPropName() );

		// Fixes HHH-4751 issue (@IdClass with @ManyToOne relation mapping inside)
		// Note that identifiers are always audited
		// Replace identifier proxies if do not point to audit tables
		replaceNonAuditIdProxies(
				originalId,
				revision,
				(RevisionType) versionsEntity.get( enversService.getAuditEntitiesConfiguration().getRevisionTypePropName() )
		);

		final Object primaryKey = idMapper.mapToIdFromMap( replaceEntityIds( originalId , idMapper) );

		// Checking if the entity is in cache
		if ( versionsReader.getFirstLevelCache().contains( entityName, revision, primaryKey ) ) {
			return versionsReader.getFirstLevelCache().get( entityName, revision, primaryKey );
		}

		// If it is not in the cache, creating a new entity instance
		Object ret;
		try {
			EntityConfiguration entCfg = enversService.getEntitiesConfigurations().get( entityName );
			if ( entCfg == null ) {
				// a relation marked as RelationTargetAuditMode.NOT_AUDITED
				entCfg = enversService.getEntitiesConfigurations().getNotVersionEntityConfiguration( entityName );
			}

			final Class<?> cls = ReflectionTools.loadClass( entCfg.getEntityClassName(), enversService.getClassLoaderService() );
			ret = ReflectHelper.getDefaultConstructor( cls ).newInstance();
		}
		catch (Exception e) {
			throw new AuditException( e );
		}

		// Putting the newly created entity instance into the first level cache, in case a one-to-one bidirectional
		// relation is present (which is eagerly loaded).
		versionsReader.getFirstLevelCache().put( entityName, revision, primaryKey, ret );

		enversService.getEntitiesConfigurations().get( entityName ).getPropertyMapper().mapToEntityFromMap(
				enversService,
				ret,
				versionsEntity,
				primaryKey,
				versionsReader,
				revision
		);
		idMapper.mapToEntityFromMap( ret, originalId );

		// Put entity on entityName cache after mapping it from the map representation
		versionsReader.getFirstLevelCache().putOnEntityNameCache( primaryKey, revision, ret, entityName );

		return ret;
	}

	@SuppressWarnings({"unchecked"})
	private void replaceNonAuditIdProxies(Map originalId, Number revision, RevisionType revisionType) {
		for ( Object key : originalId.keySet() ) {
			final Object value = originalId.get( key );
			if ( value instanceof HibernateProxy ) {
				final HibernateProxy hibernateProxy = (HibernateProxy) value;
				final LazyInitializer initializer = hibernateProxy.getHibernateLazyInitializer();
				final String entityName = initializer.getEntityName();
				final Serializable entityId = initializer.getIdentifier();
				if ( enversService.getEntitiesConfigurations().isVersioned( entityName ) ) {
					final String entityClassName = enversService.getEntitiesConfigurations().get( entityName ).getEntityClassName();
					final Class entityClass = ReflectionTools.loadClass(
							entityClassName,
							enversService.getClassLoaderService()
					);
					final ToOneDelegateSessionImplementor delegate = new ToOneDelegateSessionImplementor(
							versionsReader,
							entityClass,
							entityId,
							revision,
							RevisionType.DEL.equals(
									revisionType
							),
							enversService
					);
					originalId.put(
							key,
							versionsReader.getSessionImplementor()
									.getFactory()
									.getEntityPersister( entityName )
									.createProxy( entityId, delegate )
					);
				}
			}
		}
	}

	private Map replaceEntityIds( Map originalId, IdMapper idMapper ) {
		final Map map;
		//For composite identifiers where one of the identifiers is an entity (i..e @ManyToOne), the @IdClass is specified to use the primary key of that entity as the property type representing that identifier
		//    Because of this we must unwrap the entities for those composite IDs
		if ( idMapper instanceof MultipleIdMapper ) {  //Only for composite IDs and not for Embedded Ids
			final Class<?> compositeClass = ( (MultipleIdMapper) idMapper ).getCompositeIdClass();
			if ( compositeClass != null ) {
				map = new HashMap( originalId.size() );
				for ( final Map.Entry<PropertyData, SingleIdMapper> entry : ( (MultipleIdMapper) idMapper ).getIds() ) {
					final Object value = entry.getValue().mapToIdFromMap( originalId );
					final Class<?> propertyType = ReflectionTools.getType( compositeClass, entry.getKey(), enversService.getServiceRegistry() );
					if ( !propertyType.isInstance( value ) && value instanceof HibernateProxy ) {
						final String entityName = ( (HibernateProxy) value ).getHibernateLazyInitializer().getEntityName();
						final Object entityId = enversService.getEntitiesConfigurations().get( entityName ).getIdMapper().mapToIdFromEntity( value );
						entry.getValue().mapToMapFromId( map, entityId );
					}
					else {
						entry.getValue().mapToMapFromId( map, value );
					}
				}
			}
			else {
				map = originalId;
			}
		}
		else {
			map = originalId;
		}
		return map;
	}

	@SuppressWarnings({"unchecked"})
	public void addInstancesFromVersionsEntities(
			String entityName,
			Collection addTo,
			List<Map> versionsEntities,
			Number revision) {
		for ( Map versionsEntity : versionsEntities ) {
			addTo.add( createInstanceFromVersionsEntity( entityName, versionsEntity, revision ) );
		}
	}

	public EnversService getEnversService() {
		return enversService;
	}

	public AuditReaderImplementor getAuditReaderImplementor() {
		return versionsReader;
	}
}
