/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.exec;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import org.hibernate.engine.jdbc.internal.DDLFormatterImpl;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;

/**
 * A {@link GenerationTarget} which exports DDL directly to the database.
 *
 * @author Steve Ebersole
 */
public class GenerationTargetToDatabase implements GenerationTarget {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( GenerationTargetToDatabase.class );

	private final DdlTransactionIsolator ddlTransactionIsolator;
	private final boolean releaseAfterUse;

	private Statement jdbcStatement;
	private final boolean autocommit;

	public GenerationTargetToDatabase(DdlTransactionIsolator ddlTransactionIsolator) {
		this( ddlTransactionIsolator, true );
	}

	public GenerationTargetToDatabase(DdlTransactionIsolator ddlTransactionIsolator, boolean releaseAfterUse) {
		this( ddlTransactionIsolator, releaseAfterUse, true );
	}

	public GenerationTargetToDatabase(DdlTransactionIsolator ddlTransactionIsolator, boolean releaseAfterUse, boolean autocommit) {
		this.ddlTransactionIsolator = ddlTransactionIsolator;
		this.releaseAfterUse = releaseAfterUse;
		this.autocommit = autocommit;
	}

	private SqlStatementLogger getSqlStatementLogger() {
		return ddlTransactionIsolator.getJdbcContext().getSqlStatementLogger();
	}

	private SqlExceptionHelper getSqlExceptionHelper() {
		return ddlTransactionIsolator.getJdbcContext().getSqlExceptionHelper();
	}

	private Connection getIsolatedConnection() {
		return ddlTransactionIsolator.getIsolatedConnection( autocommit );
	}

	@Override
	public void prepare() {
	}

	@Override
	public void accept(String command) {
		getSqlStatementLogger().logStatement( command, DDLFormatterImpl.INSTANCE );

		try {
			final Statement jdbcStatement = jdbcStatement();
			jdbcStatement.execute( command );

			try {
				SQLWarning warnings = jdbcStatement.getWarnings();
				if ( warnings != null) {
					getSqlExceptionHelper().logAndClearWarnings( jdbcStatement );
				}
			}
			catch( SQLException e ) {
				log.unableToLogSqlWarnings( e );
			}
		}
		catch (SQLException e) {
			throw new CommandAcceptanceException(
					"Error executing DDL \"" + command + "\" via JDBC [" + e.getMessage() + "]",
					e
			);
		}
	}

	private Statement jdbcStatement() {
		if ( jdbcStatement == null ) {
			try {
				jdbcStatement = getIsolatedConnection().createStatement();
			}
			catch (SQLException e) {
				throw getSqlExceptionHelper().convert( e, "Unable to create JDBC Statement for DDL execution" );
			}
		}

		return jdbcStatement;
	}

	@Override
	public void release() {
		if ( jdbcStatement != null ) {
			try {
				jdbcStatement.close();
				jdbcStatement = null;
			}
			catch (SQLException e) {
				throw getSqlExceptionHelper().convert( e, "Unable to close JDBC Statement after DDL execution" );
			}
		}
		if ( releaseAfterUse ) {
			ddlTransactionIsolator.release();
		}
	}
}
