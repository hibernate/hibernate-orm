/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import org.hibernate.dialect.type.spi.TypeSizingProfile;

import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.function.spi.TupleCountSupport;

import java.sql.Types;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.community.dialect.pagination.TimesTenLimitHandler;
import org.hibernate.community.dialect.sequence.TimesTenSequenceSupport;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.dialect.type.spi.BooleanDecoder;
import org.hibernate.dialect.type.spi.StringValueSemantics;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.OracleTruncFunction;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.dialect.lock.spi.EntityLockingStrategies;
import org.hibernate.dialect.lock.spi.EntityLockingStrategyFactory;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.dialect.temptable.spi.StandardGlobalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.query.sqm.CastType;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.CurrentFunction;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
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
public class TimesTenDialect extends Dialect implements CurrentTemporalSupport, TemporalOperationSupport {

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalOperationSupport getTemporalOperationSupport() {
		return this;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CurrentTemporalSupport getCurrentTemporalSupport() {
		return this;
	}
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.defaultDecimalPrecision( 40 )
			.maxVarcharLength( 4_194_304 ).maxVarcharCapacity( 4_194_304 )
			.maxNVarcharLength( 4_194_304 ).maxNVarcharCapacity( 4_194_304 )
			.maxVarbinaryLength( 4_194_304 ).maxVarbinaryCapacity( 4_194_304 )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

	public TimesTenDialect() {
		super( ZERO_VERSION );
	}

	public TimesTenDialect(DialectResolutionInfo info) {
		super( info );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			//Note: these are the correct type mappings
			//      for the default Oracle type mode
			//      TypeMode=0
			case SqlTypes.BOOLEAN, SqlTypes.BIT, SqlTypes.TINYINT -> "tt_tinyint";
			case SqlTypes.SMALLINT -> "tt_smallint";
			case SqlTypes.INTEGER -> "tt_integer";
			case SqlTypes.BIGINT -> "tt_bigint";
			//note that 'binary_float'/'binary_double' might
			//be better mappings for Java Float/Double

			case SqlTypes.VARCHAR, SqlTypes.LONGVARCHAR -> "varchar2($l)";

			case SqlTypes.LONGVARBINARY -> "varbinary($l)";

			//'numeric'/'decimal' are synonyms for 'number'
			case SqlTypes.NUMERIC, SqlTypes.DECIMAL -> "number($p,$s)";
			case SqlTypes.FLOAT -> "binary_float";
			case SqlTypes.DOUBLE -> "binary_double";

			case SqlTypes.DATE -> "tt_date";
			case SqlTypes.TIME -> "tt_time";
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE -> "timestamp($p)";
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, Integer.toString( 15 ) );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BIT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
		CommonFunctionFactory functionFactory     = new CommonFunctionFactory(functionContributions);
		final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		final BasicType<Date>   timestampType     = basicTypeRegistry.resolve( StandardBasicTypes.TIMESTAMP );
		final BasicType<String> stringType        = basicTypeRegistry.resolve( StandardBasicTypes.STRING );
		final BasicType<Long>   longType          = basicTypeRegistry.resolve( StandardBasicTypes.LONG );
		final BasicType<Integer>intType           = basicTypeRegistry.resolve( StandardBasicTypes.INTEGER );

		// String Functions
		functionFactory.trim2();
		functionFactory.characterLength_length( SqlAstNodeRenderingMode.DEFAULT );
		functionFactory.concat_pipeOperator();
		functionFactory.toCharNumberDateTimestamp();
		functionFactory.char_chr();
		functionFactory.instr();
		functionFactory.substr();
		functionFactory.substring_substr();
		functionFactory.soundex();

		// Date/Time Functions
		functionContributions.getFunctionRegistry().register(
				"sysdate", new CurrentFunction("sysdate", "sysdate", timestampType)
		);
		functionContributions.getFunctionRegistry().register(
				"getdate", new CurrentFunction("getdate", "getdate()", timestampType )
		);

		// Multi-param date dialect functions
		functionFactory.addMonths();
		functionFactory.monthsBetween();

		// Math functions
		functionFactory.ceiling_ceil();
		functionFactory.radians_acos();
		functionFactory.degrees_acos();
		functionFactory.sinh();
		functionFactory.tanh();
		functionContributions.getFunctionRegistry().register( "trunc", new OracleTruncFunction() );
		functionContributions.getFunctionRegistry().registerAlternateKey( "truncate", "trunc" );
		functionFactory.round();

		// Bitwise functions
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
		functionContributions.getFunctionRegistry().namedDescriptorBuilder( "nvl" )
				.setMinArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.useFirstNonNull() )
				.register();

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
				"vsize", new StandardSQLFunction("vsize", StandardBasicTypes.DOUBLE)
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
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new TimesTenSqlAstTranslator<>( request );
			}
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		return switch (unit) {
			case NANOSECOND, NATIVE -> "timestampadd(sql_tsi_frac_second,?2,?3)";
			default -> "timestampadd(sql_tsi_?1,?2,?3)";
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		return switch (unit) {
			case NANOSECOND, NATIVE -> "timestampdiff(sql_tsi_frac_second,?2,?3)";
			default -> "timestampdiff(sql_tsi_?1,?2,?3)";
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public IndexNameQualification nameQualification() {
		return IndexNameQualification.UNQUALIFIED;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String addColumnPrefix() {
		return "add";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return TimesTenSequenceSupport.INSTANCE;
	}

	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder( "select name from sys.sequences" )
					.sequenceNameColumn( "name" )
					.withoutCatalog()
					.withoutSchema()
					.withoutStartValue()
					.minimumValueColumn( "minval" )
					.maximumValueColumn( "maxval" )
					.build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	public LockingSupport getLockingSupport() {
		return StandardLockingSupports.timesTen();
	}















	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean supports(org.hibernate.dialect.constraint.spi.CheckConstraintPlacement placement) {
		return false;
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.OFFSET, true )
				.build();
	}

	@Override
	public LimitHandler getLimitHandler() {
		return TimesTenLimitHandler.INSTANCE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( "select sysdate from sys.dual" );
	}

	@Override
	public boolean supportsCrossJoin() {
		return false;
	}

	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return MultiTableMutationSupport.GLOBAL_TEMPORARY_TABLE;
	}

	@Override
	public TemporaryTableStrategy getGlobalTemporaryTableStrategy() {
		return StandardGlobalTemporaryTableStrategy.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public EntityLockingStrategyFactory getEntityLockingStrategyFactory() {
		// TimesTen has no known variation of a "SELECT ... FOR UPDATE" syntax.
		return EntityLockingStrategies.pessimisticUpdate();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxAliasLength() {
		// Max identifier length is 30, but Hibernate needs to add "uniqueing info" so we account for that
		return 20;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxIdentifierLength() {
		return 30;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getSelectClauseNullString(SqlTypedMapping sqlTypeMapping, TypeConfiguration typeConfiguration) {
		final int sqlType = sqlTypeMapping.getJdbcMapping().getJdbcType().getDdlTypeCode();
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

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentDate() {
		return "sysdate";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTime() {
		return "sysdate";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return "sysdate";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public StringValueSemantics getStringValueSemantics() {
		return StringValueSemantics.EMPTY_STRING_AS_NULL;
	}

	@Override
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.NONE;
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( "dual" )
				.selectOnlyFromClause( " from dual" )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String castPattern(CastType from, CastType to) {
		String result;
		switch ( to ) {
			case INTEGER:
			case LONG:
				result = BooleanDecoder.toInteger( from );
				if ( result != null ) {
					return result;
				}
				break;
			case STRING:
				switch ( from ) {
					case BOOLEAN:
					case INTEGER_BOOLEAN:
					case TF_BOOLEAN:
					case YN_BOOLEAN:
						return BooleanDecoder.toString( from );
					case DATE:
						return "to_char(?1,'YYYY-MM-DD')";
					case TIME:
						return "to_char(?1,'HH24:MI:SS')";
					case TIMESTAMP:
						return "to_char(?1,'YYYY-MM-DD HH24:MI:SS.FF9')";
				}
				break;
			case CLOB:
				return "to_clob(?1)";
			case DATE:
				if ( from == CastType.STRING ) {
					return "to_date(?1,'YYYY-MM-DD')";
				}
				break;
			case TIME:
				if ( from == CastType.STRING ) {
					return "to_date(?1,'HH24:MI:SS')";
				}
				break;
			case TIMESTAMP:
				if ( from == CastType.STRING ) {
					return "to_timestamp(?1,'YYYY-MM-DD HH24:MI:SS.FF9')";
				}
				break;
		}
		return super.castPattern(from, to);
	}


	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.NONE;
	}

}
