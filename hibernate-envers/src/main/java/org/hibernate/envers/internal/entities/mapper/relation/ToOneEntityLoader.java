/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.internal.entities.mapper.relation;

import java.io.Serializable;

import org.hibernate.envers.configuration.spi.AuditConfiguration;
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
			AuditConfiguration verCfg) {
		if ( verCfg.getEntCfg().getNotVersionEntityConfiguration( entityName ) == null ) {
			// Audited relation, look up entity with Envers.
			// When user traverses removed entities graph, do not restrict revision type of referencing objects
			// to ADD or MOD (DEL possible). See HHH-5845.
			return versionsReader.find( entityClass, entityName, entityId, revision, removed );
		}
		else {
			// Not audited relation, look up entity with Hibernate.
			return versionsReader.getSessionImplementor().immediateLoad( entityName, (Serializable) entityId );
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
			AuditConfiguration verCfg) {
		final EntityPersister persister = versionsReader.getSessionImplementor()
				.getFactory()
				.getEntityPersister( entityName );
		return persister.createProxy(
				(Serializable) entityId,
				new ToOneDelegateSessionImplementor( versionsReader, entityClass, entityId, revision, removed, verCfg )
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
			AuditConfiguration verCfg) {
		final EntityPersister persister = versionsReader.getSessionImplementor()
				.getFactory()
				.getEntityPersister( entityName );
		if ( persister.hasProxy() ) {
			return createProxy( versionsReader, entityClass, entityName, entityId, revision, removed, verCfg );
		}
		return loadImmediate( versionsReader, entityClass, entityName, entityId, revision, removed, verCfg );
	}
}
