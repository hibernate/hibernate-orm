/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import java.sql.Types;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.community.dialect.pagination.TimesTenLimitHandler;
import org.hibernate.community.dialect.sequence.SequenceInformationExtractorTimesTenDatabaseImpl;
import org.hibernate.community.dialect.sequence.TimesTenSequenceSupport;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadUpdateLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteUpdateLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.dialect.lock.UpdateLockingStrategy;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.TemporalUnit;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.CurrentFunction;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import jakarta.persistence.GenerationType;
import java.util.Date;

import jakarta.persistence.TemporalType;

import static org.hibernate.dialect.SimpleDatabaseVersion.ZERO_VERSION;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

/**
 * A SQL dialect for Oracle TimesTen
 * <p>
 * Known limitations:
 * joined-subclass support because of no CASE support in TimesTen
 * No support for subqueries that includes aggregation
 * - size() in HQL not supported
 * - user queries that does subqueries with aggregation
 * No cascade delete support.
 * No Calendar support
 * No support for updating primary keys.
 *
 * @author Sherry Listgarten, Max Andersen, Chris Jenkins
 */
public class TimesTenDialect extends Dialect {

	public TimesTenDialect() {
		super( ZERO_VERSION );
	}

	public TimesTenDialect(DialectResolutionInfo info) {
		super( info );
	}

	/*
	 * Copyright (c) 2025, Oracle and/or its affiliates.
	 * Licensed under the Universal Permissive License v 1.0 as shown
	 * at http://oss.oracle.com/licenses/upl
	 *
	 * - Added more datatypes into columnType():
   *     BIT, CHAR, VARCHAR, LONGVARCHAR, BINARY, VARBINARY
   *     LONGVARBINARY, BINARY_FLOAT, BINARY_DOUBLE, TIMESTAMP,
   *     BLOB, CLOB, NCLOB
	 * 
   * @Author: Carlos Blanco
	*/
	@Override
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			//Note: these are the correct type mappings
			//      for the default Oracle type mode
			//      TypeMode=0
			case SqlTypes.BOOLEAN:
			case SqlTypes.BIT:
			case SqlTypes.TINYINT:
				return "TT_TINYINT";
			case SqlTypes.SMALLINT:
				return "TT_SMALLINT";
			case SqlTypes.INTEGER:
				return "TT_INTEGER";
			case SqlTypes.BIGINT:
				return "TT_BIGINT";
			//note that 'binary_float'/'binary_double' might
			//be better mappings for Java Float/Double

			case SqlTypes.CHAR:
				return "CHAR($l)";
			case SqlTypes.VARCHAR:
			case SqlTypes.LONGVARCHAR:
				return "VARCHAR2($l)";

			case SqlTypes.BINARY:
				return "BINARY($l)";
			case SqlTypes.VARBINARY:
			case SqlTypes.LONGVARBINARY:
				return "VARBINARY($l)";

			//'numeric'/'decimal' are synonyms for 'number'
			case SqlTypes.NUMERIC:
			case SqlTypes.DECIMAL:
				return "NUMBER($p,$s)";
			case SqlTypes.FLOAT:
				return "BINARY_FLOAT";
			case SqlTypes.DOUBLE:
				return "BINARY_DOUBLE";

			case SqlTypes.DATE:
				return "TT_DATE";
			case SqlTypes.TIME:
				return "TT_TIME";
			case SqlTypes.TIMESTAMP:
				return "TIMESTAMP";
			//`timestamp` has more precision than `tt_timestamp`
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
				return "timestamp($p)";

			case SqlTypes.BLOB:
				return "BLOB";
			case SqlTypes.CLOB:
				return "CLOB";
			case SqlTypes.NCLOB:
				return "NCLOB";

			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	public int getDefaultStatementBatchSize() {
		return 15;
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
		//the maximum
		return 40;
	}

