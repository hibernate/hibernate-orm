/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DB2iDialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.from.FromClause;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.SqlAstJoinType;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoin;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests the immutable single-row-table provider contract, maintained Dialect
/// profiles, semantic SQL AST consumers, and independent cross-join boundary.
///
/// @author Steve Ebersole
public class SingleRowTableSupportTests {
	@Test
	void standardAndBuildersPreserveExactIndependentValues() {
		assertProfile( SingleRowTableSupport.STANDARD, "(values(0))", "" );

		final SingleRowTableSupport named = SingleRowTableSupport.builder()
				.tableExpression( "  fixture_table  " )
				.selectOnlyFromClause( " from fixture_table fixture_alias where fixture_key=1 " )
				.build();
		assertProfile(
				named,
				"  fixture_table  ",
				" from fixture_table fixture_alias where fixture_key=1 "
		);

		final SingleRowTableSupport copied = SingleRowTableSupport.builder( named )
				.tableExpression( "(select 1)" )
				.selectOnlyFromClause( "" )
				.build();
		assertProfile( copied, "(select 1)", "" );
		assertProfile(
				named,
				"  fixture_table  ",
				" from fixture_table fixture_alias where fixture_key=1 "
		);
	}

	@Test
	@SuppressWarnings("NullAway")
	void constructionRejectsNullAndBlankInputs() {
		assertThatIllegalArgumentException().isThrownBy( () -> SingleRowTableSupport.builder( null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> SingleRowTableSupport.builder().tableExpression( null ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> SingleRowTableSupport.builder().tableExpression( "" ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> SingleRowTableSupport.builder().tableExpression( " \t" ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> SingleRowTableSupport.builder().selectOnlyFromClause( null ) );
	}

	@Test
	void maintainedCoreProfilesAndInheritedDescendantsPreserveProviderPairs() {
		assertProfile( new Dialect( DatabaseVersion.make( 1 ) ) {
		}, "(values(0))", "" );
		assertProfile( new H2Dialect(), "dual", "" );
		assertProfile( new HANADialect(), "sys.dummy", " from sys.dummy" );
		assertProfile( new HSQLDialect(), "(values(0))", " from (values(0))" );
		assertProfile( new MySQLDialect(), "dual", "" );
		assertProfile( new MariaDBDialect(), "dual", "" );
		assertProfile( new DB2Dialect(), "sysibm.sysdummy1", " from sysibm.sysdummy1" );
		assertProfile( new DB2iDialect(), "sysibm.sysdummy1", " from sysibm.sysdummy1" );
		assertProfile( new DB2zDialect(), "sysibm.sysdummy1", " from sysibm.sysdummy1" );
		assertProfile( new SpannerDialect(), "unnest([1])", " from unnest([1]) dual" );
		assertProfile(
				new SpannerPostgreSQLDialect(),
				"unnest(ARRAY[1])",
				" from unnest(ARRAY[1]) dual"
		);
		assertProfile( new SybaseASEDialect(), "(select 1 c1)", "" );
	}

	@Test
	void oracleSelectOnlyFragmentTransitionRemainsExact() {
		assertProfile( new OracleDialect( DatabaseVersion.make( 22 ) ), "dual", " from dual" );
		assertProfile( new OracleDialect( DatabaseVersion.make( 23 ) ), "dual", "" );
	}

	@Test
	void semanticConsumersUseTheIndependentRenderingValues() {
		final SingleRowTableSupport support = SingleRowTableSupport.builder()
				.tableExpression( "(select 1 as fixture_value)" )
				.selectOnlyFromClause( " from fixture_table fixture_alias where fixture_key=1" )
				.build();

		final TestingTranslator selectTranslator = createTranslator( support, true );
		selectTranslator.renderEmptyFromClause();
		assertThat( selectTranslator.renderedSql() )
				.isEqualTo( " from fixture_table fixture_alias where fixture_key=1" );

		final TestingTranslator tupleTranslator = createTranslator( support, true );
		final Expression lhs = renderingExpression( tupleTranslator, "lhs" );
		final Expression rhs = renderingExpression( tupleTranslator, "rhs" );
		tupleTranslator.renderNotDistinctFromEmulation( lhs, rhs );
		assertThat( tupleTranslator.renderedSql() )
				.isEqualTo(
						"exists (select 1 from (select 1 as fixture_value) d_ where "
								+ "(lhs=rhs or lhs is null and rhs is null))"
				);
	}

	@Test
	void crossJoinRemainsAnIndependentNativeOrEmulatedSupplyPoint() {
		final TestingTranslator nativeTranslator = createTranslator( SingleRowTableSupport.STANDARD, true );
		nativeTranslator.renderCrossJoin();
		assertThat( nativeTranslator.renderedSql() )
				.isEqualTo( " from root_table r cross join joined_table j" );

		final TestingTranslator emulatingTranslator = createTranslator( SingleRowTableSupport.STANDARD, false );
		emulatingTranslator.renderCrossJoin();
		assertThat( emulatingTranslator.renderedSql() )
				.isEqualTo( " from root_table r join joined_table j on (1=1)" );
	}

	private static void assertProfile(Dialect dialect, String tableExpression, String selectOnlyFromClause) {
		assertProfile( dialect.getSingleRowTableSupport(), tableExpression, selectOnlyFromClause );
	}

	private static void assertProfile(
			SingleRowTableSupport support,
			String tableExpression,
			String selectOnlyFromClause) {
		assertThat( support.getTableExpression() ).isEqualTo( tableExpression );
		assertThat( support.getSelectOnlyFromClause() ).isEqualTo( selectOnlyFromClause );
	}

	private static Expression renderingExpression(TestingTranslator translator, String sql) {
		final Expression expression = mock( Expression.class );
		doAnswer( invocation -> {
			translator.appendSql( sql );
			return null;
		} ).when( expression ).accept( translator );
		return expression;
	}

	private static TestingTranslator createTranslator(
			SingleRowTableSupport support,
			boolean crossJoinSupported) {
		final SessionFactoryImplementor sessionFactory = mock( SessionFactoryImplementor.class );
		final JdbcServices jdbcServices = mock( JdbcServices.class );
		when( sessionFactory.getJdbcServices() ).thenReturn( jdbcServices );
		when( sessionFactory.getTypeConfiguration() ).thenReturn( new TypeConfiguration() );
		final Dialect dialect = new Dialect( DatabaseVersion.make( 1 ) ) {
			@Override
			public SingleRowTableSupport getSingleRowTableSupport() {
				return support;
			}

			@Override
			public SetOperationSupport getSetOperationSupport() {
				return SetOperationSupport.NONE;
			}

			@Override
			public boolean supportsCrossJoin() {
				return crossJoinSupported;
			}
		};
		when( jdbcServices.getDialect() ).thenReturn( dialect );
		return new TestingTranslator( new SqlAstTranslationRequest.Select(
				sessionFactory,
				new SelectStatement( new QuerySpec( true ) )
		) );
	}

	private static TableGroup tableGroup(String tableExpression, String alias) {
		final TableGroup tableGroup = mock( TableGroup.class );
		when( tableGroup.isInitialized() ).thenReturn( true );
		when( tableGroup.canUseInnerJoins() ).thenReturn( true );
		when( tableGroup.getPrimaryTableReference() )
				.thenReturn( new NamedTableReference( tableExpression, alias ) );
		when( tableGroup.getTableReferenceJoins() ).thenReturn( List.of() );
		return tableGroup;
	}

	private static class TestingTranslator extends StandardSqlAstTranslator<JdbcSelect> {
		private TestingTranslator(SqlAstTranslationRequest.Select request) {
			super( request );
		}

		private void renderEmptyFromClause() {
			visitFromClause( new FromClause() );
		}

		private void renderNotDistinctFromEmulation(Expression lhs, Expression rhs) {
			emulateTupleComparison(
					List.of( lhs ),
					List.of( rhs ),
					ComparisonOperator.NOT_DISTINCT_FROM,
					false
			);
		}

		private void renderCrossJoin() {
			final TableGroup root = tableGroup( "root_table", "r" );
			final TableGroup joined = tableGroup( "joined_table", "j" );
			final TableGroupJoin join = new TableGroupJoin(
					new NavigablePath( "joined" ),
					SqlAstJoinType.CROSS,
					joined,
					null
			);
			doAnswer( invocation -> {
				final Consumer<TableGroupJoin> consumer = invocation.getArgument( 0 );
				consumer.accept( join );
				return null;
			} ).when( root ).visitTableGroupJoins( any() );
			final FromClause fromClause = new FromClause();
			fromClause.addRoot( root );
			visitFromClause( fromClause );
		}

		private String renderedSql() {
			return getSql();
		}
	}
}
