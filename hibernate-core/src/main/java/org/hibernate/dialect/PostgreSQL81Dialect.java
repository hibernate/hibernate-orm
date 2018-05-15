/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.PessimisticLockException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.LocateEmulationUsingPositionAndSubstring;
import org.hibernate.query.sqm.consume.multitable.internal.StandardIdTableSupport;
import org.hibernate.query.sqm.consume.multitable.spi.IdTableStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.LocalTempTableExporter;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.LocalTemporaryTableStrategy;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
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
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.spi.ConcatFunctionTemplate;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.type.descriptor.sql.spi.BlobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.ClobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * An SQL dialect for Postgres
 * <p/>
 * For discussion of BLOB support in Postgres, as of 8.4, have a peek at
 * <a href="http://jdbc.postgresql.org/documentation/84/binary-data.html">http://jdbc.postgresql.org/documentation/84/binary-data.html</a>.
 * For the effects in regards to Hibernate see <a href="http://in.relation.to/15492.lace">http://in.relation.to/15492.lace</a>
 *
 * @author Gavin King
 */
@SuppressWarnings("deprecation")
public class PostgreSQL81Dialect extends Dialect {

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
	public PostgreSQL81Dialect() {
		super();
		registerColumnType( Types.BIT, "bool" );
		registerColumnType( Types.BIGINT, "int8" );
		registerColumnType( Types.SMALLINT, "int2" );
		registerColumnType( Types.TINYINT, "int2" );
		registerColumnType( Types.INTEGER, "int4" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float4" );
		registerColumnType( Types.DOUBLE, "float8" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARBINARY, "bytea" );
		registerColumnType( Types.BINARY, "bytea" );
		registerColumnType( Types.LONGVARCHAR, "text" );
		registerColumnType( Types.LONGVARBINARY, "bytea" );
		registerColumnType( Types.CLOB, "text" );
		registerColumnType( Types.BLOB, "oid" );
		registerColumnType( Types.NUMERIC, "numeric($p, $s)" );
		registerColumnType( Types.OTHER, "uuid" );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
		getDefaultProperties().setProperty( Environment.NON_CONTEXTUAL_LOB_CREATION, "true" );
	}

	@Override
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );

		CommonFunctionFactory.abs( registry );
		CommonFunctionFactory.sign( registry );

