/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.Map;
import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NullPrecedence;
import org.hibernate.PessimisticLockException;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.PositionSubstringFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.identity.CockroachDB1920IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.inline.InlineIdsInClauseBulkIdStrategy;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.PostgresUUIDType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarbinaryTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * An SQL dialect for CockroachDB 19.2 and later. This is the first dialect for CockroachDB. It is largely adapted
 * from the PostgreSQL dialects, with changes where appropriate.
 */
public class CockroachDB192Dialect extends Dialect {

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

	public CockroachDB192Dialect() {
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
		registerColumnType( Types.BLOB, "bytea" );
		registerColumnType( Types.NUMERIC, "numeric($p, $s)" );
		registerColumnType( Types.OTHER, "uuid" );
		registerColumnType( Types.JAVA_OBJECT, "json" );

		registerFunction( "abs", new StandardSQLFunction("abs") );
		registerFunction( "sign", new StandardSQLFunction("sign", StandardBasicTypes.INTEGER) );

		registerFunction( "acos", new StandardSQLFunction("acos", StandardBasicTypes.DOUBLE) );
		registerFunction( "asin", new StandardSQLFunction("asin", StandardBasicTypes.DOUBLE) );
		registerFunction( "atan", new StandardSQLFunction("atan", StandardBasicTypes.DOUBLE) );
		registerFunction( "cos", new StandardSQLFunction("cos", StandardBasicTypes.DOUBLE) );
		registerFunction( "cot", new StandardSQLFunction("cot", StandardBasicTypes.DOUBLE) );
		registerFunction( "exp", new StandardSQLFunction("exp", StandardBasicTypes.DOUBLE) );
		registerFunction( "ln", new StandardSQLFunction("ln", StandardBasicTypes.DOUBLE) );
		registerFunction( "log", new StandardSQLFunction("log", StandardBasicTypes.DOUBLE) );
		registerFunction( "sin", new StandardSQLFunction("sin", StandardBasicTypes.DOUBLE) );
		registerFunction( "sqrt", new StandardSQLFunction("sqrt", StandardBasicTypes.DOUBLE) );
		registerFunction( "cbrt", new StandardSQLFunction("cbrt", StandardBasicTypes.DOUBLE) );
		registerFunction( "tan", new StandardSQLFunction("tan", StandardBasicTypes.DOUBLE) );
		registerFunction( "radians", new StandardSQLFunction("radians", StandardBasicTypes.DOUBLE) );
		registerFunction( "degrees", new StandardSQLFunction("degrees", StandardBasicTypes.DOUBLE) );

		registerFunction( "random", new NoArgSQLFunction("random", StandardBasicTypes.DOUBLE) );

		registerFunction( "round", new StandardSQLFunction("round") );
		registerFunction( "trunc", new StandardSQLFunction("trunc") );
		registerFunction( "ceil", new StandardSQLFunction("ceil") );
		registerFunction( "floor", new StandardSQLFunction("floor") );

		registerFunction( "chr", new StandardSQLFunction("chr", StandardBasicTypes.CHARACTER) );
		registerFunction( "lower", new StandardSQLFunction("lower") );
		registerFunction( "upper", new StandardSQLFunction("upper") );
		registerFunction( "substr", new StandardSQLFunction("substr", StandardBasicTypes.STRING) );
		registerFunction( "initcap", new StandardSQLFunction("initcap") );
		registerFunction( "quote_ident", new StandardSQLFunction("quote_ident", StandardBasicTypes.STRING) );
		registerFunction( "quote_literal", new StandardSQLFunction("quote_literal", StandardBasicTypes.STRING) );
		registerFunction( "md5", new StandardSQLFunction("md5", StandardBasicTypes.STRING) );
		registerFunction( "ascii", new StandardSQLFunction("ascii", StandardBasicTypes.INTEGER) );
		registerFunction( "char_length", new StandardSQLFunction("char_length", StandardBasicTypes.LONG) );
		registerFunction( "bit_length", new StandardSQLFunction("bit_length", StandardBasicTypes.LONG) );
		registerFunction( "octet_length", new StandardSQLFunction("octet_length", StandardBasicTypes.LONG) );

		registerFunction( "age", new StandardSQLFunction("age") );
		registerFunction( "current_date", new NoArgSQLFunction("current_date", StandardBasicTypes.DATE, false) );
		registerFunction( "current_timestamp", new NoArgSQLFunction("current_timestamp", StandardBasicTypes.TIMESTAMP, false) );
		registerFunction( "date_trunc", new StandardSQLFunction( "date_trunc", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "now", new NoArgSQLFunction("now", StandardBasicTypes.TIMESTAMP) );

		registerFunction( "current_user", new NoArgSQLFunction("current_user", StandardBasicTypes.STRING, false) );
		registerFunction( "session_user", new NoArgSQLFunction("session_user", StandardBasicTypes.STRING, false) );
		registerFunction( "user", new NoArgSQLFunction("user", StandardBasicTypes.STRING, false) );
		registerFunction( "current_database", new NoArgSQLFunction("current_database", StandardBasicTypes.STRING, true) );
		registerFunction( "current_schema", new NoArgSQLFunction("current_schema", StandardBasicTypes.STRING, true) );

		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "(","||",")" ) );

		registerFunction( "locate", new PositionSubstringFunction() );

