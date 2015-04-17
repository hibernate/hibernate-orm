/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.hql.spi;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.internal.AbstractSessionImpl;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.SelectValues;
import org.hibernate.type.UUIDCharType;

import org.jboss.logging.Logger;

/**
 * This is a strategy that mimics temporary tables for databases which do not support
 * temporary tables.  It follows a pattern similar to the ANSI SQL definition of global
 * temporary table using a "session id" column to segment rows from the various sessions.
 *
 * @author Steve Ebersole
 */
public class PersistentTableBulkIdStrategy implements MultiTableBulkIdStrategy {
	private static final CoreMessageLogger log = Logger.getMessageLogger(
			CoreMessageLogger.class,
			PersistentTableBulkIdStrategy.class.getName()
	);

	public static final String SHORT_NAME = "persistent";

	public static final String CLEAN_UP_ID_TABLES = "hibernate.hql.bulk_id_strategy.persistent.clean_up";
	public static final String SCHEMA = "hibernate.hql.bulk_id_strategy.persistent.schema";
	public static final String CATALOG = "hibernate.hql.bulk_id_strategy.persistent.catalog";

	private String catalog;
	private String schema;
	private boolean cleanUpTables;
	private List<String> tableCleanUpDdl;

	/**
	 * Creates the tables for all the entities that might need it
	 *
	 * @param jdbcServices The JdbcService object
	 * @param connectionAccess Access to the JDBC Connection
	 * @param metadata Access to the O/RM mapping information
	 */
	@Override
	public void prepare(
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess,
			MetadataImplementor metadata) {
		final ConfigurationService configService = metadata.getMetadataBuildingOptions()
				.getServiceRegistry()
				.getService( ConfigurationService.class );
		this.catalog = configService.getSetting(
				CATALOG,
				StandardConverters.STRING,
				configService.getSetting( AvailableSettings.DEFAULT_CATALOG, StandardConverters.STRING )
		);
		this.schema = configService.getSetting(
				SCHEMA,
				StandardConverters.STRING,
				configService.getSetting( AvailableSettings.DEFAULT_SCHEMA, StandardConverters.STRING )
		);
		this.cleanUpTables = configService.getSetting(
				CLEAN_UP_ID_TABLES,
				StandardConverters.BOOLEAN,
				false
		);

		final List<Table> idTableDefinitions = new ArrayList<Table>();

		for ( PersistentClass entityBinding : metadata.getEntityBindings() ) {
			if ( !MultiTableBulkIdHelper.INSTANCE.needsIdTable( entityBinding ) ) {
				continue;
			}
			final Table idTableDefinition = generateIdTableDefinition( entityBinding, metadata );
			idTableDefinitions.add( idTableDefinition );

			if ( cleanUpTables ) {
				if ( tableCleanUpDdl == null ) {
					tableCleanUpDdl = new ArrayList<String>();
				}
				tableCleanUpDdl.add( idTableDefinition.sqlDropString( jdbcServices.getDialect(), null, null  ) );
			}
		}

		// we export them all at once to better reuse JDBC resources
		exportTableDefinitions( idTableDefinitions, jdbcServices, connectionAccess, metadata );
	}

	protected Table generateIdTableDefinition(PersistentClass entityMapping, MetadataImplementor metadata) {
		return MultiTableBulkIdHelper.INSTANCE.generateIdTableDefinition(
				entityMapping,
				catalog,
				schema,
				true
		);
	}

	protected void exportTableDefinitions(
			List<Table> idTableDefinitions,
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess,
			MetadataImplementor metadata) {
		MultiTableBulkIdHelper.INSTANCE.exportTableDefinitions(
				idTableDefinitions,
				jdbcServices,
				connectionAccess,
				metadata
		);
	}

	@Override
	public void release(JdbcServices jdbcServices, JdbcConnectionAccess connectionAccess) {
		if ( ! cleanUpTables ) {
			return;
		}

		MultiTableBulkIdHelper.INSTANCE.cleanupTableDefinitions( jdbcServices, connectionAccess, tableCleanUpDdl );
	}

	@Override
	public UpdateHandler buildUpdateHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
		return new TableBasedUpdateHandlerImpl( factory, walker, catalog, schema ) {
			@Override
			protected void addAnyExtraIdSelectValues(SelectValues selectClause) {
				selectClause.addParameter( Types.CHAR, 36 );
			}

			@Override
			protected String generateIdSubselect(Queryable persister) {
				return super.generateIdSubselect( persister ) + " where hib_sess_id=?";
			}

			@Override
			protected int handlePrependedParametersOnIdSelection(PreparedStatement ps, SessionImplementor session, int pos) throws SQLException {
				bindSessionIdentifier( ps, session, pos );
				return 1;
			}

			@Override
			protected void handleAddedParametersOnUpdate(PreparedStatement ps, SessionImplementor session, int position) throws SQLException {
				bindSessionIdentifier( ps, session, position );
			}

			@Override
			protected void releaseFromUse(Queryable persister, SessionImplementor session) {
				// clean up our id-table rows
				cleanUpRows( determineIdTableName( persister ), session );
			}
		};
	}

	private void bindSessionIdentifier(PreparedStatement ps, SessionImplementor session, int position) throws SQLException {
		if ( ! AbstractSessionImpl.class.isInstance( session ) ) {
			throw new HibernateException( "Only available on SessionImpl instances" );
		}
		UUIDCharType.INSTANCE.set( ps, ( (AbstractSessionImpl) session ).getSessionIdentifier(), position, session );
	}

	private void cleanUpRows(String tableName, SessionImplementor session) {
		final String sql = "delete from " + tableName + " where hib_sess_id=?";
		try {
			PreparedStatement ps = null;
			try {
				ps = session.getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().prepareStatement( sql, false );
				bindSessionIdentifier( ps, session, 1 );
				session.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().executeUpdate( ps );
			}
			finally {
				if ( ps != null ) {
					try {
						session.getTransactionCoordinator().getJdbcCoordinator().release( ps );
					}
					catch( Throwable ignore ) {
						// ignore
					}
				}
			}
		}
		catch (SQLException e) {
			throw convert( session.getFactory(), e, "Unable to clean up id table [" + tableName + "]", sql );
		}
	}

	protected JDBCException convert(SessionFactoryImplementor factory, SQLException e, String message, String sql) {
		throw factory.getSQLExceptionHelper().convert( e, message, sql );
	}

	@Override
	public DeleteHandler buildDeleteHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
		return new TableBasedDeleteHandlerImpl( factory, walker, catalog, schema ) {
			@Override
			protected void addAnyExtraIdSelectValues(SelectValues selectClause) {
				selectClause.addParameter( Types.CHAR, 36 );
			}

			@Override
			protected String generateIdSubselect(Queryable persister) {
				return super.generateIdSubselect( persister ) + " where hib_sess_id=?";
			}

			@Override
			protected int handlePrependedParametersOnIdSelection(PreparedStatement ps, SessionImplementor session, int pos) throws SQLException {
				bindSessionIdentifier( ps, session, pos );
				return 1;
			}

			@Override
			protected void handleAddedParametersOnDelete(PreparedStatement ps, SessionImplementor session) throws SQLException {
				bindSessionIdentifier( ps, session, 1 );
			}

			@Override
			protected void releaseFromUse(Queryable persister, SessionImplementor session) {
				// clean up our id-table rows
				cleanUpRows( determineIdTableName( persister ), session );
			}
		};
	}
}
