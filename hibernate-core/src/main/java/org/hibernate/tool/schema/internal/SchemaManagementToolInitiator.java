/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

/**
 * @author Steve Ebersole
 */
public class SchemaManagementToolInitiator implements StandardServiceInitiator<SchemaManagementTool> {
	public static final SchemaManagementToolInitiator INSTANCE = new SchemaManagementToolInitiator();

	public SchemaManagementTool initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		final Object setting = configurationValues.get( AvailableSettings.SCHEMA_MANAGEMENT_TOOL );
		SchemaManagementTool tool =
				registry.requireService( StrategySelector.class )
						.resolveStrategy( SchemaManagementTool.class, setting );
		if ( tool == null ) {
			tool = registry.requireService( JdbcServices.class ).getDialect()
					.getFallbackSchemaManagementTool( configurationValues, registry );
		}

		return tool;
	}

	@Override
	public Class<SchemaManagementTool> getServiceInitiated() {
		return SchemaManagementTool.class;
	}
}
