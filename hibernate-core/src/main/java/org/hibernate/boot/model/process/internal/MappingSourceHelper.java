/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.process.internal;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.engine.config.spi.ConfigurationService;

import static org.hibernate.cfg.MappingSettings.XML_MAPPING_ENABLED;
import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;

/// Applies configuration references at the initial preparation boundary.
///
/// @author Steve Ebersole
public final class MappingSourceHelper {
	private MappingSourceHelper() {
	}

	public static void applyConfigurationMappings(MetadataSources sources, ServiceRegistry registry) {
		final var config = registry.requireService( CfgXmlAccessService.class ).getAggregatedConfig();
		if ( config != null && config.getMappingReferences() != null ) {
			final boolean xmlEnabled = registry.requireService( ConfigurationService.class )
					.getSetting( XML_MAPPING_ENABLED, BOOLEAN, true );
			config.getMappingReferences().forEach( reference -> {
				switch ( reference.getType() ) {
					case CLASS, PACKAGE -> reference.apply( sources );
					default -> {
						if ( xmlEnabled ) {
							reference.apply( sources );
						}
					}
				}
			} );
		}
	}
}
