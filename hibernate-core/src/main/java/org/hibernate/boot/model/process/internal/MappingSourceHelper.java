package org.hibernate.boot.model.process.internal;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.service.ServiceRegistry;

/// Applies configuration references at the initial preparation boundary.
///
/// @author Steve Ebersole
public final class MappingSourceHelper {
	private MappingSourceHelper() {
	}

	public static void applyConfigurationMappings(MetadataSources sources, ServiceRegistry registry) {
		final var config = registry.requireService( CfgXmlAccessService.class ).getAggregatedConfig();
		if ( config != null && config.getMappingReferences() != null ) {
			config.getMappingReferences().forEach( reference -> reference.apply( sources ) );
		}
	}
}
