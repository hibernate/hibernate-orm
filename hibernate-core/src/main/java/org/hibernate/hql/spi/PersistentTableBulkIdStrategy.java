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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Mappings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.internal.AbstractSessionImpl;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.type.UUIDCharType;

/**
 * @author Steve Ebersole
 */
public class PersistentTableBulkIdStrategy implements MultiTableBulkIdStrategy {
	private static final Logger log = Logger.getLogger( PersistentTableBulkIdStrategy.class );

	public static final String CLEAN_UP_ID_TABLES = "hibernate.hql.bulk_id_strategy.persistent.clean_up";

	private boolean cleanUpTables;
	private List<String> tableCleanUpDdl;

	@Override
	public void prepare(
			Dialect dialect,
			JdbcConnectionAccess connectionAccess,
			Mappings mappings,
			Mapping mapping,
			Map settings) {
		cleanUpTables = ConfigurationHelper.getBoolean( CLEAN_UP_ID_TABLES, settings, false );
		final Iterator<PersistentClass> entityMappings = mappings.iterateClasses();
		final List<Table> idTableDefinitions = new ArrayList<Table>();
		while ( entityMappings.hasNext() ) {
			final PersistentClass entityMapping = entityMappings.next();
			final Table idTableDefinition = generateIdTableDefinition( entityMapping );
			idTableDefinitions.add( idTableDefinition );
		}
		exportTableDefinitions( idTableDefinitions, dialect, connectionAccess, mappings, mapping );
	}

	protected Table generateIdTableDefinition(PersistentClass entityMapping) {
		Table idTable = new Table( entityMapping.getTemporaryIdTableName() );
		Iterator itr = entityMapping.getIdentityTable().getPrimaryKey().getColumnIterator();
		while( itr.hasNext() ) {
			Column column = (Column) itr.next();
			idTable.addColumn( column.clone()  );
		}
		Column sessionIdColumn = new Column( "hib_sess_id" );
		sessionIdColumn.setSqlType( "CHAR(36)" );
		sessionIdColumn.setComment( "Used to hold the Hibernate Session identifier" );
		idTable.addColumn( sessionIdColumn );

		idTable.setComment( "Used to hold id values for the " + entityMapping.getEntityName() + " class" );
		return idTable;
	}

	protected void exportTableDefinitions(
			List<Table> idTableDefinitions,
			Dialect dialect,
			JdbcConnectionAccess connectionAccess,
			Mappings mappings,
			Mapping mapping) {
		try {
			Connection connection = connectionAccess.obtainConnection();

			try {
				Statement statement = connection.createStatement();

				for ( Table idTableDefinition : idTableDefinitions ) {
					if ( cleanUpTables ) {
						if ( tableCleanUpDdl == null ) {
							tableCleanUpDdl = new ArrayList<String>();
						}
						tableCleanUpDdl.add( idTableDefinition.sqlDropString( dialect, null, null  ) );
					}
					try {
						statement.execute( idTableDefinition.sqlCreateString( dialect, mapping, null, null ) );
					}
					catch (SQLException e) {
						log.debugf( "Error attempting to export id-table [%s] : %s", idTableDefinition.getName(), e.getMessage() );
					}
				}
			}
			catch (SQLException e) {
				log.error( "Unable to use JDBC Connection to create Statement", e );
			}
			finally {
				try {
					connectionAccess.releaseConnection( connection );
				}
				catch (SQLException ignore) {
				}
			}
		}
		catch (SQLException e) {
			log.error( "Unable obtain JDBC Connection", e );
		}
	}

	@Override
	public void release(Dialect dialect, JdbcConnectionAccess connectionAccess) {
		if ( ! cleanUpTables ) {
			return;
		}

		try {
			Connection connection = connectionAccess.obtainConnection();

			try {
				Statement statement = connection.createStatement();

				for ( String cleanupDdl : tableCleanUpDdl ) {
					try {
						statement.execute( cleanupDdl );
					}
					catch (SQLException e) {
						log.debugf( "Error attempting to cleanup id-table : [%s]", e.getMessage() );
					}
				}
			}
			catch (SQLException e) {
				log.error( "Unable to use JDBC Connection to create Statement", e );
			}
			finally {
				try {
					connectionAccess.releaseConnection( connection );
				}
				catch (SQLException ignore) {
				}
			}
		}
		catch (SQLException e) {
			log.error( "Unable obtain JDBC Connection", e );
		}
	}

	@Override
	public UpdateHandler buildUpdateHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
		return new TableBasedUpdateHandlerImpl( factory, walker ) {
			@Override
			protected String extraIdSelectValues() {
				return "cast(? as char)";
			}

			@Override
			protected String generateIdSubselect(Queryable persister) {
				return super.generateIdSubselect( persister ) + " where hib_sess_id=?";
			}

			@Override
			protected int handlePrependedParametersOnIdSelection(PreparedStatement ps, SessionImplementor session, int pos) throws SQLException {
				if ( ! AbstractSessionImpl.class.isInstance( session ) ) {
					throw new HibernateException( "Only available on SessionImpl instances" );
				}
				UUIDCharType.INSTANCE.set( ps, ( (AbstractSessionImpl) session ).getSessionIdentifier(), pos, session );
				return 1;
			}

			@Override
			protected void handleAddedParametersOnUpdate(PreparedStatement ps, SessionImplementor session, int position) throws SQLException {
				if ( ! AbstractSessionImpl.class.isInstance( session ) ) {
					throw new HibernateException( "Only available on SessionImpl instances" );
				}
				UUIDCharType.INSTANCE.set( ps, ( (AbstractSessionImpl) session ).getSessionIdentifier(), position, session );
			}
		};
	}

	@Override
	public DeleteHandler buildDeleteHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
		return new TableBasedDeleteHandlerImpl( factory, walker ) {
			@Override
			protected String extraIdSelectValues() {
				return "cast(? as char)";
			}

			@Override
			protected String generateIdSubselect(Queryable persister) {
				return super.generateIdSubselect( persister ) + " where hib_sess_id=?";
			}

			@Override
			protected int handlePrependedParametersOnIdSelection(PreparedStatement ps, SessionImplementor session, int pos) throws SQLException {
				if ( ! AbstractSessionImpl.class.isInstance( session ) ) {
					throw new HibernateException( "Only available on SessionImpl instances" );
				}
				UUIDCharType.INSTANCE.set( ps, ( (AbstractSessionImpl) session ).getSessionIdentifier(), pos, session );
				return 1;
			}

			@Override
			protected void handleAddedParametersOnDelete(PreparedStatement ps, SessionImplementor session) throws SQLException {
				if ( ! AbstractSessionImpl.class.isInstance( session ) ) {
					throw new HibernateException( "Only available on SessionImpl instances" );
				}
				UUIDCharType.INSTANCE.set( ps, ( (AbstractSessionImpl) session ).getSessionIdentifier(), 1, session );
			}
		};
	}
}
