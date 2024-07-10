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
import java.util.Map;

import org.hibernate.LockOptions;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.community.dialect.identity.Teradata14IdentityColumnSupport;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.TopLimitHandler;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.TemporalUnit;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.sql.ForUpdateFragment;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.internal.StandardIndexExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
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
public class TeradataDialect extends Dialect {

	private static final DatabaseVersion DEFAULT_VERSION = DatabaseVersion.make( 12, 0 );

	private static final int PARAM_LIST_SIZE_LIMIT = 1024;

	public TeradataDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( DEFAULT_VERSION ) );
		registerKeywords( info );
	}

	public TeradataDialect() {
		this( DEFAULT_VERSION );
	}

	public TeradataDialect(DatabaseVersion version) {
		super( version );
	}

	@Override
	protected void registerDefaultKeywords() {
		super.registerDefaultKeywords();
		registerKeyword( "password" );
		registerKeyword( "type" );
		registerKeyword( "title" );
		registerKeyword( "year" );
		registerKeyword( "month" );
		registerKeyword( "summary" );
		registerKeyword( "alias" );
		registerKeyword( "value" );
		registerKeyword( "first" );
		registerKeyword( "role" );
		registerKeyword( "account" );
		registerKeyword( "class" );
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case BOOLEAN:
			case TINYINT:
				return "byteint";
			//'bigint' has been there since at least version 13
			case BIGINT:
				return getVersion().isBefore( 13 ) ? "numeric(19,0)" : "bigint";
			case BINARY:
				return "byte($l)";
			case VARBINARY:
				return "varbyte($l)";
			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	public int getDefaultStatementBatchSize() {
		return getVersion().isBefore( 14 )
				? 0 : 15;
	}

	@Override
	public boolean useInputStreamToInsertBlob() {
		return getVersion().isSameOrAfter( 14 );
	}

	@Override
	public boolean useConnectionToCreateLob() {
		return false;
	}

	@Override
	public int getMaxVarcharLength() {
		//for the unicode server character set
		return 32_000;
	}

	@Override
	public int getMaxVarbinaryLength() {
		return 64_000;
	}

	@Override
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
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new TeradataSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BIT;
	}

	@Override
	public int getDefaultDecimalPrecision() {
		return getVersion().isBefore( 14 ) ? 18 : 38;
	}

	@Override
	public long getFractionalSecondPrecisionInNanos() {
	 	// Do duration arithmetic in a seconds, but
		// with the fractional part
		return 1_000_000_000; //seconds!!
	}

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

	/**
	 * Does this dialect support the {@code FOR UPDATE} syntax?
	 *
	 * @return empty string ... Teradata does not support {@code FOR UPDATE} syntax
	 */
	@Override
	public String getForUpdateString() {
		return "";
	}

	@Override
	public String getAddColumnString() {
		return getVersion().isBefore( 14 ) ? super.getAddColumnString() : "add";
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new GlobalTemporaryTableMutationStrategy(
				TemporaryTable.createIdTable(
						rootEntityDescriptor,
						basename -> TemporaryTable.ID_TABLE_PREFIX + basename,
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
		return "on commit preserve rows";
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
	public boolean supportsCascadeDelete() {
		return false;
	}

	@Override
	public boolean supportsCircularCascadeDeleteConstraints() {
		return false;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}

	@Override
	public boolean supportsOrderByInSubquery() {
		// This is just a guess
		return false;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return getVersion().isSameOrAfter( 16, 10 );
	}

	@Override
	public String getSelectClauseNullString(int sqlType, TypeConfiguration typeConfiguration) {
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
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return false;
	}

	@Override
	public String getCreateMultisetTableString() {
		return "create multiset table ";
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return true;
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return true;
	}

	@Override
	public boolean supportsBindAsCallableArgument() {
		return false;
	}

	@Override
	public int getInExpressionCountLimit() {
		return PARAM_LIST_SIZE_LIMIT;
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		statement.registerOutParameter( col, Types.REF );
		col++;
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement cs) throws SQLException {
		boolean isResultSet = cs.execute();
		while ( !isResultSet && cs.getUpdateCount() != -1 ) {
			isResultSet = cs.getMoreResults();
		}
		return cs.getResultSet();
	}

	@Override
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

				if ( constraintName != null ) {
					int i = constraintName.indexOf( '.' );
					if ( i != -1 ) {
						constraintName = constraintName.substring( i + 1 );
					}
				}
				return constraintName;
			} );

	@Override
	public boolean supportsLockTimeouts() {
		return false;
	}

	@Override
	public boolean supportsNoWait() {
		return true;
	}

	@Override
	public boolean supportsWait() {
		return false;
	}

	@Override
	public boolean useFollowOnLocking(String sql, QueryOptions queryOptions) {
		return getVersion().isSameOrAfter( 14 );
	}

	@Override
	public String getWriteLockString(int timeout) {
		if ( getVersion().isBefore( 14 ) ) {
			return super.getWriteLockString( timeout );
		}
		String sMsg = " Locking row for write ";
		if ( timeout == LockOptions.NO_WAIT ) {
			return sMsg + " nowait ";
		}
		return sMsg;
	}

	@Override
	public String getReadLockString(int timeout) {
		if ( getVersion().isBefore( 14 ) ) {
			return super.getReadLockString( timeout );
		}
		String sMsg = " Locking row for read  ";
		if ( timeout == LockOptions.NO_WAIT ) {
			return sMsg + " nowait ";
		}
		return sMsg;
	}

	@Override
	public Exporter<Index> getIndexExporter() {
		return new TeradataIndexExporter(this);
	}

	private static class TeradataIndexExporter extends StandardIndexExporter implements Exporter<Index> {

		private TeradataIndexExporter(Dialect dialect) {
			super(dialect);
		}

		@Override
		public String[] getSqlCreateStrings(Index index, Metadata metadata, SqlStringGenerationContext context) {
			QualifiedTableName qualifiedTableName = index.getTable().getQualifiedTableName();
			final String tableName = context.format( qualifiedTableName );

			final String indexNameForCreation;
			if ( getDialect().qualifyIndexName() ) {
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
			for ( Column column : index.getColumns() ) {
				if ( first ) {
					first = false;
				}
				else {
					columnList.append( ", " );
				}
				columnList.append( column.getName() );
			}

			return new String[] {
					"create index " + indexNameForCreation
							+ "(" + columnList + ") on " + tableName
			};
		}
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return getVersion().isBefore( 14 )
				? super.getIdentityColumnSupport()
				: Teradata14IdentityColumnSupport.INSTANCE;
	}

	@Override
	public String applyLocksToSql(String sql, LockOptions aliasedLockOptions, Map<String, String[]> keyColumnNames) {
		return getVersion().isBefore( 14 )
				? super.applyLocksToSql( sql, aliasedLockOptions, keyColumnNames )
				: new ForUpdateFragment( this, aliasedLockOptions, keyColumnNames ).toFragmentString() + " " + sql;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return new TopLimitHandler( false );
	}

}
