/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.TemporalType;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.cfg.Environment;
import org.hibernate.community.dialect.function.InterSystemsIRISLogFunction;
import org.hibernate.community.dialect.identity.InterSystemsIRISIdentityColumnSupport;
import org.hibernate.community.dialect.pagination.InterSystemsIRISLimitHandler;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.dialect.Replacer;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.ExtractFunction;
import org.hibernate.dialect.function.LengthFunction;
import org.hibernate.dialect.function.TruncFunction;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadSelectLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadUpdateLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteSelectLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteUpdateLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.dialect.lock.UpdateLockingStrategy;
import org.hibernate.dialect.lock.internal.NoLockingSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.mutation.spi.BeforeUseAction;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.ANY;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.NUMERIC;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;
import static org.hibernate.sql.ast.internal.NonLockingClauseStrategy.NON_CLAUSE_STRATEGY;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithNanos;


/**
 * A Hibernate dialect for InterSystems IRIS
 * intended for  Hibernate 7.1+  and jdk 1.8+
 */
public class InterSystemsIRISDialect extends Dialect {


	private BasicType<Date> dateType;
	private BasicType<String> stringType;
	private BasicType<Date> timestampType;
	private BasicType<Double> doubleType;
	private BasicType<Integer> integerType;
	private BasicType<Date> timeType;
	private SqmFunctionRegistry functionRegistry;
	private LimitHandler limitHandler = InterSystemsIRISLimitHandler.INSTANCE;

	public InterSystemsIRISDialect() {
		this((DatabaseVersion) null);
	}


	public InterSystemsIRISDialect(DatabaseVersion version) {
		super(version);
		registerDefaultKeywords();

	}

	public InterSystemsIRISDialect(DialectResolutionInfo info) {
		super(info);
		registerDefaultKeywords();

	}