	/*
	 * Copyright (c) 2025, Oracle and/or its affiliates.
	 * Licensed under the Universal Permissive License v 1.0 as shown
	 * at http://oss.oracle.com/licenses/upl
	 *
	 * - Added more SQL functions support into initializeFunctionRegistry():
   *     lower, upper, rtrim, ltrim, length, to_char, chr, instr, instrb,
   *     lpad, rpad, substr, substrb, substring, str, soundex, replace,
   *     to_date, sysdate, getdate, current_date, current_time, current_timestamp,
   *     to_timestamp, add_months, months_between, abs, acos, asin, atan, atan2,
   *     ceil, cos, cosh, exp, ln, sin, sign, sinh, mod, round, trunc, tan, tanh,
   *     floor, power, bitnot, bitor, bitxor, nvl, user, rowid, uid, rownum, 
   *     vsize, SESSION_USER, SYSTEM_USER, CURRENT_USER
   *
   * @Author: Carlos Blanco
	*/
	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
		CommonFunctionFactory functionFactory     = new CommonFunctionFactory(functionContributions);
		final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		final BasicType<Date>   timestampType     = basicTypeRegistry.resolve( StandardBasicTypes.TIMESTAMP );
		final BasicType<Date>   dateType          = basicTypeRegistry.resolve( StandardBasicTypes.DATE );
		final BasicType<Date>   timeType          = basicTypeRegistry.resolve( StandardBasicTypes.TIME );
		final BasicType<String> stringType        = basicTypeRegistry.resolve( StandardBasicTypes.STRING );
		final BasicType<Long>   longType          = basicTypeRegistry.resolve( StandardBasicTypes.LONG );
		final BasicType<Integer>intType           = basicTypeRegistry.resolve( StandardBasicTypes.INTEGER );

		// String Functions
		functionContributions.getFunctionRegistry().register( 
				"lower", new StandardSQLFunction("lower") 
		);
		functionContributions.getFunctionRegistry().register( 
				"upper", new StandardSQLFunction("upper") 
		);
		functionContributions.getFunctionRegistry().register( 
				"rtrim", new StandardSQLFunction("rtrim") 
		);
		functionContributions.getFunctionRegistry().register( 
				"ltrim", new StandardSQLFunction("ltrim") 
		);
		functionContributions.getFunctionRegistry().register( 
				"length", new StandardSQLFunction("length", StandardBasicTypes.LONG)
		);
		functionFactory.concat_pipeOperator();
		functionContributions.getFunctionRegistry().register( 
				"to_char", new StandardSQLFunction("to_char", StandardBasicTypes.STRING)
		);
		functionContributions.getFunctionRegistry().register( 
				"chr", new StandardSQLFunction("chr", StandardBasicTypes.CHARACTER)
		);
		functionContributions.getFunctionRegistry().register( 
				"instr", new StandardSQLFunction("instr", StandardBasicTypes.INTEGER) 
		);
		functionContributions.getFunctionRegistry().register( 
				"instrb", new StandardSQLFunction("instrb", StandardBasicTypes.INTEGER)
		);
		functionContributions.getFunctionRegistry().register( 
				"lpad", new StandardSQLFunction("lpad", StandardBasicTypes.STRING)
		);
		functionContributions.getFunctionRegistry().register( 
				"rpad", new StandardSQLFunction("rpad", StandardBasicTypes.STRING)
		);
		functionContributions.getFunctionRegistry().register( 
				"substr", new StandardSQLFunction("substr", StandardBasicTypes.STRING)
		);
		functionContributions.getFunctionRegistry().register( 
				"substrb", new StandardSQLFunction("substrb", StandardBasicTypes.STRING) 
		);
		functionContributions.getFunctionRegistry().register( 
				"substring", new StandardSQLFunction( "substr", StandardBasicTypes.STRING )
		);
		functionFactory.locate();
		functionContributions.getFunctionRegistry().register( 
				"str", new StandardSQLFunction("to_char", StandardBasicTypes.STRING)
		);
		functionContributions.getFunctionRegistry().register( 
				"soundex", new StandardSQLFunction("soundex") 
		);
		functionContributions.getFunctionRegistry().register( 
				"replace", new StandardSQLFunction("replace", StandardBasicTypes.STRING)
		);

