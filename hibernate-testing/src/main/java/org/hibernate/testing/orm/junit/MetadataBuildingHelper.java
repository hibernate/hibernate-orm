/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.pipeline.internal.MappingCustomizations;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;

/**
 * Test helper for resolving boot metadata from the 9.0 mapping-source pipeline.
 */
public final class MetadataBuildingHelper {
	private MetadataBuildingHelper() {
	}

	public static MetadataImplementor buildMetadata(StandardServiceRegistry serviceRegistry, MappingSources mappingSources) {
		return buildMetadata( serviceRegistry, mappingSources, MappingCustomizations.NONE );
	}

	public static MetadataImplementor buildMetadata(
			StandardServiceRegistry serviceRegistry,
			MappingSources mappingSources,
			MappingCustomizations metadataCustomizations) {
		return org.hibernate.boot.pipeline.internal.MetadataBuildingHelper.buildMetadata(
				serviceRegistry,
				mappingSources,
				metadataCustomizations
		);
	}
}
