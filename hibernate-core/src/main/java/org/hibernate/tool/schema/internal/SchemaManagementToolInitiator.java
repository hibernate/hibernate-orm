/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

import static org.hibernate.cfg.SchemaToolingSettings.SCHEMA_MANAGEMENT_TOOL;

/**
 * @author Steve Ebersole
 */
public class SchemaManagementToolInitiator implements StandardServiceInitiator<SchemaManagementTool> {
	public static final SchemaManagementToolInitiator INSTANCE = new SchemaManagementToolInitiator();

	public SchemaManagementTool initiateService(
			Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		return registry.requireService( StrategySelector.class )
				.<SchemaManagementTool>resolveDefaultableStrategy( SchemaManagementTool.class,
						configurationValues.get( SCHEMA_MANAGEMENT_TOOL ),
						() -> registry.requireService( JdbcServices.class ).getDialect()
								.getFallbackSchemaManagementTool( configurationValues, registry ) );
	}

	@Override
	public Class<SchemaManagementTool> getServiceInitiated() {
		return SchemaManagementTool.class;
	}
}