		// Date/Time Functions
		functionContributions.getFunctionRegistry().register( 
				"to_date", new StandardSQLFunction("to_date", StandardBasicTypes.TIMESTAMP)
		);
		functionContributions.getFunctionRegistry().register( 
				"sysdate", new CurrentFunction("sysdate", "sysdate", timestampType)
		);
		functionContributions.getFunctionRegistry().register( 
				"getdate", new StandardSQLFunction("getdate", StandardBasicTypes.TIMESTAMP)
		);

		functionContributions.getFunctionRegistry().register( 
				"current_date",      new CurrentFunction("sysdate", "sysdate", dateType) 
		);
		functionContributions.getFunctionRegistry().register( 
				"current_time",      new CurrentFunction("sysdate", "sysdate", timeType) 
		);
		functionContributions.getFunctionRegistry().register( 
				"current_timestamp", new CurrentFunction("sysdate", "sysdate", timestampType) 
		);
		functionContributions.getFunctionRegistry().register( 
				"to_timestamp", new StandardSQLFunction("to_timestamp", StandardBasicTypes.TIMESTAMP)
		);

		// Multi-param date dialect functions
		functionContributions.getFunctionRegistry().register( 
				"add_months",     new StandardSQLFunction("add_months", StandardBasicTypes.DATE)
		);
		functionContributions.getFunctionRegistry().register( 
				"months_between", new StandardSQLFunction("months_between", StandardBasicTypes.FLOAT)
		);

		// Math functions
		functionContributions.getFunctionRegistry().register( 
				"abs", new StandardSQLFunction("abs")
		);
		functionContributions.getFunctionRegistry().register( 
				"acos", new StandardSQLFunction("acos")
		);
		functionContributions.getFunctionRegistry().register( 
				"asin", new StandardSQLFunction("asin")
		);
		functionContributions.getFunctionRegistry().register( 
				"atan", new StandardSQLFunction("atan")
		);
		functionContributions.getFunctionRegistry().register( 
				"atan2", new StandardSQLFunction("atan2")
		);
		functionContributions.getFunctionRegistry().register( 
				"ceil", new StandardSQLFunction("ceil")
		);
		functionContributions.getFunctionRegistry().register( 
				"cos", new StandardSQLFunction("cos")
		);
		functionContributions.getFunctionRegistry().register( 
				"cosh", new StandardSQLFunction("cosh")
		);
		functionContributions.getFunctionRegistry().register( 
				"exp", new StandardSQLFunction("exp")
		);
		functionContributions.getFunctionRegistry().register( 
				"ln", new StandardSQLFunction("ln")
		);
		functionFactory.log();
		functionContributions.getFunctionRegistry().register( 
				"sin", new StandardSQLFunction("sin")
		);
		functionContributions.getFunctionRegistry().register( 
				"sign", new StandardSQLFunction("sign", StandardBasicTypes.INTEGER)
		);
		functionContributions.getFunctionRegistry().register( 
				"sinh", new StandardSQLFunction("sinh")
		);
		functionContributions.getFunctionRegistry().register( 
				"mod", new StandardSQLFunction("mod")
		);
		functionContributions.getFunctionRegistry().register( 
				"round", new StandardSQLFunction("round")
		);
		functionContributions.getFunctionRegistry().register( 
				"trunc", new StandardSQLFunction("trunc")
		);
		functionContributions.getFunctionRegistry().register( 
				"tan", new StandardSQLFunction("tan")
		);
		functionContributions.getFunctionRegistry().register( 
				"tanh", new StandardSQLFunction("tanh")
		);
		functionContributions.getFunctionRegistry().register( 
				"floor", new StandardSQLFunction("floor")
		);
		functionContributions.getFunctionRegistry().register( 
				"power", new StandardSQLFunction("power", StandardBasicTypes.FLOAT)
		);

		// Bitwise functions
		functionFactory.bitand();
		functionContributions.getFunctionRegistry().register( 
				"bitnot", new StandardSQLFunction("bitnot")
		);

		functionContributions.getFunctionRegistry()
				.patternDescriptorBuilder( "bitor", "(?1+?2-bitand(?1,?2))")
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers
				.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();

		functionContributions.getFunctionRegistry()
				.patternDescriptorBuilder( "bitxor", "(?1+?2-2*bitand(?1,?2))")
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers
				.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();

