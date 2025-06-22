/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.boot.MetadataBuilder;

/**
 * Internal API for {@link MetadataBuilder} exposing the building options being collected.
 *
 * @author Steve Ebersole
 */
public interface MetadataBuilderImplementor extends MetadataBuilder {
	BootstrapContext getBootstrapContext();

	/**
	 * Get the options being collected on this MetadataBuilder that will ultimately be used in
	 * building the Metadata.
	 *
	 * @return The current building options
	 */
	MetadataBuildingOptions getMetadataBuildingOptions();
}
