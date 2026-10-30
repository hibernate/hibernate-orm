package org.hibernate.orm.test.dialect.unit.locking;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import jakarta.persistence.PessimisticLockScope;

import org.hibernate.dialect.lock.internal.NonNullLockTargetChecker;
import org.hibernate.dialect.lock.spi.RowLockStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseConstant;
import org.hibernate.sql.ast.spi.query.expression.QueryLiteral;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.SqlAstJoinType;
import org.hibernate.sql.ast.spi.query.from.StandardTableGroup;
import org.hibernate.sql.ast.spi.query.from.StandardVirtualTableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoin;
import org.hibernate.sql.ast.spi.query.from.TableReferenceJoin;
import org.hibernate.sql.ast.spi.query.from.UnionTableReference;
import org.hibernate.sql.ast.spi.query.predicate.SqlFragmentPredicate;
import org.hibernate.sql.ast.spi.query.select.QueryGroup;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static jakarta.persistence.PessimisticLockScope.NORMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/// Checks conservative eligibility independently of rendering and database
/// behavior, including opaque groups and null-extension below nested joins.
///
/// @author Steve Ebersole
class NonNullLockTargetCheckerTest {
	@Test
	void acceptsOnlyExplicitlyTargetedPreservedRoots() {
		final var f = new Fixture();
		assertThat( f.eligible() ).isTrue();
		f.child.addTableGroupJoin( f.join( f.child, SqlAstJoinType.INNER, "nested" ) );
		assertThat( f.eligible() ).isTrue();
		f.child.addNestedTableGroupJoin( f.join( f.child, SqlAstJoinType.LEFT, "optional" ) );
		assertThat( f.eligible() ).isTrue();
	}

	static Stream<Arguments> rejectedShapes() {
		return Stream.of(
				change( "unresolved target", f -> f.targets = null ),
				change( "unqualified locking", f -> f.targets = List.of() ),
				change( "nullable target", f -> f.targets = List.of( f.child.getNavigablePath() ) ),
				change( "multiple targets", f -> f.targets = List.of( f.path, f.child.getNavigablePath() ) ),
				change( "multiple query targets", f -> f.query.applyRootPathForLocking( f.child.getNavigablePath() ) ),
				change( "scalar result", f -> f.statement = new SelectStatement( f.query ) ),
				change( "unrelated entity result", f -> when( f.result.getNavigablePath() ).thenReturn( new NavigablePath( "Other" ) ) ),
				change( "multiple roots", f -> f.query.getFromClause().addRoot( f.child ) ),
				change( "multi-table root", f -> when( f.persister.getSubclassTableSpan() ).thenReturn( 2 ) ),
				change( "secondary table", f -> when( f.root.getTableReferenceJoins() ).thenReturn( List.of( new TableReferenceJoin( false, new NamedTableReference( "secondary", "s" ), null ) ) ) ),
				change( "missing physical reference", f -> when( f.root.getPrimaryTableReference() ).thenReturn( null ) ),
				change( "opaque root", f -> f.query.getFromClause().getRoots().set( 0, mock( TableGroup.class ) ) ),
				change( "union table", f -> f.query.getFromClause().getRoots().set( 0, f.group( new UnionTableReference( "(select id from a)", new String[] { "a" }, "p" ) ) ) ),
				change( "mapped subselect", f -> when( f.root.getPrimaryTableReference() ).thenReturn( new NamedTableReference( " (select id from parent)", "p" ) ) ),
				change( "joined mapped subselect", f -> when( f.child.getPrimaryTableReference() ).thenReturn( new NamedTableReference( "(select id from detail)", "detail" ) ) ),
				change( "temporal source", f -> {
					final var reference = spy( new NamedTableReference( "parent", "p" ) );
					when( reference.getAsOfTransactionIdentifier() ).thenReturn( f.literal );
					when( f.root.getPrimaryTableReference() ).thenReturn( reference );
				} ),
				change( "lateral root", f -> when( f.root.isLateral() ).thenReturn( true ) ),
				change( "virtual root", f -> when( f.root.isVirtual() ).thenReturn( true ) ),
				change( "unqualified alias", f -> f.query.getFromClause().getRoots().set( 0, f.group( new NamedTableReference( "parent", "" ) ) ) ),
				change( "missing source alias", f -> when( f.root.getSourceAlias() ).thenReturn( null ) ),
				change( "colliding source aliases", f -> when( f.child.getSourceAlias() ).thenReturn( "p" ) ),
				change( "right join", f -> f.root.getTableGroupJoins().get( 0 ).setJoinType( SqlAstJoinType.RIGHT ) ),
				change( "full join", f -> f.root.getTableGroupJoins().get( 0 ).setJoinType( SqlAstJoinType.FULL ) ),
				change( "cross join", f -> f.root.getTableGroupJoins().get( 0 ).setJoinType( SqlAstJoinType.CROSS ) ),
				change( "nested null extension", f -> f.child.addNestedTableGroupJoin( f.join( f.child, SqlAstJoinType.RIGHT, "nested" ) ) ),
				change( "opaque join", f -> {
					final var opaque = mock( TableGroup.class );
					when( opaque.isInitialized() ).thenReturn( true );
					f.root.addTableGroupJoin( new TableGroupJoin( f.path.append( "opaque" ), SqlAstJoinType.LEFT, opaque ) );
				} ),
				change( "joined multi-table entity", f -> when( f.child.getTableReferenceJoins() ).thenReturn( List.of( new TableReferenceJoin( false, new NamedTableReference( "detail_secondary", "ds" ), null ) ) ) ),
				change( "extended scope", f -> f.scope = PessimisticLockScope.EXTENDED ),
				change( "fetched scope", f -> f.scope = PessimisticLockScope.FETCHED ),
				change( "no targeting", f -> f.targeting = RowLockStrategy.NONE ),
				change( "unknown targeting", f -> f.targeting = null ),
				change( "query option limit", f -> f.paginated = true ),
				change( "AST limit", f -> f.query.setFetchClauseExpression( f.literal, org.hibernate.query.common.FetchClauseType.ROWS_ONLY ) ),
				change( "AST offset", f -> f.query.setOffsetClauseExpression( f.literal ) ),
				change( "distinct", f -> f.query.getSelectClause().makeDistinct( true ) ),
				change( "grouping", f -> f.query.setGroupByClauseExpressions( List.of( f.literal ) ) ),
				change( "having", f -> f.query.setHavingClauseRestrictions( new SqlFragmentPredicate( "1=1" ) ) ),
				change( "set query", f -> f.statement = new SelectStatement( new QueryGroup( true, SetOperator.UNION_ALL, List.of( f.query ) ), List.of( f.result ) ) )
		);
	}

