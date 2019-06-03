package org.hibernate.dialect;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.PessimisticLockException;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.PostgresExtractEmulation;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.PostgreSQL81IdentityColumnSupport;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.procedure.internal.PostgresCallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.LocalTempTableExporter;
import org.hibernate.query.sqm.mutation.spi.idtable.LocalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.StandardIdTableSupport;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.AbstractJdbcValueBinder;
import org.hibernate.sql.AbstractJdbcValueExtractor;
import org.hibernate.sql.JdbcValueBinder;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.AbstractTemplateSqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.BlobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.ClobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hibernate.query.TemporalUnit.DAY;
import static org.hibernate.query.TemporalUnit.EPOCH;
import static org.hibernate.query.TemporalUnit.MONTH;
import static org.hibernate.query.TemporalUnit.QUARTER;
import static org.hibernate.query.TemporalUnit.YEAR;
import static org.hibernate.query.TemporalUnit.conversionFactor;
import static org.hibernate.type.descriptor.internal.DateTimeUtils.wrapAsAnsiDateLiteral;
import static org.hibernate.type.descriptor.internal.DateTimeUtils.wrapAsAnsiTimeLiteral;

/**
 * An SQL dialect for Postgres
 * <p/>
 * For discussion of BLOB support in Postgres, as of 8.4, have a peek at
 * <a href="http://jdbc.postgresql.org/documentation/84/binary-data.html">http://jdbc.postgresql.org/documentation/84/binary-data.html</a>.
 * For the effects in regards to Hibernate see <a href="http://in.relation.to/15492.lace">http://in.relation.to/15492.lace</a>
 *
 * @author Gavin King
 */
public class PostgreSQLDialect extends Dialect {

	int getVersion() {
		return 800;
	}

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

	/**
	 * Constructs a PostgreSQL81Dialect
	 */
	public PostgreSQLDialect() {
		super();

		registerColumnType( Types.TINYINT, "smallint" ); //no tinyint, not even in Postgres 11

		registerColumnType( Types.VARBINARY, "bytea" );
		registerColumnType( Types.BINARY, "bytea" );
		registerColumnType( Types.LONGVARBINARY, "bytea" );
		registerColumnType( Types.LONGVARCHAR, "text" );

		registerColumnType( Types.BLOB, "bytea" );
		registerColumnType( Types.CLOB, "text" );

		//there are no nchar/nvarchar types in Postgres
		registerColumnType( Types.NCHAR, "char($l)" );
		registerColumnType( Types.NVARCHAR, "varchar($l)" );
		registerColumnType( Types.LONGNVARCHAR, "varchar($l)" );

		if ( getVersion() >= 920 ) {
			registerColumnType( Types.JAVA_OBJECT, "json" );
		}

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
		getDefaultProperties().setProperty( Environment.NON_CONTEXTUAL_LOB_CREATION, "true" );
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BOOLEAN;
	}

	public void timestampadd(
			TemporalUnit unit,
			Renderer magnitude, Renderer to,
			Appender sqlAppender,
			boolean timestamp) {
		sqlAppender.append("(");
		to.render();
		boolean subtract = false;
		//TODO: do something nicer for negative magnitude
//		if ( magnitude.startsWith("-") ) {
//			subtract = true;
//			magnitude = magnitude.substring(1);
//		}
		sqlAppender.append(subtract ? " - " : " + ");
		switch ( unit ) {
			case NANOSECOND:
				sqlAppender.append("(");
				magnitude.render();
				sqlAppender.append(")/1e3 * interval '1 microsecond'");
				break;
			case QUARTER: //quarter is not supported in interval literals
				sqlAppender.append("(");
				magnitude.render();
				sqlAppender.append(") * interval '3 month'");
				break;
			default:
				//TODO: do something nicer for literal magnitude
//				if ( magnitude.matches("\\d+") ) {
//					sqlAppender.append("interval '");
//					sqlAppender.append( magnitude );
//				}
//				else {
					sqlAppender.append("(");
					magnitude.render();
					sqlAppender.append(") * interval '1");
//				}
				sqlAppender.append(" ");
				sqlAppender.append( unit.toString() );
				sqlAppender.append("'");
		}
		sqlAppender.append(")");
	}

