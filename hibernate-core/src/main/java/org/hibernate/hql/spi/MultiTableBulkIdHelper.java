/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UnionSubclass;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class MultiTableBulkIdHelper {
	private static final Logger log = Logger.getLogger( MultiTableBulkIdHelper.class );

	/**
	 * Singleton access
	 */
	public static final MultiTableBulkIdHelper INSTANCE = new MultiTableBulkIdHelper();

	private MultiTableBulkIdHelper() {
	}

	public boolean needsIdTable(PersistentClass entityBinding) {
		// need id table if the entity has secondary tables (joins)
		if ( entityBinding.getJoinClosureSpan() > 0 ) {
			return true;
		}

		// need an id table if the entity is part of either a JOINED or UNION inheritance
		// hierarchy.  We do not allow inheritance strategy mixing, so work on that assumption
		// here...
		final RootClass rootEntityBinding = entityBinding.getRootClass();
		final Iterator itr = rootEntityBinding.getSubclassIterator();
		if ( itr.hasNext() ) {
			final Subclass subclassEntityBinding = (Subclass) itr.next();
			if ( subclassEntityBinding instanceof JoinedSubclass || subclassEntityBinding instanceof UnionSubclass ) {
				return true;
			}
		}

		return false;
	}


	public Table generateIdTableDefinition(
			PersistentClass entityMapping,
			String catalog,
			String schema,
			boolean generateSessionIdColumn) {
		Table idTable = new Table( entityMapping.getTemporaryIdTableName() );
		idTable.setComment( "Used to hold id values for the " + entityMapping.getEntityName() + " class" );

		if ( catalog != null ) {
			idTable.setCatalog( catalog );
		}
		if ( schema != null ) {
			idTable.setSchema( schema );
		}

		Iterator itr = entityMapping.getTable().getPrimaryKey().getColumnIterator();
		while( itr.hasNext() ) {
			Column column = (Column) itr.next();
			idTable.addColumn( column.clone()  );
		}

		if ( generateSessionIdColumn ) {
			Column sessionIdColumn = new Column( "hib_sess_id" );
			sessionIdColumn.setSqlType( "CHAR(36)" );
			sessionIdColumn.setComment( "Used to hold the Hibernate Session identifier" );
			idTable.addColumn( sessionIdColumn );
		}

		return idTable;
	}

	protected void exportTableDefinitions(
			List<Table> idTableDefinitions,
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess,
			MetadataImplementor metadata) {
		try {
			Connection connection;
			try {
				connection = connectionAccess.obtainConnection();
			}
			catch (UnsupportedOperationException e) {
				// assume this comes from org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl
				log.debug( "Unable to obtain JDBC connection; assuming ID tables already exist or wont be needed" );
				return;
			}

			try {
				// TODO: session.getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().createStatement();
				Statement statement = connection.createStatement();
				for ( Table idTableDefinition : idTableDefinitions ) {
					try {
						final String sql = idTableDefinition.sqlCreateString( jdbcServices.getDialect(), metadata, null, null );
						jdbcServices.getSqlStatementLogger().logStatement( sql );
						// TODO: ResultSetExtractor
						statement.execute( sql );
					}
					catch (SQLException e) {
						log.debugf( "Error attempting to export id-table [%s] : %s", idTableDefinition.getName(), e.getMessage() );
					}
				}

				// TODO
//				session.getTransactionCoordinator().getJdbcCoordinator().release( statement );
				statement.close();
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

	public void cleanupTableDefinitions(
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess,
			List<String> tableCleanUpDdl) {
		if ( tableCleanUpDdl == null ) {
			return;
		}

		try {
			Connection connection = connectionAccess.obtainConnection();

			try {
				// TODO: session.getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().createStatement();
				Statement statement = connection.createStatement();

				for ( String cleanupDdl : tableCleanUpDdl ) {
					try {
						jdbcServices.getSqlStatementLogger().logStatement( cleanupDdl );
						statement.execute( cleanupDdl );
					}
					catch (SQLException e) {
						log.debugf( "Error attempting to cleanup id-table : [%s]", e.getMessage() );
					}
				}

				// TODO
//				session.getTransactionCoordinator().getJdbcCoordinator().release( statement );
				statement.close();
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
}
