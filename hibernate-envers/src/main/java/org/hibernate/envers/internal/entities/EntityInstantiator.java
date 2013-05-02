/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.internal.entities;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
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
	private final AuditConfiguration verCfg;
	private final AuditReaderImplementor versionsReader;

	public EntityInstantiator(AuditConfiguration verCfg, AuditReaderImplementor versionsReader) {
		this.verCfg = verCfg;
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
		final String type = verCfg.getEntCfg().getEntityNameForVersionsEntityName( (String) versionsEntity.get( "$type$" ) );

		if ( type != null ) {
			entityName = type;
		}

		// First mapping the primary key
		final IdMapper idMapper = verCfg.getEntCfg().get( entityName ).getIdMapper();
		final Map originalId = (Map) versionsEntity.get( verCfg.getAuditEntCfg().getOriginalIdPropName() );

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
		Object ret;
		try {
			EntityConfiguration entCfg = verCfg.getEntCfg().get( entityName );
			if ( entCfg == null ) {
				// a relation marked as RelationTargetAuditMode.NOT_AUDITED
				entCfg = verCfg.getEntCfg().getNotVersionEntityConfiguration( entityName );
			}

			final Class<?> cls = ReflectionTools.loadClass( entCfg.getEntityClassName(), verCfg.getClassLoaderService() );
			ret = ReflectHelper.getDefaultConstructor( cls ).newInstance();
		}
		catch (Exception e) {
			throw new AuditException( e );
		}

		// Putting the newly created entity instance into the first level cache, in case a one-to-one bidirectional
		// relation is present (which is eagerly loaded).
		versionsReader.getFirstLevelCache().put( entityName, revision, primaryKey, ret );

		verCfg.getEntCfg().get( entityName ).getPropertyMapper().mapToEntityFromMap(
				verCfg,
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
	private void replaceNonAuditIdProxies(Map versionsEntity, Number revision) {
		final Map originalId = (Map) versionsEntity.get( verCfg.getAuditEntCfg().getOriginalIdPropName() );
		for ( Object key : originalId.keySet() ) {
			final Object value = originalId.get( key );
			if ( value instanceof HibernateProxy ) {
				final HibernateProxy hibernateProxy = (HibernateProxy) value;
				final LazyInitializer initializer = hibernateProxy.getHibernateLazyInitializer();
				final String entityName = initializer.getEntityName();
				final Serializable entityId = initializer.getIdentifier();
				if ( verCfg.getEntCfg().isVersioned( entityName ) ) {
					final String entityClassName = verCfg.getEntCfg().get( entityName ).getEntityClassName();
					final Class entityClass = ReflectionTools.loadClass(
							entityClassName,
							verCfg.getClassLoaderService()
					);
					final ToOneDelegateSessionImplementor delegate = new ToOneDelegateSessionImplementor(
							versionsReader, entityClass, entityId, revision,
							RevisionType.DEL.equals(
									versionsEntity.get(
											verCfg.getAuditEntCfg()
													.getRevisionTypePropName()
									)
							),
							verCfg
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

	public AuditConfiguration getAuditConfiguration() {
		return verCfg;
	}

	public AuditReaderImplementor getAuditReaderImplementor() {
		return versionsReader;
	}
}
