/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.ToOneDelegateSessionImplementor;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;


/**
 * @author Adam Warski (adam at warski dot org)
 * @author Hern&aacute;n Chanfreau
 * @author Chris Cranford
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
		final Map originalId = (Map) versionsEntity.get( enversService.getConfig().getOriginalIdPropertyName() );

		// Fixes HHH-4751 issue (@IdClass with @ManyToOne relation mapping inside)
		// Note that identifiers are always audited
		// Replace identifier proxies if do not point to audit tables
		replaceNonAuditIdProxies( versionsEntity, revision );

		final Object primaryKey = idMapper.mapToIdFromMap( originalId );

		// Checking if the entity is in cache
		if ( versionsReader.getFirstLevelCache().contains( entityName, revision, primaryKey ) ) {
			return versionsReader.getFirstLevelCache().get( entityName, revision, primaryKey );
		}

		// If it is not in the cache, creating a new entity instance
		Object ret = versionsReader.getSessionImplementor()
				.getFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( entityName )
				.getRepresentationStrategy().getInstantiator()
				.instantiate();

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

	@SuppressWarnings("unchecked")
	private void replaceNonAuditIdProxies(Map versionsEntity, Number revision) {
		final Map originalId = (Map) versionsEntity.get( enversService.getConfig().getOriginalIdPropertyName() );
		for ( Object key : originalId.keySet() ) {
			final Object value = originalId.get( key );
			final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( value );
			if ( lazyInitializer != null ) {
				final String entityName = lazyInitializer.getEntityName();
				final Object entityId = lazyInitializer.getInternalIdentifier();
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
									versionsEntity.get(
											enversService.getConfig().getRevisionTypePropertyName()
									)
							),
							enversService
					);
					originalId.put(
							key,
							versionsReader.getSessionImplementor()
									.getFactory()
									.getMappingMetamodel().
									getEntityDescriptor( entityName )
									.createProxy( entityId, delegate )
					);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
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
