/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import org.hibernate.dialect.type.spi.TypeSizingProfile;

import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import java.sql.Types;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.community.dialect.sequence.MaxDBSequenceSupport;
import org.hibernate.dialect.AbstractTransactSQLDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lob.spi.LobSupport;
import org.hibernate.dialect.lob.spi.LobSupports;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.LimitLimitHandler;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.community.dialect.temptable.internal.MaxDBLocalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import static org.hibernate.dialect.SimpleDatabaseVersion.ZERO_VERSION;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;

/**
 * A SQL dialect compatible with SAP MaxDB.
 *
 * @author Brad Clow
 */
public class MaxDBDialect extends Dialect {
	private SchemaDropSupport schemaDropSupport;

	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.maxVarbinaryLength( TypeSizingProfile.UNSUPPORTED )
			.maxVarbinaryCapacity( TypeSizingProfile.UNSUPPORTED )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

	public MaxDBDialect() {
		super( ZERO_VERSION );
	}

	public MaxDBDialect(DialectResolutionInfo info) {
		super( info );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case TINYINT:
				return "smallint";
			case BIGINT:
				return "fixed(19,0)";
			case NUMERIC:
			case DECIMAL:
				return "fixed($p,$s)";
			//no explicit precision
			case TIMESTAMP:
			case TIMESTAMP_WITH_TIMEZONE:
				return "timestamp";
			case VARBINARY:
			case BLOB:
				return "long byte";
			case CLOB:
				return "long varchar";
			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		switch ( jdbcTypeCode ) {
			case Types.NUMERIC:
			case Types.DECIMAL:
				if ( precision == 19 && scale == 0 ) {
					return jdbcTypeRegistry.getDescriptor( Types.BIGINT );
				}
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
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, Integer.toString( 15 ) );
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LimitLimitHandler.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.log();
		functionFactory.pi();
		functionFactory.cot();
		functionFactory.cosh();
		functionFactory.sinh();
		functionFactory.tanh();
		functionFactory.radians();
		functionFactory.degrees();
		functionFactory.trunc();
		functionFactory.trim2();
		functionFactory.substr();
		functionFactory.substring_substr();
		functionFactory.translate();
		functionFactory.initcap();
		functionFactory.soundex();
		functionFactory.yearMonthDay();
		functionFactory.hourMinuteSecond();
		functionFactory.dayofweekmonthyear();
		functionFactory.daynameMonthname();
		functionFactory.dateTimeTimestamp();
		functionFactory.ceiling_ceil();
		functionFactory.week_weekofyear();
		functionFactory.concat_pipeOperator();
		functionFactory.coalesce_value();
		//since lpad/rpad are not actually useful padding
		//functions, map them to lfill/rfill
		functionFactory.pad_fill();
		functionFactory.datediff();
		functionFactory.adddateSubdateAddtimeSubtime();
		functionFactory.addMonths();

		final BasicType<Integer> integerType = functionContributions.getTypeConfiguration().getBasicTypeRegistry()
				.resolve( StandardBasicTypes.INTEGER );
		functionContributions.getFunctionRegistry().registerPattern( "extract", "?1(?2)", integerType );

		functionContributions.getFunctionRegistry().patternDescriptorBuilder( "nullif", "case ?1 when ?2 then null else ?1 end" )
				.setExactArgumentCount(2)
				.register();

		functionContributions.getFunctionRegistry().namedDescriptorBuilder( "index" )
				.setInvariantType( integerType )
				.setArgumentCountBetween( 2, 4 )
				.register();

		functionContributions.getFunctionRegistry().registerBinaryTernaryPattern(
				"locate",
				integerType, "index(?2,?1)", "index(?2,?1,?3)",
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
				return new MaxDBSqlAstTranslator<>( request );
			}
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String trimPattern(TrimSpec specification, boolean isWhitespace) {
		return AbstractTransactSQLDialect.replaceLtrimRtrim( specification, isWhitespace );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized SchemaDropSupport getSchemaDropSupport() {
		if ( schemaDropSupport == null ) {
			schemaDropSupport = new SchemaDropSupport( java.util.List.of(), ConstraintDropMode.IMPLICIT, "" );
		}
		return schemaDropSupport;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String addColumnPrefix() {
		return "add";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String renderAddConstraint(
			org.hibernate.dialect.constraint.spi.ForeignKeyConstraintRequest request) {
		if ( request.isExplicitDefinition() ) {
			return request.explicitDefinition();
		}
		final StringBuilder res = new StringBuilder( 30 )
				.append( " foreign key " )
				.append( request.constraintName() )
				.append( " (" )
				.append( String.join( ", ", request.sourceColumnNames() ) )
				.append( ") references " )
				.append( request.referencedTableName() );

		if ( !request.referencesPrimaryKey() ) {
			res.append( " (" )
					.append( String.join( ", ", request.targetColumnNames() ) )
					.append( ')' );
		}

		return res.toString().stripLeading();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDefinition(org.hibernate.sql.spi.SqlAppender appender, ColumnDefinitionRequest request) {
		super.appendDefinition( appender, request );
		if ( request.nullable() ) {
			appender.appendSql( " null" );
		}
	}

	@Override
	public LockingSupport getLockingSupport() {
		return StandardLockingSupports.standard();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return MaxDBSequenceSupport.INSTANCE;
	}

	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder( "select * from domain.sequences" )
					.withoutCatalog()
					.schemaColumn( "schemaname" )
					.withoutStartValue()
					.minimumValueColumn( "min_value" )
					.maximumValueColumn( "max_value" )
					.incrementValueColumn( "increment_by" )
					.build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.OFFSET, true )
				.build();
	}

	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return MultiTableMutationSupport.LOCAL_TEMPORARY_TABLE;
	}

	@Override
	public TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		return MaxDBLocalTemporaryTableStrategy.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public LobSupport getLobSupport() {
		return LobSupports.noContextualCreation();
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		final String tableExpression = "dual";
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( tableExpression )
				.selectOnlyFromClause( " from " + tableExpression )
				.build();
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.NONE;
	}

}
