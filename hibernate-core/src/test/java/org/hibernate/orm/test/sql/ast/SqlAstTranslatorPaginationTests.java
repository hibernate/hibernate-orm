package org.hibernate.orm.test.sql.ast;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.sql.ast.internal.OracleSqlAstTranslator;
import org.hibernate.dialect.sql.ast.internal.SQLServerSqlAstTranslator;
import org.hibernate.dialect.sql.ast.internal.SybaseSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingRequest;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PostgreSQLSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.StandardPaginationRenderingSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.SortDirection;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseConstant;
import org.hibernate.sql.ast.spi.query.expression.QueryLiteral;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.StandardTableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupProducer;
import org.hibernate.sql.ast.spi.query.select.QueryGroup;
import org.hibernate.sql.ast.spi.query.select.QueryPart;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.query.select.SortSpecification;
import org.hibernate.sql.exec.internal.JdbcSelectWithActions;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Step 3 of the SQL AST translator convention work, pagination: the trailing
/// offset/fetch visitor is final, a Dialect rejects what it cannot render
/// through its pagination support, and grammar the visitor used to be
/// overridden for is carried by the rendering plan.
class SqlAstTranslatorPaginationTests {

	@Test
	void theTrailingPaginationVisitorIsFinal() throws NoSuchMethodException {
		assertThat( Modifier.isFinal( AbstractSqlAstTranslator.class
				.getDeclaredMethod( "visitOffsetFetchClause", QueryPart.class ).getModifiers() ) ).isTrue();
	}

