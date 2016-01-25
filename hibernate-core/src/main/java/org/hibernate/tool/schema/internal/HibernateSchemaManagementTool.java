/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.Map;

import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.SchemaValidator;

/**
 * The standard Hibernate implementation for performing schema management.
 *
 * @author Steve Ebersole
 */
public class HibernateSchemaManagementTool implements SchemaManagementTool, ServiceRegistryAwareService {
	private ServiceRegistry serviceRegistry;
	
	@Override
	public SchemaCreator getSchemaCreator(Map options) {
		return new SchemaCreatorImpl( getSchemaFilterProvider( options ).getCreateFilter() );
	}

	@Override
	public SchemaDropper getSchemaDropper(Map options) {
		return new SchemaDropperImpl( getSchemaFilterProvider( options ).getDropFilter() );
	}

	@Override
	public SchemaMigrator getSchemaMigrator(Map options) {
		return new SchemaMigratorImpl( getSchemaFilterProvider( options ).getMigrateFilter() );
	}

	@Override
	public SchemaValidator getSchemaValidator(Map options) {
		final Dialect dialect = serviceRegistry.getService( JdbcServices.class ).getDialect();
		return new SchemaValidatorImpl( getSchemaFilterProvider( options ).getValidateFilter(), dialect );
	}
	
	private SchemaFilterProvider getSchemaFilterProvider(Map options) {
		final Object configuredOption = (options == null)
				? null
				: options.get( AvailableSettings.SCHEMA_FILTER_PROVIDER );
		return serviceRegistry.getService( StrategySelector.class ).resolveDefaultableStrategy(
				SchemaFilterProvider.class,
				configuredOption,
				DefaultSchemaFilterProvider.INSTANCE
		);
	}
	
	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
}
