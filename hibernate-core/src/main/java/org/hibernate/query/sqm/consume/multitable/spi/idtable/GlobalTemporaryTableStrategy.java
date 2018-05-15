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
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.sqm.consume.multitable.internal.StandardIdTableSupport;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * Strategy based on ANSI SQL's definition of a "global temporary table".
 *
 * @author Steve Ebersole
 */
public class GlobalTemporaryTableStrategy
		extends AbstractTableBasedStrategy {
	public static final String DROP_ID_TABLES = "hibernate.hql.bulk_id_strategy.global_temporary.drop_tables";

	public static final String SHORT_NAME = "global_temporary";
	private final IdTableSupport idTableSupport;

	private IdTableManagementTransactionality transactionality;
	private List<IdTableHelper> idTableHelpers;

	public GlobalTemporaryTableStrategy() {
		this( generateStandardExporter() );
	}

	private static Exporter<IdTable> generateStandardExporter() {
		return new GlobalTempTableExporter();
	}

	public GlobalTemporaryTableStrategy(Exporter<IdTable> exporter) {
		this( new StandardIdTableSupport( exporter ) );
	}

	public GlobalTemporaryTableStrategy(IdTableSupport idTableSupport) {
		this.idTableSupport = idTableSupport;
	}

	@Override
	protected IdTableSupport getIdTableSupport() {
		return idTableSupport;
	}

	@Override
	protected NamespaceHandling getNamespaceHandling() {
		return NamespaceHandling.USE_NONE;
	}

	@Override
	public void prepare(
			Metamodel runtimeMetadata,
			SessionFactoryOptions sessionFactoryOptions,
			JdbcConnectionAccess connectionAccess) {

		final StandardServiceRegistry serviceRegistry = sessionFactoryOptions.getServiceRegistry();
		final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );
		final boolean dropIdTables = configService.getSetting(
				DROP_ID_TABLES,
				StandardConverters.BOOLEAN,
				false
		);

		if ( dropIdTables ) {
			// trigger #generateIdTableDefinition to keep around the IdTableHelper for drops
			idTableHelpers = new ArrayList<>();
		}

		// see if the user explicitly requested specific transactionality via settings...
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
				// nothing to do - null will be handled properly by super
			}
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
	public BeforeUseAction getBeforeUseAction() {
		return BeforeUseAction.NONE;
	}

	@Override
	public AfterUseAction getAfterUseAction() {
		return AfterUseAction.CLEAN;
	}

	@Override
	public IdTableManagementTransactionality getTableManagementTransactionality() {
		return transactionality;
	}

	@Override
	public void release(Metamodel runtimeMetadata, JdbcConnectionAccess connectionAccess) {
		if ( idTableHelpers != null ) {
			for ( IdTableHelper idTableHelper : idTableHelpers ) {
				idTableHelper.dropIdTable( connectionAccess );
			}
		}

		super.release( runtimeMetadata, connectionAccess );
	}

}
