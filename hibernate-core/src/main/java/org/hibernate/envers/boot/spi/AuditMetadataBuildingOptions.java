/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.spi;

import org.hibernate.service.ServiceRegistry;

/**
 * Describes the options used while building the AuditMetadata object (during
 * {@link org.hibernate.boot.MetadataBuilder#build()} processing).
 *
 * @author Chris Cranford
 * @since 6.0
 */
public interface AuditMetadataBuildingOptions extends AuditServiceOptions {
	/**
	 * The service registry to use when building the metadata.
	 *
	 * @return The service registry to use.
	 */
	ServiceRegistry getServiceRegistry();

	/**
	 * Get the specified {@code entityName} as the audited entity name by applying any
	 * prefix/suffix configuration attributes to the specified value.
	 *
	 * @param entityName the entity name.
	 * @return The audit entity name.
	 */
	String getAuditEntityName(String entityName);

	/**
	 * Add a custom audit table name for the specified entity name.
	 *
	 * @param entityName the entity name to register an audit table for.
	 * @param tableName the table name to register.
	 */
	void addCustomAuditTableName(String entityName, String tableName);

	/**
	 * Get the entity name's audit table.
	 * <p/>
	 * This internally will first delegate to any custom registered audit tables and if
	 * none are found, it will use the specified table name applying any prefix/suffix
	 * configuration attributes to the name.
	 *
	 * @param entityName the entity name.
	 * @param tableName the table name.
	 * @return the audit table name.
	 */
	String getAuditTableName(String entityName, String tableName);

	/**
	 * Get the specified {@code propertyName}'s property path.
	 *
	 * @param propertyName the property name.
	 * @return the revision property path for the property name.
	 */
	String getRevisionPropertyPath(String propertyName);
}
