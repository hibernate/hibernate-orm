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
 * This is a strategy that mimics temporary tables for databases which do not support
 * temporary tables.  It follows a pattern similar to the ANSI SQL definition of global
 * temporary table using a "session id" column to segment rows from the various sessions.
 *
 * @author Steve Ebersole
 */
public class PersistentTableStrategy implements SqmMultiTableMutationStrategy {
	private static final Logger log = Logger.getLogger( PersistentTableStrategy.class );

	public static final String SHORT_NAME = "persistent";

	public static final String DROP_ID_TABLES = "hibernate.hql.bulk_id_strategy.persistent.drop_tables";

	public static final String SCHEMA = "hibernate.hql.bulk_id_strategy.persistent.schema";
	public static final String CATALOG = "hibernate.hql.bulk_id_strategy.persistent.catalog";

	private final IdTable idTable;
	private final AfterUseAction afterUseAction;
	private final Supplier<IdTableExporter> idTableExporterAccess;

	private final SessionFactoryImplementor sessionFactory;

	private boolean prepared;
	private boolean created;
	private boolean released;

	public PersistentTableStrategy(
			IdTable idTable,
			AfterUseAction afterUseAction,
			Supplier<IdTableExporter> idTableExporterAccess,
			SessionFactoryImplementor sessionFactory) {
		this.idTable = idTable;
		this.afterUseAction = afterUseAction;
		this.idTableExporterAccess = idTableExporterAccess;
		this.sessionFactory = sessionFactory;

		if ( afterUseAction == AfterUseAction.DROP ) {
			throw new IllegalArgumentException( "Persistent ID tables cannot use AfterUseAction.DROP : " + idTable.getTableExpression() );
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

		log.debugf( "Creating persistent ID table : %s", idTable.getTableExpression() );

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
			created = true;
		}
		finally {
			try {
				connectionAccess.releaseConnection( connection );
			}
			catch (SQLException ignore) {
			}
		}

		if ( created ) {
			// todo (6.0) : register strategy for dropping of the table if requested - DROP_ID_TABLES
		}
	}

	@Override
	public void release(
			SessionFactoryImplementor sessionFactory,
			JdbcConnectionAccess connectionAccess) {
		if ( released ) {
			return;
		}

		released = true;

		if ( created ) {
			return;
		}


		log.debugf( "Dropping persistent ID table : %s", idTable.getTableExpression() );

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
			log.debugf( "Unable to obtain JDBC connection; unable to drop persistent ID table : %s", idTable.getTableExpression() );
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
