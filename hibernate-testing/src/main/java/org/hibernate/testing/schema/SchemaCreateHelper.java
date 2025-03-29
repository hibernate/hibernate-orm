/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.schema;

import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.build.AllowSysOut;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.DelayedDropRegistryNotAvailableImpl;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

/**
 * @author Steve Ebersole
 */
public class SchemaCreateHelper {
	public static void create(Metadata metadata) {
		create(
				metadata,
				( (MetadataImplementor) metadata ).getMetadataBuildingOptions().getServiceRegistry()
		);
	}

	public static void create(Metadata metadata, ServiceRegistry serviceRegistry) {
		final Map settings = serviceRegistry.getService( ConfigurationService.class ).getSettings();
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, Action.CREATE );
		SchemaManagementToolCoordinator.process(
				metadata,
				serviceRegistry,
				settings,
				DelayedDropRegistryNotAvailableImpl.INSTANCE
		);
	}

	public static void create(
			Metadata metadata,
			StandardServiceRegistry serviceRegistry,
			Connection connection) {
		final Map settings = serviceRegistry.getService( ConfigurationService.class ).getSettings();
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, Action.CREATE );
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_CONNECTION, connection );
		SchemaManagementToolCoordinator.process(
				metadata,
				serviceRegistry,
				settings,
				DelayedDropRegistryNotAvailableImpl.INSTANCE
		);
	}

	@AllowSysOut
	public static void toStdOut(Metadata metadata) {
		toWriter( metadata, new OutputStreamWriter( System.out ) );
	}

	public static void toWriter(Metadata metadata, Writer writer) {
		final ServiceRegistry serviceRegistry = ( (MetadataImplementor) metadata ).getMetadataBuildingOptions().getServiceRegistry();
		final Map<String,Object> settings = serviceRegistry.requireService( ConfigurationService.class ).getSettings();
		final Map<String,Object> copy = new HashMap<>( settings );
		copy.put( SchemaToolingSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, Action.CREATE );
		copy.put( SchemaToolingSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, writer );
		SchemaManagementToolCoordinator.process(
				metadata,
				serviceRegistry,
				copy,
				DelayedDropRegistryNotAvailableImpl.INSTANCE
		);
	}

	public static String toCreateDdl(Metadata metadata) {
		final StringWriter writer = new StringWriter();

		final ServiceRegistry serviceRegistry = ( (MetadataImplementor) metadata ).getMetadataBuildingOptions().getServiceRegistry();
		final Map<String,Object> settings = serviceRegistry.requireService( ConfigurationService.class ).getSettings();
		final Map<String,Object> copy = new HashMap<>( settings );
		copy.put( SchemaToolingSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, Action.CREATE_ONLY );
		copy.put( SchemaToolingSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, writer );
		SchemaManagementToolCoordinator.process(
				metadata,
				serviceRegistry,
				copy,
				DelayedDropRegistryNotAvailableImpl.INSTANCE
		);

		return writer.toString();
	}

	@AllowSysOut
	public static void createOnlyToStdOut(Metadata metadata, ServiceRegistry serviceRegistry) {
		createOnlyToWriter( metadata, serviceRegistry, new OutputStreamWriter( System.out ) );
	}

	public static void createOnlyToWriter(Metadata metadata, ServiceRegistry serviceRegistry, Writer target) {
		final Map<String,Object> settings = serviceRegistry.requireService( ConfigurationService.class ).getSettings();
		final Map<String,Object> copy = new HashMap<>( settings );
		copy.put( SchemaToolingSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, Action.CREATE_ONLY );
		copy.put( SchemaToolingSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, target );
		SchemaManagementToolCoordinator.process(
				metadata,
				serviceRegistry,
				copy,
				DelayedDropRegistryNotAvailableImpl.INSTANCE
		);
	}
}
