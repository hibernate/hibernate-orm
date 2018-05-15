/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation;

import org.hibernate.envers.boot.AuditService;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.ToOneDelegateSessionImplementor;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
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
			boolean removed) {
		final AuditService auditService = versionsReader.getAuditService();
		if ( auditService.getEntityBindings().getNotVersionEntityConfiguration( entityName ) == null ) {
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
			boolean removed) {
		final EntityTypeDescriptor entityDescriptor = resolveEntityDescriptorByName( versionsReader, entityName );
		return entityDescriptor.createProxy(
				entityId,
				new ToOneDelegateSessionImplementor( versionsReader, entityClass, entityId, revision, removed )
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
			boolean removed) {
		final EntityTypeDescriptor entityDescriptor = resolveEntityDescriptorByName( versionsReader, entityName );
		if ( entityDescriptor.hasProxy() ) {
			return createProxy( versionsReader, entityClass, entityName, entityId, revision, removed );
		}
		return loadImmediate( versionsReader, entityClass, entityName, entityId, revision, removed );
	}

	private static EntityTypeDescriptor resolveEntityDescriptorByName(
			AuditReaderImplementor versionsReader,
			String entityName) {
		return versionsReader.getSessionImplementor()
				.getFactory()
				.getMetamodel()
				.findEntityDescriptor( entityName );
	}
}
