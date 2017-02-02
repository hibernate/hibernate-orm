/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

/**
 * @author Steve Ebersole
 */
public class SchemaManagementToolInitiator implements StandardServiceInitiator<SchemaManagementTool> {
	public static final SchemaManagementToolInitiator INSTANCE = new SchemaManagementToolInitiator();

	public SchemaManagementTool initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object setting = configurationValues.get( AvailableSettings.SCHEMA_MANAGEMENT_TOOL );
		SchemaManagementTool tool = registry.getService( StrategySelector.class ).resolveStrategy( SchemaManagementTool.class, setting );
		if ( tool == null ) {
			tool = new HibernateSchemaManagementTool();
		}

		return tool;
	}

	@Override
	public Class<SchemaManagementTool> getServiceInitiated() {
		return SchemaManagementTool.class;
	}
}
