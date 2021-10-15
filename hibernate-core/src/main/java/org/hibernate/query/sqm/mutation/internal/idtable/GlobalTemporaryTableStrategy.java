/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.idtable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

import org.jboss.logging.Logger;

/**
 * Strategy based on ANSI SQL's definition of a "global temporary table".
 *
 * @author Steve Ebersole
 */
public class GlobalTemporaryTableStrategy implements SqmMultiTableMutationStrategy {
	private static final Logger log = Logger.getLogger( GlobalTemporaryTableStrategy.class );

	public static final String SHORT_NAME = "global_temporary";
	public static final String DROP_ID_TABLES = "hibernate.hql.bulk_id_strategy.global_temporary.drop_tables";

	private final IdTable idTable;
	private final AfterUseAction afterUseAction;
	private final Supplier<IdTableExporter> idTableExporterAccess;

	private final SessionFactoryImplementor sessionFactory;

	private boolean prepared;
	private boolean dropIdTables;

	public GlobalTemporaryTableStrategy(
			IdTable idTable,
			Supplier<IdTableExporter> idTableExporterAccess,
			AfterUseAction afterUseAction,
			SessionFactoryImplementor sessionFactory) {
		this.idTable = idTable;
		this.idTableExporterAccess = idTableExporterAccess;
		this.afterUseAction = afterUseAction;
		this.sessionFactory = sessionFactory;

		if ( afterUseAction == AfterUseAction.DROP ) {
			throw new IllegalArgumentException( "Global-temp ID tables cannot use AfterUseAction.DROP : " + idTable.getTableExpression() );
		}
	}

	@Override
	public int executeUpdate(
			SqmUpdateStatement sqmUpdate,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		return new TableBasedUpdateHandler(
				sqmUpdate,
				domainParameterXref,
				idTable,
				// generally a global temp table should already track a Connection-specific uid,
				// but just in case a particular env needs it...
				session -> session.getSessionIdentifier().toString(),
				idTableExporterAccess,
				BeforeUseAction.CREATE,
				afterUseAction,
				TempTableDdlTransactionHandling.NONE,
				sessionFactory
		).execute( context );
	}

	@Override
	public int executeDelete(
			SqmDeleteStatement sqmDelete,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		return new TableBasedDeleteHandler(
				sqmDelete,
				domainParameterXref,
				idTable,
				// generally a global temp table should already track a Connection-specific uid,
				// but just in case a particular env needs it...
				session -> session.getSessionIdentifier().toString(),
				idTableExporterAccess,
				BeforeUseAction.CREATE,
				afterUseAction,
				TempTableDdlTransactionHandling.NONE,
				sessionFactory
		).execute( context );
	}

	@Override
	public void prepare(
			MappingModelCreationProcess mappingModelCreationProcess,
			JdbcConnectionAccess connectionAccess) {
		if ( prepared ) {
			return;
		}

		prepared = true;

		log.debugf( "Creating global-temp ID table : %s", idTable.getTableExpression() );

		final IdTableHelper.IdTableCreationWork idTableCreationWork = new IdTableHelper.IdTableCreationWork(
				idTable,
				idTableExporterAccess.get(),
				sessionFactory
		);
		Connection connection;
		try {
			connection = connectionAccess.obtainConnection();
		}
		catch (UnsupportedOperationException e) {
			// assume this comes from org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl
			log.debug( "Unable to obtain JDBC connection; assuming ID tables already exist or wont be needed" );
			return;
		}
		catch (SQLException e) {
			log.error( "Unable obtain JDBC Connection", e );
			return;
		}

		try {
			idTableCreationWork.execute( connection );
			final ConfigurationService configService = mappingModelCreationProcess.getCreationContext()
					.getBootstrapContext()
					.getServiceRegistry().getService( ConfigurationService.class );
			this.dropIdTables = configService.getSetting(
					DROP_ID_TABLES,
					StandardConverters.BOOLEAN,
					false
			);
		}
		finally {
			try {
				connectionAccess.releaseConnection( connection );
			}
			catch (SQLException ignore) {
			}
		}
	}

	@Override
	public void release(
			SessionFactoryImplementor sessionFactory,
			JdbcConnectionAccess connectionAccess) {
		if ( !dropIdTables ) {
			return;
		}

		dropIdTables = false;

		log.debugf( "Dropping global-temp ID table : %s", idTable.getTableExpression() );

		final IdTableHelper.IdTableDropWork idTableDropWork = new IdTableHelper.IdTableDropWork(
				idTable,
				idTableExporterAccess.get(),
				sessionFactory
		);
		Connection connection;
		try {
			connection = connectionAccess.obtainConnection();
		}
		catch (UnsupportedOperationException e) {
			// assume this comes from org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl
			log.debugf( "Unable to obtain JDBC connection; unable to drop global-temp ID table : %s", idTable.getTableExpression() );
			return;
		}
		catch (SQLException e) {
			log.error( "Unable obtain JDBC Connection", e );
			return;
		}

		try {
			idTableDropWork.execute( connection );
		}
		finally {
			try {
				connectionAccess.releaseConnection( connection );
			}
			catch (SQLException ignore) {
			}
		}
	}
}
