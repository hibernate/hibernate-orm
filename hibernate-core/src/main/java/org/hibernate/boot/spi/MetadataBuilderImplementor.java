/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import org.hibernate.boot.MetadataBuilder;

/**
 * Internal API for MetadataBuilder exposing the building options being collected.
 *
 * @author Steve Ebersole
 */
public interface MetadataBuilderImplementor extends MetadataBuilder {
	/**
	 * Get the options being collected on this MetadataBuilder that will ultimately be used in
	 * building the Metadata.
	 *
	 * @return The current building options
	 */
	MetadataBuildingOptions getMetadataBuildingOptions();
}
