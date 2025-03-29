/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.schema;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.build.AllowSysOut;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.DelayedDropRegistryNotAvailableImpl;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

/**
 * @author Steve Ebersole
 */
public class SchemaUpdateHelper {
	public static void update(Metadata metadata) {
		update( metadata, ( ( MetadataImplementor) metadata ).getMetadataBuildingOptions().getServiceRegistry() );
	}

	public static void update(Metadata metadata, ServiceRegistry serviceRegistry) {
		final Map settings = serviceRegistry.getService( ConfigurationService.class ).getSettings();
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, Action.UPDATE );
		SchemaManagementToolCoordinator.process(
				metadata,
				serviceRegistry,
				settings,
				DelayedDropRegistryNotAvailableImpl.INSTANCE
		);
	}

	@AllowSysOut
	public static void toStdout(Metadata metadata) {
		toWriter( metadata, new OutputStreamWriter( System.out ) );
	}

	public static void toWriter(Metadata metadata, Writer writer) {
		final ServiceRegistry serviceRegistry = ( (MetadataImplementor) metadata ).getMetadataBuildingOptions().getServiceRegistry();
		final Map settings = serviceRegistry.getService( ConfigurationService.class ).getSettings();
		settings.put( AvailableSettings.HBM2DDL_SCRIPTS_ACTION, Action.UPDATE );
		// atm we reuse the CREATE scripts setting
		settings.put( AvailableSettings.HBM2DDL_SCRIPTS_CREATE_TARGET, writer );
		SchemaManagementToolCoordinator.process(
				metadata,
				serviceRegistry,
				settings,
				DelayedDropRegistryNotAvailableImpl.INSTANCE
		);
	}
}