		// Misc. functions
		functionContributions.getFunctionRegistry().register( 
				"nvl", new StandardSQLFunction("nvl")
		);
		functionFactory.coalesce();
		functionContributions.getFunctionRegistry().register( 
				"user",  new CurrentFunction("user", "user", stringType)
		);
		functionContributions.getFunctionRegistry().register( 
				"rowid", new CurrentFunction("rowid", "rowid", stringType)
		);
		functionContributions.getFunctionRegistry().register( 
				"uid", new CurrentFunction("uid", "uid", intType)
		);
		functionContributions.getFunctionRegistry().register( 
				"rownum", new CurrentFunction("rownum", "rownum", longType)
		);
		functionContributions.getFunctionRegistry().register( 
				"vsize", new StandardSQLFunction("vsize")
		);
		functionContributions.getFunctionRegistry().register( 
				"SESSION_USER", new CurrentFunction("SESSION_USER","SESSION_USER", stringType)
		);
		functionContributions.getFunctionRegistry().register( 
				"SYSTEM_USER",  new CurrentFunction("SYSTEM_USER", "SYSTEM_USER",  stringType)
		);
		functionContributions.getFunctionRegistry().register( 
				"CURRENT_USER", new CurrentFunction("CURRENT_USER","CURRENT_USER", stringType)
		);

