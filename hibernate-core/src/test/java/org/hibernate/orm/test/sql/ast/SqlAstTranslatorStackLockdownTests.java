package org.hibernate.orm.test.sql.ast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.PostgreSQLSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseConstant;
import org.hibernate.sql.ast.spi.query.expression.QueryLiteral;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.StandardTableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupProducer;
import org.hibernate.sql.ast.spi.query.select.QueryGroup;
import org.hibernate.sql.ast.spi.query.select.QueryPart;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectClause;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.query.select.SortSpecification;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.exec.internal.JdbcSelectWithActions;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.spi.StringBuilderSqlAppender;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Step 1 of the SQL AST translator convention work: the translator's stacks
/// are entered through scoped operations and inspected through focused
/// queries, and are not reachable for mutation outside the base.
class SqlAstTranslatorStackLockdownTests {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// withClause

	@Test
	void withClauseMakesTheClauseCurrentAndRestoresThePreviousOne() {
		final var translator = translator( new SelectStatement( querySpec( true, "work_item", "w" ) ) );
		final List<Clause> observed = new ArrayList<>();

		assertThat( translator.getCurrentClause() ).isNull();
		translator.withClause( Clause.WHERE, () -> observed.add( translator.getCurrentClause() ) );

		assertThat( observed ).containsExactly( Clause.WHERE );
		assertThat( translator.getCurrentClause() ).isNull();
	}

	@Test
	void withClauseNests() {
		final var translator = translator( new SelectStatement( querySpec( true, "work_item", "w" ) ) );
		final List<Clause> observed = new ArrayList<>();

		translator.withClause( Clause.SELECT, () -> {
			observed.add( translator.getCurrentClause() );
			translator.withClause( Clause.WITHIN_GROUP, () -> {
				observed.add( translator.getCurrentClause() );
				assertThat( translator.withinClause( Clause.SELECT ) ).isTrue();
				assertThat( translator.withinClause( Clause.WITHIN_GROUP ) ).isTrue();
				assertThat( translator.withinClause( Clause.WHERE ) ).isFalse();
			} );
			observed.add( translator.getCurrentClause() );
		} );

		assertThat( observed ).containsExactly( Clause.SELECT, Clause.WITHIN_GROUP, Clause.SELECT );
		assertThat( translator.getCurrentClause() ).isNull();
	}

