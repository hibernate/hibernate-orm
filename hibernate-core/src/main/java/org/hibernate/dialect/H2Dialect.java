/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.dialect;

import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.JDBCException;
import org.hibernate.PessimisticLockException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.function.AvgWithArgumentCastFunction;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.StandardBasicTypes;
import org.jboss.logging.Logger;

/**
 * A dialect compatible with the H2 database.
 *
 * @author Thomas Mueller
 */
public class H2Dialect extends Dialect {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			H2Dialect.class.getName()
	);

	private final String querySequenceString;

	/**
	 * Constructs a H2Dialect
	 */
	public H2Dialect() {
		super();

		String querySequenceString = "select sequence_name from information_schema.sequences";
		try {
			// HHH-2300
			final Class h2ConstantsClass = ReflectHelper.classForName( "org.h2.engine.Constants" );
			final int majorVersion = (Integer) h2ConstantsClass.getDeclaredField( "VERSION_MAJOR" ).get( null );
			final int minorVersion = (Integer) h2ConstantsClass.getDeclaredField( "VERSION_MINOR" ).get( null );
			final int buildId = (Integer) h2ConstantsClass.getDeclaredField( "BUILD_ID" ).get( null );
			if ( buildId < 32 ) {
				querySequenceString = "select name from information_schema.sequences";
			}
			if ( ! ( majorVersion > 1 || minorVersion > 2 || buildId >= 139 ) ) {
				LOG.unsupportedMultiTableBulkHqlJpaql( majorVersion, minorVersion, buildId );
			}
		}
		catch ( Exception e ) {
			// probably H2 not in the classpath, though in certain app server environments it might just mean we are
			// not using the correct classloader
			LOG.undeterminedH2Version();
		}

		this.querySequenceString = querySequenceString;

		registerColumnType( Types.BOOLEAN, "boolean" );
		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.BINARY, "binary" );
		registerColumnType( Types.BIT, "boolean" );
		registerColumnType( Types.CHAR, "char($l)" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.DECIMAL, "decimal($p,$s)" );
		registerColumnType( Types.NUMERIC, "decimal($p,$s)" );
		registerColumnType( Types.DOUBLE, "double" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.LONGVARBINARY, "longvarbinary" );
		registerColumnType( Types.LONGVARCHAR, "longvarchar" );
		registerColumnType( Types.REAL, "real" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "tinyint" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.VARBINARY, "binary($l)" );
		registerColumnType( Types.BLOB, "blob" );
		registerColumnType( Types.CLOB, "clob" );

		// Aggregations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		registerFunction( "avg", new AvgWithArgumentCastFunction( "double" ) );

		// select topic, syntax from information_schema.help
		// where section like 'Function%' order by section, topic
		//
		// see also ->  http://www.h2database.com/html/functions.html

		// Numeric Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		registerFunction( "acos", new StandardSQLFunction( "acos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "asin", new StandardSQLFunction( "asin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "atan", new StandardSQLFunction( "atan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "atan2", new StandardSQLFunction( "atan2", StandardBasicTypes.DOUBLE ) );
		registerFunction( "bitand", new StandardSQLFunction( "bitand", StandardBasicTypes.INTEGER ) );
		registerFunction( "bitor", new StandardSQLFunction( "bitor", StandardBasicTypes.INTEGER ) );
		registerFunction( "bitxor", new StandardSQLFunction( "bitxor", StandardBasicTypes.INTEGER ) );
		registerFunction( "ceiling", new StandardSQLFunction( "ceiling", StandardBasicTypes.DOUBLE ) );
		registerFunction( "cos", new StandardSQLFunction( "cos", StandardBasicTypes.DOUBLE ) );
		registerFunction( "compress", new StandardSQLFunction( "compress", StandardBasicTypes.BINARY ) );
		registerFunction( "cot", new StandardSQLFunction( "cot", StandardBasicTypes.DOUBLE ) );
		registerFunction( "decrypt", new StandardSQLFunction( "decrypt", StandardBasicTypes.BINARY ) );
		registerFunction( "degrees", new StandardSQLFunction( "degrees", StandardBasicTypes.DOUBLE ) );
		registerFunction( "encrypt", new StandardSQLFunction( "encrypt", StandardBasicTypes.BINARY ) );
		registerFunction( "exp", new StandardSQLFunction( "exp", StandardBasicTypes.DOUBLE ) );
		registerFunction( "expand", new StandardSQLFunction( "compress", StandardBasicTypes.BINARY ) );
		registerFunction( "floor", new StandardSQLFunction( "floor", StandardBasicTypes.DOUBLE ) );
		registerFunction( "hash", new StandardSQLFunction( "hash", StandardBasicTypes.BINARY ) );
		registerFunction( "log", new StandardSQLFunction( "log", StandardBasicTypes.DOUBLE ) );
		registerFunction( "log10", new StandardSQLFunction( "log10", StandardBasicTypes.DOUBLE ) );
		registerFunction( "pi", new NoArgSQLFunction( "pi", StandardBasicTypes.DOUBLE ) );
		registerFunction( "power", new StandardSQLFunction( "power", StandardBasicTypes.DOUBLE ) );
		registerFunction( "radians", new StandardSQLFunction( "radians", StandardBasicTypes.DOUBLE ) );
		registerFunction( "rand", new NoArgSQLFunction( "rand", StandardBasicTypes.DOUBLE ) );
		registerFunction( "round", new StandardSQLFunction( "round", StandardBasicTypes.DOUBLE ) );
		registerFunction( "roundmagic", new StandardSQLFunction( "roundmagic", StandardBasicTypes.DOUBLE ) );
		registerFunction( "sign", new StandardSQLFunction( "sign", StandardBasicTypes.INTEGER ) );
		registerFunction( "sin", new StandardSQLFunction( "sin", StandardBasicTypes.DOUBLE ) );
		registerFunction( "tan", new StandardSQLFunction( "tan", StandardBasicTypes.DOUBLE ) );
		registerFunction( "truncate", new StandardSQLFunction( "truncate", StandardBasicTypes.DOUBLE ) );

		// String Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		registerFunction( "ascii", new StandardSQLFunction( "ascii", StandardBasicTypes.INTEGER ) );
		registerFunction( "char", new StandardSQLFunction( "char", StandardBasicTypes.CHARACTER ) );
		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "(", "||", ")" ) );
		registerFunction( "difference", new StandardSQLFunction( "difference", StandardBasicTypes.INTEGER ) );
		registerFunction( "hextoraw", new StandardSQLFunction( "hextoraw", StandardBasicTypes.STRING ) );
		registerFunction( "insert", new StandardSQLFunction( "lower", StandardBasicTypes.STRING ) );
		registerFunction( "left", new StandardSQLFunction( "left", StandardBasicTypes.STRING ) );
		registerFunction( "lcase", new StandardSQLFunction( "lcase", StandardBasicTypes.STRING ) );
		registerFunction( "ltrim", new StandardSQLFunction( "ltrim", StandardBasicTypes.STRING ) );
		registerFunction( "octet_length", new StandardSQLFunction( "octet_length", StandardBasicTypes.INTEGER ) );
		registerFunction( "position", new StandardSQLFunction( "position", StandardBasicTypes.INTEGER ) );
		registerFunction( "rawtohex", new StandardSQLFunction( "rawtohex", StandardBasicTypes.STRING ) );
		registerFunction( "repeat", new StandardSQLFunction( "repeat", StandardBasicTypes.STRING ) );
		registerFunction( "replace", new StandardSQLFunction( "replace", StandardBasicTypes.STRING ) );
		registerFunction( "right", new StandardSQLFunction( "right", StandardBasicTypes.STRING ) );
		registerFunction( "rtrim", new StandardSQLFunction( "rtrim", StandardBasicTypes.STRING ) );
		registerFunction( "soundex", new StandardSQLFunction( "soundex", StandardBasicTypes.STRING ) );
		registerFunction( "space", new StandardSQLFunction( "space", StandardBasicTypes.STRING ) );
		registerFunction( "stringencode", new StandardSQLFunction( "stringencode", StandardBasicTypes.STRING ) );
		registerFunction( "stringdecode", new StandardSQLFunction( "stringdecode", StandardBasicTypes.STRING ) );
		registerFunction( "stringtoutf8", new StandardSQLFunction( "stringtoutf8", StandardBasicTypes.BINARY ) );
		registerFunction( "ucase", new StandardSQLFunction( "ucase", StandardBasicTypes.STRING ) );
		registerFunction( "utf8tostring", new StandardSQLFunction( "utf8tostring", StandardBasicTypes.STRING ) );

		// Time and Date Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		registerFunction( "curdate", new NoArgSQLFunction( "curdate", StandardBasicTypes.DATE ) );
		registerFunction( "curtime", new NoArgSQLFunction( "curtime", StandardBasicTypes.TIME ) );
		registerFunction( "curtimestamp", new NoArgSQLFunction( "curtimestamp", StandardBasicTypes.TIME ) );
		registerFunction( "current_date", new NoArgSQLFunction( "current_date", StandardBasicTypes.DATE ) );
		registerFunction( "current_time", new NoArgSQLFunction( "current_time", StandardBasicTypes.TIME ) );
		registerFunction( "current_timestamp", new NoArgSQLFunction( "current_timestamp", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "datediff", new StandardSQLFunction( "datediff", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayname", new StandardSQLFunction( "dayname", StandardBasicTypes.STRING ) );
		registerFunction( "dayofmonth", new StandardSQLFunction( "dayofmonth", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayofweek", new StandardSQLFunction( "dayofweek", StandardBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new StandardSQLFunction( "dayofyear", StandardBasicTypes.INTEGER ) );
		registerFunction( "monthname", new StandardSQLFunction( "monthname", StandardBasicTypes.STRING ) );
		registerFunction( "now", new NoArgSQLFunction( "now", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "quarter", new StandardSQLFunction( "quarter", StandardBasicTypes.INTEGER ) );
		registerFunction( "week", new StandardSQLFunction( "week", StandardBasicTypes.INTEGER ) );

		// System Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		registerFunction( "database", new NoArgSQLFunction( "database", StandardBasicTypes.STRING ) );
		registerFunction( "user", new NoArgSQLFunction( "user", StandardBasicTypes.STRING ) );

		getDefaultProperties().setProperty( AvailableSettings.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
		// http://code.google.com/p/h2database/issues/detail?id=235
		getDefaultProperties().setProperty( AvailableSettings.NON_CONTEXTUAL_LOB_CREATION, "true" );
	}

	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getIdentityColumnString() {
		// not null is implicit
		return "generated by default as identity";
	}

	@Override
	public String getIdentitySelectString() {
		return "call identity()";
	}

	@Override
	public String getIdentityInsertString() {
		return "null";
	}

	@Override
	public String getForUpdateString() {
		return " for update";
	}

	@Override
    public LimitHandler buildLimitHandler(String sql, RowSelection selection) {
        return new AbstractLimitHandler(sql, selection) {
        	@Override
        	public String getProcessedSql() {
        		boolean hasOffset = LimitHelper.hasFirstRow(selection);
        		return sql + (hasOffset ? " limit ? offset ?" : " limit ?");
        	}

        	@Override
        	public boolean supportsLimit() {
        		return true;
        	}

        	@Override
        	public boolean bindLimitParametersInReverseOrder() {
        		return true;
        	}
        };
    }

	@Override
	public boolean supportsIfExistsAfterTableName() {
		return true;
	}

	@Override
	public boolean supportsIfExistsAfterConstraintName() {
		return true;
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public boolean supportsPooledSequences() {
		return true;
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "next value for " + sequenceName;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "call next value for " + sequenceName;
	}

	@Override
	public String getQuerySequencesString() {
		return querySequenceString;
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	private static final ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {
		/**
		 * Extract the name of the violated constraint from the given SQLException.
		 *
		 * @param sqle The exception that was the result of the constraint violation.
		 * @return The extracted constraint name.
		 */
		public String extractConstraintName(SQLException sqle) {
			String constraintName = null;
			// 23000: Check constraint violation: {0}
			// 23001: Unique index or primary key violation: {0}
			if ( sqle.getSQLState().startsWith( "23" ) ) {
				final String message = sqle.getMessage();
				final int idx = message.indexOf( "violation: " );
				if ( idx > 0 ) {
					constraintName = message.substring( idx + "violation: ".length() );
				}
			}
			return constraintName;
		}
	};

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		SQLExceptionConversionDelegate delegate = super.buildSQLExceptionConversionDelegate();
		if (delegate == null) {
			delegate = new SQLExceptionConversionDelegate() {
				@Override
				public JDBCException convert(SQLException sqlException, String message, String sql) {
					final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );

					if (40001 == errorCode) {
						// DEADLOCK DETECTED
						return new LockAcquisitionException(message, sqlException, sql);
					}

					if (50200 == errorCode) {
						// LOCK NOT AVAILABLE
						return new PessimisticLockException(message, sqlException, sql);
					}

					if ( 90006 == errorCode ) {
						// NULL not allowed for column [90006-145]
						final String constraintName = getViolatedConstraintNameExtracter().extractConstraintName( sqlException );
						return new ConstraintViolationException( message, sqlException, sql, constraintName );
					}

					return null;
				}
			};
		}
		return delegate;
	}

	@Override
	public boolean supportsTemporaryTables() {
		return true;
	}

	@Override
	public String getCreateTemporaryTableString() {
		return "create cached local temporary table if not exists";
	}

	@Override
	public String getCreateTemporaryTablePostfix() {
		// actually 2 different options are specified here:
		//		1) [on commit drop] - says to drop the table on transaction commit
		//		2) [transactional] - says to not perform an implicit commit of any current transaction
		return "on commit drop transactional";
	}

	@Override
	public Boolean performTemporaryTableDDLInIsolation() {
		// explicitly create the table using the same connection and transaction
		return Boolean.FALSE;
	}

	@Override
	public boolean dropTemporaryTableAfterUse() {
		return false;
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "call current_timestamp()";
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsLobValueChangePropogation() {
		return false;
	}

	@Override
	public boolean requiresParensForTupleDistinctCounts() {
		return true;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		// see http://groups.google.com/group/h2-database/browse_thread/thread/562d8a49e2dabe99?hl=en
		return true;
	}
	
	@Override
	public boolean supportsTuplesInSubqueries() {
		return false;
	}

	@Override
	public String getCurrentSchemaCommand() {
		return "call schema()";
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}
}
