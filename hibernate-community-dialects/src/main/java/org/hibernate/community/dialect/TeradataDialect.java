/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.identifier.spi.KeywordRegistration;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import org.hibernate.dialect.type.spi.TypeSizingProfile;

import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;

import org.hibernate.LockOptions;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.community.dialect.identity.internal.Teradata14IdentityColumnSupport;
import org.hibernate.community.dialect.lock.internal.TeradataLockingSupport;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.dialect.lob.spi.LobSupport;
import org.hibernate.dialect.lob.spi.LobSupports;
import org.hibernate.dialect.jdbc.spi.ParameterLimits;
import org.hibernate.dialect.lock.spi.RowLockStrategy;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.schema.spi.TableCreationKind;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.dialect.schema.spi.TruncateRequest;
import org.hibernate.dialect.pagination.spi.TopLimitHandler;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.community.dialect.temptable.internal.TeradataGlobalTemporaryTableStrategy;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.mapping.Index;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.dialect.lock.spi.LockingClauseStrategy;
import org.hibernate.dialect.lock.spi.StandardLockingClauseStrategies;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.spi.StandardIndexExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

import java.sql.Types;
import java.util.List;
import java.util.Set;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.internal.util.StringHelper.unroot;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;

/**
 * A dialect for the Teradata database created by MCR as part of the
 * dialect certification process.
 *
 * @author Jay Nance
 */
public class TeradataDialect extends Dialect implements TemporalOperationSupport {

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalOperationSupport getTemporalOperationSupport() {
		return this;
	}
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.defaultDecimalPrecision( getVersion().isBefore( 14 ) ? 18 : 38 )
			.maxVarcharLength( 32_000 ).maxVarcharCapacity( 32_000 )
			.maxNVarcharLength( 32_000 ).maxNVarcharCapacity( 32_000 )
			.maxVarbinaryLength( 64_000 ).maxVarbinaryCapacity( 64_000 )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

	private static final DatabaseVersion DEFAULT_VERSION = DatabaseVersion.make( 12, 0 );

	private static final int PARAM_LIST_SIZE_LIMIT = 1024;

