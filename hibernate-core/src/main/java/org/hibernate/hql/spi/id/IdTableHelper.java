/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class IdTableHelper {
	private static final Logger log = Logger.getLogger( IdTableHelper.class );

	/**
	 * Singleton access
	 */
	public static final IdTableHelper INSTANCE = new IdTableHelper();

	private IdTableHelper() {
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

	public void executeIdTableCreationStatements(
			List<String> creationStatements,
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess) {
		if ( creationStatements == null || creationStatements.isEmpty() ) {
			return;
		}
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
				for ( String creationStatement : creationStatements ) {
					try {
						jdbcServices.getSqlStatementLogger().logStatement( creationStatement );
						// TODO: ResultSetExtractor
						statement.execute( creationStatement );
					}
					catch (SQLException e) {
						log.debugf( "Error attempting to export id-table [%s] : %s", creationStatement, e.getMessage() );
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

	public void executeIdTableDropStatements(
			String[] dropStatements,
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess) {
		if ( dropStatements == null || dropStatements.length == 0 ) {
			return;
		}

		try {
			Connection connection = connectionAccess.obtainConnection();

			try {
				// TODO: session.getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().createStatement();
				try ( java.sql.Statement statement = connection.createStatement() ) {

					for ( String dropStatement : dropStatements ) {
						try {
							jdbcServices.getSqlStatementLogger().logStatement( dropStatement );
							statement.execute( dropStatement );
						}
						catch (java.sql.SQLException e) {
							log.debugf( "Error attempting to cleanup id-table : [%s]", e.getMessage() );
						}
					}

					// TODO
					// session.getTransactionCoordinator().getJdbcCoordinator().release( statement );
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

	public void executeIdTableCreationStatements(
			List<String> creationStatements,
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess,
			ServiceRegistry serviceRegistry) {
		if ( creationStatements == null || creationStatements.isEmpty() ) {
			return;
		}
		final GenerationTargetToDatabase target = new GenerationTargetToDatabase(
				getDdlTransactionIsolator(
						jdbcServices,
						connectionAccess,
						serviceRegistry
				)
		);
		try {
			for ( String createStatement : creationStatements ) {
				try {
					target.accept( createStatement );
				}
				catch ( CommandAcceptanceException e) {
					log.debugf( "Error attempting to export id-table [%s] : %s", createStatement, e.getMessage() );
				}
			}
		}
		finally {
			target.release();
		}
	}

	public void executeIdTableDropStatements(
			String[] dropStatements,
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess,
			ServiceRegistry serviceRegistry) {
		if ( dropStatements == null || dropStatements.length == 0 ) {
			return;
		}

		final GenerationTargetToDatabase target = new GenerationTargetToDatabase(
				getDdlTransactionIsolator(
						jdbcServices,
						connectionAccess,
						serviceRegistry
				)
		);
		try {
			for ( String dropStatement : dropStatements ) {
				try {
					target.accept( dropStatement );
				}
				catch ( CommandAcceptanceException e) {
					log.debugf( "Error attempting to drop id-table : [%s]", e.getMessage() );
				}
			}
		}
		finally {
			target.release();
		}
	}

	public DdlTransactionIsolator getDdlTransactionIsolator(
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess,
			ServiceRegistry serviceRegistry) {
		return serviceRegistry.getService( TransactionCoordinatorBuilder.class ).buildDdlTransactionIsolator(
			new JdbcContext() {

				@Override
				public JdbcConnectionAccess getJdbcConnectionAccess() {
					return connectionAccess;
				}

				@Override
				public Dialect getDialect() {
					return jdbcServices.getJdbcEnvironment().getDialect();
				}

				@Override
				public SqlStatementLogger getSqlStatementLogger() {
					return jdbcServices.getSqlStatementLogger();
				}

				@Override
				public SqlExceptionHelper getSqlExceptionHelper() {
					return jdbcServices.getSqlExceptionHelper();
				}

				@Override
				public ServiceRegistry getServiceRegistry() {
					return serviceRegistry;
				}
			}
		);
	}

}