	private static Arguments change(String name, Consumer<Fixture> change) {
		return arguments( name, change );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("rejectedShapes")
	void rejectsUnprovenShapes(String name, Consumer<Fixture> change) {
		final var f = new Fixture();
		change.accept( f );
		assertThat( f.eligible() ).as( name ).isFalse();
	}

	@Test
	void traversesTransparentEmbeddableGroups() {
		final var f = new Fixture();
		final var path = f.path.append( "id" );
		final var embedded = new StandardVirtualTableGroup( path, mock( EmbeddableValuedModelPart.class ), f.root, true );
		f.root.addTableGroupJoin( new TableGroupJoin( path, SqlAstJoinType.INNER, embedded ) );
		assertThat( f.eligible() ).isTrue();
		embedded.addNestedTableGroupJoin( f.join( embedded, SqlAstJoinType.RIGHT, "nested" ) );
		assertThat( f.eligible() ).isFalse();
	}

	@Test
	void doesNotInitializeUnrenderedLazyGroups() {
		final var f = new Fixture();
		final var lazy = mock( TableGroup.class );
		f.root.addTableGroupJoin( new TableGroupJoin( f.path.append( "lazy" ), SqlAstJoinType.LEFT, lazy ) );
		assertThat( f.eligible() ).isTrue();
		verify( lazy, never() ).getPrimaryTableReference();
		when( lazy.hasRealJoins() ).thenReturn( true );
		assertThat( f.eligible() ).isFalse();
		verify( lazy, never() ).getPrimaryTableReference();
	}

	static class Fixture {
		final SessionFactoryImplementor factory;
		final EntityPersister persister = mock( EntityPersister.class );
		final NavigablePath path = new NavigablePath( "Parent" );
		final QuerySpec query = new QuerySpec( true );
		final EntityResult<?> result = mock( EntityResult.class );
		final QueryLiteral<Integer> literal;
		final StandardTableGroup root;
		final StandardTableGroup child;
		SelectStatement statement;
		java.util.Collection<NavigablePath> targets = new ArrayList<>( Set.of( path ) );
		PessimisticLockScope scope = NORMAL;
		RowLockStrategy targeting = RowLockStrategy.TABLE;
		boolean paginated;

		Fixture() { this( mock( SessionFactoryImplementor.class ) ); }
		Fixture(SessionFactoryImplementor factory) {
			this.factory = factory;
			when( persister.getEntityPersister() ).thenReturn( persister );
			when( persister.getSubclassTableSpan() ).thenReturn( 1 );
			when( persister.getQuerySpaces() ).thenReturn( new String[] { "parent" } );
			root = spy( group( new NamedTableReference( "parent", "p" ) ) );
			final var join = join( root, SqlAstJoinType.LEFT, "detail" );
			child = (StandardTableGroup) join.getJoinedGroup();
			root.addTableGroupJoin( join );
			query.getFromClause().addRoot( root );
			query.applyRootPathForLocking( path );
			when( result.getNavigablePath() ).thenReturn( path );
			when( result.getFetches() ).thenReturn( ImmutableFetchList.EMPTY );
			statement = new SelectStatement( query, List.of( result ) );
			literal = new QueryLiteral<>( 1, new TypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ) );
			query.getSelectClause().addSqlSelection( new SqlSelectionImpl( 0, literal ) );
		}

		StandardTableGroup group(NamedTableReference reference) {
			return new StandardTableGroup( true, path, persister, "p", reference, new SqlAliasBaseConstant( "p" ), factory );
		}

		TableGroupJoin join(TableGroup parent, SqlAstJoinType type, String name) {
			final var mapping = mock( ToOneAttributeMapping.class );
			when( mapping.getAssociatedEntityMappingType() ).thenReturn( persister );
			final var joinPath = parent.getNavigablePath().append( name );
			final var group = spy( new StandardTableGroup( true, joinPath, mapping, name,
					new NamedTableReference( "detail", name ), new SqlAliasBaseConstant( name ), factory ) );
			return new TableGroupJoin( joinPath, type, group, new SqlFragmentPredicate( "1=1" ) );
		}

		boolean eligible() {
			return NonNullLockTargetChecker.canApply( query, statement, targets, scope, targeting, paginated );
		}
	}
}