		functionContributions.getFunctionRegistry().registerBinaryTernaryPattern(
				"locate",
				functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ),
				"instr(?2,?1)",
				"instr(?2,?1,?3)",
				STRING, STRING, INTEGER,
				functionContributions.getTypeConfiguration()
		).setArgumentListSignature("(pattern, string[, start])");
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new TimesTenSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		switch (unit) {
			case NANOSECOND:
			case NATIVE:
				return "timestampadd(sql_tsi_frac_second,?2,?3)";
			default:
				return "timestampadd(sql_tsi_?1,?2,?3)";
		}
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		switch (unit) {
			case NANOSECOND:
			case NATIVE:
				return "timestampdiff(sql_tsi_frac_second,?2,?3)";
			default:
				return "timestampdiff(sql_tsi_?1,?2,?3)";
		}
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return TimesTenSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return "select name from sys.sequences";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorTimesTenDatabaseImpl.INSTANCE;
	}

	@Override
	public boolean supportsNoWait() {
		return true;
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return RowLockStrategy.COLUMN;
	}

  
	/*
	* Copyright (c) 2025, Oracle and/or its affiliates.
	* Licensed under the Universal Permissive License v 1.0 as shown
	* at http://oss.oracle.com/licenses/upl
	*
	* - Updated the custom definition for 'getForUpdateString()'
	*
  * @Author: Carlos Blanco
	*/
	@Override
	public String getForUpdateString() {
		return " for update";
	}

	@Override
	public String getForUpdateNowaitString() {
		return " for update nowait";
	}

	@Override
	public String getWriteLockString(int timeout) {
		return withTimeout( getForUpdateString(), timeout );
	}

	@Override
	public String getWriteLockString(String aliases, int timeout) {
		return withTimeout( getForUpdateString(aliases), timeout );
	}

	@Override
	public String getReadLockString(int timeout) {
		return getWriteLockString( timeout );
	}

	@Override
	public String getReadLockString(String aliases, int timeout) {
		return getWriteLockString( aliases, timeout );
	}


	private String withTimeout(String lockString, int timeout) {
		switch (timeout) {
			case LockOptions.NO_WAIT:
				return supportsNoWait() ? lockString + " nowait" : lockString;
			case LockOptions.SKIP_LOCKED:
			case LockOptions.WAIT_FOREVER:
				return lockString;
			default:
				return supportsWait() ? lockString + " wait " + getTimeoutInSeconds( timeout ) : lockString;
		}
	}

	@Override
	public boolean supportsColumnCheck() {
		return false;
	}

	@Override
	public boolean supportsTableCheck() {
		return false;
	}

	@Override
	public boolean supportsOffsetInSubquery() {
		return true;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return TimesTenLimitHandler.INSTANCE;
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select sysdate from sys.dual";
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new GlobalTemporaryTableMutationStrategy(
				TemporaryTable.createIdTable(
						rootEntityDescriptor,
						name -> TemporaryTable.ID_TABLE_PREFIX + name,
						this,
						runtimeModelCreationContext
				),
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new GlobalTemporaryTableInsertStrategy(
				TemporaryTable.createEntityTable(
						rootEntityDescriptor,
						name -> TemporaryTable.ENTITY_TABLE_PREFIX + name,
						this,
						runtimeModelCreationContext
				),
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	@Override
	public TemporaryTableKind getSupportedTemporaryTableKind() {
		return TemporaryTableKind.GLOBAL;
	}

	@Override
	public String getTemporaryTableCreateOptions() {
		return "on commit delete rows";
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		// TimesTen has no known variation of a "SELECT ... FOR UPDATE" syntax...
		switch ( lockMode ) {
			case OPTIMISTIC:
				return new OptimisticLockingStrategy( lockable, lockMode );
			case OPTIMISTIC_FORCE_INCREMENT:
				return new OptimisticForceIncrementLockingStrategy( lockable, lockMode );
			case PESSIMISTIC_READ:
				return new PessimisticReadUpdateLockingStrategy( lockable, lockMode );
			case PESSIMISTIC_WRITE:
				return new PessimisticWriteUpdateLockingStrategy( lockable, lockMode );
			case PESSIMISTIC_FORCE_INCREMENT:
				return new PessimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
		if ( lockMode.greaterThan( LockMode.READ ) ) {
			return new UpdateLockingStrategy( lockable, lockMode );
		}
		else {
			return new SelectLockingStrategy( lockable, lockMode );
		}
	}

	@Override
	public int getMaxAliasLength() {
		// Max identifier length is 30, but Hibernate needs to add "uniqueing info" so we account for that
		return 20;
	}

	@Override
	public int getMaxIdentifierLength() {
		return 30;
	}

	@Override
	public boolean supportsCircularCascadeDeleteConstraints() {
		return false;
	}

	@Override
	public String getSelectClauseNullString(int sqlType, TypeConfiguration typeConfiguration) {
		switch (sqlType) {
			case Types.VARCHAR:
			case Types.CHAR:
				return "to_char(null)";

			case Types.DATE:
			case Types.TIME:
			case Types.TIMESTAMP:
			case Types.TIMESTAMP_WITH_TIMEZONE:
				return "to_date(null)";

			default:
				return "to_number(null)";
		}
	}

	/*
	 * Copyright (c) 2025, Oracle and/or its affiliates.
	 * Licensed under the Universal Permissive License v 1.0 as shown
	 * at http://oss.oracle.com/licenses/upl
	 *
	 *  - Added a custom definition for 'getNativeIdentifierGeneratorStrategy()'
	 *  - Added a custom definition for 'currentDate()'
	 *  - Added a custom definition for 'currentTime()'
	 *  - Added a custom definition for 'getMaxVarcharLength()'
	 *  - Added a custom definition for 'getMaxVarbinaryLength()'
	 *  - Added a custom definition for 'isEmptyStringTreatedAsNull()'
	 *  - Added a custom definition for 'supportsTupleDistinctCounts()'
	 *  - Added a custom definition for 'getDual()'
	 *  - Added a custom definition for 'getFromDualForSelectOnly()'
	 *
   * @Author: Carlos Blanco
	*/

	@Override 
	public String getNativeIdentifierGeneratorStrategy() {
		return "sequence";
	}

	@Override
	public String currentDate() {
		return "sysdate";
	}

	@Override
	public String currentTime() {
		return "sysdate";
	}

	@Override
	public int getMaxVarcharLength() {
		// 1 to 4,194,304 bytes according to TimesTen Doc
		return 4194304;
	}

	@Override
	public int getMaxVarbinaryLength() {
		// 1 to 4,194,304 bytes according to TimesTen Doc
		return 4194304;
	}

	@Override
	public boolean isEmptyStringTreatedAsNull() {
		return true;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	public String getDual() {
		return "dual"; 
	}

	public String getFromDualForSelectOnly() {
		return " from dual";
	}

}
