/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.AuditService;
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
 * @author Chris Cranford
 */
public class EntityInstantiator {

	private final AuditReaderImplementor versionsReader;

	public EntityInstantiator(AuditReaderImplementor versionsReader) {
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
		final String type = getAuditService().getEntityBindings()
				.getEntityNameForVersionsEntityName( (String) versionsEntity.get( "$type$" ) );

		if ( type != null ) {
			entityName = type;
		}

		// First mapping the primary key
		final IdMapper idMapper = getAuditService().getEntityBindings().get( entityName ).getIdMapper();
		final Map originalId = (Map) versionsEntity.get( getAuditService().getOptions().getOriginalIdPropName() );

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
			EntityConfiguration entCfg = getAuditService().getEntityBindings().get( entityName );
			if ( entCfg == null ) {
				// a relation marked as RelationTargetAuditMode.NOT_AUDITED
				entCfg = getAuditService().getEntityBindings().getNotVersionEntityConfiguration( entityName );
			}

			final Class<?> cls = ReflectionTools.loadClass(
					entCfg.getEntityClassName(),
					getAuditService().getClassLoaderService()
			);
			ret = ReflectHelper.getDefaultConstructor( cls ).newInstance();
		}
		catch (Exception e) {
			throw new AuditException( e );
		}

		// Putting the newly created entity instance into the first level cache, in case a one-to-one bidirectional
		// relation is present (which is eagerly loaded).
		versionsReader.getFirstLevelCache().put( entityName, revision, primaryKey, ret );

		getAuditService().getEntityBindings().get( entityName ).getPropertyMapper().mapToEntityFromMap(
				ret,
				versionsEntity,
				primaryKey,
				versionsReader,
				revision
		);
		idMapper.mapToEntityFromMap( ret, originalId );

		// Put entity on entityName cache afterQuery mapping it from the map representation
		versionsReader.getFirstLevelCache().putOnEntityNameCache( primaryKey, revision, ret, entityName );

		return ret;
	}

	@SuppressWarnings({"unchecked"})
	private void replaceNonAuditIdProxies(Map versionsEntity, Number revision) {
		final Map originalId = (Map) versionsEntity.get( getAuditService().getOptions().getOriginalIdPropName() );
		for ( Object key : originalId.keySet() ) {
			final Object value = originalId.get( key );
			if ( value instanceof HibernateProxy ) {
				final HibernateProxy hibernateProxy = (HibernateProxy) value;
				final LazyInitializer initializer = hibernateProxy.getHibernateLazyInitializer();
				final String entityName = initializer.getEntityName();
				final Object entityId = initializer.getIdentifier();
				if ( getAuditService().getEntityBindings().isVersioned( entityName ) ) {
					final String entityClassName = getAuditService().getEntityBindings().get( entityName ).getEntityClassName();
					final Class entityClass = ReflectionTools.loadClass(
							entityClassName,
							getAuditService().getClassLoaderService()
					);
					final ToOneDelegateSessionImplementor delegate = new ToOneDelegateSessionImplementor(
							versionsReader,
							entityClass,
							entityId,
							revision,
							RevisionType.DEL.equals(
									versionsEntity.get( getAuditService().getOptions().getRevisionTypePropName() )
							)
					);
					originalId.put(
							key,
							versionsReader.getSessionImplementor()
									.getFactory()
									.getMetamodel()
									.findEntityDescriptor( entityName )
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

	public AuditService getAuditService() {
		return getAuditReaderImplementor().getAuditService();
	}

	public AuditReaderImplementor getAuditReaderImplementor() {
		return versionsReader;
	}
}
