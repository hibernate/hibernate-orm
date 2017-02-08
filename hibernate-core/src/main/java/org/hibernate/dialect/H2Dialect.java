/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.JDBCException;
import org.hibernate.PessimisticLockException;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.function.AvgWithArgumentCastFunction;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.identity.H2IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.AfterUseAction;
import org.hibernate.hql.spi.id.local.LocalTemporaryTableBulkIdStrategy;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorH2DatabaseImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

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

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			final boolean hasOffset = LimitHelper.hasFirstRow( selection );
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

	private final String querySequenceString;
	private final SequenceInformationExtractor sequenceInformationExtractor;

	/**
	 * Constructs a H2Dialect
	 */
	public H2Dialect() {
		super();

		String querySequenceString = "select sequence_name from information_schema.sequences";
		SequenceInformationExtractor sequenceInformationExtractor = SequenceInformationExtractorH2DatabaseImpl.INSTANCE;
		try {
			// HHH-2300
			final Class h2ConstantsClass = ReflectHelper.classForName( "org.h2.engine.Constants" );
			final int majorVersion = (Integer) h2ConstantsClass.getDeclaredField( "VERSION_MAJOR" ).get( null );
			final int minorVersion = (Integer) h2ConstantsClass.getDeclaredField( "VERSION_MINOR" ).get( null );
			final int buildId = (Integer) h2ConstantsClass.getDeclaredField( "BUILD_ID" ).get( null );
			if ( buildId < 32 ) {
				querySequenceString = "select name from information_schema.sequences";
				sequenceInformationExtractor = SequenceInformationExtractorLegacyImpl.INSTANCE;
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
		this.sequenceInformationExtractor = sequenceInformationExtractor;

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
		// H2 does define "longvarchar", but it is a simple alias to "varchar"
		registerColumnType( Types.LONGVARCHAR, String.format( "varchar(%d)", Integer.MAX_VALUE ) );
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
		registerFunction( "acos", new StandardSQLFunction( "acos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "asin", new StandardSQLFunction( "asin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "atan", new StandardSQLFunction( "atan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "atan2", new StandardSQLFunction( "atan2", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "bitand", new StandardSQLFunction( "bitand", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "bitor", new StandardSQLFunction( "bitor", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "bitxor", new StandardSQLFunction( "bitxor", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "ceiling", new StandardSQLFunction( "ceiling", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cos", new StandardSQLFunction( "cos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "compress", new StandardSQLFunction( "compress", StandardSpiBasicTypes.BINARY ) );
		registerFunction( "cot", new StandardSQLFunction( "cot", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "decrypt", new StandardSQLFunction( "decrypt", StandardSpiBasicTypes.BINARY ) );
		registerFunction( "degrees", new StandardSQLFunction( "degrees", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "encrypt", new StandardSQLFunction( "encrypt", StandardSpiBasicTypes.BINARY ) );
		registerFunction( "exp", new StandardSQLFunction( "exp", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "expand", new StandardSQLFunction( "compress", StandardSpiBasicTypes.BINARY ) );
		registerFunction( "floor", new StandardSQLFunction( "floor", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "hash", new StandardSQLFunction( "hash", StandardSpiBasicTypes.BINARY ) );
		registerFunction( "log", new StandardSQLFunction( "log", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log10", new StandardSQLFunction( "log10", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "pi", new NoArgSQLFunction( "pi", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "power", new StandardSQLFunction( "power", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "radians", new StandardSQLFunction( "radians", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "rand", new NoArgSQLFunction( "rand", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "round", new StandardSQLFunction( "round", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "roundmagic", new StandardSQLFunction( "roundmagic", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sign", new StandardSQLFunction( "sign", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "sin", new StandardSQLFunction( "sin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "tan", new StandardSQLFunction( "tan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "truncate", new StandardSQLFunction( "truncate", StandardSpiBasicTypes.DOUBLE ) );

		// String Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		registerFunction( "ascii", new StandardSQLFunction( "ascii", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "char", new StandardSQLFunction( "char", StandardSpiBasicTypes.CHARACTER ) );
		registerFunction( "concat", new VarArgsSQLFunction( StandardSpiBasicTypes.STRING, "(", "||", ")" ) );
		registerFunction( "difference", new StandardSQLFunction( "difference", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "hextoraw", new StandardSQLFunction( "hextoraw", StandardSpiBasicTypes.STRING ) );
		registerFunction( "insert", new StandardSQLFunction( "lower", StandardSpiBasicTypes.STRING ) );
		registerFunction( "left", new StandardSQLFunction( "left", StandardSpiBasicTypes.STRING ) );
		registerFunction( "lcase", new StandardSQLFunction( "lcase", StandardSpiBasicTypes.STRING ) );
		registerFunction( "ltrim", new StandardSQLFunction( "ltrim", StandardSpiBasicTypes.STRING ) );
		registerFunction( "octet_length", new StandardSQLFunction( "octet_length", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "position", new StandardSQLFunction( "position", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "rawtohex", new StandardSQLFunction( "rawtohex", StandardSpiBasicTypes.STRING ) );
		registerFunction( "repeat", new StandardSQLFunction( "repeat", StandardSpiBasicTypes.STRING ) );
		registerFunction( "replace", new StandardSQLFunction( "replace", StandardSpiBasicTypes.STRING ) );
		registerFunction( "right", new StandardSQLFunction( "right", StandardSpiBasicTypes.STRING ) );
		registerFunction( "rtrim", new StandardSQLFunction( "rtrim", StandardSpiBasicTypes.STRING ) );
		registerFunction( "soundex", new StandardSQLFunction( "soundex", StandardSpiBasicTypes.STRING ) );
		registerFunction( "space", new StandardSQLFunction( "space", StandardSpiBasicTypes.STRING ) );
		registerFunction( "stringencode", new StandardSQLFunction( "stringencode", StandardSpiBasicTypes.STRING ) );
		registerFunction( "stringdecode", new StandardSQLFunction( "stringdecode", StandardSpiBasicTypes.STRING ) );
		registerFunction( "stringtoutf8", new StandardSQLFunction( "stringtoutf8", StandardSpiBasicTypes.BINARY ) );
		registerFunction( "ucase", new StandardSQLFunction( "ucase", StandardSpiBasicTypes.STRING ) );
		registerFunction( "utf8tostring", new StandardSQLFunction( "utf8tostring", StandardSpiBasicTypes.STRING ) );

		// Time and Date Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		registerFunction( "curdate", new NoArgSQLFunction( "curdate", StandardSpiBasicTypes.DATE ) );
		registerFunction( "curtime", new NoArgSQLFunction( "curtime", StandardSpiBasicTypes.TIME ) );
		registerFunction( "curtimestamp", new NoArgSQLFunction( "curtimestamp", StandardSpiBasicTypes.TIME ) );
		registerFunction( "current_date", new NoArgSQLFunction( "current_date", StandardSpiBasicTypes.DATE ) );
		registerFunction( "current_time", new NoArgSQLFunction( "current_time", StandardSpiBasicTypes.TIME ) );
		registerFunction( "current_timestamp", new NoArgSQLFunction( "current_timestamp", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "datediff", new StandardSQLFunction( "datediff", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayname", new StandardSQLFunction( "dayname", StandardSpiBasicTypes.STRING ) );
		registerFunction( "dayofmonth", new StandardSQLFunction( "dayofmonth", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofweek", new StandardSQLFunction( "dayofweek", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new StandardSQLFunction( "dayofyear", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "monthname", new StandardSQLFunction( "monthname", StandardSpiBasicTypes.STRING ) );
		registerFunction( "now", new NoArgSQLFunction( "now", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "quarter", new StandardSQLFunction( "quarter", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "week", new StandardSQLFunction( "week", StandardSpiBasicTypes.INTEGER ) );

		// System Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		registerFunction( "database", new NoArgSQLFunction( "database", StandardSpiBasicTypes.STRING ) );
		registerFunction( "user", new NoArgSQLFunction( "user", StandardSpiBasicTypes.STRING ) );

		getDefaultProperties().setProperty( AvailableSettings.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
		// http://code.google.com/p/h2database/issues/detail?id=235
		getDefaultProperties().setProperty( AvailableSettings.NON_CONTEXTUAL_LOB_CREATION, "true" );
	}

	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public String getForUpdateString() {
		return " for update";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public String getLimitString(String sql, boolean hasOffset) {
		return sql + (hasOffset ? " limit ? offset ?" : " limit ?");
	}

	@Override
	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return false;
	}

	@Override
	public boolean supportsIfExistsAfterTableName() {
		return true;
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
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
		return "drop sequence if exists " + sequenceName;
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
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return sequenceInformationExtractor;
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
		@Override
		protected String doExtractConstraintName(SQLException sqle) throws NumberFormatException {
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
	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		return new LocalTemporaryTableBulkIdStrategy(
				new IdTableSupportStandardImpl() {
					@Override
					public String getCreateIdTableCommand() {
						return "create cached local temporary table if not exists";
					}

					@Override
					public String getCreateIdTableStatementOptions() {
						// actually 2 different options are specified here:
						//		1) [on commit drop] - says to drop the table on transaction commit
						//		2) [transactional] - says to not perform an implicit commit of any current transaction
						return "on commit drop transactional";					}
				},
				AfterUseAction.CLEAN,
				TempTableDdlTransactionHandling.NONE
		);
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
	public boolean dropConstraints() {
		// We don't need to drop constraints beforeQuery dropping tables, that just leads to error
		// messages about missing tables when we don't have a schema in the database
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new H2IdentityColumnSupport();
	}
}
