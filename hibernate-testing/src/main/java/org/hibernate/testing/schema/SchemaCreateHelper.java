/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.schema;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistry;
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
public class SchemaCreateHelper {
	public static void create(Metadata metadata) {
		create(
				metadata,
				( (MetadataImplementor) metadata ).getMetadataBuildingOptions().getServiceRegistry()
		);
	}

	public static void create(Metadata metadata, ServiceRegistry serviceRegistry) {
		final Map settings = serviceRegistry.getService( ConfigurationService.class ).getSettings();
		settings.put( AvailableSettings.HBM2DDL_DATABASE_ACTION, Action.CREATE );
		SchemaManagementToolCoordinator.process(
				metadata,
				serviceRegistry,
				settings,
				DelayedDropRegistryNotAvailableImpl.INSTANCE
		);
	}

	public static void createOnly(Metadata metadata, ServiceRegistry serviceRegistry) {
		final Map settings = serviceRegistry.getService( ConfigurationService.class ).getSettings();
		settings.put( AvailableSettings.HBM2DDL_DATABASE_ACTION, Action.CREATE_ONLY );
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
		settings.put( AvailableSettings.HBM2DDL_DATABASE_ACTION, Action.CREATE );
		settings.put( AvailableSettings.HBM2DDL_CONNECTION, connection );
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
		final Map settings = serviceRegistry.getService( ConfigurationService.class ).getSettings();
		settings.put( AvailableSettings.HBM2DDL_SCRIPTS_ACTION, Action.CREATE );
		settings.put( AvailableSettings.HBM2DDL_SCRIPTS_CREATE_TARGET, writer );
		SchemaManagementToolCoordinator.process(
				metadata,
				serviceRegistry,
				settings,
				DelayedDropRegistryNotAvailableImpl.INSTANCE
		);
	}
}
