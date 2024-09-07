/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.LockMode;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.cfg.Environment;
import org.hibernate.community.dialect.identity.CacheIdentityColumnSupport;
import org.hibernate.community.dialect.sequence.CacheSequenceSupport;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SimpleDatabaseVersion;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadUpdateLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteUpdateLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.dialect.lock.UpdateLockingStrategy;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.TopLimitHandler;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import jakarta.persistence.TemporalType;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;

/**
 * Dialect for Intersystems Cach&eacute; SQL 2007.1 and above.
 *
 * @author Jonathan Levinson
 */
public class CacheDialect extends Dialect {

	public CacheDialect() {
		super( SimpleDatabaseVersion.ZERO_VERSION );
	}

	public CacheDialect(DialectResolutionInfo info) {
		super( info );
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		// Note: For object <-> SQL datatype mappings see:
		// Configuration Manager > Advanced > SQL > System DDL Datatype Mappings
		switch ( sqlTypeCode ) {
			case BOOLEAN:
				return "bit";
			//no explicit precision
			case TIMESTAMP:
			case TIMESTAMP_WITH_TIMEZONE:
				return "timestamp";
			case BLOB:
				return "image";
			case CLOB:
				return "text";
			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	protected void initDefaultProperties() {
		super.initDefaultProperties();
		getDefaultProperties().setProperty( Environment.USE_SQL_COMMENTS, "false" );
	}

	@Override
	public int getDefaultStatementBatchSize() {
		return 15;
	}

	private static void useJdbcEscape(FunctionContributions queryEngine, String name) {
		//Yep, this seems to be truly necessary for certain functions
		queryEngine.getFunctionRegistry().wrapInJdbcEscape(
				name,
				queryEngine.getFunctionRegistry().findFunctionDescriptor(name)
		);
	}

	@Override
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		if ( jdbcTypeCode == Types.BIT ) {
			return jdbcTypeRegistry.getDescriptor( Types.BOOLEAN );
		}
		return super.resolveSqlTypeDescriptor(
				columnTypeName,
				jdbcTypeCode,
				precision,
				scale,
				jdbcTypeRegistry
		);
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BIT;
	}

	@Override
	public int getDefaultDecimalPrecision() {
		//the largest *meaningful* value
		return 19;
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.repeat();
		functionFactory.trim2();
		functionFactory.substr();
		//also natively supports ANSI-style substring()
		functionFactory.concat_pipeOperator();
		functionFactory.cot();
		functionFactory.log10();
		functionFactory.log();
		functionFactory.pi();
		functionFactory.space();
		functionFactory.hourMinuteSecond();
		functionFactory.yearMonthDay();
		functionFactory.weekQuarter();
		functionFactory.daynameMonthname();
		functionFactory.toCharNumberDateTimestamp();
		functionFactory.trunc_truncate();
		functionFactory.dayofweekmonthyear();
		functionFactory.repeat_replicate();
		functionFactory.datepartDatename();
		functionFactory.ascii();
		functionFactory.chr_char();
		functionFactory.nowCurdateCurtime();
		functionFactory.sysdate();
		functionFactory.stddev();
		functionFactory.stddevPopSamp();
		functionFactory.variance();
		functionFactory.varPopSamp();
		functionFactory.lastDay();

		functionContributions.getFunctionRegistry().registerBinaryTernaryPattern(
				"locate",
				functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ),
				"$find(?2,?1)",
				"$find(?2,?1,?3)",
				STRING, STRING, INTEGER,
				functionContributions.getTypeConfiguration()
		).setArgumentListSignature("(pattern, string[, start])");
		functionFactory.bitLength_pattern( "($length(?1)*8)" );

		useJdbcEscape(functionContributions, "sin");
		useJdbcEscape(functionContributions, "cos");
		useJdbcEscape(functionContributions, "tan");
		useJdbcEscape(functionContributions, "asin");
		useJdbcEscape(functionContributions, "acos");
		useJdbcEscape(functionContributions, "atan");
		useJdbcEscape(functionContributions, "atan2");
		useJdbcEscape(functionContributions, "exp");
		useJdbcEscape(functionContributions, "log");
		useJdbcEscape(functionContributions, "log10");
		useJdbcEscape(functionContributions, "pi");
		useJdbcEscape(functionContributions, "truncate");

		useJdbcEscape(functionContributions, "left");
		useJdbcEscape(functionContributions, "right");

		useJdbcEscape(functionContributions, "hour");
		useJdbcEscape(functionContributions, "minute");
		useJdbcEscape(functionContributions, "second");
		useJdbcEscape(functionContributions, "week");
		useJdbcEscape(functionContributions, "quarter");
		useJdbcEscape(functionContributions, "dayname");
		useJdbcEscape(functionContributions, "monthname");
		useJdbcEscape(functionContributions, "dayofweek");
		useJdbcEscape(functionContributions, "dayofmonth");
		useJdbcEscape(functionContributions, "dayofyear");

	}

