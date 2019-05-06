/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.io.Serializable;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.identity.HSQLIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadSelectLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteSelectLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.model.domain.spi.Lockable;
import org.hibernate.naming.Identifier;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.spi.idtable.StandardIdTableSupport;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTable;
import org.hibernate.query.sqm.mutation.spi.idtable.LocalTempTableExporter;
import org.hibernate.query.sqm.mutation.spi.idtable.LocalTemporaryTableStrategy;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorHSQLDBDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import org.jboss.logging.Logger;

/**
 * An SQL dialect compatible with HSQLDB (HyperSQL).
 * <p/>
 * Note this version supports HSQLDB version 1.8 and higher, only.
 * <p/>
 * Enhancements to version 3.5.0 GA to provide basic support for both HSQLDB 1.8.x and 2.x
 * Does not works with Hibernate 3.2 - 3.4 without alteration.
 *
 * @author Christoph Sturm
 * @author Phillip Baird
 * @author Fred Toussi
 */
@SuppressWarnings("deprecation")
public class HSQLDialect extends Dialect {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			HSQLDialect.class.getName()
	);

	private final class HSQLLimitHandler extends AbstractLimitHandler {
		@Override
		public String processSql(String sql, RowSelection selection) {
			final boolean hasOffset = LimitHelper.hasFirstRow( selection );
			if ( hsqldbVersion < 200 ) {
				return new StringBuilder( sql.length() + 10 )
						.append( sql )
						.insert(
								sql.toLowerCase(Locale.ROOT).indexOf( "select" ) + 6,
								hasOffset ? " limit ? ?" : " top ?"
						)
						.toString();
			}
			else {
				return sql + (hasOffset ? " offset ? limit ?" : " limit ?");
			}
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}

		@Override
		public boolean bindLimitParametersFirst() {
			return hsqldbVersion < 200;
		}
	}

	/**
	 * version is 180 for 1.8.0 or 200 for 2.0.0
	 */
	private int hsqldbVersion = 180;
	private final LimitHandler limitHandler;


	/**
	 * Constructs a HSQLDialect
	 */
	public HSQLDialect() {
		super();

		try {
			final Class props = ReflectHelper.classForName( "org.hsqldb.persist.HsqlDatabaseProperties" );
			final String versionString = (String) props.getDeclaredField( "THIS_VERSION" ).get( null );

			hsqldbVersion = Integer.parseInt( versionString.substring( 0, 1 ) ) * 100;
			hsqldbVersion += Integer.parseInt( versionString.substring( 2, 3 ) ) * 10;
			hsqldbVersion += Integer.parseInt( versionString.substring( 4, 5 ) );
		}
		catch ( Throwable e ) {
			// must be a very old version
		}

		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.BINARY, "binary($l)" );
		registerColumnType( Types.BIT, "bit" );
		registerColumnType( Types.BOOLEAN, "boolean" );
		registerColumnType( Types.CHAR, "char($l)" );
		registerColumnType( Types.DATE, "date" );

		registerColumnType( Types.DECIMAL, "decimal($p,$s)" );
		registerColumnType( Types.DOUBLE, "double" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.LONGVARBINARY, "longvarbinary" );
		registerColumnType( Types.LONGVARCHAR, "longvarchar" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "tinyint" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.VARBINARY, "varbinary($l)" );
		registerColumnType( Types.NCLOB, "clob" );

		if ( hsqldbVersion < 200 ) {
			registerColumnType( Types.NUMERIC, "numeric" );
		}
		else {
			registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
		}

		//HSQL has no Blob/Clob support .... but just put these here for now!
		if ( hsqldbVersion < 200 ) {
			registerColumnType( Types.BLOB, "longvarbinary" );
			registerColumnType( Types.CLOB, "longvarchar" );
		}
		else {
			registerColumnType( Types.BLOB, "blob($l)" );
			registerColumnType( Types.CLOB, "clob($l)" );
		}

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );

		limitHandler = new HSQLLimitHandler();
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		// string functions
		queryEngine.getSqmFunctionRegistry().registerNamed( "ascii", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "char", StandardSpiBasicTypes.CHARACTER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "soundex", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "ltrim", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "rtrim", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "reverse", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "space", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerPattern( "str", "cast(?1 as varchar(256))", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "to_char", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "rawtohex" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "hextoraw" );

		// system functions
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "user", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "database", StandardSpiBasicTypes.STRING );

		// datetime functions
		if ( hsqldbVersion < 200 ) {
			queryEngine.getSqmFunctionRegistry().registerNoArgs( "sysdate", StandardSpiBasicTypes.DATE );
		}
		else {
			queryEngine.getSqmFunctionRegistry().registerNoArgs( "sysdate", StandardSpiBasicTypes.TIMESTAMP );
		}
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "now", StandardSpiBasicTypes.TIMESTAMP );
		queryEngine.getSqmFunctionRegistry().registerNamed( "day", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dayofweek", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dayofyear", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dayofmonth", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "month", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "year", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "week", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "quarter", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "hour", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "minute", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerPattern( "second", "cast(second(?1) as int)", StandardSpiBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().registerNamed( "dayname", StandardSpiBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerNamed( "monthname", StandardSpiBasicTypes.STRING );

		// numeric functions
		queryEngine.getSqmFunctionRegistry().registerNamed( "acos", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "asin", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "atan", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "cos", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "cot", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "log", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "log10", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "sin", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "tan", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "pi", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "rand", StandardSpiBasicTypes.FLOAT );

		queryEngine.getSqmFunctionRegistry().registerNamed( "radians", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "degrees", StandardSpiBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().registerNamed( "round" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "roundmagic" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "truncate" );
		queryEngine.getSqmFunctionRegistry().registerNamed( "trunc" );

		// special functions
		// from v. 2.2.0 ROWNUM() is supported in all modes as the equivalent of Oracle ROWNUM
		if ( hsqldbVersion > 219 ) {
			queryEngine.getSqmFunctionRegistry().registerNoArgs( "rownum", StandardSpiBasicTypes.INTEGER );
		}

		// function templates
		queryEngine.getSqmFunctionRegistry().registerVarArgs( "concat", StandardSpiBasicTypes.STRING, "(", "||", ")" );
	}

	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public boolean supportsLockTimeouts() {
		return false;
	}

	@Override
	public String getForUpdateString() {
		if ( hsqldbVersion >= 200 ) {
			return " for update";
		}
		else {
			return "";
		}
	}

	@Override
	public LimitHandler getLimitHandler() {
		return limitHandler;
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public String getLimitString(String sql, boolean hasOffset) {
		if ( hsqldbVersion < 200 ) {
			return new StringBuilder( sql.length() + 10 )
					.append( sql )
					.insert(
							sql.toLowerCase(Locale.ROOT).indexOf( "select" ) + 6,
							hasOffset ? " limit ? ?" : " top ?"
					)
					.toString();
		}
		else {
			return sql + (hasOffset ? " offset ? limit ?" : " limit ?");
		}
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return hsqldbVersion < 200;
	}

	// Note : HSQLDB actually supports [IF EXISTS] before AND after the <tablename>
	// But as CASCADE has to be AFTER IF EXISTS in case it's after the tablename,
	// We put the IF EXISTS before the tablename to be able to add CASCADE after.
	@Override
	public boolean supportsIfExistsAfterTableName() {
		return false;
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public boolean supportsColumnCheck() {
		return hsqldbVersion >= 200;
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public boolean supportsPooledSequences() {
		return true;
	}

	/**
	 * HSQL will start with 0, by default.  In order for Hibernate to know that this not transient,
	 * manually start with 1.
	 */
	@Override
	protected String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName + " start with 1";
	}
	
	/**
	 * Because of the overridden {@link #getCreateSequenceString(String)}, we must also override
	 * {@link #getCreateSequenceString(String, int, int)} to prevent 2 instances of "start with".
	 */
	@Override
	protected String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) throws MappingException {
		if ( supportsPooledSequences() ) {
			return "create sequence " + sequenceName + " start with " + initialValue + " increment by " + incrementSize;
		}
		throw new MappingException( getClass().getName() + " does not support pooled sequences" );
	}

	@Override
	protected String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName + " if exists";
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
		// this assumes schema support, which is present in 1.8.0 and later...
		return "select * from information_schema.sequences";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorHSQLDBDatabaseImpl.INSTANCE;
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return hsqldbVersion < 200 ? EXTRACTER_18 : EXTRACTER_20;
	}

	private static final ViolatedConstraintNameExtracter EXTRACTER_18 = new TemplatedViolatedConstraintNameExtracter() {
		@Override
		protected String doExtractConstraintName(SQLException sqle) throws NumberFormatException {
			String constraintName = null;

			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqle );

			if ( errorCode == -8 ) {
				constraintName = extractUsingTemplate(
						"Integrity constraint violation ", " table:", sqle.getMessage()
				);
			}
			else if ( errorCode == -9 ) {
				constraintName = extractUsingTemplate(
						"Violation of unique index: ", " in statement [", sqle.getMessage()
				);
			}
			else if ( errorCode == -104 ) {
				constraintName = extractUsingTemplate(
						"Unique constraint violation: ", " in statement [", sqle.getMessage()
				);
			}
			else if ( errorCode == -177 ) {
				constraintName = extractUsingTemplate(
						"Integrity constraint violation - no parent ", " table:",
						sqle.getMessage()
				);
			}
			return constraintName;
		}

	};

	/**
	 * HSQLDB 2.0 messages have changed
	 * messages may be localized - therefore use the common, non-locale element " table: "
	 */
	private static final ViolatedConstraintNameExtracter EXTRACTER_20 = new TemplatedViolatedConstraintNameExtracter() {
		@Override
		protected String doExtractConstraintName(SQLException sqle) throws NumberFormatException {
			String constraintName = null;

			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqle );

			if ( errorCode == -8 ) {
				constraintName = extractUsingTemplate(
						"; ", " table: ", sqle.getMessage()
				);
			}
			else if ( errorCode == -9 ) {
				constraintName = extractUsingTemplate(
						"; ", " table: ", sqle.getMessage()
				);
			}
			else if ( errorCode == -104 ) {
				constraintName = extractUsingTemplate(
						"; ", " table: ", sqle.getMessage()
				);
			}
			else if ( errorCode == -177 ) {
				constraintName = extractUsingTemplate(
						"; ", " table: ", sqle.getMessage()
				);
			}
			return constraintName;
		}
	};

	@Override
	public String getSelectClauseNullString(int sqlType) {
		String literal;
		switch ( sqlType ) {
			case Types.LONGVARCHAR:
			case Types.VARCHAR:
			case Types.CHAR:
				literal = "cast(null as varchar(100))";
				break;
			case Types.LONGVARBINARY:
			case Types.VARBINARY:
			case Types.BINARY:
				literal = "cast(null as varbinary(100))";
				break;
			case Types.CLOB:
				literal = "cast(null as clob)";
				break;
			case Types.BLOB:
				literal = "cast(null as blob)";
				break;
			case Types.DATE:
				literal = "cast(null as date)";
				break;
			case Types.TIMESTAMP:
				literal = "cast(null as timestamp)";
				break;
			case Types.BOOLEAN:
				literal = "cast(null as boolean)";
				break;
			case Types.BIT:
				literal = "cast(null as bit)";
				break;
			case Types.TIME:
				literal = "cast(null as time)";
				break;
			default:
				literal = "cast(null as int)";
		}
		return literal;
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	@Override
	public SqmMutationStrategy getDefaultIdTableStrategy() {
		// Hibernate uses this information for temporary tables that it uses for its own operations
		// therefore the appropriate strategy is taken with different versions of HSQLDB

		// All versions of HSQLDB support GLOBAL TEMPORARY tables where the table
		// definition is shared by all users but data is private to the session
		// HSQLDB 2.0 also supports session-based LOCAL TEMPORARY tables where
		// the definition and data is private to the session and table declaration
		// can happen in the middle of a transaction

		if ( hsqldbVersion < 200 ) {
			return new GlobalTemporaryTableStrategy();
		}
		else {
			return new LocalTemporaryTableStrategy(
					new StandardIdTableSupport( generateLocalTempTableExporter() ) {
						@Override
						protected Identifier determineIdTableName(Identifier baseName) {
							// With HSQLDB 2.0, the table name is qualified with MODULE to assist the drop
							// statement (in-case there is a global name beginning with HT_)
							return new Identifier( "MODULE.HT_" + baseName.getText(), false );
						}
					}
			);
		}
	}

	private Exporter<IdTable> generateLocalTempTableExporter() {
		return new LocalTempTableExporter() {
			@Override
			protected String getCreateCommand() {
				return "declare local temporary table";
			}
		};
	}

	// current timestamp support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * HSQLDB 1.8.x requires CALL CURRENT_TIMESTAMP but this should not
	 * be treated as a callable statement. It is equivalent to
	 * "select current_timestamp from dual" in some databases.
	 * HSQLDB 2.0 also supports VALUES CURRENT_TIMESTAMP
	 * <p/>
	 * {@inheritDoc}
	 */
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
		return "call current_timestamp";
	}

	@Override
	public String getCurrentTimestampSQLFunctionName() {
		// the standard SQL function name is current_timestamp...
		return "current_timestamp";
	}

	/**
	 * For HSQLDB 2.0, this is a copy of the base class implementation.
	 * For HSQLDB 1.8, only READ_UNCOMMITTED is supported.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		if ( lockMode == LockMode.PESSIMISTIC_FORCE_INCREMENT ) {
			return new PessimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.PESSIMISTIC_WRITE ) {
			return new PessimisticWriteSelectLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.PESSIMISTIC_READ ) {
			return new PessimisticReadSelectLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.OPTIMISTIC ) {
			return new OptimisticLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.OPTIMISTIC_FORCE_INCREMENT ) {
			return new OptimisticForceIncrementLockingStrategy( lockable, lockMode );
		}

		if ( hsqldbVersion < 200 ) {
			return new ReadUncommittedLockingStrategy( lockable, lockMode );
		}
		else {
			return new SelectLockingStrategy( lockable, lockMode );
		}
	}

	private static class ReadUncommittedLockingStrategy extends SelectLockingStrategy {
		public ReadUncommittedLockingStrategy(Lockable lockable, LockMode lockMode) {
			super( lockable, lockMode );
		}

		@Override
		public void lock(Serializable id, Object version, Object object, int timeout, SharedSessionContractImplementor session)
				throws StaleObjectStateException, JDBCException {
			if ( getLockMode().greaterThan( LockMode.READ ) ) {
				LOG.hsqldbSupportsOnlyReadCommittedIsolation();
			}
			super.lock( id, version, object, timeout, session );
		}
	}

	@Override
	public boolean supportsCommentOn() {
		return hsqldbVersion >= 200;
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean requiresCastingOfParametersInSelectClause() {
		return true;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return hsqldbVersion >= 200;
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return hsqldbVersion >= 200;
	}

	@Override
	public boolean supportsLobValueChangePropogation() {
		return false;
	}

	@Override
	public String toBooleanValueString(boolean bool) {
		return String.valueOf( bool );
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		// from v. 2.2.9 is added support for COUNT(DISTINCT ...) with multiple arguments
		return hsqldbVersion >= 229;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new HSQLIdentityColumnSupport( this.hsqldbVersion );
	}

	@Override
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.SCHEMA;
	}

	@Override
	public boolean supportsNamedParameters(DatabaseMetaData databaseMetaData) throws SQLException {
		return false;
	}

	// Do not drop constraints explicitly, just do this by cascading instead.
	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getCascadeConstraintsString() {
		return " CASCADE ";
	}
}
