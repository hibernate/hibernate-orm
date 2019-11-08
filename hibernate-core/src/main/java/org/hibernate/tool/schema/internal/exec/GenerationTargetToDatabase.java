/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.exec;

import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import org.hibernate.engine.jdbc.internal.DDLFormatterImpl;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;

/**
 * GenerationTarget implementation for handling generation directly to the database
 *
 * @author Steve Ebersole
 */
public class GenerationTargetToDatabase implements GenerationTarget {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( GenerationTargetToDatabase.class );

	private final DdlTransactionIsolator ddlTransactionIsolator;
	private final boolean releaseAfterUse;

	private Statement jdbcStatement;

	public GenerationTargetToDatabase(DdlTransactionIsolator ddlTransactionIsolator) {
		this( ddlTransactionIsolator, true );
	}

	public GenerationTargetToDatabase(DdlTransactionIsolator ddlTransactionIsolator, boolean releaseAfterUse) {
		this.ddlTransactionIsolator = ddlTransactionIsolator;
		this.releaseAfterUse = releaseAfterUse;
	}

	@Override
	public void prepare() {
	}

	@Override
	public void accept(String command) {
		ddlTransactionIsolator.getJdbcContext().getSqlStatementLogger().logStatement(
				command,
				DDLFormatterImpl.INSTANCE
		);

		try {
			final Statement jdbcStatement = jdbcStatement();
			jdbcStatement.execute( command );

			try {
				SQLWarning warnings = jdbcStatement.getWarnings();
				if ( warnings != null) {
					ddlTransactionIsolator.getJdbcContext().getSqlExceptionHelper().logAndClearWarnings( jdbcStatement );
				}
			}
			catch( SQLException e ) {
				log.unableToLogSqlWarnings( e );
			}
		}
		catch (SQLException e) {
			throw new CommandAcceptanceException(
					"Error executing DDL \"" + command + "\" via JDBC Statement",
					e
			);
		}
	}

	private Statement jdbcStatement() {
		if ( jdbcStatement == null ) {
			try {
				this.jdbcStatement = ddlTransactionIsolator.getIsolatedConnection().createStatement();
			}
			catch (SQLException e) {
				throw ddlTransactionIsolator.getJdbcContext().getSqlExceptionHelper().convert( e, "Unable to create JDBC Statement for DDL execution" );
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
				throw ddlTransactionIsolator.getJdbcContext().getSqlExceptionHelper().convert( e, "Unable to close JDBC Statement after DDL execution" );
			}
		}
		if ( releaseAfterUse ) {
			ddlTransactionIsolator.release();
		}
	}
}
