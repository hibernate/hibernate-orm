/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Metamodel;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.naming.Identifier;
import org.hibernate.query.sqm.consume.multitable.internal.PersistentTableSessionUidSupport;
import org.hibernate.query.sqm.consume.multitable.internal.StandardIdTableSupport;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * This is a strategy that mimics temporary tables for databases which do not support
 * temporary tables.  It follows a pattern similar to the ANSI SQL definition of global
 * temporary table using a "session id" column to segment rows from the various sessions.
 *
 * @author Steve Ebersole
 */
public class PersistentTableStrategy
		extends AbstractTableBasedStrategy {

	public static final String SHORT_NAME = "persistent";

	public static final String DROP_ID_TABLES = "hibernate.hql.bulk_id_strategy.persistent.drop_tables";

	public static final String SCHEMA = "hibernate.hql.bulk_id_strategy.persistent.schema";
	public static final String CATALOG = "hibernate.hql.bulk_id_strategy.persistent.catalog";

	private final SessionUidSupport sessionUidSupport = new PersistentTableSessionUidSupport();

	private final IdTableSupport idTableSupport;

	private Identifier configuredCatalog;
	private Identifier configuredSchema;

	private List<IdTableHelper> idTableHelpers;

	public PersistentTableStrategy(Exporter<IdTable> exporter) {
		this( new StandardIdTableSupport( exporter ) );
	}

	public PersistentTableStrategy(IdTableSupport idTableSupport) {
		this.idTableSupport = idTableSupport;
	}

	@Override
	protected SessionUidSupport getSessionUidSupport() {
		return sessionUidSupport;
	}

	@Override
	protected IdTableSupport getIdTableSupport() {
		return idTableSupport;
	}

	@Override
	public AfterUseAction getAfterUseAction() {
		return AfterUseAction.CLEAN;
	}

	@Override
	public void prepare(
			Metamodel runtimeMetadata,
			SessionFactoryOptions sessionFactoryOptions,
			JdbcConnectionAccess connectionAccess) {
		final ServiceRegistry serviceRegistry = sessionFactoryOptions.getServiceRegistry();
		final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );

		final String catalogName = configService.getSetting(
				CATALOG,
				StandardConverters.STRING,
				configService.getSetting( AvailableSettings.DEFAULT_CATALOG, StandardConverters.STRING )
		);
		final String schemaName = configService.getSetting(
				SCHEMA,
				StandardConverters.STRING,
				configService.getSetting( AvailableSettings.DEFAULT_SCHEMA, StandardConverters.STRING )
		);

		this.configuredCatalog = jdbcEnvironment.getIdentifierHelper().toIdentifier( catalogName );
		this.configuredSchema = jdbcEnvironment.getIdentifierHelper().toIdentifier( schemaName );

		final boolean dropIdTables = configService.getSetting(
				DROP_ID_TABLES,
				StandardConverters.BOOLEAN,
				false
		);


		if ( dropIdTables ) {
			// trigger #generateIdTableDefinition to keep around the IdTableHelper for drops
			idTableHelpers = new ArrayList<>();
		}

		super.prepare( runtimeMetadata, sessionFactoryOptions, connectionAccess );
	}

	@Override
	protected IdTable generateIdTableDefinition(
			EntityTypeDescriptor entityDescriptor,
			SessionFactoryOptions sessionFactoryOptions,
			JdbcConnectionAccess connectionAccess) {
		final IdTable idTable = super.generateIdTableDefinition(
				entityDescriptor,
				sessionFactoryOptions,
				connectionAccess
		);

		// see if the user explicitly requested specific transactionality via settings...
		final IdTableManagementTransactionality transactionality;

		switch ( sessionFactoryOptions.getTempTableDdlTransactionHandling() ) {
			case ISOLATE: {
				transactionality = IdTableManagementTransactionality.ISOLATE;
				break;
			}
			case ISOLATE_AND_TRANSACT: {
				transactionality = IdTableManagementTransactionality.ISOLATE_AND_TRANSACT;
				break;
			}
			default: {
				transactionality = IdTableManagementTransactionality.NONE;
			}
		}

		final IdTableHelper idTableHelper = new IdTableHelper(
				idTable,
				getIdTableSupport(),
				transactionality,
				sessionFactoryOptions.getServiceRegistry().getService( JdbcServices.class )
		);

		idTableHelper.createIdTable( connectionAccess );

		if ( idTableHelpers != null ) {
			idTableHelpers.add( idTableHelper );
		}

		return idTable;
	}

	@Override
	protected NamespaceHandling getNamespaceHandling() {
		return NamespaceHandling.PREFER_SETTINGS;
	}

	@Override
	protected Identifier getConfiguredCatalog() {
		return configuredCatalog;
	}

	@Override
	protected Identifier getConfiguredSchema() {
		return configuredSchema;
	}

	@Override
	public void release(
			Metamodel runtimeMetadata,
			JdbcConnectionAccess connectionAccess) {
		if ( idTableHelpers != null ) {
			for ( IdTableHelper idTableHelper : idTableHelpers ) {
				idTableHelper.dropIdTable( connectionAccess );
			}
		}

		super.release( runtimeMetadata, connectionAccess );
	}
}