	@Override
	public String extractPattern(TemporalUnit unit) {
		return "datepart(?1,?2)";
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		switch (unit) {
			case NANOSECOND:
			case NATIVE:
				return "dateadd(millisecond,(?2)/1e6,?3)";
			default:
				return "dateadd(?1,?2,?3)";
		}
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		switch (unit) {
			case NANOSECOND:
			case NATIVE:
				return "datediff(millisecond,?2,?3)*1e6";
			default:
				return "datediff(?1,?2,?3)";
		}
	}

	// DDL support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean qualifyIndexName() {
		// Do we need to qualify index names with the schema name?
		return false;
	}

	@Override
	@SuppressWarnings("StringBufferReplaceableByString")
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		// The syntax used to add a foreign key constraint to a table.
		return new StringBuilder( 300 )
				.append( " ADD CONSTRAINT " )
				.append( constraintName )
				.append( " FOREIGN KEY " )
				.append( constraintName )
				.append( " (" )
				.append( String.join( ", ", foreignKey ) )
				.append( ") REFERENCES " )
				.append( referencedTable )
				.append( " (" )
				.append( String.join( ", ", primaryKey ) )
				.append( ") " )
				.toString();
	}

	@Override
	public boolean hasSelfReferentialForeignKeyBug() {
		return true;
	}


	@Override
	public String getNativeIdentifierGeneratorStrategy() {
		return "identity";
	}

	// IDENTITY support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return CacheIdentityColumnSupport.INSTANCE;
	}

	// SEQUENCE support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public SequenceSupport getSequenceSupport() {
		return CacheSequenceSupport.INSTANCE;
	}

	public String getQuerySequencesString() {
		return "select name from InterSystems.Sequences";
	}

	// lock acquisition support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public LockingStrategy getLockingStrategy(EntityPersister lockable, LockMode lockMode) {
		// InterSystems Cache' does not current support "SELECT ... FOR UPDATE" syntax...
		// Set your transaction mode to READ_COMMITTED before using
		switch (lockMode) {
			case PESSIMISTIC_FORCE_INCREMENT:
				return new PessimisticForceIncrementLockingStrategy(lockable, lockMode);
			case PESSIMISTIC_WRITE:
				return new PessimisticWriteUpdateLockingStrategy(lockable, lockMode);
			case PESSIMISTIC_READ:
				return new PessimisticReadUpdateLockingStrategy(lockable, lockMode);
			case OPTIMISTIC:
				return new OptimisticLockingStrategy(lockable, lockMode);
			case OPTIMISTIC_FORCE_INCREMENT:
				return new OptimisticForceIncrementLockingStrategy(lockable, lockMode);
		}
		if ( lockMode.greaterThan( LockMode.READ ) ) {
			return new UpdateLockingStrategy( lockable, lockMode );
		}
		else {
			return new SelectLockingStrategy( lockable, lockMode );
		}
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new CacheSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	// LIMIT support (ala TOP) ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public LimitHandler getLimitHandler() {
		return TopLimitHandler.INSTANCE;
	}

	// callable statement support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) {
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		ps.execute();
		return (ResultSet) ps.getObject( 1 );
	}

	// miscellaneous support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public String getNullColumnString() {
		// The keyword used to specify a nullable column.
		return " null";
	}

	@Override
	public String getNoColumnsInsertString() {
		// The keyword used to insert a row without specifying
		// any column values
		return " default values";
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			String sqlStateClassCode = JdbcExceptionHelper.extractSqlStateClassCode( sqlException );
			if ( sqlStateClassCode != null ) {
				int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
				if ( errorCode >= 119 && errorCode <= 127 && errorCode != 126 ) {
					final String constraintName = getViolatedConstraintNameExtractor().extractConstraintName(sqlException);
					return new ConstraintViolationException( message, sqlException, sql, constraintName );
				}

				if ( sqlStateClassCode.equals("22")
						|| sqlStateClassCode.equals("21")
						|| sqlStateClassCode.equals("02") ) {
					return new DataException( message, sqlException, sql );
				}
			}
			return null; // allow other delegates the chance to look
		};
	}

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle ->
					extractUsingTemplate( "constraint (", ") violated", sqle.getMessage() )
			);


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsOrderByInSubquery() {
		// This is just a guess
		return false;
	}

	@Override
	public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		return false;
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		//I don't think Cache needs FM
		appender.appendSql( OracleDialect.datetimeFormat( format, false, false ).result() );
	}

}