	@Test
	void withClauseRestoresThePreviousClauseWhenRenderingFails() {
		final var translator = translator( new SelectStatement( querySpec( true, "work_item", "w" ) ) );

		translator.withClause( Clause.SELECT, () -> {
			assertThatExceptionOfType( IllegalStateException.class ).isThrownBy( () ->
					translator.withClause( Clause.WHERE, () -> {
						throw new IllegalStateException( "Expected test failure" );
					} ) );
			assertThat( translator.getCurrentClause() ).isEqualTo( Clause.SELECT );
		} );

		assertThat( translator.getCurrentClause() ).isNull();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Read-only queries

	@Test
	void queriesDescribeAPlainQuerySpecWhileItIsRendered() {
		final var statement = new SelectStatement( querySpec( true, "work_item", "w" ) );
		final var translator = translator( statement );

		translator.translate( null, new QueryOptionsImpl() );

		assertThat( translator.snapshots ).hasSize( 1 );
		final var snapshot = translator.snapshots.get( 0 );
		assertThat( snapshot.currentClause() ).isEqualTo( Clause.SELECT );
		assertThat( snapshot.parentQueryPart() ).isNull();
		assertThat( snapshot.withinQueryGroup() ).isFalse();
		assertThat( snapshot.currentStatement() ).isSameAs( statement );
		assertThat( snapshot.outermostStatement() ).isSameAs( statement );
	}

	@Test
	void queriesDescribeEachBranchOfASetOperationWhileItIsRendered() {
		final var group = new QueryGroup( true, SetOperator.UNION_ALL, List.of(
				querySpec( false, "work_item", "w" ), querySpec( false, "archived_item", "a" ) ) );
		final var translator = translator( new SelectStatement( group ) );

		translator.translate( null, new QueryOptionsImpl() );

		assertThat( translator.snapshots ).hasSize( 2 ).allSatisfy( snapshot -> {
			assertThat( snapshot.currentClause() ).isEqualTo( Clause.SELECT );
			assertThat( snapshot.parentQueryPart() ).isSameAs( group );
			assertThat( snapshot.withinQueryGroup() ).isTrue();
		} );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Surface

	@Test
	void scopedOperationsAndQueriesAreFinal() throws NoSuchMethodException {
		assertFinal( "withClause", Clause.class, Runnable.class );
		assertFinal( "getCurrentClause" );
		assertFinal( "isWithinClause", Clause.class );
		assertFinal( "getParentQueryPart" );
		assertFinal( "isWithinQueryGroup" );
		assertFinal( "getCurrentStatement" );
		assertFinal( "withParameterRenderingMode", SqlAstNodeRenderingMode.class, Runnable.class );
		assertFinal( "getParameterRenderingMode" );
		assertFinal( "getSqlBuffer" );
		assertFinal( "registerAffectedTable", String.class );
		assertFinal( "registerAffectedTable", NamedTableReference.class );
	}

	@Test
	void sqlAppenderMethodsAreFinal() {
		final var appenderMethods = Set.of( "appendSql", "append", "appendLiteral", "appendLiteralContent",
				"appendDoubleQuoteEscapedString", "appendSingleQuoteEscapedString" );
		final var declared = Stream.of( AbstractSqlAstTranslator.class.getDeclaredMethods() )
				.filter( method -> appenderMethods.contains( method.getName() ) )
				.toList();

		assertThat( declared ).hasSizeGreaterThanOrEqualTo( appenderMethods.size() );
		assertThat( declared ).allSatisfy( method ->
				assertThat( Modifier.isFinal( method.getModifiers() ) ).as( method.toString() ).isTrue() );
	}

	@Test
	void stackAccessorsAreDeprecatedForRemoval() throws NoSuchMethodException {
		assertDeprecatedForRemoval( SqlAstTranslator.class.getMethod( "getCurrentClauseStack" ) );
		assertDeprecatedForRemoval( AbstractSqlAstTranslator.class.getDeclaredMethod( "getClauseStack" ) );
		assertDeprecatedForRemoval( AbstractSqlAstTranslator.class.getDeclaredMethod( "getQueryPartStack" ) );
		assertDeprecatedForRemoval( AbstractSqlAstTranslator.class.getDeclaredMethod( "getStatementStack" ) );
	}

	/// The deprecated accessors still hand out the live stacks, so nothing in
	/// the type system stops a push or a pop. This check does.
	///
	/// `getCurrentClauseStack()` is also the name of an unrelated operation on
	/// the SQL AST creation side (`SqlAstCreationState`, `SqmToSqlAstConverter`),
	/// so a file which names one of those types is taken to mean that one. A
	/// file using both would slip through.
	@Test
	void translatorStacksAreNotAccessedOutsideTheBase() throws IOException {
		final Path root = findRepositoryRoot();
		assumeTrue( root != null, "Sources are not available to this test run" );
		final List<String> violations = new ArrayList<>();
		for ( String module : List.of( "hibernate-core", "hibernate-community-dialects" ) ) {
			final Path sources = root.resolve( module ).resolve( "src/main/java" );
			if ( !Files.isDirectory( sources ) ) {
				continue;
			}
			try ( Stream<Path> files = Files.walk( sources ) ) {
				for ( Path file : files.filter( path -> path.toString().endsWith( ".java" ) ).toList() ) {
					final String name = file.getFileName().toString();
					if ( name.equals( "AbstractSqlAstTranslator.java" ) && file.toString().contains( "dialect" ) ) {
						continue;
					}
					final String source = Files.readString( file );
					for ( String accessor : List.of( "getClauseStack()", "getQueryPartStack()", "getStatementStack()" ) ) {
						if ( source.contains( accessor ) ) {
							violations.add( root.relativize( file ) + " calls " + accessor );
						}
					}
					if ( source.contains( "getCurrentClauseStack()" )
							&& !name.equals( "SqlAstTranslator.java" )
							&& !source.contains( "SqlAstCreationState" )
							&& !source.contains( "SqmToSqlAstConverter" ) ) {
						violations.add( root.relativize( file ) + " calls getCurrentClauseStack()" );
					}
				}
			}
		}
		assertThat( violations ).isEmpty();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Within-group orderings with more than one sort key

	@Test
	void namedFunctionRendersEveryWithinGroupSortKey() {
		final var rendering = new FunctionRendering();
		final var descriptor = new NamedSqmFunctionDescriptor( "listagg", true, null, null );

		descriptor.render( rendering.appender, List.of(), null, rendering.sortKeys( 3 ), null, rendering.translator );

		assertThat( rendering.sql() ).isEqualTo( "listagg() within group (order by k0,k1,k2)" );
		assertThat( rendering.clauses ).containsExactly( Clause.WITHIN_GROUP );
	}

	@Test
	void patternRendererRendersEveryWithinGroupSortKey() {
		final var rendering = new FunctionRendering();
		final var renderer = new PatternRenderer( "percentile()" );

		renderer.render( rendering.appender, List.of(), null, rendering.sortKeys( 3 ), rendering.translator );

		assertThat( rendering.sql() ).startsWith( "percentile() within group (order by" ).endsWith( "k0,k1,k2)" );
		assertThat( rendering.clauses ).containsExactly( Clause.WITHIN_GROUP );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Support

	private static void assertFinal(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
		final Method method = AbstractSqlAstTranslator.class.getDeclaredMethod( name, parameterTypes );
		assertThat( Modifier.isFinal( method.getModifiers() ) ).as( method.toString() ).isTrue();
	}

	private static void assertDeprecatedForRemoval(Method method) {
		final Deprecated deprecated = method.getAnnotation( Deprecated.class );
		assertThat( deprecated ).as( method.toString() ).isNotNull();
		assertThat( deprecated.forRemoval() ).as( method.toString() ).isTrue();
	}

	private static Path findRepositoryRoot() {
		Path candidate = Path.of( "" ).toAbsolutePath();
		while ( candidate != null ) {
			if ( Files.isDirectory( candidate.resolve( "hibernate-core/src/main/java" ) ) ) {
				return candidate;
			}
			candidate = candidate.getParent();
		}
		return null;
	}

	private static final TypeConfiguration TYPES = new TypeConfiguration();

	private static SessionFactoryImplementor factory() {
		final var factory = mock( SessionFactoryImplementor.class, RETURNS_DEEP_STUBS );
		when( factory.getTypeConfiguration() ).thenReturn( TYPES );
		when( factory.getJdbcServices().getDialect() ).thenReturn( new PostgreSQLDialect() );
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

	record Snapshot(
			Clause currentClause,
			QueryPart parentQueryPart,
			boolean withinQueryGroup,
			Statement currentStatement,
			Statement outermostStatement) {
	}

	/// Records what the read-only queries report each time a select clause is rendered.
	static final class RecordingTranslator<T extends JdbcOperation> extends PostgreSQLSqlAstTranslator<T> {
		final List<Snapshot> snapshots = new ArrayList<>();

		@SuppressWarnings({ "unchecked", "rawtypes" })
		RecordingTranslator(SqlAstTranslationRequest request) {
			super( request );
		}

		boolean withinClause(Clause clause) {
			return isWithinClause( clause );
		}

		@Override
		protected void renderSelectClause(SelectClause selectClause) {
			snapshots.add( new Snapshot(
					getCurrentClause(), getParentQueryPart(), isWithinQueryGroup(), getCurrentStatement(), getStatement() ) );
			super.renderSelectClause( selectClause );
		}
	}

	/// Renders a function against a translator which records the clauses entered
	/// and renders each sort key as its name.
	static final class FunctionRendering {
		final StringBuilderSqlAppender appender = new StringBuilderSqlAppender();
		final SqlAstTranslator<?> translator = mock( SqlAstTranslator.class );
		final List<Clause> clauses = new ArrayList<>();
		private final Map<SqlAstNode, String> names = new IdentityHashMap<>();

		FunctionRendering() {
			doAnswer( invocation -> {
				clauses.add( invocation.getArgument( 0 ) );
				invocation.<Runnable>getArgument( 1 ).run();
				return null;
			} ).when( translator ).withClause( any(), any() );
			doAnswer( invocation -> {
				appender.appendSql( names.get( invocation.<SqlAstNode>getArgument( 0 ) ) );
				return null;
			} ).when( translator ).render( any(), any() );
		}

		List<SortSpecification> sortKeys(int count) {
			final List<SortSpecification> sortKeys = new ArrayList<>();
			for ( int i = 0; i < count; i++ ) {
				final var sortKey = mock( SortSpecification.class );
				names.put( sortKey, "k" + i );
				sortKeys.add( sortKey );
			}
			return sortKeys;
		}

		String sql() {
			return appender.toString();
		}
	}
}