	@Test
	void theOriginalOffsetFetchPlanShapeIsUnchanged() {
		assertThat( new PaginationRenderingPlan.OffsetFetch( true ) )
				.isEqualTo( new PaginationRenderingPlan.OffsetFetch( true, false, false ) );
		assertThat( new PaginationRenderingPlan.OffsetFetch( false ) )
				.isEqualTo( new PaginationRenderingPlan.OffsetFetch( false, false, false ) );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Validation

	@Test
	void validationRunsOncePerQueryPart() {
		final var fixture = new Fixture( new PostgreSQLDialect() );
		final var group = fixture.union( fixture.querySpec( false, "work_item", "w" ),
				fixture.querySpec( false, "archived_item", "a" ) );
		final var translator = new ValidatingTranslator<>( fixture.request( new SelectStatement( group ) ) );

		translator.translate( null, new QueryOptionsImpl() );

		assertThat( translator.validated ).containsExactlyInAnyOrder( group,
				group.getQueryParts().get( 0 ), group.getQueryParts().get( 1 ) );
	}

	@Test
	void aRejectedRequestStopsTheTranslation() {
		final var fixture = new Fixture( new PostgreSQLDialect() );
		final var translator = new ValidatingTranslator<>(
				fixture.request( new SelectStatement( fixture.querySpec( true, "work_item", "w" ) ) ) );
		translator.reject = true;

		assertThatExceptionOfType( IllegalArgumentException.class )
				.isThrownBy( () -> translator.translate( null, new QueryOptionsImpl() ) )
				.withMessage( "Expected test rejection" );
	}

	@Test
	void sybaseRejectsAnOffsetInANonRootQueryPart() {
		final var fixture = new Fixture( new SybaseDialect() );
		final var first = fixture.querySpec( false, "work_item", "w" );
		first.setOffsetClauseExpression( fixture.literal( 5 ) );
		final var group = fixture.union( first, fixture.querySpec( false, "archived_item", "a" ) );
		final var translator = new SybaseSqlAstTranslator<JdbcSelect>( fixture.request( new SelectStatement( group ) ) );

		assertThatExceptionOfType( IllegalArgumentException.class )
				.isThrownBy( () -> translator.translate( null, new QueryOptionsImpl() ) )
				.withMessage( "Can't emulate offset clause in subquery" );
	}

	@Test
	void sybaseAcceptsAnUnpaginatedSetOperation() {
		final var fixture = new Fixture( new SybaseDialect() );
		final var group = fixture.union( fixture.querySpec( false, "work_item", "w" ),
				fixture.querySpec( false, "archived_item", "a" ) );

		final JdbcSelect operation = new SybaseSqlAstTranslator<JdbcSelect>(
				fixture.request( new SelectStatement( group ) ) ).translate( null, new QueryOptionsImpl() );

		assertThat( operation.getSqlString() ).contains( "union all" );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SQL Server: ordering required, offset required before fetch

	@Test
	void sqlServerSuppliesAnOrderingAndAZeroOffsetForAFetchOnASetOperation() {
		final var fixture = new Fixture( new SQLServerDialect() );
		final var group = fixture.union( fixture.querySpec( false, "work_item", "w" ),
				fixture.querySpec( false, "archived_item", "a" ) );
		group.setFetchClauseExpression( fixture.literal( 10 ), FetchClauseType.ROWS_ONLY );

		final JdbcSelect operation = new SQLServerSqlAstTranslator<JdbcSelect>(
				fixture.request( new SelectStatement( group ) ) ).translate( null, new QueryOptionsImpl() );

		assertThat( operation.getSqlString() )
				.endsWith( " order by (select 0) offset 0 rows fetch first 10 rows only" );
	}

	@Test
	void sqlServerRendersAGivenOffsetAndKeepsAGivenOrdering() {
		final var fixture = new Fixture( new SQLServerDialect() );
		final var querySpec = fixture.querySpec( true, "work_item", "w" );
		querySpec.addSortSpecification( new SortSpecification( fixture.literal( 1 ), SortDirection.ASCENDING ) );
		querySpec.setOffsetClauseExpression( fixture.literal( 5 ) );
		querySpec.setFetchClauseExpression( fixture.literal( 10 ), FetchClauseType.ROWS_ONLY );

		final JdbcSelect operation = new SQLServerSqlAstTranslator<JdbcSelect>(
				fixture.request( new SelectStatement( querySpec ) ) ).translate( null, new QueryOptionsImpl() );

		assertThat( operation.getSqlString() )
				.endsWith( " offset 5 rows fetch first 10 rows only" )
				.doesNotContain( "(select 0)" )
				.doesNotContain( "offset 0 rows" );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Oracle: a sorted branch of a set operation needs an offset clause

	@Test
	void oracleRendersAZeroOffsetForASortedBranchWithoutPagination() {
		final var fixture = new Fixture( new OracleDialect() );
		final var first = fixture.querySpec( false, "work_item", "w" );
		first.addSortSpecification( new SortSpecification( fixture.literal( 1 ), SortDirection.ASCENDING ) );
		final var group = fixture.union( first, fixture.querySpec( false, "archived_item", "a" ) );

		final JdbcSelect operation = new OracleSqlAstTranslator<JdbcSelect>(
				fixture.request( new SelectStatement( group ) ) ).translate( null, new QueryOptionsImpl() );

		assertThat( operation.getSqlString() ).containsOnlyOnce( " offset 0 rows" );
		assertThat( operation.getSqlString().indexOf( " offset 0 rows" ) )
				.isLessThan( operation.getSqlString().indexOf( "union all" ) );
	}

	@Test
	void oracleRendersTheBranchsOwnPaginationWhenItHasOne() {
		final var fixture = new Fixture( new OracleDialect() );
		final var first = fixture.querySpec( false, "work_item", "w" );
		first.addSortSpecification( new SortSpecification( fixture.literal( 1 ), SortDirection.ASCENDING ) );
		first.setFetchClauseExpression( fixture.literal( 10 ), FetchClauseType.ROWS_ONLY );
		final var group = fixture.union( first, fixture.querySpec( false, "archived_item", "a" ) );

		final JdbcSelect operation = new OracleSqlAstTranslator<JdbcSelect>(
				fixture.request( new SelectStatement( group ) ) ).translate( null, new QueryOptionsImpl() );

		assertThat( operation.getSqlString() ).doesNotContain( "offset 0 rows" ).contains( "fetch first 10 rows only" );
	}

	@Test
	void oracleRendersNoOffsetForAnUnsortedBranch() {
		final var fixture = new Fixture( new OracleDialect() );
		final var group = fixture.union( fixture.querySpec( false, "work_item", "w" ),
				fixture.querySpec( false, "archived_item", "a" ) );

		final JdbcSelect operation = new OracleSqlAstTranslator<JdbcSelect>(
				fixture.request( new SelectStatement( group ) ) ).translate( null, new QueryOptionsImpl() );

		assertThat( operation.getSqlString() ).doesNotContain( "offset" );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Support

	static final class Fixture {
		final SessionFactoryImplementor factory = mock( SessionFactoryImplementor.class, RETURNS_DEEP_STUBS );
		final TypeConfiguration types = new TypeConfiguration();
		final BasicType<Integer> integerType = types.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER );

		Fixture(Dialect dialect) {
			when( factory.getTypeConfiguration() ).thenReturn( types );
			when( factory.getJdbcServices().getDialect() ).thenReturn( dialect );
			when( factory.getJdbcSelectWithActionsBuilder() ).thenAnswer( invocation -> new JdbcSelectWithActions.Builder() );
		}

		SqlAstTranslationRequest.Select request(SelectStatement statement) {
			return new SqlAstTranslationRequest.Select( factory, statement );
		}

		QueryLiteral<Integer> literal(int value) {
			return new QueryLiteral<>( value, integerType );
		}

		QuerySpec querySpec(boolean root, String table, String alias) {
			final var querySpec = new QuerySpec( root );
			querySpec.getFromClause().addRoot( new StandardTableGroup(
					true, new NavigablePath( table ), mock( TableGroupProducer.class ), alias,
					new NamedTableReference( table, alias ), new SqlAliasBaseConstant( alias ), factory ) );
			querySpec.getSelectClause().addSqlSelection( new SqlSelectionImpl( 0, literal( 1 ) ) );
			return querySpec;
		}

		QueryGroup union(QuerySpec first, QuerySpec second) {
			return new QueryGroup( true, SetOperator.UNION_ALL, List.of( first, second ) );
		}
	}

	/// Records each query part offered for validation, and can reject them all.
	static final class ValidatingTranslator<T extends JdbcOperation> extends PostgreSQLSqlAstTranslator<T> {
		final List<QueryPart> validated = new ArrayList<>();
		boolean reject;

		@SuppressWarnings({ "unchecked", "rawtypes" })
		ValidatingTranslator(SqlAstTranslationRequest request) {
			super( request );
		}

		@Override
		protected PaginationRenderingSupport getPaginationRenderingSupport() {
			return PaginationRenderingSupport.withValidation(
					StandardPaginationRenderingSupport.OFFSET_FETCH, this::validate );
		}

		private void validate(PaginationRenderingRequest request) {
			validated.add( request.queryPart() );
			if ( reject ) {
				throw new IllegalArgumentException( "Expected test rejection" );
			}
		}
	}
}
