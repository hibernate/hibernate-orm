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
import org.hibernate.tool.schema.spi.SchemaFilter;
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
		return new SchemaCreatorImpl( getSchemaFilter( options, AvailableSettings.SCHEMA_CREATE_FILTER ) );
	}

	@Override
	public SchemaDropper getSchemaDropper(Map options) {
		return new SchemaDropperImpl( getSchemaFilter( options, AvailableSettings.SCHEMA_DROP_FILTER ) );
	}

	@Override
	public SchemaMigrator getSchemaMigrator(Map options) {
		return new SchemaMigratorImpl( getSchemaFilter( options, AvailableSettings.SCHEMA_MIGRATE_FILTER ) );
	}

	@Override
	public SchemaValidator getSchemaValidator(Map options) {
		final Dialect dialect = serviceRegistry.getService( JdbcServices.class ).getDialect();
		return new SchemaValidatorImpl(dialect);
	}
	
	private SchemaFilter getSchemaFilter(Map options, String key) {
		return serviceRegistry.getService( StrategySelector.class )
				.resolveDefaultableStrategy( SchemaFilter.class, options.get( key ), DefaultSchemaFilter.INSTANCE );
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
}