	public TeradataDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( DEFAULT_VERSION ) );
	}

	public TeradataDialect() {
		this( DEFAULT_VERSION );
	}

	public TeradataDialect(DatabaseVersion version) {
		super( version );
		lockingSupport = new TeradataLockingSupport( version.isSameOrAfter( 14 ) );
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected void contributeKeywords(KeywordRegistration registration) {
		super.contributeKeywords( registration );
		registration.registerKeyword( "password" );
		registration.registerKeyword( "type" );
		registration.registerKeyword( "title" );
		registration.registerKeyword( "year" );
		registration.registerKeyword( "month" );
		registration.registerKeyword( "summary" );
		registration.registerKeyword( "alias" );
		registration.registerKeyword( "value" );
		registration.registerKeyword( "first" );
		registration.registerKeyword( "role" );
		registration.registerKeyword( "account" );
		registration.registerKeyword( "class" );
		registration.registerKeyword( "title" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case BOOLEAN,TINYINT -> "byteint";
			//'bigint' has been there since at least version 13
			case BIGINT -> getVersion().isBefore( 13 ) ? "numeric(19,0)" : "bigint";
			case BINARY -> "byte($l)";
			case VARBINARY -> "varbyte($l)";
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, Integer.toString( getVersion().isBefore( 14 )
				? 0 : 15 ) );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public LobSupport getLobSupport() {
		return LobSupports.nonStreaming();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName, int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		switch ( jdbcTypeCode ) {
			case Types.BIT:
				return jdbcTypeRegistry.getDescriptor( Types.BOOLEAN );
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
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new TeradataSqlAstTranslator<>( request );
			}
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BIT;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public long fractionalSecondPrecisionInNanos() {
		// Do duration arithmetic in a seconds, but
		// with the fractional part
		return 1_000_000_000; //seconds!!
	}

	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		StringBuilder pattern = new StringBuilder();
		//TODO: TOTALLY UNTESTED CODE!
		pattern.append("cast((?3-?2) ");
		switch (unit) {
			case NANOSECOND:
			case NATIVE:
				//default fractional precision is 6, the maximum
				pattern.append("second");
				break;
			case WEEK:
				pattern.append("day");
				break;
			case QUARTER:
				pattern.append("month");
				break;
			default:
				pattern.append( "?1" );
		}
		pattern.append("(4) as bigint)");
		switch (unit) {
			case WEEK:
				pattern.append("/7");
				break;
			case QUARTER:
				pattern.append("/3");
				break;
			case NANOSECOND:
				pattern.append("*1e9");
				break;
		}
		return pattern.toString();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		//TODO: TOTALLY UNTESTED CODE!
		switch ( unit ) {
			case NANOSECOND:
				return "(?3+(?2)/1e9*interval '1' second)";
			case NATIVE:
				return "(?3+(?2)*interval '1' second)";
			case QUARTER:
				return "(?3+(?2)*interval '3' month)";
			case WEEK:
				return "(?3+(?2)*interval '7' day)";
			default:
				return "(?3+(?2)*interval '1' ?1)";
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);
		final BasicTypeRegistry basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<String> stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.concat_pipeOperator();
		functionFactory.octetLength();
		functionFactory.moreHyperbolic();
		functionFactory.instr();
		functionFactory.substr();
		functionFactory.substring_substr();
		//also natively supports ANSI-style substring()
		functionFactory.position();
		functionFactory.bitLength_pattern( "octet_length(cast(?1 as char))*4" );

		functionContributions.getFunctionRegistry().patternDescriptorBuilder( "mod", "(?1 mod ?2)" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 2 )
				.register();

		if ( getVersion().isSameOrAfter( 14 ) ) {

			//list actually taken from Teradata 15 docs
			functionFactory.lastDay();
			functionFactory.initcap();
			functionFactory.trim2();
			functionFactory.soundex();
			functionFactory.ascii();
			functionFactory.char_chr();
			functionFactory.trunc();
			functionFactory.moreHyperbolic();
			functionFactory.monthsBetween();
			functionFactory.addMonths();
			functionFactory.stddevPopSamp();
			functionFactory.varPopSamp();
		}

		// No idea since when this is supported
		functionFactory.windowFunctions();
		functionFactory.inverseDistributionOrderedSetAggregates();
		functionFactory.hypotheticalOrderedSetAggregates();
	}


	@Override
	@SPI({ USE, IMPLEMENT })
	public String addColumnPrefix() {
		return getVersion().isBefore( 14 ) ? super.addColumnPrefix() : "add";
	}

	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return MultiTableMutationSupport.GLOBAL_TEMPORARY_TABLE;
	}

	@Override
	public TemporaryTableStrategy getGlobalTemporaryTableStrategy() {
		return TeradataGlobalTemporaryTableStrategy.INSTANCE;
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
	public boolean supportsOnDeleteAction(org.hibernate.annotations.OnDeleteAction action) {
		return action == org.hibernate.annotations.OnDeleteAction.NO_ACTION;
	}

	@Override
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.NONE;
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.EXISTS_IN_SELECT, false )
				.feature( SubquerySupport.Feature.ORDER_BY, false )
				.build();
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		return getVersion().isBefore( 16, 10 )
				? WindowFunctionSupport.NONE
				: WindowFunctionSupport.builder()
						.features(
								WindowFunctionSupport.Feature.WINDOW_FUNCTIONS,
								WindowFunctionSupport.Feature.PARTITION_BY,
								WindowFunctionSupport.Feature.ROWS_FRAME
						)
						.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getSelectClauseNullString(SqlTypedMapping sqlTypeMapping, TypeConfiguration typeConfiguration) {
		final int sqlType = sqlTypeMapping.getJdbcMapping().getJdbcType().getDdlTypeCode();
		String v = "null";

		switch ( sqlType ) {
			case Types.BIT:
			case Types.TINYINT:
			case Types.SMALLINT:
			case Types.INTEGER:
			case Types.BIGINT:
			case Types.FLOAT:
			case Types.REAL:
			case Types.DOUBLE:
			case Types.NUMERIC:
			case Types.DECIMAL:
				v = "cast(null as decimal)";
				break;
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
				v = "cast(null as varchar(255))";
				break;
			case Types.DATE:
			case Types.TIME:
			case Types.TIMESTAMP:
			case Types.TIMESTAMP_WITH_TIMEZONE:
				v = "cast(null as timestamp)";
				break;
			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
			case Types.NULL:
			case Types.OTHER:
			case Types.JAVA_OBJECT:
			case Types.DISTINCT:
			case Types.STRUCT:
			case Types.ARRAY:
			case Types.BLOB:
			case Types.CLOB:
			case Types.REF:
			case Types.DATALINK:
			case Types.BOOLEAN:
				break;
		}
		return v;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String createTableCommand(TableCreationKind kind) {
		return kind == TableCreationKind.MULTISET ? "create multiset table " : super.createTableCommand( kind );
	}

	@Override
	public ParameterLimits getParameterLimits() {
		return ParameterLimits.of( PARAM_LIST_SIZE_LIMIT );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return getVersion().isBefore( 14 ) ? super.getViolatedConstraintNameExtractor() : EXTRACTOR;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				String constraintName;
				switch ( sqle.getErrorCode() ) {
					case 27003:
						constraintName = extractUsingTemplate( "Unique constraint (", ") violated.", sqle.getMessage() );
						break;
					case 2700:
						constraintName = extractUsingTemplate( "Referential constraint", "violation:", sqle.getMessage() );
						break;
					case 5317:
						constraintName = extractUsingTemplate( "Check constraint (", ") violated.", sqle.getMessage() );
						break;
					default:
						return null;
				}

				return constraintName == null ? null : unroot( constraintName );
			} );

	private final LockingSupport lockingSupport;

	@Override
	public LockingSupport getLockingSupport() {
		return lockingSupport;
	}

	@Override
	protected LockingClauseStrategy buildLockingClauseStrategy(
			PessimisticLockKind lockKind,
			RowLockStrategy rowLockStrategy,
			LockOptions lockOptions,
			Set<NavigablePath> rootPathsForLocking) {
		if ( getVersion().isBefore( 14 ) ) {
			return StandardLockingClauseStrategies.none();
		}
		// we'll reuse the StandardLockingClauseStrategy for the collecting
		// aspect and just handle the special rendering in the SQL AST translator
		return super.buildLockingClauseStrategy( lockKind, rowLockStrategy, lockOptions, rootPathsForLocking );
	}

	@Override
	public Exporter<Index> getIndexExporter() {
		return new TeradataIndexExporter(this);
	}

	private static final class TeradataIndexExporter implements Exporter<Index> {
		private final Dialect dialect;
		private final StandardIndexExporter standardExporter;

		private TeradataIndexExporter(Dialect dialect) {
			this.dialect = dialect;
			this.standardExporter = new StandardIndexExporter( dialect );
		}

		@Override
		public String[] getSqlCreateStrings(Index index, Metadata metadata, SqlStringGenerationContext context) {
			QualifiedTableName qualifiedTableName = index.getTable().getQualifiedTableName();
			final String tableName = context.format( qualifiedTableName );

			final String indexNameForCreation;
			if ( dialect.getIndexDdlSupport().nameQualification() == IndexNameQualification.QUALIFIED ) {
				indexNameForCreation = context.format(
						new QualifiedNameImpl(
								qualifiedTableName.getCatalogName(),
								qualifiedTableName.getSchemaName(),
								Identifier.toIdentifier( index.getName() )
						)
				);
			}
			else {
				indexNameForCreation = index.getName();
			}

			final StringBuilder columnList = new StringBuilder();
			boolean first = true;
			for ( var selectable : index.getSelectables() ) {
				if ( first ) {
					first = false;
				}
				else {
					columnList.append( ", " );
				}
				columnList.append( selectable.getText( dialect ) );
			}

			return new String[] {
					"create index " + indexNameForCreation
							+ "(" + columnList + ") on " + tableName
			};
		}

		@Override
		public String[] getSqlDropStrings(Index index, Metadata metadata, SqlStringGenerationContext context) {
			return standardExporter.getSqlDropStrings( index, metadata, context );
		}
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return getVersion().isBefore( 14 )
				? super.getIdentityColumnSupport()
				: Teradata14IdentityColumnSupport.INSTANCE;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return new TopLimitHandler( false );
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.NONE;
	}

	/**
	 * Teradata uses the syntax {@code DELETE FROM <tablename> ALL instead of TRUNCATE <tablename>}
	 * @param request the truncate request
	 * @return the commands which implement the truncate request
	 */
	public List<String> renderCommands(TruncateRequest request) {
		return request.tableNames().stream().map( name -> "delete from " + name + " all" ).toList();
	}

}
