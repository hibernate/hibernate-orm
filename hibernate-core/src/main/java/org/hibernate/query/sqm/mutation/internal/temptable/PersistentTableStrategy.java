/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableHelper;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;

import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.jboss.logging.Logger;

/**
 * This is a strategy that mimics temporary tables for databases which do not support
 * temporary tables.  It follows a pattern similar to the ANSI SQL definition of global
 * temporary table using a "session id" column to segment rows from the various sessions.
 *
 * @author Steve Ebersole
 */
public abstract class PersistentTableStrategy {
	private static final Logger log = Logger.getLogger( PersistentTableStrategy.class );

	public static final String SHORT_NAME = "persistent";

	public static final String CREATE_ID_TABLES = "hibernate.query.mutation_strategy.persistent.create_tables";

	public static final String DROP_ID_TABLES = "hibernate.query.mutation_strategy.persistent.drop_tables";

	public static final String SCHEMA = "hibernate.query.mutation_strategy.persistent.schema";

	public static final String CATALOG = "hibernate.query.mutation_strategy.persistent.catalog";

	private final TemporaryTable temporaryTable;
	private final SessionFactoryImplementor sessionFactory;

	private boolean prepared;

	private boolean dropIdTables;

	public PersistentTableStrategy(
			TemporaryTable temporaryTable,
			SessionFactoryImplementor sessionFactory) {
		this.temporaryTable = temporaryTable;
		this.sessionFactory = sessionFactory;

		if ( sessionFactory.getJdbcServices().getDialect().getTemporaryTableAfterUseAction() == AfterUseAction.DROP ) {
			throw new IllegalArgumentException( "Persistent ID tables cannot use AfterUseAction.DROP : " + temporaryTable.getTableExpression() );
		}
	}

	public EntityMappingType getEntityDescriptor() {
		return getTemporaryTable().getEntityDescriptor();
	}

	public void prepare(
			MappingModelCreationProcess mappingModelCreationProcess,
			JdbcConnectionAccess connectionAccess) {
		if ( prepared ) {
			return;
		}

		prepared = true;

		final ConfigurationService configService =
				mappingModelCreationProcess.getCreationContext()
						.getBootstrapContext().getServiceRegistry()
						.requireService( ConfigurationService.class );
		boolean createIdTables = configService.getSetting(
				CREATE_ID_TABLES,
				StandardConverters.BOOLEAN,
				true
		);

		if (!createIdTables ) {
			return;
		}

		log.debugf( "Creating persistent ID table : %s", getTemporaryTable().getTableExpression() );

		final TemporaryTableHelper.TemporaryTableCreationWork temporaryTableCreationWork = new TemporaryTableHelper.TemporaryTableCreationWork(
				getTemporaryTable(),
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
			temporaryTableCreationWork.execute( connection );
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

	public void release(
			SessionFactoryImplementor sessionFactory,
			JdbcConnectionAccess connectionAccess) {
		if ( !dropIdTables ) {
			return;
		}

		dropIdTables = false;

		final TemporaryTable temporaryTable = getTemporaryTable();
		log.debugf( "Dropping persistent ID table : %s", temporaryTable.getTableExpression() );

		final TemporaryTableHelper.TemporaryTableDropWork temporaryTableDropWork = new TemporaryTableHelper.TemporaryTableDropWork(
				temporaryTable,
				sessionFactory
		);
		Connection connection;
		try {
			connection = connectionAccess.obtainConnection();
		}
		catch (UnsupportedOperationException e) {
			// assume this comes from org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl
			log.debugf( "Unable to obtain JDBC connection; unable to drop persistent ID table : %s", temporaryTable.getTableExpression() );
			return;
		}
		catch (SQLException e) {
			log.error( "Unable obtain JDBC Connection", e );
			return;
		}

		try {
			temporaryTableDropWork.execute( connection );
		}
		finally {
			try {
				connectionAccess.releaseConnection( connection );
			}
			catch (SQLException ignore) {
			}
		}
	}

	public TemporaryTable getTemporaryTable() {
		return temporaryTable;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}
}