	@Override
	public void timestampdiff(
			TemporalUnit unit,
			Renderer from, Renderer to,
			Appender sqlAppender,
			boolean fromTimestamp, boolean toTimestamp) {
		if ( !toTimestamp && !fromTimestamp && unit==DAY ) {
			// special case: subtraction of two dates
			// results in an integer number of days
			// instead of an INTERVAL
			sqlAppender.append("(");
			to.render();
			sqlAppender.append("-");
			from.render();
			sqlAppender.append(")");
		}
		else {
			switch (unit) {
				case YEAR:
					extractField(sqlAppender, from, to, YEAR, fromTimestamp, toTimestamp, unit);
					break;
				case QUARTER:
					sqlAppender.append("(");
					extractField(sqlAppender, from, to, YEAR, fromTimestamp, toTimestamp, unit);
					sqlAppender.append("+");
					extractField(sqlAppender, from, to, QUARTER, fromTimestamp, toTimestamp, unit);
					sqlAppender.append(")");
					break;
				case MONTH:
					sqlAppender.append("(");
					extractField(sqlAppender, from, to, YEAR, fromTimestamp, toTimestamp, unit);
					sqlAppender.append("+");
					extractField(sqlAppender, from, to, MONTH, fromTimestamp, toTimestamp, unit);
					sqlAppender.append(")");
					break;
				case WEEK: //week is not supported by extract() when the argument is a duration
				case DAY:
					extractField(sqlAppender, from, to, DAY, fromTimestamp, toTimestamp, unit);
					break;
				case HOUR:
				case MINUTE:
				case SECOND:
				case NANOSECOND:
					extractField(sqlAppender, from, to, EPOCH, fromTimestamp, toTimestamp, unit);
					break;
				default:
					throw new SemanticException("unrecognized field: " + unit);
			}
		}
	}

