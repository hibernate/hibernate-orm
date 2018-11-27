/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.metamodel.spi;

import java.util.Map;

import org.hibernate.envers.boot.AuditService;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;

/**
 * @author Chris Cranford
 */
public interface EntityInstantiator {
	/**
	 * Create an instance of the entity from the entity-map data.
	 *
	 * @param entityName The entity name.
	 * @param versionedEntity The entity-map data.
	 * @param revision The revision number.
	 *
	 * @return The constructed entity instance.
	 */
	Object createInstanceFromVersionsEntity(
			String entityName,
			Map<?,?> versionedEntity,
			Number revision);

	AuditService getAuditService();

	AuditReaderImplementor getAuditReaderImplementor();
}
