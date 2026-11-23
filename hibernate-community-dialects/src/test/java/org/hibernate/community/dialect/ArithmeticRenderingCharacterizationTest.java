package org.hibernate.community.dialect;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseConstant;
import org.hibernate.sql.ast.spi.query.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.StandardTableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupProducer;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcSelectWithActions;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Characterizes how each Dialect renders modulo, portable integer division
/// and an ordinary arithmetic operator between two integer columns.
///
/// The expectations were recorded from the translators while each still
/// overrode `visitBinaryArithmeticExpression`, and are unchanged by moving
/// that customization to `ArithmeticSupport`.
class ArithmeticRenderingCharacterizationTest {

	static Stream<Arguments> dialects() {
		return Stream.of(
				dialect( "H2", H2Dialect::new,
						"(x.a%x.b)", "(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "PostgreSQL", PostgreSQLDialect::new,
						"(x.a%x.b)", "(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "SQLServer", SQLServerDialect::new,
						"(x.a%x.b)", "(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "Sybase", SybaseDialect::new,
						"(x.a%x.b)", "(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "SybaseASE", SybaseASEDialect::new,
						"(x.a%x.b)", "(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "Cockroach", CockroachDialect::new,
						"mod(x.a,x.b)", "floor(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "Oracle", OracleDialect::new,
						"mod(x.a,x.b)", "floor(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "MariaDB", MariaDBDialect::new,
						"mod(x.a,x.b)", "(x.a div x.b)", "(x.a+x.b)" ),
				dialect( "MySQL", MySQLDialect::new,
						"mod(x.a,x.b)", "(x.a div x.b)", "(x.a+x.b)" ),
				dialect( "HANA", HANADialect::new,
						"mod(x.a,x.b)", "cast(x.a/x.b as int)", "(x.a+x.b)" ),
				dialect( "Spanner", SpannerDialect::new,
						"mod(x.a,x.b)", "div(x.a,x.b)", "(x.a+x.b)" ),
				dialect( "DB2", DB2Dialect::new,
						"mod(x.a,x.b)", "(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "HSQL", HSQLDialect::new,
						"mod(x.a,x.b)", "(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "H2Legacy", H2LegacyDialect::new,
						"(x.a%x.b)", "(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "PostgreSQLLegacy", PostgreSQLLegacyDialect::new,
						"(x.a%x.b)", "(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "SQLServerLegacy", SQLServerLegacyDialect::new,
						"(x.a%x.b)", "(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "SybaseASELegacy", SybaseASELegacyDialect::new,
						"(x.a%x.b)", "(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "SybaseAnywhere", SybaseAnywhereDialect::new,
						"(x.a%x.b)", "(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "SybaseLegacy", SybaseLegacyDialect::new,
						"(x.a%x.b)", "(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "Altibase", AltibaseDialect::new,
						"mod(x.a,x.b)", "floor(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "CockroachLegacy", CockroachLegacyDialect::new,
						"mod(x.a,x.b)", "floor(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "Informix", InformixDialect::new,
						"mod(x.a,x.b)", "floor(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "GaussDB", GaussDBDialect::new,
						"(x.a%x.b)", "floor(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "SingleStore", SingleStoreDialect::new,
						"mod(x.a,x.b)", "(x.a div x.b)", "(x.a+x.b)" ),
				dialect( "TiDB", TiDBDialect::new,
						"mod(x.a,x.b)", "(x.a div x.b)", "(x.a+x.b)" ),
				dialect( "HANALegacy", HANALegacyDialect::new,
						"mod(x.a,x.b)", "cast(x.a/x.b as int)", "(x.a+x.b)" ),
				dialect( "InterSystemsIRIS", InterSystemsIRISDialect::new,
						"mod(x.a,x.b)", "x.a \\ x.b", "(x.a+x.b)" ),
				dialect( "Derby", DerbyDialect::new,
						"mod(x.a,x.b)", "(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "MySQLLegacy", MySQLLegacyDialect::new,
						"mod(x.a,x.b)", "(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "MariaDBLegacy", MariaDBLegacyDialect::new,
						"mod(x.a,x.b)", "(x.a/x.b)", "(x.a+x.b)" ),
				dialect( "OracleLegacy", OracleLegacyDialect::new,
						"mod(x.a,x.b)", "(x.a/x.b)", "(x.a+x.b)" )
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dialects")
	void rendersArithmeticAsBefore(
			String name, Supplier<Dialect> dialect, String modulo, String integerDivision, String addition) {
		assertThat( render( dialect.get(), BinaryArithmeticOperator.MODULO ) ).as( "modulo" ).isEqualTo( modulo );
		assertThat( render( dialect.get(), BinaryArithmeticOperator.DIVIDE_PORTABLE ) )
				.as( "portable integer division" ).isEqualTo( integerDivision );
		assertThat( render( dialect.get(), BinaryArithmeticOperator.ADD ) ).as( "addition" ).isEqualTo( addition );
	}

	static String render(Dialect dialect, BinaryArithmeticOperator operator) {
		final var factory = mock( SessionFactoryImplementor.class, RETURNS_DEEP_STUBS );
		final var types = new TypeConfiguration();
		final BasicType<Integer> integerType = types.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER );
		when( factory.getTypeConfiguration() ).thenReturn( types );
		when( factory.getJdbcServices().getDialect() ).thenReturn( dialect );
		when( factory.getJdbcSelectWithActionsBuilder() ).thenAnswer( invocation -> new JdbcSelectWithActions.Builder() );

		final var table = new NamedTableReference( "t", "x" );
		final var querySpec = new QuerySpec( true );
		querySpec.getFromClause().addRoot( new StandardTableGroup(
				true, new NavigablePath( "T" ), mock( TableGroupProducer.class ), "x", table,
				new SqlAliasBaseConstant( "x" ), factory ) );
		querySpec.getSelectClause().addSqlSelection( new SqlSelectionImpl( 0, new BinaryArithmeticExpression(
				new ColumnReference( table, "a", integerType ),
				operator,
				new ColumnReference( table, "b", integerType ),
				integerType
		) ) );
		final String sql = dialect.getSqlAstTranslatorFactory()
				.buildTranslator( new SqlAstTranslationRequest.Select( factory, new SelectStatement( querySpec ) ) )
				.translate( null, new QueryOptionsImpl() )
				.getSqlString();
		// keep only the select item
		return sql.substring( "select ".length(), sql.indexOf( " from " ) );
	}

	private static Arguments dialect(
			String name, Supplier<Dialect> dialect, String modulo, String integerDivision, String addition) {
		return arguments( name, dialect, modulo, integerDivision, addition );
	}
}
