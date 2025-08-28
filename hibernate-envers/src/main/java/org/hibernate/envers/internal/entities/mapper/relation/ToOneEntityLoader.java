/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation;


import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.ToOneDelegateSessionImplementor;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public final class ToOneEntityLoader {
	private ToOneEntityLoader() {
	}

	/**
	 * Immediately loads historical entity or its current state when excluded from audit process. Returns {@code null}
	 * reference if entity has not been found in the database.
	 */
	public static Object loadImmediate(
			AuditReaderImplementor versionsReader,
			Class<?> entityClass,
			String entityName,
			Object entityId,
			Number revision,
			boolean removed,
			EnversService enversService) {
		if ( enversService.getEntitiesConfigurations().getNotVersionEntityConfiguration( entityName ) == null ) {
			// Audited relation, look up entity with Envers.
			// When user traverses removed entities graph, do not restrict revision type of referencing objects
			// to ADD or MOD (DEL possible). See HHH-5845.
			return versionsReader.find( entityClass, entityName, entityId, revision, removed );
		}
		else {
			// Not audited relation, look up entity with Hibernate.
			return versionsReader.getSessionImplementor().immediateLoad( entityName, entityId );
		}
	}

	/**
	 * Creates proxy of referenced *-to-one entity.
	 */
	public static Object createProxy(
			AuditReaderImplementor versionsReader,
			Class<?> entityClass,
			String entityName,
			Object entityId,
			Number revision,
			boolean removed,
			EnversService enversService) {
		final EntityPersister persister = versionsReader.getSessionImplementor()
				.getFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( entityName );
		return persister.createProxy(
				entityId,
				new ToOneDelegateSessionImplementor( versionsReader, entityClass, entityId, revision, removed, enversService )
		);
	}

	/**
	 * Creates Hibernate proxy or retrieves the complete object of an entity if proxy is not
	 * allowed (e.g. @Proxy(lazy=false), final class).
	 */
	public static Object createProxyOrLoadImmediate(
			AuditReaderImplementor versionsReader,
			Class<?> entityClass,
			String entityName,
			Object entityId,
			Number revision,
			boolean removed,
			EnversService enversService) {
		final EntityPersister persister = versionsReader.getSessionImplementor()
				.getFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( entityName );
		if ( persister.hasProxy() ) {
			return createProxy( versionsReader, entityClass, entityName, entityId, revision, removed, enversService );
		}
		return loadImmediate( versionsReader, entityClass, entityName, entityId, revision, removed, enversService );
	}
}