		registerFunction( "str", new SQLFunctionTemplate(StandardBasicTypes.STRING, "cast(?1 as varchar)") );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
		getDefaultProperties().setProperty( Environment.NON_CONTEXTUAL_LOB_CREATION, "true" );
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {

		SqlTypeDescriptor descriptor;
		switch (sqlCode) {
			case Types.BLOB: {
				// Make BLOBs use byte[] storage.
				descriptor = VarbinaryTypeDescriptor.INSTANCE;
				break;
			}
			case Types.CLOB: {
				// Make CLOBs use string storage.
				descriptor = VarcharTypeDescriptor.INSTANCE;
				break;
			}
			default: {
				descriptor = super.getSqlTypeDescriptorOverride(sqlCode);
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
	public String renderOrderByElement(String expression, String collation, String order, NullPrecedence nulls) {
		final StringBuilder orderByElement = new StringBuilder();
		if ( nulls != NullPrecedence.NONE ) {
			// Workaround for NULLS FIRST / LAST support.
			orderByElement.append( "case when " ).append( expression ).append( " is null then " );
			if ( nulls == NullPrecedence.FIRST ) {
				orderByElement.append( "0 else 1" );
			}
			else {
				orderByElement.append( "1 else 0" );
			}
			orderByElement.append( " end, " );
		}
		// Nulls precedence has already been handled so passing NONE value.
		orderByElement.append( super.renderOrderByElement( expression, collation, order, NullPrecedence.NONE ) );
		return orderByElement.toString();
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
		if ( aliases != null && aliases.isEmpty() ) {
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
	 * Constraint-name extractor for CockroachDB constraint violation exceptions.
	 */
	private static final ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {
		@Override
		protected String doExtractConstraintName(SQLException sqle) throws NumberFormatException {
			final int sqlState = Integer.parseInt( JdbcExceptionHelper.extractSqlState( sqle ) );
			switch (sqlState) {
				// CHECK VIOLATION
				case 23514: return extractUsingTemplate( "failed to satisfy CHECK constraint ","\"", sqle.getMessage() );
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
	public boolean supportsPooledSequences() {
		return true;
	}

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
		return false;
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
	public boolean supportsRowValueConstructorSyntax() {
		return true;
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public boolean supportsNationalizedTypes() {
		return false;
	}

	@Override
	public boolean supportsJdbcConnectionLobCreation(DatabaseMetaData databaseMetaData) {
		return false;
	}

	@Override
	public boolean supportsSelectAliasInGroupByClause() {
		return true;
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		// CockroachDB UUID type is the same as PostgreSQL.
		typeContributions.contributeType( PostgresUUIDType.INSTANCE );
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence if exists " + sequenceName;
	}

	@Override
	public boolean supportsValuesList() {
		return true;
	}

	public boolean supportsRowValueConstructorSyntaxInInList() {
		return true;
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}

	@Override
	public boolean supportsNonQueryWithCTE() {
		return true;
	}

	@Override
	public boolean supportsIfExistsAfterAlterTable() {
		return true;
	}


	@Override
	public String getForUpdateString() {
		return " for update";
	}

	@Override
	public String getWriteLockString(int timeout) {
		if ( timeout == LockOptions.SKIP_LOCKED ) {
			return getForUpdateSkipLockedString();
		}
		else {
			return super.getWriteLockString( timeout );
		}
	}

	@Override
	public String getWriteLockString(String aliases, int timeout) {
		if ( timeout == LockOptions.SKIP_LOCKED ) {
			return getForUpdateSkipLockedString( aliases );
		}
		else {
			return super.getWriteLockString( aliases, timeout );
		}
	}

	@Override
	public String getReadLockString(int timeout) {
		if ( timeout == LockOptions.SKIP_LOCKED ) {
			return " for share skip locked";
		}
		else {
			return super.getReadLockString( timeout );
		}
	}

	@Override
	public String getReadLockString(String aliases, int timeout) {
		if ( timeout == LockOptions.SKIP_LOCKED ) {
			return String.format( " for share of %s skip locked", aliases );
		}
		else {
			return super.getReadLockString( aliases, timeout );
		}
	}

	@Override
	public String getForUpdateSkipLockedString() {
		return " for update skip locked";
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		return getForUpdateString() + " of " + aliases + " skip locked";
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
	public boolean supportsLockTimeouts() {
		return false;
	}

	@Override
	public boolean supportsSkipLocked() {
		// CockroachDB 19.2.0 doesn't support this: https://github.com/cockroachdb/cockroach/issues/40476
		// A later version will add support, so the syntax for locks is added above.
		return false;
	}

	@Override
	public boolean supportsNoWait() {
		// CockroachDB 19.2.0 doesn't support this: https://github.com/cockroachdb/cockroach/issues/40476
		// A later version will add support, so the syntax for locks is added above.
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new CockroachDB1920IdentityColumnSupport();
	}

	@Override
	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		// A later version of CockroachDB will support the same temp table syntax as PostgreSQL.
		return new InlineIdsInClauseBulkIdStrategy();
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return true;
	}

	@Override
	public boolean canCreateSchema() {
		return false;
	}

	@Override
	public NameQualifierSupport getNameQualifierSupport() {
		// This method is overridden so the correct value will be returned when
		// DatabaseMetaData is not available.
		return NameQualifierSupport.SCHEMA;
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData)
			throws SQLException {

		if ( dbMetaData == null ) {
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.LOWER );
			builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}

		return super.buildIdentifierHelper( builder, dbMetaData );
	}
}
