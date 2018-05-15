/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.spi;

import org.hibernate.envers.boot.AuditMetadataBuilder;

/**
 * Internal API for AuditMetadataBuilder exposing the building options being collected.
 *
 * @author Chris Cranford
 * @since 6.0
 */
public interface AuditMetadataBuilderImplementor extends AuditMetadataBuilder {
	/**
	 * Get the options being collected on this AuditMetadataBuilder that will utilmately be used
	 * in building the AuditMetadata.
	 *
	 * @return The current building options
	 */
	AuditMetadataBuildingOptions getAuditMetadataBuildingOptions();
}
