package org.hibernate.community.dialect;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.internal.SelectionQueryImpl;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.sql.internal.StandardSqmTranslator;
import org.hibernate.query.sqm.sql.spi.SqmTranslationRequest;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectStatement;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcSelect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code first n} and {@code rows m to n} pagination rendered by the
 * TimesTen translator, and the values bound for it, without requiring a TimesTen database.
 * <p>
 * The row numbers in {@code rows m to n} are one-based and inclusive, and the clause
 * must be rendered before {@code distinct}.
 */
@DomainModel(annotatedClasses = TimesTenPaginationRenderingTest.Book.class)
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.DIALECT, value = "org.hibernate.community.dialect.TimesTenDialect"),
		@Setting(name = AvailableSettings.HBM2DDL_AUTO, value = "none")
})
@SessionFactory
public class TimesTenPaginationRenderingTest {

	private static final String HQL = "select b.id from Book b order by b.id";
	private static final String DISTINCT_HQL = "select distinct b.grp from Book b order by b.grp";

	@Test
	void maxResultsOnlyRendersFirst(SessionFactoryScope scope) {
		final String sql = scope.fromSession( session -> translate( session, HQL, null, 5 ).getSqlString() );

		assertThat( sql ).startsWith( "select first ? " );
	}

	@Test
	void firstResultAndMaxResultsRenderRowsTo(SessionFactoryScope scope) {
		final String sql = scope.fromSession( session -> translate( session, HQL, 5, 5 ).getSqlString() );

		assertThat( sql ).startsWith( "select rows ? to ? " );
	}

	@Test
	void firstResultOnlyRendersRowsToTheLastRow(SessionFactoryScope scope) {
		final String sql = scope.fromSession( session -> translate( session, HQL, 10, null ).getSqlString() );

		assertThat( sql ).startsWith( "select rows ? to " + Integer.MAX_VALUE + " " );
	}

	@Test
	void rowsToIsRenderedBeforeDistinct(SessionFactoryScope scope) {
		final String sql = scope.fromSession( session -> translate( session, DISTINCT_HQL, 1, 2 ).getSqlString() );

		assertThat( sql ).startsWith( "select rows ? to ? distinct " );
	}

	@Test
	void firstIsRenderedBeforeDistinct(SessionFactoryScope scope) {
		final String sql = scope.fromSession( session -> translate( session, DISTINCT_HQL, null, 2 ).getSqlString() );

		assertThat( sql ).startsWith( "select first ? distinct " );
	}

	@Test
	void distinctWithoutPaginationIsUnchanged(SessionFactoryScope scope) {
		final String sql = scope.fromSession( session -> translate( session, DISTINCT_HQL, null, null ).getSqlString() );

		assertThat( sql )
				.startsWith( "select " )
				.contains( "distinct" )
				.doesNotContain( " first ", " rows " );
	}

	@Test
	void literalOffsetIsConvertedToAOneBasedRowNumber(SessionFactoryScope scope) {
		final String sql = scope.fromSession( session -> translate(
				session,
				"select b.id from Book b order by b.id limit 3 offset 5",
				null,
				null
		).getSqlString() );

		assertThat( sql ).startsWith( "select rows 6 to 8 " );
	}

	@Test
	void offsetParameterIsBoundAsAOneBasedRowNumber(SessionFactoryScope scope) {
		final List<Integer> bound = scope.fromSession( session -> boundValues( session, 5, 5 ) );

		// rows (5 + 1) to (5 + 5)
		assertThat( bound ).containsExactly( 6, 10 );
	}

	@Test
	void offsetOnlyParameterIsBoundAsAOneBasedRowNumber(SessionFactoryScope scope) {
		final List<Integer> bound = scope.fromSession( session -> boundValues( session, 10, null ) );

		assertThat( bound ).containsExactly( 11 );
	}

	@Test
	void maxResultsOnlyParameterIsBoundUnchanged(SessionFactoryScope scope) {
		final List<Integer> bound = scope.fromSession( session -> boundValues( session, null, 5 ) );

		assertThat( bound ).containsExactly( 5 );
	}

	private static List<Integer> boundValues(SessionImplementor session, Integer firstResult, Integer maxResults) {
		final SelectionQueryImpl<?> query = selectionQuery( session, HQL, firstResult, maxResults );
		final JdbcSelect operation = translate( session, query );

		final PreparedStatement statement = mock( PreparedStatement.class );
		final ExecutionContext executionContext = mock( ExecutionContext.class );
		when( executionContext.getQueryOptions() ).thenReturn( query.getQueryOptions() );
		when( executionContext.getSession() ).thenReturn( session );

		final List<? extends JdbcParameterBinder> binders = operation.getParameterBinders();
		final var parameterBindings = new JdbcParameterBindingsImpl( 0 );
		try {
			for ( JdbcParameterBinder binder : binders ) {
				binder.bindParameterValue( statement, 1, parameterBindings, executionContext );
			}
			final ArgumentCaptor<Integer> values = ArgumentCaptor.forClass( Integer.class );
			verify( statement, times( binders.size() ) ).setInt( anyInt(), values.capture() );
			return values.getAllValues();
		}
		catch (SQLException e) {
			throw new IllegalStateException( e );
		}
	}

	private static JdbcSelect translate(
			SessionImplementor session,
			String hql,
			Integer firstResult,
			Integer maxResults) {
		return translate( session, selectionQuery( session, hql, firstResult, maxResults ) );
	}

	private static SelectionQueryImpl<?> selectionQuery(
			SessionImplementor session,
			String hql,
			Integer firstResult,
			Integer maxResults) {
		final var query = (SelectionQueryImpl<?>) session.createSelectionQuery( hql, Object.class );
		if ( firstResult != null ) {
			query.setFirstResult( firstResult );
		}
		if ( maxResults != null ) {
			query.setMaxResults( maxResults );
		}
		return query;
	}

	private static JdbcSelect translate(SessionImplementor session, SelectionQueryImpl<?> query) {
		final var sqmTranslator = new StandardSqmTranslator<SelectStatement>( new SqmTranslationRequest.Select(
				(SqmSelectStatement<?>) query.getSqmStatement(),
				query.getQueryOptions(),
				query.getDomainParameterXref(),
				query.getParameterBindings(),
				session.getLoadQueryInfluencers(),
				session.getFactory().getSqlTranslationEngine(),
				true
		) );
		final SelectStatement sqlAst = sqmTranslator.translate().getSqlAst();
		final QueryOptions queryOptions = query.getQueryOptions();
		return session.getFactory().getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildTranslator( new SqlAstTranslationRequest.Select( session.getFactory(), sqlAst ) )
				.translate( null, queryOptions );
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		private Integer id;
		private Integer grp;
	}
}
