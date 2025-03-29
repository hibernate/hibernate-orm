/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
 * Strategy based on ANSI SQL's definition of a "global temporary table".
 *
 * @author Steve Ebersole
 */
public class GlobalTemporaryTableStrategy {
	private static final Logger log = Logger.getLogger( GlobalTemporaryTableStrategy.class );

	public static final String SHORT_NAME = "global_temporary";

	public static final String CREATE_ID_TABLES = "hibernate.query.mutation_strategy.global_temporary.create_tables";
	public static final String DROP_ID_TABLES = "hibernate.query.mutation_strategy.global_temporary.drop_tables";

	private final TemporaryTable temporaryTable;
	private final SessionFactoryImplementor sessionFactory;

	private boolean prepared;
	private boolean dropIdTables;

	public GlobalTemporaryTableStrategy(TemporaryTable temporaryTable, SessionFactoryImplementor sessionFactory) {
		this.temporaryTable = temporaryTable;
		this.sessionFactory = sessionFactory;

		if ( sessionFactory.getJdbcServices().getDialect().getTemporaryTableAfterUseAction() == AfterUseAction.DROP ) {
			throw new IllegalArgumentException( "Global-temp ID tables cannot use AfterUseAction.DROP : "
												+ temporaryTable.getTableExpression() );
		}
	}

	public EntityMappingType getEntityDescriptor() {
		return temporaryTable.getEntityDescriptor();
	}

	public void prepare(MappingModelCreationProcess mappingModelCreationProcess, JdbcConnectionAccess connectionAccess) {
		if ( prepared ) {
			return;
		}

		prepared = true;

		final ConfigurationService configService =
				mappingModelCreationProcess.getCreationContext()
						.getBootstrapContext().getServiceRegistry()
						.requireService( ConfigurationService.class );

		if ( configService.getSetting( CREATE_ID_TABLES, StandardConverters.BOOLEAN, true ) ) {
			log.debugf( "Creating global-temp ID table: %s", getTemporaryTable().getTableExpression() );

			final TemporaryTableHelper.TemporaryTableCreationWork temporaryTableCreationWork =
					new TemporaryTableHelper.TemporaryTableCreationWork( getTemporaryTable(), sessionFactory );
			final Connection connection;
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
				dropIdTables = configService.getSetting( DROP_ID_TABLES, StandardConverters.BOOLEAN, false );
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

	public void release(SessionFactoryImplementor sessionFactory, JdbcConnectionAccess connectionAccess) {
		if ( !dropIdTables ) {
			return;
		}

		dropIdTables = false;

		log.debugf( "Dropping global-temp ID table : %s", getTemporaryTable().getTableExpression() );

		final TemporaryTableHelper.TemporaryTableDropWork temporaryTableDropWork =
				new TemporaryTableHelper.TemporaryTableDropWork( getTemporaryTable(), sessionFactory );
		Connection connection;
		try {
			connection = connectionAccess.obtainConnection();
		}
		catch (UnsupportedOperationException e) {
			// assume this comes from org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl
			log.debugf(
					"Unable to obtain JDBC connection; unable to drop global-temp ID table : %s",
					getTemporaryTable().getTableExpression()
			);
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
