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
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;

import org.jboss.logging.Logger;

/**
 * Strategy based on ANSI SQL's definition of a "local temporary table" (local to each db session).
 *
 * @author Steve Ebersole
 */
public class LocalTemporaryTableStrategy {
	private static final Logger log = Logger.getLogger( LocalTemporaryTableStrategy.class );

	public static final String SHORT_NAME = "local_temporary";
	public static final String DROP_ID_TABLES = "hibernate.hql.bulk_id_strategy.local_temporary.drop_tables";

	private final TemporaryTable temporaryTable;
	private final SessionFactoryImplementor sessionFactory;

	private boolean dropIdTables;

	public LocalTemporaryTableStrategy(
			TemporaryTable temporaryTable,
			SessionFactoryImplementor sessionFactory) {
		this.temporaryTable = temporaryTable;
		this.sessionFactory = sessionFactory;
	}

	public void prepare(
			MappingModelCreationProcess mappingModelCreationProcess,
			JdbcConnectionAccess connectionAccess) {
		final ConfigurationService configService = mappingModelCreationProcess.getCreationContext()
				.getBootstrapContext()
				.getServiceRegistry().getService( ConfigurationService.class );
		this.dropIdTables = configService.getSetting(
				DROP_ID_TABLES,
				StandardConverters.BOOLEAN,
				false
		);
	}

	public void release(
			SessionFactoryImplementor sessionFactory,
			JdbcConnectionAccess connectionAccess) {
		if ( !dropIdTables ) {
			return;
		}

		dropIdTables = false;

		final TemporaryTable temporaryTable = getTemporaryTable();
		log.debugf( "Dropping local-temp ID table : %s", temporaryTable.getTableExpression() );

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
			log.debugf( "Unable to obtain JDBC connection; unable to drop local-temp ID table : %s", temporaryTable.getTableExpression() );
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