		CommonFunctionFactory.acos( registry );
		CommonFunctionFactory.asin( registry );
		CommonFunctionFactory.atan( registry );
		CommonFunctionFactory.cos( registry );
		CommonFunctionFactory.cot( registry );
		CommonFunctionFactory.exp( registry );
		CommonFunctionFactory.ln( registry );
		CommonFunctionFactory.sin( registry );
		CommonFunctionFactory.sqrt( registry );
		registry.namedTemplateBuilder( "cbrt" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		CommonFunctionFactory.tan( registry );
		CommonFunctionFactory.radians( registry );
		CommonFunctionFactory.degrees( registry );

		CommonFunctionFactory.stddev( registry );
		CommonFunctionFactory.variance( registry );

		registry.registerNoArgs( "random", StandardSpiBasicTypes.DOUBLE );
		registry.registerAlternateKey( "rand", "random" );

		CommonFunctionFactory.round( registry );
		CommonFunctionFactory.trunc( registry );
		CommonFunctionFactory.ceil( registry );
		CommonFunctionFactory.floor( registry );

		registry.namedTemplateBuilder( "ltrim" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 1, 2 )
				.register();
		registry.namedTemplateBuilder( "rtrim" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 1, 2 )
				.register();
		registry.namedTemplateBuilder( "chr" )
				.setInvariantType( StandardSpiBasicTypes.CHARACTER )
				.setExactArgumentCount( 1 )
				.register();
		CommonFunctionFactory.lower( registry );
		CommonFunctionFactory.upper( registry );
		registry.namedTemplateBuilder( "substr" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
		registry.namedTemplateBuilder( "initcap" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "to_ascii" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 1, 2 )
				.register();
		registry.namedTemplateBuilder( "quote_ident" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "quote_literal" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "md5" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "ascii" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "char_length" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "bit_length" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		registry.namedTemplateBuilder( "octet_length" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();

		registry.namedTemplateBuilder( "age" )
				.setArgumentCountBetween( 1, 2 )
				.register();
		registry.registerNoArgs( "current_date", StandardSpiBasicTypes.DATE );
		registry.registerNoArgs( "current_time", StandardSpiBasicTypes.TIME );
		registry.registerNoArgs( "current_timestamp", StandardSpiBasicTypes.TIMESTAMP );
		registry.namedTemplateBuilder( "date_trunc" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 2 )
				.register();
		registry.registerNoArgs( "localtime", StandardSpiBasicTypes.TIME );
		registry.registerNoArgs( "localtimestamp", StandardSpiBasicTypes.TIMESTAMP );
		registry.noArgsBuilder( "now" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setUseParenthesesWhenNoArgs( true )
				.register();
		registry.noArgsBuilder( "timeofday" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setUseParenthesesWhenNoArgs( true )
				.register();

		registry.registerNoArgs( "current_user", StandardSpiBasicTypes.STRING );
		registry.registerNoArgs( "session_user", StandardSpiBasicTypes.STRING );
		registry.registerNoArgs( "user", StandardSpiBasicTypes.STRING );
		registry.noArgsBuilder( "current_database" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setUseParenthesesWhenNoArgs( true )
				.register();
		registry.noArgsBuilder( "current_schema" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setUseParenthesesWhenNoArgs( true )
				.register();

		registry.namedTemplateBuilder( "to_char" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
		registry.namedTemplateBuilder( "to_date" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 2 )
				.register();
		registry.namedTemplateBuilder( "to_timestamp" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 2 )
				.register();
		registry.namedTemplateBuilder( "to_number" )
				.setInvariantType( StandardSpiBasicTypes.BIG_DECIMAL )
				.setExactArgumentCount( 2 )
				.register();

		registry.register( "concat", ConcatFunctionTemplate.INSTANCE );
		registry.namedTemplateBuilder( "lpad" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
		registry.namedTemplateBuilder( "replace" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
		registry.namedTemplateBuilder( "rpad" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
		registry.namedTemplateBuilder( "translate" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 3 )
				.register();

		registry.registerAlternateKey( "substring", "substr" );
		registry.register( "locate", new LocateEmulationUsingPositionAndSubstring() );
		registry.namedTemplateBuilder( "coalesce" )
				.setArgumentsValidator( StandardArgumentsValidators.min( 2 ) )
				.register();

		registry.registerPattern( "str", "cast(?1 as varchar)", StandardSpiBasicTypes.STRING );

		// Multi-param numeric dialect functions...
		registry.namedTemplateBuilder( "atan2" )
				.setInvariantType( StandardSpiBasicTypes.FLOAT )
				.setExactArgumentCount( 2 )
				.register();
		CommonFunctionFactory.log( registry );
		CommonFunctionFactory.mod( registry );
		registry.namedTemplateBuilder( "power" )
				.setInvariantType( StandardSpiBasicTypes.FLOAT )
				.setExactArgumentCount( 2 )
				.register();
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
		return "drop sequence " + sequenceName;
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
			case UPGRADE:
				return getForUpdateString(aliases);
			case PESSIMISTIC_READ:
				return getReadLockString( aliases, lockOptions.getTimeOut() );
			case PESSIMISTIC_WRITE:
				return getWriteLockString( aliases, lockOptions.getTimeOut() );
			case UPGRADE_NOWAIT:
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
		String typeName = getTypeName( sqlType, 1, 1, 0 );
		//trim off the length/precision/scale
		final int loc = typeName.indexOf( '(' );
		if ( loc > -1 ) {
			typeName = typeName.substring( 0, loc );
		}
		return "null::" + typeName;
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public IdTableStrategy getDefaultIdTableStrategy() {
		return new LocalTemporaryTableStrategy(
				new StandardIdTableSupport(
						new LocalTempTableExporter() {
							@Override
							protected String getCreateOptions() {
								return "on commit drop";
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
	public String getWriteLockString(int timeout) {
		if ( timeout == LockOptions.NO_WAIT ) {
			return " for update nowait";
		}
		else {
			return " for update";
		}
	}

	@Override
	public String getWriteLockString(String aliases, int timeout) {
		if ( timeout == LockOptions.NO_WAIT ) {
			return String.format( " for update of %s nowait", aliases );
		}
		else {
			return " for update of " + aliases;
		}
	}

	@Override
	public String getReadLockString(int timeout) {
		if ( timeout == LockOptions.NO_WAIT ) {
			return " for share nowait";
		}
		else {
			return " for share";
		}
	}

	@Override
	public String getReadLockString(String aliases, int timeout) {
		if ( timeout == LockOptions.NO_WAIT ) {
			return String.format( " for share of %s nowait", aliases );
		}
		else {
			return " for share of " + aliases;
		}
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
}
