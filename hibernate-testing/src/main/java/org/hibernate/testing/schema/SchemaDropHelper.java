/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.schema;

import java.sql.Connection;
import java.util.Map;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.internal.Helper;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class SchemaDropHelper {
	public static void drop(MetadataImplementor metadata) {
		drop( metadata, metadata.getMetadataBuildingOptions().getServiceRegistry() );
	}

	@SuppressWarnings("unchecked")
	public static void drop(MetadataImplementor metadata, ServiceRegistry serviceRegistry) {
		final Map settings = serviceRegistry.getService( ConfigurationService.class ).getSettings();
		settings.put( AvailableSettings.HBM2DDL_DATABASE_ACTION, Action.DROP );

		SchemaManagementToolCoordinator.process(
				Helper.buildDatabaseModel( metadata ),
				serviceRegistry,
				action -> {}
		);
	}

	@SuppressWarnings("unchecked")
	public static void drop(
			MetadataImplementor metadata,
			StandardServiceRegistry serviceRegistry,
			Connection connection) {
		final Map settings = serviceRegistry.getService( ConfigurationService.class ).getSettings();
		settings.put( AvailableSettings.HBM2DDL_DATABASE_ACTION, Action.DROP );
		settings.put( AvailableSettings.HBM2DDL_CONNECTION, connection );

		SchemaManagementToolCoordinator.process(
				Helper.buildDatabaseModel( metadata ),
				serviceRegistry,
				action -> {}
		);
	}
}
