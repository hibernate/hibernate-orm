package org.hibernate.community.dialect;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.AzureSQLServerDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DB2iDialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.internal.ParameterMarkerStrategyStandard;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseConstant;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.spi.query.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.spi.query.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.StandardTableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupProducer;
import org.hibernate.sql.ast.spi.query.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.spi.query.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.internal.JdbcSelectWithActions;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/// Characterizes how each Dialect renders four constructs whose customization
/// moves from translator overrides to support capabilities: a boolean-valued
/// expression used as a predicate, plain and negated; an arithmetic operand
/// which is a parameter; and searched and simple CASE expressions all of whose
/// result arms are parameters.
///
/// Each construct is rendered in the WHERE clause, where the parameter
/// rendering mode is the default one. The cast function is replaced by a
/// marker, `cast(...)`, which renders its argument, so the expectations show
/// where a Dialect asks for a typed parameter without depending on its cast
/// syntax.
///
/// The expectations were recorded from the translators while each still
/// carried its override.
class LeafRenderingCharacterizationTest {

	static Stream<Arguments> dialects() {
		return Stream.of(
				dialect( "H2", H2Dialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+cast(?))=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "PostgreSQL", PostgreSQLDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "SQLServer", SQLServerDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "Sybase", SybaseDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" ),
				dialect( "SybaseASE", SybaseASEDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" ),
				dialect( "Cockroach", CockroachDialect::new,
						"x.flag", "(x.flag=false)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "Oracle", OracleDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "MariaDB", MariaDBDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "MySQL", MySQLDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "HANA", HANADialect::new,
						"(x.flag=true)", "(x.flag=false)",
						"(x.a+cast(?))=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "Spanner", SpannerDialect::new,
						"(x.flag=true)", "(x.flag=false)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "DB2", DB2Dialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+cast(?))=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" ),
				dialect( "HSQL", HSQLDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+cast(?))=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" ),
				dialect( "DB2i", DB2iDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+cast(?))=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" ),
				dialect( "DB2z", DB2zDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+cast(?))=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" ),
				dialect( "SpannerPostgreSQL", SpannerPostgreSQLDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "PostgresPlus", PostgresPlusDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "AzureSQLServer", AzureSQLServerDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "H2Legacy", H2LegacyDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+cast(?))=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "PostgreSQLLegacy", PostgreSQLLegacyDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "SQLServerLegacy", SQLServerLegacyDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "SybaseASELegacy", SybaseASELegacyDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" ),
				dialect( "SybaseAnywhere", SybaseAnywhereDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" ),
				dialect( "SybaseLegacy", SybaseLegacyDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" ),
				dialect( "Altibase", AltibaseDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "CockroachLegacy", CockroachLegacyDialect::new,
						"x.flag", "(x.flag=false)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "Informix", InformixDialect::new,
						"(x.flag=cast('t' as boolean))", "(x.flag=cast('f' as boolean))",
						"(x.a+cast(?))=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "GaussDB", GaussDBDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "SingleStore", SingleStoreDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "TiDB", TiDBDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "HANALegacy", HANALegacyDialect::new,
						"(x.flag=true)", "(x.flag=false)",
						"(x.a+cast(?))=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "InterSystemsIRIS", InterSystemsIRISDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "Derby", DerbyDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+cast(?))=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" ),
				dialect( "MySQLLegacy", MySQLLegacyDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "MariaDBLegacy", MariaDBLegacyDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "OracleLegacy", OracleLegacyDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"decode( x.a,?,?)=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "Firebird", FirebirdDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" ),
				dialect( "DB2Legacy", DB2LegacyDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+cast(?))=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" ),
				dialect( "DerbyLegacy", DerbyLegacyDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+cast(?))=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" ),
				dialect( "HSQLLegacy", HSQLLegacyDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+cast(?))=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" ),
				dialect( "DB2iLegacy", DB2iLegacyDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+cast(?))=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" ),
				dialect( "DB2zLegacy", DB2zLegacyDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+cast(?))=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" ),
				dialect( "Teradata", TeradataDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "TimesTen", TimesTenDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "Ingres", IngresDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "Cache", CacheDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "CUBRID", CUBRIDDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "MaxDB", MaxDBDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"decode( x.a,?,?)=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "MimerSQL", MimerSQLDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "RDMSOS2200", RDMSOS2200Dialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"decode( x.a,?,?)=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "SQLite", SQLiteDialect::new,
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "PostgresPlusLegacy", PostgresPlusLegacyDialect::new,
						"x.flag", "not(x.flag)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then ? else ? end=x.b",
						"case x.a when x.b then ? else ? end=x.b" ),
				dialect( "Firebird 2.5", () -> new FirebirdDialect( DatabaseVersion.make( 2, 5 ) ),
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" ),
				dialect( "Firebird 3", () -> new FirebirdDialect( DatabaseVersion.make( 3 ) ),
						"x.flag", "not(x.flag)",
						"(x.a+?)=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" ),
				dialect( "DB2Legacy 10.5", () -> new DB2LegacyDialect( DatabaseVersion.make( 10, 5 ) ),
						"(x.flag=1)", "(x.flag=0)",
						"(x.a+cast(?))=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" ),
				dialect( "DB2Legacy 11", () -> new DB2LegacyDialect( DatabaseVersion.make( 11 ) ),
						"x.flag", "not(x.flag)",
						"(x.a+cast(?))=x.b",
						"case when x.a=x.b then cast(?) else ? end=x.b",
						"case x.a when x.b then cast(?) else ? end=x.b" )
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dialects")
	void rendersAsBefore(
			String name,
			Supplier<Dialect> dialect,
			String booleanPredicate,
			String negatedBooleanPredicate,
			String parameterOperand,
			String searchedCase,
			String simpleCase) {
		assertThat( render( dialect.get(), Construct.BOOLEAN ) ).as( "boolean predicate" ).isEqualTo( booleanPredicate );
		assertThat( render( dialect.get(), Construct.NEGATED_BOOLEAN ) )
				.as( "negated boolean predicate" ).isEqualTo( negatedBooleanPredicate );
		assertThat( render( dialect.get(), Construct.PARAMETER_OPERAND ) )
				.as( "parameter as arithmetic operand" ).isEqualTo( parameterOperand );
		assertThat( render( dialect.get(), Construct.SEARCHED_CASE ) ).as( "searched case" ).isEqualTo( searchedCase );
		assertThat( render( dialect.get(), Construct.SIMPLE_CASE ) ).as( "simple case" ).isEqualTo( simpleCase );
	}

	enum Construct { BOOLEAN, NEGATED_BOOLEAN, PARAMETER_OPERAND, SEARCHED_CASE, SIMPLE_CASE }

	static String render(Dialect dialect, Construct construct) {
		final var factory = mock( SessionFactoryImplementor.class, RETURNS_DEEP_STUBS );
		final var types = new TypeConfiguration();
		final BasicType<Integer> integerType = types.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER );
		final BasicType<Boolean> booleanType = types.getBasicTypeRegistry().resolve( StandardBasicTypes.BOOLEAN );
		when( factory.getTypeConfiguration() ).thenReturn( types );
		when( factory.getJdbcServices().getDialect() ).thenReturn( dialect );
		when( factory.getJdbcServices().getParameterMarkerStrategy() )
				.thenReturn( ParameterMarkerStrategyStandard.INSTANCE );
		when( factory.getJdbcSelectWithActionsBuilder() ).thenAnswer( invocation -> new JdbcSelectWithActions.Builder() );
		final var cast = mock( SqmFunctionDescriptor.class, withSettings().extraInterfaces( FunctionRenderer.class ) );
		doAnswer( invocation -> {
			final SqlAppender appender = invocation.getArgument( 0 );
			final List<? extends SqlAstNode> arguments = invocation.getArgument( 1 );
			appender.appendSql( "cast(" );
			arguments.get( 0 ).accept( invocation.<SqlAstTranslator<?>>getArgument( 3 ) );
			appender.appendSql( ")" );
			return null;
		} ).when( (FunctionRenderer) cast ).render( any(), any(), any(), any() );
		when( factory.getQueryEngine().getSqmFunctionRegistry().findFunctionDescriptor( "cast" ) ).thenReturn( cast );

		final var table = new NamedTableReference( "t", "x" );
		final var a = new ColumnReference( table, "a", integerType );
		final var b = new ColumnReference( table, "b", integerType );
		final var flag = new ColumnReference( table, "flag", booleanType );
		final Predicate predicate = switch ( construct ) {
			case BOOLEAN -> new BooleanExpressionPredicate( flag );
			case NEGATED_BOOLEAN -> new BooleanExpressionPredicate( flag, true, booleanType );
			case PARAMETER_OPERAND -> new ComparisonPredicate(
					new BinaryArithmeticExpression( a, BinaryArithmeticOperator.ADD, new JdbcParameterImpl( integerType ), integerType ),
					ComparisonOperator.EQUAL,
					b );
			case SEARCHED_CASE -> {
				final var searched = new CaseSearchedExpression( integerType );
				searched.when( new ComparisonPredicate( a, ComparisonOperator.EQUAL, b ), new JdbcParameterImpl( integerType ) );
				searched.otherwise( new JdbcParameterImpl( integerType ) );
				yield new ComparisonPredicate( searched, ComparisonOperator.EQUAL, b );
			}
			case SIMPLE_CASE -> {
				final var simple = new CaseSimpleExpression( integerType, a );
				simple.when( b, new JdbcParameterImpl( integerType ) );
				simple.otherwise( new JdbcParameterImpl( integerType ) );
				yield new ComparisonPredicate( simple, ComparisonOperator.EQUAL, b );
			}
		};

		final var querySpec = new QuerySpec( true );
		querySpec.getFromClause().addRoot( new StandardTableGroup(
				true, new NavigablePath( "T" ), mock( TableGroupProducer.class ), "x", table,
				new SqlAliasBaseConstant( "x" ), factory ) );
		querySpec.getSelectClause().addSqlSelection( new SqlSelectionImpl( 0, a ) );
		querySpec.applyPredicate( predicate );
		final String sql = dialect.getSqlAstTranslatorFactory()
				.buildTranslator( new SqlAstTranslationRequest.Select( factory, new SelectStatement( querySpec ) ) )
				.translate( JdbcParameterBindings.NO_BINDINGS, new QueryOptionsImpl() )
				.getSqlString();
		// keep only the restriction
		return sql.substring( sql.indexOf( " where " ) + " where ".length() );
	}

	private static Arguments dialect(
			String name,
			Supplier<Dialect> dialect,
			String booleanPredicate,
			String negatedBooleanPredicate,
			String parameterOperand,
			String searchedCase,
			String simpleCase) {
		return arguments( name, dialect, booleanPredicate, negatedBooleanPredicate, parameterOperand, searchedCase, simpleCase );
	}
}
