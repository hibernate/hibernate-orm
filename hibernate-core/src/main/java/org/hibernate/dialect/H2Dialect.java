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
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.hint.IndexQueryHintHandler;
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
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.query.sqm.consume.multitable.internal.StandardIdTableSupport;
import org.hibernate.query.sqm.consume.multitable.spi.IdTableStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTable;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTableManagementTransactionality;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTableSupport;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.LocalTempTableExporter;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.LocalTemporaryTableStrategy;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.spi.ConcatFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.StandardAnsiSqlSqmAggregationFunctionTemplates.AvgFunctionTemplate;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorH2DatabaseImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.spi.Exporter;
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

		int buildId = Integer.MIN_VALUE;

		try {
			// HHH-2300
			final Class h2ConstantsClass = ReflectHelper.classForName( "org.h2.engine.Constants" );
			final int majorVersion = (Integer) h2ConstantsClass.getDeclaredField( "VERSION_MAJOR" ).get( null );
			final int minorVersion = (Integer) h2ConstantsClass.getDeclaredField( "VERSION_MINOR" ).get( null );
			buildId = (Integer) h2ConstantsClass.getDeclaredField( "BUILD_ID" ).get( null );

			if ( ! ( majorVersion > 1 || minorVersion > 2 || buildId >= 139 ) ) {
				LOG.unsupportedMultiTableBulkHqlJpaql( majorVersion, minorVersion, buildId );
			}
		}
		catch ( Exception e ) {
			// probably H2 not in the classpath, though in certain app server environments it might just mean we are
			// not using the correct classloader
			LOG.undeterminedH2Version();
		}

		if ( buildId >= 32 ) {
			this.sequenceInformationExtractor = SequenceInformationExtractorH2DatabaseImpl.INSTANCE;
			this.querySequenceString = "select * from information_schema.sequences";
		}
		else {
			this.sequenceInformationExtractor = SequenceInformationExtractorNoOpImpl.INSTANCE;
			this.querySequenceString = null;
		}

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

		getDefaultProperties().setProperty( AvailableSettings.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
		// http://code.google.com/p/h2database/issues/detail?id=235
		getDefaultProperties().setProperty( AvailableSettings.NON_CONTEXTUAL_LOB_CREATION, "true" );
	}

	@Override
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );

		// Aggregations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		registry.register( "avg", new AvgFunctionTemplate( "double" ) );


		// Numeric Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		CommonFunctionFactory.acos( registry );
		CommonFunctionFactory.asin( registry );
		CommonFunctionFactory.atan( registry );

		registry.namedTemplateBuilder( "atan2" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();

		registry.namedTemplateBuilder( "bitand" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		registry.namedTemplateBuilder( "bitor" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		registry.namedTemplateBuilder( "bitxor" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		CommonFunctionFactory.ceiling( registry );

		CommonFunctionFactory.cos( registry );

		registry.namedTemplateBuilder( "compress" )
				.setArgumentCountBetween( 1, 2 )
				.setInvariantType( StandardSpiBasicTypes.BINARY )
				.register();

		CommonFunctionFactory.cot( registry );

		registry.namedTemplateBuilder( "decrypt" )
				.setExactArgumentCount( 3 )
				.setInvariantType( StandardSpiBasicTypes.BINARY )
				.register();

		CommonFunctionFactory.degrees( registry );

		registry.namedTemplateBuilder( "encrypt" )
				.setExactArgumentCount( 3 )
				.setInvariantType( StandardSpiBasicTypes.BINARY )
				.register();

		CommonFunctionFactory.exp( registry );

		registry.namedTemplateBuilder( "expand" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.BINARY )
				.register();

		CommonFunctionFactory.floor( registry );

		registry.namedTemplateBuilder( "hash" )
				.setExactArgumentCount( 3 )
				.setInvariantType( StandardSpiBasicTypes.BINARY )
				.register();

		CommonFunctionFactory.log( registry );
		CommonFunctionFactory.log10( registry );

		registry.namedTemplateBuilder( "pi" )
				.setExactArgumentCount( 0 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();

		registry.namedTemplateBuilder( "power" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();

		CommonFunctionFactory.radians( registry );

		registry.namedTemplateBuilder( "rand" )
				.setArgumentCountBetween( 0, 1 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();

		CommonFunctionFactory.round( registry );

		registry.namedTemplateBuilder( "roundmagic" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();

		CommonFunctionFactory.sign( registry );

		CommonFunctionFactory.sin( registry );

		CommonFunctionFactory.tan( registry );

		registry.namedTemplateBuilder( "truncate" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();


		// String Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		registry.namedTemplateBuilder( "ascii" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		registry.namedTemplateBuilder( "char" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.CHARACTER )
				.register();

		registry.register( "concat", ConcatFunctionTemplate.INSTANCE );

		registry.namedTemplateBuilder( "difference" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		registry.namedTemplateBuilder( "hextoraw" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

		registry.namedTemplateBuilder( "insert" )
				.setExactArgumentCount( 4 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

		registry.namedTemplateBuilder( "insert" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

		registry.namedTemplateBuilder( "insert" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

		registry.registerAlternateKey( "lcase", "lower" );

		registry.namedTemplateBuilder( "ltrim" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

		registry.namedTemplateBuilder( "octet_length" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		CommonFunctionFactory.position( registry );

		registry.namedTemplateBuilder( "rawtohex" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

		registry.namedTemplateBuilder( "repeat" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

		registry.namedTemplateBuilder( "replace" )
				.setArgumentCountBetween( 2, 3 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

		registry.namedTemplateBuilder( "right" )
				.setArgumentCountBetween( 2, 3 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

		registry.namedTemplateBuilder( "rtrim" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

		CommonFunctionFactory.soundex( registry );

		registry.namedTemplateBuilder( "space" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

		registry.namedTemplateBuilder( "stringencode" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

		registry.namedTemplateBuilder( "stringdecode" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

		registry.namedTemplateBuilder( "stringtoutf8" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.BINARY )
				.register();

		registry.registerAlternateKey( "ucase", "upper" );

		registry.namedTemplateBuilder( "utf8tostring" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();


		// Time and Date Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		registry.registerAlternateKey( "curdate", "current_date" );
		registry.registerAlternateKey( "curtime", "current_time" );
		registry.registerAlternateKey( "curtimestamp", "current_timestamp" );

		registry.namedTemplateBuilder( "datediff" )
				.setExactArgumentCount( 3 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		registry.namedTemplateBuilder( "dayname" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

		registry.namedTemplateBuilder( "dayofmonth" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		registry.namedTemplateBuilder( "dayofweek" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		registry.namedTemplateBuilder( "dayofyear" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		registry.registerAlternateKey( "now", "current_timestamp" );

		registry.namedTemplateBuilder( "quarter" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();

		registry.namedTemplateBuilder( "week" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();


		// System Functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		registry.namedTemplateBuilder( "database" )
				.setExactArgumentCount( 0 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

		registry.namedTemplateBuilder( "user" )
				.setExactArgumentCount( 0 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();
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
	public IdTableStrategy getDefaultIdTableStrategy() {
		return new LocalTemporaryTableStrategy( generateIdTableSupport() );
	}

	private IdTableSupport generateIdTableSupport() {
		return new StandardIdTableSupport( generateIdTableExporter(), IdTableManagementTransactionality.NONE );
	}

	private Exporter<IdTable> generateIdTableExporter() {
		return new LocalTempTableExporter() {
			@Override
			protected String getCreateCommand() {
				return "create cached local temporary table if not exists";
			}

			@Override
			protected String getCreateOptions() {
				// actually 2 different options are specified here:
				//		1) [on commit drop] - says to drop the table on transaction commit
				//		2) [transactional] - says to not perform an implicit commit of any current transaction
				return "on commit drop transactional";
			}
		};
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
		// We don't need to drop constraints before dropping tables, that just leads to error
		// messages about missing tables when we don't have a schema in the database
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new H2IdentityColumnSupport();
	}

	@Override
	public String getQueryHintString(String query, String hints) {
		return IndexQueryHintHandler.INSTANCE.addQueryHints( query, hints );
	}
}
