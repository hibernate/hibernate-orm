package org.hibernate.orm.test.sql.ast;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sql.ast.internal.H2SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.SessionFactoryImplementor;
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
import org.hibernate.sql.exec.internal.JdbcSelectWithActions;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Step 3 of the SQL AST translator convention work, query parts: the query
/// spec and query group visitors are final, customization happens in a
/// callback invoked in the caller's context, and the standard rendering is a
/// final path that callback may invoke.
class SqlAstTranslatorQueryPartTests {

	@Test
	void queryPartVisitorsAndStandardPathsAreFinal() throws NoSuchMethodException {
		assertFinal( "visitQuerySpec", QuerySpec.class );
		assertFinal( "renderStandardQuerySpec", QuerySpec.class );
		assertFinal( "visitQueryGroup", QueryGroup.class );
		assertFinal( "renderStandardQueryGroup", QueryGroup.class );
		assertFinal( "renderQueryGroupWithoutOrderByAndOffsetFetch", QueryGroup.class );
		assertFinal( "renderForUpdateClause", QuerySpec.class );
	}

	@Test
	void querySpecCallbackRunsBeforeTheQuerySpecIsCurrent() {
		final var querySpec = querySpec( true, "work_item", "w" );
		final var translator = translator( new SelectStatement( querySpec ) );

		translator.translate( null, new QueryOptionsImpl() );

		assertThat( translator.currentAtQuerySpecCallback ).containsExactly( (QueryPart) null );
		assertThat( translator.currentAtQueryClauses ).containsExactly( querySpec );
	}

	@Test
	void queryGroupCallbackRunsBeforeTheQueryGroupIsCurrent() {
		final var first = querySpec( false, "work_item", "w" );
		final var second = querySpec( false, "archived_item", "a" );
		final var group = new QueryGroup( true, SetOperator.UNION_ALL, List.of( first, second ) );
		final var translator = translator( new SelectStatement( group ) );

		translator.translate( null, new QueryOptionsImpl() );

		assertThat( translator.currentAtQueryGroupCallback ).containsExactly( (QueryPart) null );
		// each branch is rendered with the group as the current query part
		assertThat( translator.currentAtQuerySpecCallback ).containsExactly( group, group );
		assertThat( translator.currentAtQueryClauses ).containsExactly( first, second );
	}

	@Test
	void anUnsupportedSetOperatorIsRejectedBeforeTheCallback() {
		// H2 does not support INTERSECT ALL
		final var group = new QueryGroup( true, SetOperator.INTERSECT_ALL, List.of(
				querySpec( false, "work_item", "w" ), querySpec( false, "archived_item", "a" ) ) );
		final var translator = translator( new SelectStatement( group ) );

		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> translator.translate( null, new QueryOptionsImpl() ) );

		assertThat( translator.currentAtQueryGroupCallback ).isEmpty();
	}

	private static void assertFinal(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
		final Method method = AbstractSqlAstTranslator.class.getDeclaredMethod( name, parameterTypes );
		assertThat( Modifier.isFinal( method.getModifiers() ) ).as( method.toString() ).isTrue();
	}

	private static final TypeConfiguration TYPES = new TypeConfiguration();

	private static SessionFactoryImplementor factory() {
		final var factory = mock( SessionFactoryImplementor.class, RETURNS_DEEP_STUBS );
		when( factory.getTypeConfiguration() ).thenReturn( TYPES );
		when( factory.getJdbcServices().getDialect() ).thenReturn( new H2Dialect() );
		when( factory.getJdbcSelectWithActionsBuilder() ).thenAnswer( invocation -> new JdbcSelectWithActions.Builder() );
		return factory;
	}

	private static QuerySpec querySpec(boolean root, String table, String alias) {
		final var querySpec = new QuerySpec( root );
		querySpec.getFromClause().addRoot( new StandardTableGroup(
				true, new NavigablePath( table ), mock( TableGroupProducer.class ), alias,
				new NamedTableReference( table, alias ), new SqlAliasBaseConstant( alias ), factory() ) );
		querySpec.getSelectClause().addSqlSelection( new SqlSelectionImpl( 0,
				new QueryLiteral<>( 1, TYPES.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ) ) ) );
		return querySpec;
	}

	private static RecordingTranslator<JdbcOperation> translator(SelectStatement statement) {
		return new RecordingTranslator<>( new SqlAstTranslationRequest.Select( factory(), statement ) );
	}

	/// Records which query part is current at each callback.
	static final class RecordingTranslator<T extends JdbcOperation> extends H2SqlAstTranslator<T> {
		final List<QueryPart> currentAtQuerySpecCallback = new ArrayList<>();
		final List<QueryPart> currentAtQueryGroupCallback = new ArrayList<>();
		final List<QueryPart> currentAtQueryClauses = new ArrayList<>();

		@SuppressWarnings({ "unchecked", "rawtypes" })
		RecordingTranslator(SqlAstTranslationRequest request) {
			super( request );
		}

		@Override
		protected void renderQuerySpec(QuerySpec querySpec) {
			currentAtQuerySpecCallback.add( getCurrentQueryPart() );
			super.renderQuerySpec( querySpec );
		}

		@Override
		protected void renderQueryGroup(QueryGroup queryGroup) {
			currentAtQueryGroupCallback.add( getCurrentQueryPart() );
			super.renderQueryGroup( queryGroup );
		}

		@Override
		protected void renderQueryClauses(QuerySpec querySpec) {
			currentAtQueryClauses.add( getCurrentQueryPart() );
			super.renderQueryClauses( querySpec );
		}
	}
}