	/**
	 * Register SQL Functions
	 */
	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);
		final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
		functionRegistry = functionContributions.getFunctionRegistry();
		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		dateType = basicTypeRegistry.resolve(StandardBasicTypes.DATE);
		stringType = basicTypeRegistry.resolve(StandardBasicTypes.STRING);
		timestampType = basicTypeRegistry.resolve(StandardBasicTypes.TIMESTAMP);
		doubleType = basicTypeRegistry.resolve(StandardBasicTypes.DOUBLE);
		integerType = basicTypeRegistry.resolve(StandardBasicTypes.INTEGER);
		timeType = basicTypeRegistry.resolve(StandardBasicTypes.TIME);


		functionFactory.math();
		functionFactory.trigonometry();
		functionFactory.ascii();
		functionFactory.bitLength_pattern("length(?1)*8");
		functionFactory.char_chr();
		functionFactory.chr_char();
		functionFactory.length_characterLength();
		functionFactory.cot();
		functionFactory.coalesce();
		functionFactory.concat_pipeOperator();
		functionRegistry.namedDescriptorBuilder("convert")
				.setExactArgumentCount(2)
				.setParameterTypes(ANY, ANY)
				.register();
		functionFactory.nowCurdateCurtime();
		functionRegistry.noArgsBuilder("database")
				.setInvariantType(stringType)
				.setUseParenthesesWhenNoArgs(true)
				.register();

		functionRegistry.namedDescriptorBuilder("dateadd")
				.setReturnTypeResolver(useArgType(3))
				.setExactArgumentCount(3)
				.setParameterTypes(ANY, NUMERIC, ANY)
				.setArgumentListSignature("(STRING string, NUMERIC unit, ANY datetime)")
				.register();


		functionRegistry.namedDescriptorBuilder("datediff")
				.setInvariantType(stringType)
				.setExactArgumentCount(3)
				.setParameterTypes(ANY, ANY, ANY)
				.setArgumentListSignature("(ANY value, ANY date, ANY date)")
				.register();


		functionRegistry.namedDescriptorBuilder("datename")
				.setInvariantType(stringType)
				.setExactArgumentCount(1)
				.setParameterTypes(ANY)
				.setArgumentListSignature("(ANY value)")
				.register();

		functionRegistry.namedDescriptorBuilder("datepart")
				.setExactArgumentCount(2)
				.setParameterTypes(ANY, ANY)
				.setArgumentListSignature("(ANY value, ANY value)")
				.register();

		functionRegistry.namedDescriptorBuilder("day")
				.setInvariantType(integerType)
				.setExactArgumentCount(1)
				.setParameterTypes(ANY)
				.register();

		functionRegistry.namedDescriptorBuilder("dayname")
				.setInvariantType(stringType)
				.setExactArgumentCount(1)
				.setParameterTypes(ANY)
				.register();

		functionRegistry.namedDescriptorBuilder("dayofmonth")
				.setInvariantType(integerType)
				.setExactArgumentCount(1)
				.setParameterTypes(ANY)
				.register();


		functionRegistry.namedDescriptorBuilder("dayofweek")
				.setInvariantType(integerType)
				.setExactArgumentCount(1)
				.setParameterTypes(ANY)
				.register();

		functionRegistry.namedDescriptorBuilder("dayofyear")
				.setInvariantType(integerType)
				.setExactArgumentCount(1)
				.setParameterTypes(ANY)
				.register();


		functionRegistry.namedDescriptorBuilder("%exact")
				.setInvariantType(integerType)
				.setExactArgumentCount(1)
				.setParameterTypes(ANY)
				.register();

		functionRegistry.namedDescriptorBuilder("%external")
				.setExactArgumentCount(1)
				.setParameterTypes(ANY)
				.register();

		functionRegistry.namedDescriptorBuilder("$extract")
				.setInvariantType(stringType)
				.setArgumentCountBetween(1, 3)
				.setParameterTypes(STRING, INTEGER)
				.register();


		functionRegistry.namedDescriptorBuilder("$find")
				.setArgumentCountBetween(2, 3)
				.setInvariantType(integerType)
				.setParameterTypes(STRING, STRING, INTEGER)
				.register();

		functionRegistry.namedDescriptorBuilder("getdate")
				.setArgumentCountBetween(0, 1)
				.setInvariantType(timestampType)
				.setParameterTypes(INTEGER)
				.setUseParenthesesWhenNoArgs(true)
				.setArgumentListSignature("([INTEGER precision])")
				.register();


		functionRegistry.namedDescriptorBuilder("ifnull")
				.setArgumentCountBetween(2, 3)
				.setParameterTypes(ANY, ANY, ANY)
				.register();

		functionRegistry.namedDescriptorBuilder("%internal")
				.setExactArgumentCount(1)
				.setParameterTypes(ANY)
				.register();

		functionRegistry.namedDescriptorBuilder("%isnull")
				.setExactArgumentCount(2)
				.setParameterTypes(ANY, ANY)
				.register();

		functionRegistry.namedDescriptorBuilder("%isnumeric")
				.setExactArgumentCount(1)
				.setParameterTypes(ANY)
				.setInvariantType(integerType)
				.register();


		functionRegistry.namedDescriptorBuilder("lcase")
				.setExactArgumentCount(1)
				.setParameterTypes(STRING)
				.setInvariantType(stringType)
				.register();

		functionRegistry.patternDescriptorBuilder("log10", "log10(?1)")
				.setInvariantType(doubleType)
				.setExactArgumentCount(1)
				.register();
		functionRegistry.namedDescriptorBuilder("lower")
				.setExactArgumentCount(1)
				.setParameterTypes(STRING)
				.setInvariantType(stringType)
				.register();

		functionRegistry.namedDescriptorBuilder("upper")
				.setExactArgumentCount(1)
				.setParameterTypes(STRING)
				.setInvariantType(stringType)
				.register();


		functionRegistry.namedDescriptorBuilder("nullif")
				.setExactArgumentCount(2)
				.setParameterTypes(ANY, ANY)
				.setReturnTypeResolver(useArgType(1))
				.register();

		functionRegistry.namedDescriptorBuilder("nvl")
				.setExactArgumentCount(2)
				.setParameterTypes(ANY, ANY)
				.setReturnTypeResolver(useArgType(1))
				.register();

		functionFactory.round_round();

		functionRegistry.namedDescriptorBuilder("to_number")
				.setExactArgumentCount(1)
				.setParameterTypes(STRING)
				.setArgumentListSignature("STRING string")
				.register();


		functionRegistry.namedDescriptorBuilder("to_char")
				.setArgumentCountBetween(1, 2)
				.setParameterTypes(ANY, STRING)
				.setInvariantType(stringType)
				.register();

		functionRegistry.namedDescriptorBuilder("to_date")
				.setArgumentCountBetween(1, 2)
				.setParameterTypes(STRING, STRING)
				.setInvariantType(dateType)
				.register();

		functionRegistry.register(
				"trunc",
				new TruncFunction( "truncate(?1,0)", "truncate(?1,?2)",
						TruncFunction.DatetimeTrunc.FORMAT, "to_timestamp", typeConfiguration )
		);
		functionRegistry.registerAlternateKey( "truncate", "trunc" );

		functionRegistry.namedDescriptorBuilder("%sqlstring")
				.setArgumentCountBetween(1, 2)
				.setParameterTypes(STRING, INTEGER)
				.setInvariantType(stringType)
				.register();

		functionRegistry.namedDescriptorBuilder("%sqlupper")
				.setArgumentCountBetween(1, 2)
				.setParameterTypes(STRING, INTEGER)
				.setInvariantType(stringType)
				.register();

		functionRegistry.namedDescriptorBuilder("$piece")
				.setArgumentCountBetween(1, 3)
				.setParameterTypes(STRING, STRING, INTEGER, INTEGER)
				.setInvariantType(stringType)
				.register();

		functionRegistry.namedDescriptorBuilder("%ODBCIN")
				.setExactArgumentCount(1)
				.setParameterTypes(ANY)
				.register();

		functionRegistry.namedDescriptorBuilder("%odbcout")
				.setExactArgumentCount(1)
				.setParameterTypes(ANY)
				.register();

		functionRegistry.namedDescriptorBuilder("ucase")
				.setExactArgumentCount(1)
				.setParameterTypes(STRING)
				.setInvariantType(stringType)
				.setArgumentListSignature("STRING string")
				.register();

		functionRegistry.namedDescriptorBuilder("user")
				.setInvariantType(timestampType)
				.setParameterTypes(INTEGER)
				.setUseParenthesesWhenNoArgs(true)
				.register();

		functionFactory.xmlelement();
		functionFactory.xmlconcat();


		functionRegistry.namedDescriptorBuilder("$list")
				.setArgumentCountBetween(1, 3)
				.setInvariantType(stringType)
				.register();

		functionRegistry.namedDescriptorBuilder("$listdata")
				.setArgumentCountBetween(1, 2)
				.register();

		functionRegistry.namedDescriptorBuilder("$listfind")
				.setArgumentCountBetween(2, 3)
				.register();

		functionRegistry.namedDescriptorBuilder("$listget")
				.setArgumentCountBetween(1, 3)
				.register();

		functionRegistry.namedDescriptorBuilder("$listlength")
				.setExactArgumentCount(1)
				.register();


		functionRegistry.namedDescriptorBuilder("current_timestamp")
				.setUseParenthesesWhenNoArgs(false)
				.setInvariantType(timestampType)
				.register();


		functionContributions.getFunctionRegistry().register(
				"extract",
				new ExtractFunction(this, typeConfiguration)
		);

		functionRegistry.namedDescriptorBuilder("current_time")
				.setUseParenthesesWhenNoArgs(false)
				.setInvariantType(timeType)
				.register();

		functionRegistry.namedDescriptorBuilder("current_date")
				.setUseParenthesesWhenNoArgs(false)
				.setInvariantType(dateType)
				.register();


		functionRegistry.namedDescriptorBuilder("char_length")
				.setExactArgumentCount(1)
				.setParameterTypes(ANY)
				.setInvariantType(integerType)
				.setArgumentListSignature("ANY any")
				.register();

		functionRegistry.registerBinaryTernaryPattern(
				"locate",
				integerType,
				"position(?1 in ?2)",
				"position(?1 in substring(?2 from ?3))",
				FunctionParameterType.STRING,
				FunctionParameterType.STRING,
				FunctionParameterType.INTEGER,
				typeConfiguration
		);

		functionRegistry.registerPattern(
				"Extract",
				"?1(?2)",
				integerType
		);

		functionContributions.getFunctionRegistry()
				.register("log", new InterSystemsIRISLogFunction(typeConfiguration));
		functionRegistry.registerAlternateKey( "ln", "log" );
		functionFactory.leftRight();
		functionFactory.characterLength_len();
		functionFactory.hourMinuteSecond();
		functionFactory.yearMonthDay();
		functionFactory.daynameMonthname();
		functionFactory.nowCurdateCurtime();
		functionFactory.substr();
		functionFactory.substring();
		functionFactory.sysdate();
		functionFactory.yearMonthDay();
		functionFactory.weekQuarter();
		functionFactory.position();
		functionFactory.weekQuarter();
		functionFactory.repeat_replicate();
		functionFactory.trim1();
		functionFactory.pi();
		functionFactory.space();
		functionFactory.lowerUpper();
		functionFactory.degrees();
		functionFactory.radians();
		functionFactory.concat_pipeOperator("SUBSTRING(?1,1) || SUBSTRING(?2,1)");
		functionRegistry.register(
				"bit_length",
				new LengthFunction("bit_length", "LENGTH(?1)*8", "(CHARACTER_LENGTH(?1) * 8)", typeConfiguration)
		);

		functionRegistry.register(
				"LENGTH",
				new LengthFunction(
						"LENGTH",
						"LENGTH(?1)",
						"CHARACTER_LENGTH(?1)",
						typeConfiguration
				)
		);
		functionRegistry.register(
				"octet_length",
				new LengthFunction(
						"octet_length",
						"LENGTH(?1)",
						"CHARACTER_LENGTH(?1)",
						typeConfiguration
				)
		);

	}


	@Override
	protected void initDefaultProperties() {
		getDefaultProperties().setProperty(Environment.USE_SQL_COMMENTS, "false");
	}


	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes(typeContributions, serviceRegistry);

		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.BINARY));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.BIGINT));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.BIT));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.CHAR));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.DATE));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.DECIMAL));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.DOUBLE));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.FLOAT));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.INTEGER));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.LONGVARBINARY));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.LONGVARCHAR));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.NUMERIC));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.REAL));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.SMALLINT));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.TIMESTAMP));
		ddlTypeRegistry.addDescriptor(simpleSqlType(TIMESTAMP_UTC));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.TIME));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.TINYINT));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.LOCAL_DATE_TIME));
		ddlTypeRegistry.addDescriptor(
				sqlTypeBuilder(SqlTypes.VARBINARY, SqlTypes.LONGVARBINARY, SqlTypes.VARBINARY)
						.withTypeCapacity(getMaxVarbinaryLength(), columnType(SqlTypes.VARBINARY))
						.build()
		);
		ddlTypeRegistry.addDescriptor(
				sqlTypeBuilder(SqlTypes.VARCHAR, SqlTypes.LONGVARCHAR, SqlTypes.VARCHAR)
						.withTypeCapacity(getMaxVarcharLength(), columnType(SqlTypes.VARCHAR))
						.build()
		);

		ddlTypeRegistry.addDescriptor(
				sqlTypeBuilder(SqlTypes.NVARCHAR, SqlTypes.LONGVARCHAR, SqlTypes.VARCHAR)
						.withTypeCapacity(getMaxVarcharLength(), columnType(SqlTypes.NVARCHAR))
						.build()
		);

		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.BLOB));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.CLOB));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.BOOLEAN));
		ddlTypeRegistry.addDescriptor(new DdlTypeImpl(SqlTypes.UUID, "CHAR(36)", this));
		ddlTypeRegistry.addDescriptor(new DdlTypeImpl(SqlTypes.CHAR, "CHAR($l)", this));
		ddlTypeRegistry.addDescriptor(new DdlTypeImpl(SqlTypes.VARCHAR, "VARCHAR($l)", this));
		ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.TIMESTAMP_WITH_TIMEZONE));

	}
	@Override
	public void contributeTypes(TypeContributions tc, ServiceRegistry sr) {
		super.contributeTypes(tc, sr);
		JdbcTypeRegistry jdbcTypeRegistry = tc.getTypeConfiguration().getJdbcTypeRegistry();
		jdbcTypeRegistry.addDescriptor(org.hibernate.type.descriptor.jdbc.TimestampJdbcType.INSTANCE);
		jdbcTypeRegistry.addDescriptor(SqlTypes.UUID, VarcharJdbcType.INSTANCE);
		jdbcTypeRegistry.addDescriptor( SqlTypes.TIMESTAMP,
				org.hibernate.type.descriptor.jdbc.TimestampJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( SqlTypes.BLOB,
				BlobJdbcType.MATERIALIZED );
	}

	private DdlTypeImpl simpleSqlType(int sqlTypeCode) {
		return new DdlTypeImpl(sqlTypeCode, columnType(sqlTypeCode), castType(sqlTypeCode), this);
	}

	private CapacityDependentDdlType.Builder sqlTypeBuilder(int sqlTypeCode, int biggestSqlTypeCode, int castTypeCode) {
		return CapacityDependentDdlType.builder(
				sqlTypeCode,
				columnType(biggestSqlTypeCode),
				castType(castTypeCode),
				this
		);
	}

	//sql type to column type mapping
	@Override
	protected String columnType(int sqlTypeCode) {
		switch (sqlTypeCode) {
			case SqlTypes.BOOLEAN:
				return "bit";
			case SqlTypes.TINYINT:
				return "tinyint";
			case SqlTypes.SMALLINT:
				return "smallint";
			case SqlTypes.BIT:
				return "bit";
			case SqlTypes.CHAR:
				return "char(1)";
			case SqlTypes.INTEGER:
				return "integer";
			case SqlTypes.BIGINT:
				return "BigInt";
			case SqlTypes.DOUBLE:
				return "double";
			case SqlTypes.FLOAT:
				return "float";
			case SqlTypes.REAL:
				return "real";
			case SqlTypes.NUMERIC:
				return "numeric($p,$s)";
			case SqlTypes.DECIMAL:
				return "decimal($p,$s)";
			case SqlTypes.DATE:
				return "date";
			case SqlTypes.TIME:
				return "time";
			case SqlTypes.TIMESTAMP:
				return "timestamp";
			case TIMESTAMP_UTC:
				return "timestamp";
			case SqlTypes.VARCHAR:
				return "varchar($l)";
			case SqlTypes.NVARCHAR:
				return "varchar($l)";
			case SqlTypes.BINARY:
				return "varbinary($l)";
			case SqlTypes.VARBINARY:
				return "varbinary($l)";
			case SqlTypes.LONGVARBINARY:
				return "longvarbinary";
			case SqlTypes.LONGVARCHAR:
				return "longvarchar";
			case SqlTypes.BLOB:
				return "longvarbinary";
			case SqlTypes.CLOB:
				return "longvarchar";
			case SqlTypes.NCLOB:
				return "longvarchar";
			case SqlTypes.LOCAL_DATE_TIME:
				return "timestamp2";
		}
		return super.columnType(sqlTypeCode);
	}


	@Override
	public boolean supportsSubselectAsInPredicateLHS() {
		return false;
	}

	public boolean supportsSubqueryOnMutatingTable() {
		return false;
	}

	@Override
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}


	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new InterSystemsIRISSqlAstTranslator<>(sessionFactory, statement);
			}
		};
	}


	@Override
	protected void registerDefaultKeywords() {
		super.registerDefaultKeywords();
		String[] irisExtraKeywords = {
				"ASSERTION","AVG","BIT","BIT_LENGTH","CHARACTER_LENGTH",
				"CHAR_LENGTH","COALESCE","CONNECTION","CONSTRAINTS","CONVERT","COUNT","DEFERRABLE","DEFERRED","DESCRIPTOR","DIAGNOSTICS",
				"DOMAIN","ENDEXEC","EXCEPTION","EXTRACT","FOUND","INITIALLY","ISOLATION",
				"LEVEL","LOWER","MAX","MIN","NAMES","NULLIF","OCTET_LENGTH","OPTION","PAD","PARTIAL","PRIOR","PRIVILEGES","PUBLIC","READ","RELATIVE",
				"RESTRICT","SCHEMA","SESSION_USER","SHARD",
				"SPACE","SQLERROR","STATISTICS","SUBSTRING","SUM","SYSDATE",
				"TEMPORARY","TOP","TRIM",
				"UPPER","WORK","WRITE"
		};

		for (String kw : irisExtraKeywords) {
			registerKeyword(kw.toUpperCase(Locale.ROOT));
			registerKeyword(kw.toLowerCase( Locale.ROOT));
		}
	}


	// DDL support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean hasAlterTable() {
		// Does this dialect support the ALTER TABLE syntax?
		return true;
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}


	@Override
	public String getAddColumnString() {
		// The syntax used to add a column to a table
		return " add column";
	}

	@Override
	public String getCascadeConstraintsString() {
		return "";
	}

	@Override
	public boolean dropConstraints() {
		return true;
	}

	@Override
	public boolean supportsCascadeDelete() {
		return true;
	}

	@Override
	public boolean hasSelfReferentialForeignKeyBug() {
		return true;
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {

		return new GlobalTemporaryTableMutationStrategy( rootEntityDescriptor, runtimeModelCreationContext );

	}


	@Override
	public boolean canCreateSchema() {
		return false;
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {

		return new GlobalTemporaryTableInsertStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}


	@Override
	public TemporaryTableKind getSupportedTemporaryTableKind() {
		return TemporaryTableKind.GLOBAL;
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return "create global temporary table if not exists";
	}

	@Override
	public String getTemporaryTableDropCommand() {
		return "drop table";
	}

	@Override
	public AfterUseAction getTemporaryTableAfterUseAction() {
		return AfterUseAction.CLEAN;
	}


	@Override
	public BeforeUseAction getTemporaryTableBeforeUseAction() {
		return BeforeUseAction.CREATE;
	}


	// IDENTITY support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new InterSystemsIRISIdentityColumnSupport();
	}


	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public LockingStrategy getLockingStrategy(EntityPersister lockable, LockMode lockMode) {

		// Just to make some tests happy, but InterSystems IRIS doesn't really support this.
		// need to use READ_COMMITTED as isolation level

		//InterSystemsIRIS does not current support "SELECT ... FOR UPDATE" syntax...
		// Set your transaction mode to READ_COMMITTED before using
		if (lockMode == LockMode.PESSIMISTIC_FORCE_INCREMENT) {
			return new PessimisticForceIncrementLockingStrategy(lockable, lockMode);
		}
		else if (lockMode == LockMode.PESSIMISTIC_WRITE) {
			return lockable.isVersioned()
					? new PessimisticWriteUpdateLockingStrategy(lockable, lockMode)
					: new PessimisticWriteSelectLockingStrategy(lockable, lockMode);
		}
		else if (lockMode == LockMode.PESSIMISTIC_READ) {
			return lockable.isVersioned()
					? new PessimisticReadUpdateLockingStrategy(lockable, lockMode)
					: new PessimisticReadSelectLockingStrategy(lockable, lockMode);
		}
		else if (lockMode == LockMode.OPTIMISTIC) {
			return new OptimisticLockingStrategy(lockable, lockMode);
		}
		else if (lockMode == LockMode.OPTIMISTIC_FORCE_INCREMENT) {
			return new OptimisticForceIncrementLockingStrategy(lockable, lockMode);
		}
		else if (lockMode.greaterThan(LockMode.READ)) {
			return new UpdateLockingStrategy(lockable, lockMode);
		}
		else {
			return new SelectLockingStrategy(lockable, lockMode);
		}

	}


	// The syntax used to add a foreign key constraint to a table.
	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		final String cols = String.join(", ", foreignKey);
		final String referencedCols = String.join(", ", primaryKey);
		return String.format(
				" add constraint %s foreign key (%s) references %s (%s)",
				constraintName,
				cols,
				referencedTable,
				referencedCols
		);
	}


	// LIMIT support (also TOP) ~~~~~~~~~~~~~~~~~~~

	@Override
	public LimitHandler getLimitHandler() {
		return limitHandler;
	}


	// callable statement support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		ps.execute();
		return (ResultSet) ps.getObject(1);
	}

	@Override
	public String getLowercaseFunction() {
		// The name of the SQL function that transforms a string to lowercase
		return "lower";
	}

	@Override
	public String getNullColumnString() {
		return "";
	}


	// miscellaneous support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public String getNoColumnsInsertString() {
		// The keyword used to insert a row without specifying
		// any column values
		return " default values";
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			switch ( sqlException.getErrorCode() ) {
				case 110:
					return new LockTimeoutException( message, sqlException, sql );
				case 114:
					return new LockAcquisitionException( message, sqlException, sql );
				case 30: // Table or view not found
					return new SQLGrammarException( message, sqlException, sql );
				case 119, 120, 125:
					// Unique constraint violation
					return new ConstraintViolationException( message, sqlException, sql, ConstraintViolationException.ConstraintKind.UNIQUE,
							getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
				case 108:
					// Null constraint violation
					return new ConstraintViolationException( message, sqlException, sql, ConstraintViolationException.ConstraintKind.NOT_NULL,
							getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
				case 121, 122, 123, 124, 126,127:
					// Foreign key constraint violation
					return new ConstraintViolationException( message, sqlException, sql, ConstraintViolationException.ConstraintKind.FOREIGN_KEY,
							getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
				case 3819:
					// Check constraint violation
					return new ConstraintViolationException( message, sqlException, sql, ConstraintViolationException.ConstraintKind.CHECK,
							getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
				case 02, 21, 22:
					return new DataException(message, sqlException, sql);

			}
			return null;
		};

	}

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	/**
	 * The InterSystemsIRIS ViolatedConstraintNameExtracter.
	 */

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor(sqle -> {
				return extractUsingTemplate("(", ")", sqle.getMessage());
			});


	/**
	 * ddl like ""value" integer null check ("value">=2 AND "value"<=10)" isn't supported
	 */
	@Override
	public boolean supportsColumnCheck() {
		return false;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public ScrollMode defaultScrollMode() {
		return super.defaultScrollMode();
	}

	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(
			IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData) throws SQLException {
		if (dbMetaData == null) {
			builder.setUnquotedCaseStrategy(IdentifierCaseStrategy.MIXED);
			builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}
		else {
			builder.applyIdentifierCasing(dbMetaData);
		}

		builder.applyReservedWords(getKeywords());

		builder.setNameQualifierSupport(getNameQualifierSupport());
		builder.setAutoQuoteKeywords(true);
		return super.buildIdentifierHelper(builder, dbMetaData);
	}


	@Override
	protected void registerKeyword(String word) {
		super.getKeywords().add(word);
	}

	@Override
	public boolean supportsNullPrecedence() {
		return false;
	}

	@Override
	public boolean supportsLockTimeouts() {
		return false;
	}


	@Override
	public boolean supportsFetchClause(FetchClauseType type) {
		if (type == FetchClauseType.ROWS_ONLY) {
			return true;
		}
		return false;
	}

	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return false;
	}

	@Override
	public boolean supportsExpectedLobUsagePattern() {
		return false;
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "SELECT CURRENT_TIMESTAMP";
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public boolean supportsValuesListForInsert() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntax() {
		return false;
	}


	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorGtLtSyntax() {
		return false;
	}

	@Override
	public int getMaxVarcharLength() {
		return 32767;
	}

	@Override
	public boolean supportsOrderByInSubquery() {
		return false;
	}

	@Override
	public boolean supportsLateral() {
		return true;
	}

	@Override
	public String getWriteLockString(int timeout) {
		return "";
	}

	@Override
	public String getReadLockString(int timeout) {
		return "";
	}

	@Override
	public String getForUpdateString() {
		return "";
	}

	@SuppressWarnings("deprecation")
	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		switch (unit) {
			case YEAR:      return "{fn TIMESTAMPADD(SQL_TSI_YEAR, ?2, ?3)}";
			case QUARTER:   return "{fn TIMESTAMPADD(SQL_TSI_QUARTER, ?2, ?3)}";
			case MONTH:     return "{fn TIMESTAMPADD(SQL_TSI_MONTH, ?2, ?3)}";
			case WEEK:      return "{fn TIMESTAMPADD(SQL_TSI_WEEK, ?2, ?3)}";
			case DAY:
			case DAY_OF_MONTH:
				return "{fn TIMESTAMPADD(SQL_TSI_DAY, ?2, ?3)}";
			case HOUR:      return "{fn TIMESTAMPADD(SQL_TSI_HOUR, ?2, ?3)}";
			case MINUTE:    return "{fn TIMESTAMPADD(SQL_TSI_MINUTE, ?2, ?3)}";
			case SECOND:    return "dateadd(second, ?2, ?3)";
			case NANOSECOND:
				return "{fn TIMESTAMPADD(SQL_TSI_FRAC_SECOND, (?2)/1000000, ?3)}";
			case NATIVE:
				return "dateadd(microsecond, ?2, ?3)";
			case TIME:
			case DATE:
			case EPOCH:
			case DAY_OF_WEEK:
			case DAY_OF_YEAR:
			case WEEK_OF_MONTH:
			case WEEK_OF_YEAR:
			case OFFSET:
			case TIMEZONE_HOUR:
			case TIMEZONE_MINUTE:
				throw new UnsupportedOperationException("Unsupported unit for TIMESTAMPADD: " + unit);
			default:
				throw new UnsupportedOperationException("Unsupported unit for TIMESTAMPADD: " + unit);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public String timestampdiffPattern(TemporalUnit unit,
									TemporalType fromTemporalType,
									TemporalType toTemporalType) {
		if (unit == null) {
			return "{fn TIMESTAMPDIFF(SQL_TSI_SECOND, ?2, ?3)}";
		}
		switch (unit) {
			case YEAR:
				return "{fn TIMESTAMPDIFF(SQL_TSI_YEAR, ?2, ?3)}";
			case QUARTER:
				return "({fn TIMESTAMPDIFF(SQL_TSI_MONTH, ?2, ?3)}/3)";
			case MONTH:
				return "{fn TIMESTAMPDIFF(SQL_TSI_MONTH, ?2, ?3)}";
			case WEEK:
				return "{fn TIMESTAMPDIFF(SQL_TSI_WEEK, ?2, ?3)}";
			case DAY:
			case DAY_OF_MONTH:
				return "{fn TIMESTAMPDIFF(SQL_TSI_DAY, ?2, ?3)}";
			case HOUR:
				return "{fn TIMESTAMPDIFF(SQL_TSI_HOUR, ?2, ?3)}";
			case MINUTE:
				return "{fn TIMESTAMPDIFF(SQL_TSI_MINUTE, ?2, ?3)}";
			case SECOND:
				return "{fn TIMESTAMPDIFF(SQL_TSI_SECOND, ?2, ?3)}";
			case NANOSECOND:
				return "({fn TIMESTAMPDIFF(SQL_TSI_FRAC_SECOND, ?2, ?3)}*1000000)";
			case NATIVE:
				return "({fn TIMESTAMPDIFF(SQL_TSI_FRAC_SECOND, ?2, ?3)}*1000)";
			case DATE:
			case TIME:
			case EPOCH:
			case DAY_OF_WEEK:
			case DAY_OF_YEAR:
			case WEEK_OF_MONTH:
			case WEEK_OF_YEAR:
			case OFFSET:
			case TIMEZONE_HOUR:
			case TIMEZONE_MINUTE:
				throw new UnsupportedOperationException("Unsupported TemporalUnit for TIMESTAMPDIFF: " + unit);
			default:
				throw new UnsupportedOperationException("Unsupported TemporalUnit for TIMESTAMPDIFF: " + unit);
		}
	}
	@Override
	public long getFractionalSecondPrecisionInNanos() {
		return 1_000L; //default to nanoseconds for now
	}

	@Override
	public boolean supportsTableCheck() {
		return false;
	}

	@Override
	public LockingClauseStrategy getLockingClauseStrategy(QuerySpec querySpec, LockOptions lockOptions) {
		return NON_CLAUSE_STRATEGY;
	}

	@Override
	public LockingSupport getLockingSupport() {
		return NoLockingSupport.NO_LOCKING_SUPPORT;
	}


	@Override
	public void appendDateTimeLiteral(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "'" );
				appendAsDate( appender, temporalAccessor );
				appender.appendSql( "'" );
				break;
			case TIME:
				appender.appendSql( "'" );
				appendAsTime( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( "'" );
				break;
			case TIMESTAMP:
				appender.appendSql( "'");
				appendAsTimestampWithNanos( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( "'" );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		appender.appendSql( datetimeFormat( format).result() );
	}

	public static Replacer datetimeFormat(String format) {
		return new Replacer( format, "'", "" )
				//era
				.replace("GG", "AD")
				.replace("G", "AD")

				//year
				.replace("yyyy", "YYYY")
				.replace("yyy", "YYYY" )
				.replace("yy", "YY")
				.replace("y", "YYYY")

				//month of year
				.replace("MMMM",  "Month" )
				.replace("MMM", "Mon")
				.replace("MM", "MM")
				.replace("M", "MM" )

				//week of year
				.replace("ww", "IW")
				.replace("w", "IW" )
				//year for week
				.replace("YYYY", "IYYY")
				.replace("YYY", "IYYY" )
				.replace("YY", "IY")
				.replace("Y", "IYYY" )

				//week of month
				.replace("W", "W")

				//day of week
				.replace("EEEE", "Day" )
				.replace("EEE", "Dy")
				.replace("ee", "D")
				.replace("e",  "D" )

				//day of month
				.replace("dd", "DD")
				.replace("d", "DD" )

				//day of year
				.replace("DDD", "DDD")
				.replace("DD", "DDD" )
				.replace("D", "DDD" )

				//am pm
				.replace("a", "AM")

				//hour
				.replace("hh", "HH12")
				.replace("HH", "HH24")
				.replace("h", "HH12" )
				.replace("H",  "HH24" )

				//minute
				.replace("mm", "MI")
				.replace("m", "MI" )

				//second
				.replace("ss", "SS")
				.replace("s", "SS")

				//fractional seconds
				.replace("SSSSSS", "FF6")
				.replace("SSSSS", "FF5")
				.replace("SSSS", "FF4")
				.replace("SSS", "FF3")
				.replace("SS", "FF2")
				.replace("S", "FF1")

				//timezones
				.replace("zzz", "TZR")
				.replace("zz", "TZR")
				.replace("z", "TZR")
				.replace("ZZZ", "TZHTZM")
				.replace("ZZ", "TZHTZM")
				.replace("Z", "TZHTZM")
				.replace("xxx", "TZH:TZM")
				.replace("xx", "TZHTZM")
				.replace("x", "TZH");
	}

	@Override
	public String extractPattern(TemporalUnit unit) {
		switch (unit) {
			case DAY_OF_YEAR:
				return "dayofyear(?2)";
			case DAY_OF_MONTH:
				return "dayofmonth(?2)";
			case DAY_OF_WEEK:
				return "dayofweek(?2)";
			case WEEK:
			case WEEK_OF_YEAR:
				return "week(?2)";
			case DAY:
				return "day(?2)";
			case MONTH:
				return "month(?2)";
			case YEAR:
				return "year(?2)";
			case QUARTER:
				return "quarter(?2)";
			case HOUR:
				return "hour(?2)";
			case MINUTE:
				return "minute(?2)";
			case SECOND:
				return "second(?2)";
			case WEEK_OF_MONTH:
				return "ceiling( (dayofmonth(?2) + dayofweek(dateadd('day', 1 - dayofmonth(?2), ?2)) - 1) / 7 )";
			case OFFSET:
			case TIMEZONE_HOUR:
			case TIMEZONE_MINUTE:
				return null;
			default:
				return super.extractPattern(unit);
		}
	}


@Override
public boolean requiresFloatCastingOfIntegerDivision() {
	return true;
}

	@Override
	public boolean supportsWithClauseInSubquery() {
		return false;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return true;
	}

	@Override
	public boolean useInputStreamToInsertBlob() {
		return false;
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		return switch (unit) {
			case DAY_OF_MONTH -> "DAYOFMONTH";
			case DAY_OF_YEAR -> "DAYOFYEAR ";
			case DAY_OF_WEEK -> "DAYOFWEEK ";
			case EPOCH -> "TO_POSIXTIME";
			case DATE -> "DATE";
			default -> super.translateExtractField(unit);
		};
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BIT;
	}


	@Override
	public String getDual() {
		return "(select 1)";
	}

	@Override
	public int getInExpressionCountLimit() {
		return 900;
	}

	@Override
	public boolean supportsFromClauseInUpdate() {
		return true;
	}

	@Override
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}
}
