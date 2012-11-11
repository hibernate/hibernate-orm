package org.hibernate.envers.entities.mapper.relation;

import java.io.Serializable;

import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.mapper.relation.lazy.ToOneDelegateSessionImplementor;
import org.hibernate.envers.reader.AuditReaderImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class ToOneEntityLoader {
	/**
	 * Immediately loads historical entity or its current state when excluded from audit process.
	 */
	public static Object loadImmediate(AuditReaderImplementor versionsReader, Class<?> entityClass, String entityName,
									   Object entityId, Number revision, AuditConfiguration verCfg) {
		if ( verCfg.getEntCfg().getNotVersionEntityConfiguration( entityName ) == null ) {
			// Audited relation, look up entity with Envers.
			return versionsReader.find( entityClass, entityName, entityId, revision );
		}
		else {
			// Not audited relation, look up entity with Hibernate.
			return versionsReader.getSessionImplementor().immediateLoad( entityName, (Serializable) entityId );
		}
	}

	/**
	 * Creates proxy of referenced *-to-one entity.
	 */
	public static Object createProxy(AuditReaderImplementor versionsReader, Class<?> entityClass, String entityName,
									 Object entityId, Number revision, AuditConfiguration verCfg) {
		EntityPersister persister = versionsReader.getSessionImplementor().getFactory().getEntityPersister( entityName );
		return persister.createProxy(
				(Serializable) entityId,
				new ToOneDelegateSessionImplementor( versionsReader, entityClass, entityId, revision, verCfg )
		);
	}

	/**
	 * Creates Hibernate proxy or retrieves the complete object of an entity if proxy is not
	 * allowed (e.g. @Proxy(lazy=false), final class).
	 */
	public static Object createProxyOrLoadImmediate(AuditReaderImplementor versionsReader, Class<?> entityClass, String entityName,
													Object entityId, Number revision, AuditConfiguration verCfg) {
		EntityPersister persister = versionsReader.getSessionImplementor().getFactory().getEntityPersister( entityName );
		if ( persister.hasProxy() ) {
			return createProxy( versionsReader, entityClass, entityName, entityId, revision, verCfg );
		}
		return loadImmediate( versionsReader, entityClass, entityName, entityId, revision, verCfg );
	}
}