	private void extractField(
			Appender sqlAppender,
			Renderer from, Renderer to,
			TemporalUnit unit,
			boolean fromTimestamp, boolean toTimestamp,
			TemporalUnit toUnit) {
		sqlAppender.append("extract(");
		sqlAppender.append( unit.toString() );
		sqlAppender.append(" from ");
		if ( !toTimestamp && !fromTimestamp ) {
			// special case subtraction of two
			// dates results in an integer not
			// an Interval
			sqlAppender.append("age(");
			to.render();
			sqlAppender.append(",");
			from.render();
			sqlAppender.append(")");
		}
		else {
			switch (unit) {
				case YEAR:
				case MONTH:
				case QUARTER:
					sqlAppender.append("age(");
					to.render();
					sqlAppender.append(",");
					from.render();
					sqlAppender.append(")");
					break;
				case DAY:
				case HOUR:
				case MINUTE:
				case SECOND:
				case EPOCH:
					to.render();
					sqlAppender.append("-");
					from.render();
					break;
				default:
					throw new SemanticException(unit + " is not a legal field");
			}
		}
		sqlAppender.append(")");
		sqlAppender.append( conversionFactor(unit, toUnit) );
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.cot( queryEngine );
		CommonFunctionFactory.radians( queryEngine );
		CommonFunctionFactory.degrees( queryEngine );
		CommonFunctionFactory.trunc( queryEngine );
		CommonFunctionFactory.log( queryEngine );
		CommonFunctionFactory.cbrt( queryEngine );
		CommonFunctionFactory.trim2( queryEngine );
		CommonFunctionFactory.pad( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.repeat( queryEngine );
		CommonFunctionFactory.md5( queryEngine );
		CommonFunctionFactory.initcap( queryEngine );
		CommonFunctionFactory.substr( queryEngine );
		CommonFunctionFactory.substring_substr( queryEngine );
		//also natively supports ANSI-style substring()
		CommonFunctionFactory.translate( queryEngine );
		CommonFunctionFactory.toCharNumberDateTimestamp( queryEngine );
		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.leftRight( queryEngine );
		CommonFunctionFactory.localtimeLocaltimestamp( queryEngine );
		CommonFunctionFactory.dateTrunc( queryEngine );
		CommonFunctionFactory.bitLength( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.ascii( queryEngine );
		CommonFunctionFactory.char_chr( queryEngine );
		CommonFunctionFactory.position( queryEngine );
		CommonFunctionFactory.bitandorxornot_operator( queryEngine );
		CommonFunctionFactory.bitAndOr( queryEngine );
		CommonFunctionFactory.everyAny_boolAndOr( queryEngine );
		CommonFunctionFactory.median_percentileCont( queryEngine, false );
		CommonFunctionFactory.stddev( queryEngine );
		CommonFunctionFactory.stddevPopSamp( queryEngine );
		CommonFunctionFactory.variance( queryEngine );
		CommonFunctionFactory.varPopSamp( queryEngine );
		CommonFunctionFactory.makeDateTimeTimestamp( queryEngine );
		CommonFunctionFactory.insert_overlay( queryEngine );
		CommonFunctionFactory.soundex( queryEngine ); //was introduced in Postgres 9 apparently

		queryEngine.getSqmFunctionRegistry().register( "extract", new PostgresExtractEmulation() );

		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern("locate", StandardSpiBasicTypes.INTEGER, "position(?1 in ?2)", "(position(?1 in substring(?2 from ?3)) + (?3) - 1)");

		if ( getVersion() >= 940 ) {
			queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "make_interval" )
					.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
					.setArgumentCountBetween( 1, 7 )
					.register();
			queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "make_timestamp" )
					.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
					.setExactArgumentCount( 6 )
					.register();
			queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "make_timestamptz" )
					.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
					.setArgumentCountBetween( 6, 7 )
					.register();
			queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "make_date" )
					.setInvariantType( StandardSpiBasicTypes.DATE )
					.setExactArgumentCount( 3 )
					.register();
			queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "make_time" )
					.setInvariantType( StandardSpiBasicTypes.TIME )
					.setExactArgumentCount( 3 )
					.register();
		}
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		SqlTypeDescriptor descriptor;
		switch ( sqlCode ) {
			case Types.BLOB: {
				// Force BLOB binding.  Otherwise, byte[] fields annotated
				// with @Lob will attempt to use
				// BlobTypeDescriptor.PRIMITIVE_ARRAY_BINDING.  Since the
				// dialect uses oid for Blobs, byte arrays cannot be used.
				descriptor = BlobSqlDescriptor.BLOB_BINDING;
				break;
			}
			case Types.CLOB: {
				descriptor = ClobSqlDescriptor.CLOB_BINDING;
				break;
			}
			default: {
				descriptor = super.getSqlTypeDescriptorOverride( sqlCode );
				break;
			}
		}
		return descriptor;
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return getVersion() >= 820;
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return getVersion() >= 900;
	}

	@Override
	public boolean supportsIfExistsAfterAlterTable() {
		return getVersion() >= 920;
	}

	@Override
	public boolean supportsValuesList() {
		return getVersion() >= 820;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return getVersion() >= 820;
	}

	@Override
	public boolean supportsPartitionBy() {
		return getVersion() >= 910;
	}

	@Override
	public boolean supportsNonQueryWithCTE() {
		return getVersion() >= 910;
	}

	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName );
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "nextval ('" + sequenceName + "')";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		//starts with 1, implicitly
		return "create sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return getVersion() < 820
				? "drop sequence " + sequenceName
				: "drop sequence if exists " + sequenceName;
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade";
	}

	@Override
	public boolean dropConstraints() {
		return true;
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public String getQuerySequencesString() {
		return "select * from information_schema.sequences";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsLimit() {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public String getLimitString(String sql, boolean hasOffset) {
		return sql + (hasOffset ? " limit ? offset ?" : " limit ?");
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	@Override
	public String getForUpdateString(String aliases) {
		return getForUpdateString() + " of " + aliases;
	}

	@Override
	public String getForUpdateString(String aliases, LockOptions lockOptions) {
		/*
		 * Parent's implementation for (aliases, lockOptions) ignores aliases.
		 */
		if ( "".equals( aliases ) ) {
			LockMode lockMode = lockOptions.getLockMode();
			final Iterator<Map.Entry<String, LockMode>> itr = lockOptions.getAliasLockIterator();
			while ( itr.hasNext() ) {
				// seek the highest lock mode
				final Map.Entry<String, LockMode> entry = itr.next();
				final LockMode lm = entry.getValue();
				if ( lm.greaterThan( lockMode ) ) {
					aliases = entry.getKey();
				}
			}
		}
		LockMode lockMode = lockOptions.getAliasSpecificLockMode( aliases );
		if (lockMode == null ) {
			lockMode = lockOptions.getLockMode();
		}
		switch ( lockMode ) {
			//noinspection deprecation
			case UPGRADE:
				return getForUpdateString(aliases);
			case PESSIMISTIC_READ:
				return getReadLockString( aliases, lockOptions.getTimeOut() );
			case PESSIMISTIC_WRITE:
				return getWriteLockString( aliases, lockOptions.getTimeOut() );
			case UPGRADE_NOWAIT:
				//noinspection deprecation
			case FORCE:
			case PESSIMISTIC_FORCE_INCREMENT:
				return getForUpdateNowaitString(aliases);
			case UPGRADE_SKIPLOCKED:
				return getForUpdateSkipLockedString(aliases);
			default:
				return "";
		}
	}

	@Override
	public String getNoColumnsInsertString() {
		return "default values";
	}

	@Override
	public String getCaseInsensitiveLike(){
		return "ilike";
	}

	@Override
	public boolean supportsCaseInsensitiveLike() {
		return true;
	}

	@Override
	public String getNativeIdentifierGeneratorStrategy() {
		return "sequence";
	}

	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public boolean useInputStreamToInsertBlob() {
		return false;
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	/**
	 * Workaround for postgres bug #1453
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String getSelectClauseNullString(int sqlType) {
		return "null::" + getRawTypeName( sqlType );
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public SqmMutationStrategy getDefaultIdTableStrategy() {
		return new LocalTemporaryTableStrategy(
				new StandardIdTableSupport(
						getVersion() < 820
								? new LocalTempTableExporter() {
									@Override
									protected String getCreateOptions() {
										return "on commit drop";
									}
								}
								: new LocalTempTableExporter() {
									@Override
									public String getCreateCommand() {
										return "create temporary table";
									}
								}
				)
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
		return "select now()";
	}

	@Override
	public boolean requiresParensForTupleDistinctCounts() {
		return true;
	}

	@Override
	public String toBooleanValueString(boolean bool) {
		return bool ? "true" : "false";
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	/**
	 * Constraint-name extractor for Postgres constraint violation exceptions.
	 * Orginally contributed by Denny Bartelt.
	 */
	private static final ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {
		@Override
		protected String doExtractConstraintName(SQLException sqle) throws NumberFormatException {
			final int sqlState = Integer.valueOf( JdbcExceptionHelper.extractSqlState( sqle ) );
			switch (sqlState) {
				// CHECK VIOLATION
				case 23514: return extractUsingTemplate( "violates check constraint \"","\"", sqle.getMessage() );
				// UNIQUE VIOLATION
				case 23505: return extractUsingTemplate( "violates unique constraint \"","\"", sqle.getMessage() );
				// FOREIGN KEY VIOLATION
				case 23503: return extractUsingTemplate( "violates foreign key constraint \"","\"", sqle.getMessage() );
				// NOT NULL VIOLATION
				case 23502: return extractUsingTemplate( "null value in column \"","\" violates not-null constraint", sqle.getMessage() );
				// TODO: RESTRICT VIOLATION
				case 23001: return null;
				// ALL OTHER
				default: return null;
			}
		}
	};

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return new SQLExceptionConversionDelegate() {
			@Override
			public JDBCException convert(SQLException sqlException, String message, String sql) {
				final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );

				if ( "40P01".equals( sqlState ) ) {
					// DEADLOCK DETECTED
					return new LockAcquisitionException( message, sqlException, sql );
				}

				if ( "55P03".equals( sqlState ) ) {
					// LOCK NOT AVAILABLE
					return new PessimisticLockException( message, sqlException, sql );
				}

				// returning null allows other delegates to operate
				return null;
			}
		};
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		// Register the type of the out param - PostgreSQL uses Types.OTHER
		statement.registerOutParameter( col++, Types.OTHER );
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		ps.execute();
		return (ResultSet) ps.getObject( 1 );
	}

	@Override
	public boolean supportsPooledSequences() {
		return true;
	}

	/**
	 * only necessary for postgre < 7.4  See http://anoncvs.postgresql.org/cvsweb.cgi/pgsql/doc/src/sgml/ref/create_sequence.sgml
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	protected String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) {
		if ( initialValue < 0 && incrementSize > 0 ) {
			return
					String.format(
							"%s minvalue %d start %d increment %d",
							getCreateSequenceString( sequenceName ),
							initialValue,
							initialValue,
							incrementSize
					);
		}
		else if ( initialValue > 0 && incrementSize < 0 ) {
			return
					String.format(
							"%s maxvalue %d start %d increment %d",
							getCreateSequenceString( sequenceName ),
							initialValue,
							initialValue,
							incrementSize
					);
		}
		else {
			return
					String.format(
							"%s start %d increment %d",
							getCreateSequenceString( sequenceName ),
							initialValue,
							incrementSize
					);
		}
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean supportsExpectedLobUsagePattern() {
		return true;
	}

	@Override
	public boolean supportsLobValueChangePropogation() {
		return false;
	}

	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return false;
	}

	@Override
	public String getForUpdateString() {
		return " for update";
	}

	@Override
	public boolean supportsRowValueConstructorSyntax() {
		return true;
	}

	@Override
	public String getForUpdateNowaitString() {
		return getForUpdateString() + " nowait ";
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateString( aliases ) + " nowait ";
	}

	@Override
	public CallableStatementSupport getCallableStatementSupport() {
		return PostgresCallableStatementSupport.INSTANCE;
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, int position) throws SQLException {
		if ( position != 1 ) {
			throw new UnsupportedOperationException( "PostgreSQL only supports REF_CURSOR parameters as the first parameter" );
		}
		return (ResultSet) statement.getObject( 1 );
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, String name) throws SQLException {
		throw new UnsupportedOperationException( "PostgreSQL only supports accessing REF_CURSOR parameters by position" );
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new PostgreSQL81IdentityColumnSupport();
	}

	@Override
	public boolean supportsNationalizedTypes() {
		return false;
	}

	@Override
	public boolean supportsNoWait() {
		return true;
	}

	@Override
	public boolean supportsJdbcConnectionLobCreation(DatabaseMetaData databaseMetaData) {
		return false;
	}

	@Override
	public String translateDatetimeFormat(String format) {
		return datetimeFormat( format ).result();
	}

	public Replacer datetimeFormat(String format) {
		return Oracle8iDialect.datetimeFormat(format, true)
				.replace("SSSSSS", "US")
				.replace("SSSSS", "US")
				.replace("SSSS", "US")
				.replace("SSS", "MS")
				.replace("SS", "MS")
				.replace("S", "MS")
				//use ISO day in week, as per DateTimeFormatter
				.replace("ee", "ID")
				.replace("e", "fmID")
				//TZR is TZ in Postgres
				.replace("zzz", "TZ")
				.replace("zz", "TZ")
				.replace("z", "TZ");
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			//WEEK means the ISO week number on Postgres
			case DAY_OF_MONTH: return "day";
			case DAY_OF_YEAR: return "doy";
			case DAY_OF_WEEK: return "dow";
			default: return unit.toString();
		}
	}

	@Override
	protected String wrapDateLiteral(String date) {
		return wrapAsAnsiDateLiteral(date);
	}

	@Override
	protected String wrapTimeLiteral(String time) {
		return wrapAsAnsiTimeLiteral(time);
	}

	@Override
	protected String wrapTimestampLiteral(String timestamp) {
		return "timestamp with time zone '" + timestamp + "'";
	}

	@Override
	public String getWriteLockString(int timeout) {
		if ( getVersion() >= 950 && timeout == LockOptions.SKIP_LOCKED ) {
			return getForUpdateSkipLockedString();
		}
		else if ( timeout == LockOptions.NO_WAIT ) {
			return " for update nowait";
		}
		else {
			return " for update";
		}
	}

	@Override
	public String getWriteLockString(String aliases, int timeout) {
		if ( getVersion() >= 950 && timeout == LockOptions.SKIP_LOCKED ) {
			return getForUpdateSkipLockedString( aliases );
		}
		else if ( timeout == LockOptions.NO_WAIT ) {
			return String.format( " for update of %s nowait", aliases );
		}
		else {
			return " for update of " + aliases;
		}
	}

	@Override
	public String getReadLockString(int timeout) {
		if ( getVersion() >= 950 && timeout == LockOptions.SKIP_LOCKED ) {
			return " for share skip locked";
		}
		else if ( timeout == LockOptions.NO_WAIT ) {
			return " for share nowait";
		}
		else {
			return " for share";
		}
	}

	@Override
	public String getReadLockString(String aliases, int timeout) {
		if ( getVersion() >= 950 && timeout == LockOptions.SKIP_LOCKED ) {
			return String.format( " for share of %s skip locked", aliases );
		}
		else if ( timeout == LockOptions.NO_WAIT ) {
			return String.format( " for share of %s nowait", aliases );
		}
		else {
			return " for share of " + aliases;
		}
	}

	@Override
	public String getForUpdateSkipLockedString() {
		return getVersion() >= 940
				? " for update skip locked"
				: super.getForUpdateSkipLockedString();
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		return getVersion() >= 940
				? getForUpdateString() + " of " + aliases + " skip locked"
				: super.getForUpdateSkipLockedString( aliases );
	}

	@Override
	public boolean supportsSkipLocked() {
		return getVersion() >= 940;
	}

	@Override
	public void augmentRecognizedTableTypes(List<String> tableTypesList) {
		super.augmentRecognizedTableTypes( tableTypesList );
		if ( getVersion() >= 930 ) {
			tableTypesList.add( "MATERIALIZED VIEW" );
		}
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		if ( getVersion() >= 820 ) {
			// HHH-9562
			typeContributions.contributeSqlTypeDescriptor( PostgresUUIDType.INSTANCE );
		}
	}

	private static class PostgresUUIDType extends AbstractTemplateSqlTypeDescriptor {
		/**
		 * Singleton access
		 */
		private static final PostgresUUIDType INSTANCE = new PostgresUUIDType();

		/**
		 * Postgres reports its UUID type as {@link java.sql.Types#OTHER}.  Unfortunately
		 * it reports a lot of its types as {@link java.sql.Types#OTHER}, making that
		 * value useless for distinguishing one SqlTypeDescriptor from another.
		 * So here we define a "magic value" that is a (hopefully no collisions)
		 * unique key within the {@link org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptorRegistry}
		 */
		private static final int JDBC_TYPE_CODE = 3975;

		@Override
		public int getJdbcTypeCode() {
			return JDBC_TYPE_CODE;
		}

		@Override
		public boolean canBeRemapped() {
			return true;
		}

		@Override
		public <J> BasicJavaDescriptor<J> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return (BasicJavaDescriptor<J>) typeConfiguration.getJavaTypeDescriptorRegistry().getOrMakeJavaDescriptor( UUID.class );
		}

		@Override
		public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
			return null;
		}

		@Override
		protected <X> JdbcValueBinder<X> createBinder(BasicJavaDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
			return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(
						PreparedStatement st,
						int index,
						X value,
						ExecutionContext executionContext) throws SQLException {
					st.setObject( index, javaTypeDescriptor.unwrap( value, UUID.class, executionContext.getSession() ), Types.OTHER );
				}

				@Override
				protected void doBind(
						CallableStatement st,
						String name,
						X value,
						ExecutionContext executionContext) throws SQLException {
					st.setObject( name, javaTypeDescriptor.unwrap( value, UUID.class, executionContext.getSession() ), Types.OTHER );
				}
			};
		}

		@Override
		protected <X> JdbcValueExtractor<X> createExtractor(BasicJavaDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
			return new AbstractJdbcValueExtractor<X>( javaTypeDescriptor, this ) {
				@Override
				protected X doExtract(ResultSet rs, int position, ExecutionContext executionContext) throws SQLException {
					return javaTypeDescriptor.wrap( rs.getObject( position ), executionContext.getSession() );
				}

				@Override
				protected X doExtract(CallableStatement statement, int position, ExecutionContext executionContext) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getObject( position ), executionContext.getSession() );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, ExecutionContext executionContext) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getObject( name ), executionContext.getSession() );
				}
			};
		}
	}
}
