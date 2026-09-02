/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.ast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.Timeout;
import org.hibernate.LockMode;
import org.hibernate.Timeouts;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.sql.ast.internal.H2SqlAstTranslator;
import org.hibernate.dialect.lock.internal.LockingSupportSimple;
import org.hibernate.dialect.lock.internal.TransactSQLLockingSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.dialect.lock.spi.TableLockHintRenderer;
import org.hibernate.dialect.lock.spi.TableLockHintRequest;
import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.DerivedColumnAliasing;
import org.hibernate.dialect.sql.ast.spi.DerivedTableKind;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingRequest;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.LateralReferenceStyle;
import org.hibernate.dialect.sql.ast.spi.MutationKind;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;
import org.hibernate.dialect.sql.ast.spi.SelectItemReferenceStrategy;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingRequest;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PrimaryTableReferenceContext;
import org.hibernate.dialect.sql.ast.spi.PrimaryTableReferenceKind;
import org.hibernate.dialect.sql.ast.spi.PostgreSQLFamilySqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.QueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.ReturningMutationSource;
import org.hibernate.dialect.sql.ast.spi.ReturningRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.ReturningRenderingRequest;
import org.hibernate.dialect.sql.ast.spi.ReturningRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.SQLServerPaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.SetReturningFunctionRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.SetReturningFunctionRenderingRequest;
import org.hibernate.dialect.sql.ast.spi.SetReturningFunctionRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardInsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardQueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardSetReturningFunctionRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardTableJoinRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.TableJoinKind;
import org.hibernate.dialect.sql.ast.spi.TableJoinRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.TableJoinRenderingRequest;
import org.hibernate.dialect.sql.ast.spi.TableJoinRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.TableReferenceAliasContext;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.SortDirection;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.query.from.SqlAstJoinType;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslator;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.query.PathInterpretation;
import org.hibernate.sql.ast.spi.query.MutationStatement;
import org.hibernate.sql.ast.spi.query.SetReturningFunctionType;
import org.hibernate.sql.ast.spi.query.cte.CteContainer;
import org.hibernate.sql.ast.spi.query.cte.CteColumn;
import org.hibernate.sql.ast.spi.query.cte.CteStatement;
import org.hibernate.sql.ast.spi.query.cte.CteTable;
import org.hibernate.sql.ast.spi.query.cte.CteTableGroup;
import org.hibernate.sql.ast.spi.query.delete.DeleteStatement;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.FunctionExpression;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.from.FromClause;
import org.hibernate.sql.ast.spi.query.from.FunctionTableReference;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.QueryPartTableReference;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoin;
import org.hibernate.sql.ast.spi.query.from.TableReference;
import org.hibernate.sql.ast.spi.query.from.TableReferenceJoin;
import org.hibernate.sql.ast.spi.query.from.UnionTableReference;
import org.hibernate.sql.ast.spi.query.from.ValuesTableReference;
import org.hibernate.sql.ast.spi.query.insert.InsertSelectStatement;
import org.hibernate.sql.ast.spi.query.insert.ConflictClause;
import org.hibernate.sql.ast.spi.query.insert.Values;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.select.QueryGroup;
import org.hibernate.sql.ast.spi.query.select.QueryPart;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectClause;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.query.select.SortSpecification;
import org.hibernate.sql.ast.spi.query.update.Assignment;
import org.hibernate.sql.ast.spi.query.update.Assignable;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.ast.spi.model.TableDeleteStandard;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;
import org.hibernate.sql.ast.spi.model.TableUpdateStandard;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcSelect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

/// Tests the supported state-access guarantees of the generic SQL AST
/// translator base.
///
/// @since 8.0
/// @author Steve Ebersole
public class SqlAstTranslatorSurfaceTests {
	@Test
	void fromClauseWalkerCallbacksAndHelpersAreFinal() throws NoSuchMethodException {
		assertFinalMethod( "visitFromClause", FromClause.class );
		assertFinalMethod( "visitTableGroup", TableGroup.class );
		assertFinalMethod( "visitTableGroupJoin", TableGroupJoin.class );
		assertFinalMethod( "visitNamedTableReference", NamedTableReference.class );
		assertFinalMethod( "visitTableReferenceJoin", TableReferenceJoin.class );
		assertFinalMethod( "hasFrom", FromClause.class );
		assertFinalMethod( "renderFromClauseSpaces", FromClause.class );
	}

	@Test
	void derivedTableWalkerCallbacksAreFinal() throws NoSuchMethodException {
		assertFinalMethod( "visitQueryPartTableReference", QueryPartTableReference.class );
		assertFinalMethod( "visitValuesTableReference", ValuesTableReference.class );
		assertFinalMethod( "visitFunctionTableReference", FunctionTableReference.class );
	}

	@Test
	void inlineCteRenderingIsAnInternalTemplateAndCorrelationHelpersAreNotExtensionHooks()
			throws NoSuchMethodException {
		assertPrivateMethod( "inlineCteTableGroup", TableGroup.class, LockMode.class, boolean.class );
		assertFinalMethod( "hasTargetTableCorrelation", MutationStatement.class );
		assertFinalMethod( "hasNestedOrTargetTableCorrelation", MutationStatement.class );
		assertNoDeclaredMethod( "isCorrelated", CteStatement.class );
		assertNoDeclaredMethod( "hasCorrelatedSubquery", Statement.class, String.class );
		assertNoDeclaredMethod( "hasNestedCorrelation", Statement.class );
	}

	@Test
	void lobDetectionIsAProtectedStaticFamilyUtility() throws NoSuchMethodException {
		final Method method = AbstractSqlAstTranslator.class.getDeclaredMethod( "isLob", JdbcMappingContainer.class );
		assertThat( Modifier.isProtected( method.getModifiers() ) ).isTrue();
		assertThat( Modifier.isStatic( method.getModifiers() ) ).isTrue();
	}

	@Test
	void namedSetReturningFunctionRenderingIsFinal() throws NoSuchMethodException {
		assertThat( Modifier.isFinal( AbstractSqlAstTranslator.class.getDeclaredMethod(
				"renderNamedSetReturningFunction",
				String.class,
				List.class,
				SetReturningFunctionType.class,
				String.class,
				SqlAstNodeRenderingMode.class
		).getModifiers() ) ).isTrue();
	}

	@Test
	void namedTableRenderingIsPrivateAndOwnsOrdinaryAndUnionHintPlacement() throws Exception {
		assertPrivateMethod( "renderNamedTableReference", NamedTableReference.class, LockMode.class );

		final List<TableLockHintRequest> ordinaryRequests = new ArrayList<>();
		final TestingTranslator ordinaryTranslator = createTranslator( lockingSupport( request -> {
			ordinaryRequests.add( request );
			return " /*hint*/";
		} ) );
		renderNamedTableReference(
				ordinaryTranslator,
				new NamedTableReference( "example_table", "e" ),
				LockMode.PESSIMISTIC_WRITE
		);
		assertThat( ordinaryTranslator.renderedSql() ).isEqualTo( "example_table e /*hint*/" );
		assertThat( ordinaryRequests ).singleElement().satisfies( request -> {
			assertThat( request.lockKind() ).isEqualTo( PessimisticLockKind.UPDATE );
			assertThat( request.timeout().milliseconds() ).isEqualTo( Timeouts.WAIT_FOREVER_MILLI );
			assertThat( request.tableExpression() ).isEqualTo( "example_table" );
		} );

		final List<TableLockHintRequest> unionRequests = new ArrayList<>();
		final TestingTranslator unionTranslator = createTranslator( lockingSupport( request -> {
			unionRequests.add( request );
			return " /*hint*/";
		} ) );
		renderNamedTableReference(
				unionTranslator,
				new UnionTableReference(
						"(select id from root_table union all select id from child_table)",
						new String[] { "root_table", "child_table" },
						"u"
				),
				LockMode.PESSIMISTIC_READ
		);
		assertThat( unionTranslator.renderedSql() ).isEqualTo(
				"(select id from root_table /*hint*/ union all select id from child_table /*hint*/) u"
		);
		assertThat( unionRequests ).extracting( TableLockHintRequest::tableExpression )
				.containsExactly( "select id from root_table", "select id from child_table" );
		assertThat( unionRequests ).allSatisfy( request ->
				assertThat( request.lockKind() ).isEqualTo( PessimisticLockKind.SHARE ) );
		assertThat( unionTranslator.getAffectedTableNames() ).containsExactlyInAnyOrder( "root_table", "child_table" );
	}

	@Test
	void primaryTableRenderingIsPrivateAndSuppliesSemanticPrefixContext() throws Exception {
		assertPrivateMethod( "renderPrimaryTableReference", TableGroup.class, LockMode.class, boolean.class );
		assertPrivateMethod( "renderRootTableGroup", TableGroup.class, List.class );
		assertPrivateMethod( "renderDmlTargetTableGroup", TableGroup.class );
		assertPrivateMethod( "hasNestedTableGroupsToRender", List.class );
		assertPrivateMethod( "determineRootTableGroupLockMode", TableGroup.class );
		assertFinalMethod( "needsLocking", QuerySpec.class );

		final TableGroup tableGroup = mock( TableGroup.class );
		when( tableGroup.getPrimaryTableReference() )
				.thenReturn( new NamedTableReference( "(select id from example_table)", "e" ) );
		final TestingTranslator translator = createTranslator();
		renderPrimaryTableReference( translator, tableGroup, LockMode.NONE, true );

		assertThat( translator.primaryTableReferenceContext() ).satisfies( context -> {
			assertThat( context.kind() ).isEqualTo( PrimaryTableReferenceKind.NAMED );
			assertThat( context.subqueryLike() ).isTrue();
			assertThat( context.beginsNestedJoinGroup() ).isTrue();
		} );
		assertThat( translator.renderedSql() ).isEqualTo( "(select id from example_table) e" );
	}

	@Test
	void h2PrimaryTablePrefixUsesOnlyNestedSubquerySemantics() {
		final TestingH2Translator translator = createH2Translator();
		translator.renderPrimaryPrefix( new TestingPrimaryTableReferenceContext(
				PrimaryTableReferenceKind.QUERY_PART,
				true,
				false
		) );
		translator.renderPrimaryPrefix( new TestingPrimaryTableReferenceContext(
				PrimaryTableReferenceKind.NAMED,
				false,
				true
		) );
		assertThat( translator.renderedSql() ).isEmpty();

		translator.renderPrimaryPrefix( new TestingPrimaryTableReferenceContext(
				PrimaryTableReferenceKind.QUERY_PART,
				true,
				true
		) );
		assertThat( translator.renderedSql() ).isEqualTo( "dual cross join " );
	}

	@Test
	void transactSqlLockingFamiliesSupplyFocusedTableHintRenderers() {
		assertThat( renderTableLockHint(
				TransactSQLLockingSupport.SQL_SERVER,
				PessimisticLockKind.UPDATE,
				Timeouts.SKIP_LOCKED
		) ).isEqualTo( " with (updlock,rowlock,readpast)" );
		assertThat( renderTableLockHint(
				TransactSQLLockingSupport.SQL_SERVER,
				PessimisticLockKind.SHARE,
				Timeouts.NO_WAIT
		) ).isEqualTo( " with (holdlock,rowlock,nowait)" );
		assertThat( renderTableLockHint(
				TransactSQLLockingSupport.SYBASE_ASE,
				PessimisticLockKind.UPDATE,
				Timeouts.SKIP_LOCKED
		) ).isEqualTo( " holdlock readpast" );
		assertThat( renderTableLockHint(
				TransactSQLLockingSupport.SYBASE_LEGACY,
				PessimisticLockKind.UPDATE,
				Timeouts.SKIP_LOCKED
		) ).isEqualTo( " holdlock readpast" );
		assertThat( renderTableLockHint(
				TransactSQLLockingSupport.SYBASE,
				PessimisticLockKind.SHARE,
				Timeouts.WAIT_FOREVER
		) ).isEqualTo( " holdlock" );
		assertThat( renderTableLockHint(
				TransactSQLLockingSupport.forSybaseAnywhere( DatabaseVersion.make( 9 ) ),
				PessimisticLockKind.UPDATE,
				Timeouts.WAIT_FOREVER
		) ).isEqualTo( " holdlock" );
		assertThat( renderTableLockHint(
				TransactSQLLockingSupport.forSybaseAnywhere( DatabaseVersion.make( 10 ) ),
				PessimisticLockKind.UPDATE,
				Timeouts.WAIT_FOREVER
		) ).isEmpty();
	}

	@Test
	void tableReferenceAliasTemplateIsFinalAndDerivesSemanticContext() throws NoSuchMethodException {
		assertFinalMethod( "renderTableReferenceIdentificationVariable", TableReference.class );

		final NamedTableReference fromReference = new NamedTableReference( "from_table", "f" );
		final TestingTranslator selectTranslator = createTranslator();
		selectTranslator.renderAlias( fromReference );
		assertThat( selectTranslator.tableReferenceAliasContext() )
				.isEqualTo( TableReferenceAliasContext.FROM );
		assertThat( selectTranslator.renderedSql() ).isEqualTo( " f" );

		final NamedTableReference insertTarget = new NamedTableReference( "insert_table", "i" );
		assertMutationAliasContext(
				new InsertSelectStatement( insertTarget, List.of() ),
				insertTarget,
				TableReferenceAliasContext.INSERT_TARGET
		);

		final NamedTableReference updateTarget = new NamedTableReference( "update_table", "u" );
		assertMutationAliasContext(
				new UpdateStatement( updateTarget, List.of(), null ),
				updateTarget,
				TableReferenceAliasContext.UPDATE_TARGET
		);

		final NamedTableReference deleteTarget = new NamedTableReference( "delete_table", "d" );
		assertMutationAliasContext(
				new DeleteStatement( deleteTarget, null ),
				deleteTarget,
				TableReferenceAliasContext.DELETE_TARGET
		);

		final TestingPostgreSQLFamilyTranslator familyTranslator =
				createPostgreSQLFamilyTranslator( new InsertSelectStatement( insertTarget, List.of() ) );
		familyTranslator.renderAlias( insertTarget );
		familyTranslator.renderAlias( fromReference );
		assertThat( familyTranslator.renderedSql() ).isEqualTo( " as i f" );
	}

	@Test
	void tableJoinHelpersAreInternalAndFamilySupportsSelectSemanticPlans() throws NoSuchMethodException {
		assertPrivateMethod( "renderTableGroupJoin", TableGroupJoin.class, List.class );
		assertPrivateMethod( "renderJoinedTableGroup", TableGroupJoin.class, Predicate.class, List.class );
		assertPrivateMethod( "processTableGroupJoin", TableGroupJoin.class, List.class );
		assertPrivateMethod( "renderTableReferenceJoins", TableGroup.class, org.hibernate.LockMode.class );
		assertPrivateMethod(
				"renderTableReferenceJoins",
				TableGroup.class,
				org.hibernate.LockMode.class,
				int.class,
				boolean.class
		);
		assertPrivateMethod( "consumeLateralJoinPredicate" );

		assertThat( StandardTableJoinRenderingSupport.DB2.determinePlan(
				joinRequest( TableJoinKind.TABLE_GROUP, SqlAstJoinType.INNER, false, true, true )
		) ).isInstanceOf( TableJoinRenderingPlan.Comma.class );
		assertThat( StandardTableJoinRenderingSupport.DB2.determinePlan(
				joinRequest( TableJoinKind.TABLE_REFERENCE, SqlAstJoinType.CROSS, false, true, false )
		) ).isInstanceOf( TableJoinRenderingPlan.Comma.class );
		assertThat( StandardTableJoinRenderingSupport.DB2.determinePlan(
				joinRequest( TableJoinKind.TABLE_GROUP, SqlAstJoinType.LEFT, false, true, true )
		) ).isInstanceOf( TableJoinRenderingPlan.Unsupported.class );
		assertThat( StandardTableJoinRenderingSupport.DB2.determinePlan(
				joinRequest( TableJoinKind.TABLE_GROUP, SqlAstJoinType.LEFT, false, false, true )
		) ).isInstanceOf( TableJoinRenderingPlan.Standard.class );

		final TableJoinRenderingPlan.Apply outerApply = (TableJoinRenderingPlan.Apply)
				StandardTableJoinRenderingSupport.SQL_SERVER.determinePlan(
						joinRequest( TableJoinKind.TABLE_GROUP, SqlAstJoinType.LEFT, true, false, true )
				);
		assertThat( outerApply.kind() ).isEqualTo( TableJoinRenderingPlan.Apply.Kind.OUTER );
		final TableJoinRenderingPlan.Apply crossApply = (TableJoinRenderingPlan.Apply)
				StandardTableJoinRenderingSupport.SQL_SERVER.determinePlan(
						joinRequest( TableJoinKind.TABLE_GROUP, SqlAstJoinType.INNER, true, false, false )
				);
		assertThat( crossApply.kind() ).isEqualTo( TableJoinRenderingPlan.Apply.Kind.CROSS );
		assertThat( StandardTableJoinRenderingSupport.SQL_SERVER.determinePlan(
				joinRequest( TableJoinKind.TABLE_REFERENCE, SqlAstJoinType.LEFT, true, false, true )
		) ).isInstanceOf( TableJoinRenderingPlan.Standard.class );
	}

	@Test
	void tableReferenceJoinTemplateSuppliesReadOnlySemanticRequest() {
		final Predicate predicate = mock( Predicate.class );
		final TableGroup tableGroup = mock( TableGroup.class );
		when( tableGroup.getPrimaryTableReference() )
				.thenReturn( new NamedTableReference( "root_table", "r" ) );
		when( tableGroup.getTableReferenceJoins() ).thenReturn( List.of(
				new TableReferenceJoin( true, new NamedTableReference( "joined_table", "j" ), predicate )
		) );

		final TableJoinRenderingRequest[] capturedRequest = new TableJoinRenderingRequest[1];
		final TestingTranslator translator = createTranslator();
		translator.useTableJoinRenderingSupport( request -> {
			capturedRequest[0] = request;
			return new TableJoinRenderingPlan.Comma();
		} );
		translator.renderRootTableGroup( tableGroup );

		assertThat( translator.renderedSql() ).isEqualTo( "root_table r,joined_table j" );
		assertThat( capturedRequest[0].kind() ).isEqualTo( TableJoinKind.TABLE_REFERENCE );
		assertThat( capturedRequest[0].joinType() ).isEqualTo( SqlAstJoinType.INNER );
		assertThat( capturedRequest[0].lateral() ).isFalse();
		assertThat( capturedRequest[0].recursiveQueryPart() ).isFalse();
		assertThat( capturedRequest[0].predicatePresent() ).isTrue();
	}

	@Test
	void applyPlanInjectsPredicateIntoLateralSubquery() {
		final TestingTranslator translator = createTranslator();
		translator.useTableJoinRenderingSupport( StandardTableJoinRenderingSupport.SQL_SERVER );
		translator.useDerivedTableRenderingSupport( request -> new DerivedTableRenderingPlan.QueryPart(
				DerivedColumnAliasing.DECLARATION,
				LateralReferenceStyle.IMPLICIT,
				false,
				null
		) );

		final Predicate predicate = mock( Predicate.class );
		doAnswer( invocation -> {
			translator.appendSql( "joined_predicate" );
			return null;
		} ).when( predicate ).accept( translator );

		translator.renderRootTableGroup( lateralRootTableGroup( predicate ) );

		assertThat( translator.renderedSql() )
				.contains( " cross apply " )
				.contains( " where joined_predicate" )
				.doesNotContain( " on joined_predicate" );
	}

	@Test
	void applyPredicateLifecycleIsRestoredWhenRenderingFails() {
		final TestingTranslator translator = createTranslator();
		translator.useTableJoinRenderingSupport( StandardTableJoinRenderingSupport.SQL_SERVER );
		translator.failWhileRenderingDerivedTable();

		final Predicate predicate = mock( Predicate.class );
		assertThatExceptionOfType( IllegalStateException.class )
				.isThrownBy( () -> translator.renderRootTableGroup( lateralRootTableGroup( predicate ) ) )
				.withMessage( "Expected derived-table failure" );

		final String sqlAtFailure = translator.renderedSql();
		translator.renderWhere();
		assertThat( translator.renderedSql() ).isEqualTo( sqlAtFailure );
	}

	private static TableGroup lateralRootTableGroup(Predicate predicate) {
		final QueryPartTableReference queryPartReference = new QueryPartTableReference(
				new SelectStatement( new QuerySpec( true ) ),
				"j",
				List.of( "c" ),
				true,
				mock( SessionFactoryImplementor.class )
		);
		final TableGroup joinedGroup = mock( TableGroup.class );
		when( joinedGroup.isLateral() ).thenReturn( true );
		when( joinedGroup.isInitialized() ).thenReturn( true );
		when( joinedGroup.canUseInnerJoins() ).thenReturn( true );
		when( joinedGroup.getPrimaryTableReference() ).thenReturn( queryPartReference );
		when( joinedGroup.getTableReferenceJoins() ).thenReturn( List.of() );

		final TableGroupJoin join = new TableGroupJoin(
				new NavigablePath( "joined" ),
				SqlAstJoinType.INNER,
				joinedGroup,
				predicate
		);
		final TableGroup rootGroup = mock( TableGroup.class );
		when( rootGroup.getPrimaryTableReference() )
				.thenReturn( new NamedTableReference( "root_table", "r" ) );
		when( rootGroup.getTableReferenceJoins() ).thenReturn( List.of() );
		doAnswer( invocation -> {
			final Consumer<TableGroupJoin> consumer = invocation.getArgument( 0 );
			consumer.accept( join );
			return null;
		} ).when( rootGroup ).visitTableGroupJoins( any() );
		return rootGroup;
	}

	private static TableJoinRenderingRequest joinRequest(
			TableJoinKind kind,
			SqlAstJoinType joinType,
			boolean lateral,
			boolean recursiveQueryPart,
			boolean predicatePresent) {
		return new TestingTableJoinRenderingRequest(
				kind,
				joinType,
				lateral,
				recursiveQueryPart,
				predicatePresent
		);
	}

	private static void assertMutationAliasContext(
			MutationStatement statement,
			NamedTableReference target,
			TableReferenceAliasContext expectedContext) {
		final TestingMutationTranslator translator = createMutationTranslator(
				statement,
				MutationSyntaxSupport.NONE
		);
		translator.renderAlias( target );

		assertThat( translator.tableReferenceAliasContext() ).isEqualTo( expectedContext );
		assertThat( translator.renderedSql() ).isEqualTo( " " + target.getIdentificationVariable() );
	}

	@Test
	void derivedTableTemplateRestoresContextAndRenderingModeWhenRenderingFails() {
		final TestingTranslator translator = createTranslator();
		translator.useDerivedTableRenderingSupport( request -> {
			assertThat( request.kind() ).isEqualTo( DerivedTableKind.QUERY_PART );
			assertThat( request.lateral() ).isTrue();
			assertThat( request.queryPartRoot() ).isTrue();
			assertThat( request.columnNames() ).containsExactly( "c1" );
			return new DerivedTableRenderingPlan.QueryPart(
					DerivedColumnAliasing.SELECT_LIST,
					LateralReferenceStyle.IMPLICIT,
					false,
					SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS
			);
		} );
		translator.failWhileRenderingDerivedTable();
		final QueryPartTableReference tableReference = new QueryPartTableReference(
				new SelectStatement( new QuerySpec( true ) ),
				"d",
				List.of( "c1" ),
				true,
				mock( SessionFactoryImplementor.class )
		);

		assertThatExceptionOfType( IllegalStateException.class )
				.isThrownBy( () -> translator.visitQueryPartTableReference( tableReference ) );

		assertThat( translator.lateralDerivedTableWhileRendering() ).isTrue();
		assertThat( translator.derivedTableKindWhileRendering() ).isTrue();
		assertThat( translator.derivedTableRenderingMode() )
				.isEqualTo( SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS );
		assertThat( translator.isInLateralDerivedTable() ).isFalse();
		assertThat( translator.isInQueryPartDerivedTable() ).isFalse();
		assertThat( translator.restoredRenderingMode() ).isEqualTo( SqlAstNodeRenderingMode.DEFAULT );
	}

	@Test
	void functionTableTemplateUsesTheDerivedTablePlan() {
		final TestingTranslator standardTranslator = createTranslator();
		final DerivedTableRenderingRequest[] capturedRequest = new DerivedTableRenderingRequest[1];
		standardTranslator.useDerivedTableRenderingSupport( request -> {
			capturedRequest[0] = request;
			return StandardDerivedTableRenderingSupport.STANDARD.determinePlan( request );
		} );
		standardTranslator.visitFunctionTableReference(
				functionTableReference( standardTranslator, "series()", false )
		);

		assertThat( capturedRequest[0].kind() ).isEqualTo( DerivedTableKind.FUNCTION );
		assertThat( capturedRequest[0].columnNames() ).containsExactly( "value", "ordinality" );
		assertThat( standardTranslator.renderedSql() ).isEqualTo( "series() f" );

		final TestingTranslator oracleTranslator = createTranslator();
		oracleTranslator.useDerivedTableRenderingSupport( StandardDerivedTableRenderingSupport.ORACLE );
		oracleTranslator.visitFunctionTableReference(
				functionTableReference( oracleTranslator, "series()", false )
		);
		assertThat( oracleTranslator.renderedSql() ).isEqualTo( "series() f" );
	}

	@Test
	void functionTableTemplateHonorsAnExpressionWhichRendersItsOwnAlias() {
		final TestingTranslator translator = createTranslator();
		translator.useDerivedTableRenderingSupport( StandardDerivedTableRenderingSupport.ORACLE );
		translator.visitFunctionTableReference(
				functionTableReference( translator, "series() f", true )
		);

		assertThat( translator.renderedSql() ).isEqualTo( "series() f" );
	}

	@Test
	void standardSetReturningFunctionSupportsSelectFamilyPlans() {
		final SetReturningFunctionRenderingRequest ordinary =
				new TestingSetReturningFunctionRenderingRequest( false, null );
		final SetReturningFunctionRenderingRequest withOrdinality =
				new TestingSetReturningFunctionRenderingRequest( true, "ordinality_" );

		assertThat( StandardSetReturningFunctionRenderingSupport.NATIVE.determinePlan( ordinary ) )
				.isEqualTo( new SetReturningFunctionRenderingPlan.Native(
						SetReturningFunctionRenderingPlan.Native.Ordinality.NONE
				) );
		assertThat( StandardSetReturningFunctionRenderingSupport.NATIVE.determinePlan( withOrdinality ) )
				.isInstanceOf( SetReturningFunctionRenderingPlan.Unsupported.class );
		assertThat( StandardSetReturningFunctionRenderingSupport.NATIVE_WITH_ORDINALITY
				.determinePlan( withOrdinality ) )
				.isEqualTo( new SetReturningFunctionRenderingPlan.Native(
						SetReturningFunctionRenderingPlan.Native.Ordinality.WITH_ORDINALITY
				) );

		assertDerivedOrdinalityPlan(
				StandardSetReturningFunctionRenderingSupport.HANA.determinePlan( withOrdinality ),
				SetReturningFunctionRenderingPlan.DerivedOrdinality.InvocationWrapper.NONE,
				SetReturningFunctionRenderingPlan.DerivedOrdinality.OrdinalityExpression.ROW_NUMBER,
				false
		);
		assertDerivedOrdinalityPlan(
				StandardSetReturningFunctionRenderingSupport.SQL_SERVER.determinePlan( withOrdinality ),
				SetReturningFunctionRenderingPlan.DerivedOrdinality.InvocationWrapper.NONE,
				SetReturningFunctionRenderingPlan.DerivedOrdinality.OrdinalityExpression.ROW_NUMBER_DUMMY_ORDER,
				false
		);
		assertDerivedOrdinalityPlan(
				StandardSetReturningFunctionRenderingSupport.DB2.determinePlan( withOrdinality ),
				SetReturningFunctionRenderingPlan.DerivedOrdinality.InvocationWrapper.TABLE,
				SetReturningFunctionRenderingPlan.DerivedOrdinality.OrdinalityExpression.ROW_NUMBER,
				true
		);
		assertThat( StandardSetReturningFunctionRenderingSupport.DB2.determinePlan( ordinary ) )
				.isInstanceOf( SetReturningFunctionRenderingPlan.TableWrapped.class );
		assertDerivedOrdinalityPlan(
				StandardSetReturningFunctionRenderingSupport.ORACLE.determinePlan( withOrdinality ),
				SetReturningFunctionRenderingPlan.DerivedOrdinality.InvocationWrapper.TABLE,
				SetReturningFunctionRenderingPlan.DerivedOrdinality.OrdinalityExpression.ROWNUM,
				true
		);
	}

	@Test
	void genericSetReturningFunctionTemplateRendersEveryPlanShape() {
		assertThat( renderSetReturningFunction(
				StandardSetReturningFunctionRenderingSupport.NATIVE_WITH_ORDINALITY,
				false
		) ).isEqualTo( "series()" );
		assertThat( renderSetReturningFunction(
				StandardSetReturningFunctionRenderingSupport.NATIVE_WITH_ORDINALITY,
				true
		) ).isEqualTo( "series() with ordinality" );
		assertThat( renderSetReturningFunction(
				StandardSetReturningFunctionRenderingSupport.HANA,
				true
		) ).isEqualTo( "(select t.*, row_number() over() ordinality_ from series() t)" );
		assertThat( renderSetReturningFunction(
				StandardSetReturningFunctionRenderingSupport.SQL_SERVER,
				true
		) ).isEqualTo(
				"(select t.*, row_number() over(order by (select 1)) ordinality_ from series() t)"
		);
		assertThat( renderSetReturningFunction(
				StandardSetReturningFunctionRenderingSupport.DB2,
				false
		) ).isEqualTo( "table(series())" );
		assertThat( renderSetReturningFunction(
				StandardSetReturningFunctionRenderingSupport.DB2,
				true
		) ).isEqualTo(
				"lateral (select t.*, row_number() over() ordinality_ from table(series()) t)"
		);
		assertThat( renderSetReturningFunction(
				StandardSetReturningFunctionRenderingSupport.ORACLE,
				true
		) ).isEqualTo( "lateral (select t.*, rownum ordinality_ from table(series()) t)" );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> renderSetReturningFunction(
						StandardSetReturningFunctionRenderingSupport.NATIVE,
						true
				) )
				.withMessageContaining( "series" );
	}

	private static void assertDerivedOrdinalityPlan(
			SetReturningFunctionRenderingPlan plan,
			SetReturningFunctionRenderingPlan.DerivedOrdinality.InvocationWrapper wrapper,
			SetReturningFunctionRenderingPlan.DerivedOrdinality.OrdinalityExpression expression,
			boolean lateral) {
		assertThat( plan ).isEqualTo(
				new SetReturningFunctionRenderingPlan.DerivedOrdinality( wrapper, expression, lateral )
		);
	}

	private static String renderSetReturningFunction(
			SetReturningFunctionRenderingSupport support,
			boolean ordinality) {
		final TestingTranslator translator = createTranslator();
		translator.useSetReturningFunctionRenderingSupport( support );
		translator.renderNamedSetReturningFunction(
				"series",
				List.of(),
				setReturningFunctionType( ordinality ),
				"f",
				SqlAstNodeRenderingMode.DEFAULT
		);
		return translator.renderedSql();
	}

	private static SetReturningFunctionType setReturningFunctionType(boolean ordinality) {
		final SetReturningFunctionType tupleType = mock( SetReturningFunctionType.class );
		if ( ordinality ) {
			final BasicValuedModelPart ordinalityPart = mock( BasicValuedModelPart.class );
			when( ordinalityPart.asBasicValuedModelPart() ).thenReturn( ordinalityPart );
			when( ordinalityPart.getSelectionExpression() ).thenReturn( "ordinality_" );
			when( tupleType.findSubPart( any() ) ).thenReturn( ordinalityPart );
		}
		return tupleType;
	}

	@Test
	void fromClauseTemplateRestoresTheClauseStackWhenRenderingFails() {
		final FromClause fromClause = new FromClause();
		final TableGroup root = mock( TableGroup.class );
		when( root.isInitialized() ).thenReturn( true );
		when( root.getPrimaryTableReference() ).thenReturn( new NamedTableReference( "example_table", "e" ) );
		fromClause.addRoot( root );
		final TestingTranslator translator = createTranslator();
		translator.failWhileRenderingFromClause();

		assertThatExceptionOfType( IllegalStateException.class )
				.isThrownBy( () -> translator.visitFromClause( fromClause ) );

		assertThat( translator.fromClauseWhileRenderingRoot() ).isEqualTo( Clause.FROM );
		assertThat( translator.clauseStackDepth() ).isZero();
	}

	@Test
	void exposedCollectionsAreReadOnly() {
		final TestingTranslator translator = createTranslator();
		translator.addAffectedTableName( "example_table" );

		assertThat( translator.getAffectedTableNames() ).containsExactly( "example_table" );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> translator.getAffectedTableNames().clear() );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> translator.parameterBinders().clear() );
	}

	@Test
	void statementPrefixIsRenderedInIsolation() {
		final TestingTranslator translator = createTranslator();
		translator.appendSql( "select 1" );
		translator.renderPrefix( sqlAppender -> sqlAppender.appendSql( "locking " ) );

		assertThat( translator.renderedSql() ).isEqualTo( "locking select 1" );
	}

	@Test
	void selectStatementLifecycleIsRestoredWhenRenderingFails() {
		final TestingTranslator translator = createTranslator();
		final SelectStatement statement = (SelectStatement) translator.getSqlAst();
		translator.failWhileRenderingSelectWithPriorMode();

		assertThatExceptionOfType( IllegalStateException.class )
				.isThrownBy( () -> translator.visitSelectStatement( statement ) );

		assertThat( translator.selectStatementStackDepth() ).isEqualTo( 2 );
		assertThat( translator.selectRenderingMode() ).isEqualTo( SqlAstNodeRenderingMode.DEFAULT );
		assertThat( translator.statementStackDepth() ).isOne();
		assertThat( translator.restoredRenderingMode() ).isEqualTo( SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS );
	}

	@Test
	void namedQueryGroupHelperOmitsGroupOrderingAndRestoresState() {
		final TestingTranslator translator = createTranslator();
		final QueryGroup queryGroup = new QueryGroup(
				true,
				SetOperator.UNION_ALL,
				List.of( new QuerySpec( false ) )
		);
		queryGroup.addSortSpecification( mock( SortSpecification.class ) );

		translator.renderQueryGroupBody( queryGroup );

		assertThat( translator.groupOrderByVisited() ).isFalse();
		assertThat( translator.beforeQueryGroupCount() ).isOne();
		assertThat( translator.afterQueryGroupCount() ).isOne();
		assertThat( translator.queryPartStackDepth() ).isZero();
	}

	@Test
	void selectClauseStackIsRestoredWhenRenderingFails() {
		final TestingTranslator translator = createTranslator();
		translator.failWhileRenderingSelectClause();

		assertThatExceptionOfType( IllegalStateException.class )
				.isThrownBy( () -> translator.visitSelectClause( new SelectClause() ) );

		assertThat( translator.selectClauseStackDepth() ).isOne();
		assertThat( translator.clauseStackDepth() ).isZero();
	}

	@Test
	void groupByClauseStackIsRestoredWhenRenderingFails() {
		final TestingTranslator translator = createTranslator();
		final QuerySpec querySpec = new QuerySpec( false );
		querySpec.setGroupByClauseExpressions( List.of( mock( Expression.class ) ) );
		translator.failWhileRenderingPartitionItem();

		assertThatExceptionOfType( IllegalStateException.class )
				.isThrownBy( () -> translator.renderGroupBy( querySpec ) );

		assertThat( translator.partitionItemClauseStackDepth() ).isOne();
		assertThat( translator.clauseStackDepth() ).isZero();
	}

	@Test
	void partitionRenderingModeIsRestoredWhenRenderingFails() {
		final TestingTranslator translator = createTranslator();
		translator.failWhileRenderingPartitionItem();

		assertThatExceptionOfType( IllegalStateException.class )
				.isThrownBy( () -> translator.renderAliasedPartitionItem(
						mock( Expression.class ),
						mock( Expression.class )
				) );

		assertThat( translator.partitionItemRenderingMode() )
				.isEqualTo( SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS );
		assertThat( translator.restoredRenderingMode() ).isEqualTo( SqlAstNodeRenderingMode.DEFAULT );
	}

	@Test
	void havingClauseStackIsRestoredWhenRenderingFails() {
		final TestingTranslator translator = createTranslator();
		final QuerySpec querySpec = new QuerySpec( false );
		final Predicate predicate = mock( Predicate.class );
		when( predicate.isEmpty() ).thenReturn( false );
		doAnswer( invocation -> {
			translator.recordHavingClauseStackDepth();
			throw new IllegalStateException( "Expected test failure" );
		} ).when( predicate ).accept( translator );
		querySpec.setHavingClauseRestrictions( predicate );

		assertThatExceptionOfType( IllegalStateException.class )
				.isThrownBy( () -> translator.renderHaving( querySpec ) );

		assertThat( translator.havingClauseStackDepth() ).isOne();
		assertThat( translator.clauseStackDepth() ).isZero();
	}

	@Test
	void emptyWindowOrderByUsesFocusedHookAndRestoresClauseStack() {
		final TestingTranslator translator = createTranslator();

		translator.renderEmptyWindowOrderBy();

		assertThat( translator.emptyOrderByCount() ).isOne();
		assertThat( translator.renderedSql() ).isEqualTo( " order by (select 0)" );
		assertThat( translator.clauseStackDepth() ).isZero();
	}

	@Test
	void sortSpecificationTemplateExpandsTuples() {
		final TestingTranslator translator = createTranslator();
		final SqlTuple tuple = new SqlTuple(
				List.of( mock( Expression.class ), mock( Expression.class ) ),
				null
		);

		translator.visitSortSpecification( new SortSpecification( tuple, SortDirection.ASCENDING, Nulls.NONE ) );

		assertThat( translator.sortItemCount() ).isEqualTo( 2 );
	}

	@Test
	void paginationSupportReceivesReadOnlyContextAndSelectsTrailingPlan() {
		final TestingTranslator translator = createTranslator();
		final QuerySpec querySpec = new QuerySpec( false );
		final Expression offset = renderingExpression( translator, "2" );
		final Expression fetch = renderingExpression( translator, "3" );
		querySpec.setOffsetClauseExpression( offset );
		querySpec.setFetchClauseExpression( fetch, FetchClauseType.ROWS_ONLY );
		translator.usePaginationSupport( request -> {
			assertThat( request.queryPart() ).isSameAs( querySpec );
			assertThat( request.usesQueryOptionsLimit() ).isFalse();
			assertThat( request.hasOffset() ).isTrue();
			assertThat( request.hasFetch() ).isTrue();
			assertThat( request.fetchClauseType() ).isEqualTo( FetchClauseType.ROWS_ONLY );
			assertThat( request.isRowNumberingCurrentQueryPart() ).isFalse();
			assertThat( request.isNestedInQueryGroup() ).isFalse();
			return new PaginationRenderingPlan.CombinedLimit();
		} );

		translator.renderTrailingPagination( querySpec );

		assertThat( translator.renderedSql() ).isEqualTo( " limit 2,3" );
	}

	@Test
	void fetchPlusOffsetPlanRendersCombinedTrailingFetchCount() {
		final TestingTranslator translator = createTranslator();
		final QuerySpec querySpec = new QuerySpec( false );
		querySpec.setOffsetClauseExpression( renderingExpression( translator, "2" ) );
		querySpec.setFetchClauseExpression(
				renderingExpression( translator, "3" ),
				FetchClauseType.ROWS_ONLY
		);
		translator.usePaginationSupport( request -> new PaginationRenderingPlan.FetchPlusOffset() );

		translator.renderTrailingPagination( querySpec );

		assertThat( translator.renderedSql() ).isEqualTo( " fetch first 3+2 rows only" );
	}

	@Test
	void topPaginationPlanIsRenderedAtSelectClause() {
		final TestingTranslator translator = createTranslator();
		final QuerySpec querySpec = new QuerySpec( false );
		querySpec.setOffsetClauseExpression( renderingExpression( translator, "2" ) );
		querySpec.setFetchClauseExpression(
				renderingExpression( translator, "3" ),
				FetchClauseType.ROWS_ONLY
		);
		translator.usePaginationSupport( request -> new PaginationRenderingPlan.Top( true, true ) );

		translator.renderSelectClause( querySpec );

		assertThat( translator.renderedSql() ).isEqualTo( "select top (3+2) " );
	}

	@Test
	void cteSelectHintIsRenderedOnceAtTheFirstSelect() {
		final TestingTranslator translator = createTranslator();
		final QuerySpec cteQuery = new QuerySpec( false );
		final Statement cteDefinition = mock( Statement.class );
		doAnswer( invocation -> {
			translator.renderSelectClause( cteQuery );
			translator.renderSelectClause( cteQuery );
			return null;
		} ).when( cteDefinition ).accept( translator );

		final CteStatement cte = new CteStatement( new CteTable( "cte", List.of() ), cteDefinition );
		final CteContainer cteContainer = mock( CteContainer.class );
		when( cteContainer.getCteStatements() ).thenReturn( Map.of( "cte", cte ) );
		when( cteContainer.getCteObjects() ).thenReturn( Map.of() );

		translator.visitCteContainer( cteContainer );

		assertThat( translator.cteSelectHintCount() ).isOne();
		assertThat( translator.renderedSql() ).containsOnlyOnce( "/*cte*/" );
	}

	@Test
	void inlineCteSuppliesSemanticRequestAndRestoresStateWhenRenderingFails() {
		final TestingTranslator translator = createTranslator( new QuerySpec( false ) );
		final DerivedTableRenderingRequest[] capturedRequest = new DerivedTableRenderingRequest[1];
		translator.useDerivedTableRenderingSupport( request -> {
			capturedRequest[0] = request;
			return StandardDerivedTableRenderingSupport.HANA.determinePlan( request );
		} );
		translator.failWhileRenderingDerivedTable();

		final Limit originalLimit = new Limit( 2, 5 );
		translator.useLimit( originalLimit );
		final CteTable cteTable = new CteTable(
				"cte",
				List.of( new CteColumn( "c1", mock( JdbcMapping.class ) ) )
		);
		final CteStatement cte = new CteStatement(
				cteTable,
				new SelectStatement( new QuerySpec( true ) )
		);
		( (CteContainer) translator.getSqlAst() ).addCteStatement( cte );
		final CteTableGroup tableGroup = new CteTableGroup(
				new NamedTableReference( "cte", "c" )
		);

		assertThatExceptionOfType( IllegalStateException.class )
				.isThrownBy( () -> translator.inlineCteTableGroup( tableGroup ) )
				.withMessage( "Expected derived-table failure" );

		assertThat( capturedRequest[0].kind() ).isEqualTo( DerivedTableKind.INLINE_CTE );
		assertThat( capturedRequest[0].lateral() ).isTrue();
		assertThat( translator.lateralDerivedTableWhileRendering() ).isTrue();
		assertThat( translator.inlineCteKindWhileRendering() ).isTrue();
		assertThat( translator.statementStackDepth() ).isOne();
		assertThat( translator.currentLimit() ).isSameAs( originalLimit );
		assertThat( translator.isInLateralDerivedTable() ).isFalse();
		assertThat( translator.isInInlineCteDerivedTable() ).isFalse();
	}

	@Test
	void windowPaginationPlanControlsWholeQueryTraversal() {
		final TestingTranslator translator = createTranslator();
		final QuerySpec querySpec = new QuerySpec( false );
		querySpec.setFetchClauseExpression(
				renderingExpression( translator, "3" ),
				FetchClauseType.ROWS_ONLY
		);
		translator.usePaginationSupport( request -> new PaginationRenderingPlan.Window( false ) );

		translator.visitQuerySpec( querySpec );

		assertThat( translator.windowQueryPart() ).isSameAs( querySpec );
		assertThat( translator.emulateWindowFetchClause() ).isFalse();
	}

	@Test
	void sqlServerPaginationSupportSelectsTopOffsetFetchAndWindowPlans() {
		final SQLServerPaginationRenderingSupport support = SQLServerPaginationRenderingSupport.MODERN;
		final QuerySpec querySpec = new QuerySpec( false );
		final PaginationRenderingRequest request = mock( PaginationRenderingRequest.class );
		when( request.queryPart() ).thenReturn( querySpec );
		when( request.hasOffset() ).thenReturn( true );
		when( request.hasFetch() ).thenReturn( true );
		when( request.fetchClauseType() ).thenReturn( FetchClauseType.ROWS_ONLY );

		assertThat( support.determinePlan( request ) )
				.isEqualTo( new PaginationRenderingPlan.OffsetFetch( true ) );

		querySpec.getSelectClause().makeDistinct( true );
		assertThat( support.determinePlan( request ) )
				.isEqualTo( new PaginationRenderingPlan.Window( false ) );

		when( request.hasOffset() ).thenReturn( false );
		assertThat( support.determinePlan( request ) )
				.isEqualTo( new PaginationRenderingPlan.Top( true, true ) );
	}

	@Test
	void legacySqlServerPaginationSupportAccountsForVersionedSyntax() {
		final SQLServerPaginationRenderingSupport support =
				new SQLServerPaginationRenderingSupport( false, false );
		final PaginationRenderingRequest request = mock( PaginationRenderingRequest.class );
		when( request.hasOffset() ).thenReturn( true );
		when( request.hasFetch() ).thenReturn( true );
		when( request.fetchClauseType() ).thenReturn( FetchClauseType.ROWS_ONLY );

		when( request.queryPart() ).thenReturn( new QuerySpec( false ) );
		assertThat( support.determinePlan( request ) )
				.isEqualTo( new PaginationRenderingPlan.Top( true, true ) );

		when( request.queryPart() ).thenReturn( new QueryGroup(
				false,
				SetOperator.UNION_ALL,
				List.of( new QuerySpec( false ) )
		) );
		assertThat( support.determinePlan( request ) )
				.isEqualTo( new PaginationRenderingPlan.Window( false ) );
	}

	@Test
	void mandatoryWhereCapabilityIsRenderedByTheGenericMutationTemplates() {
		final MutationSyntaxSupport support = MutationSyntaxSupport.builder()
				.capability( MutationKind.UPDATE, MutationSyntaxCapability.REQUIRES_WHERE )
				.capability( MutationKind.DELETE, MutationSyntaxCapability.REQUIRES_WHERE )
				.build();
		final NamedTableReference targetTable = new NamedTableReference( "example_table", "t" );

		final DeleteStatement deleteStatement = new DeleteStatement( targetTable, null );
		final TestingMutationTranslator deleteTranslator = createMutationTranslator( deleteStatement, support );
		deleteTranslator.visitDeleteStatement( deleteStatement );
		assertThat( deleteTranslator.renderedSql() ).isEqualTo( "delete from example_table where true" );

		final UpdateStatement updateStatement = new UpdateStatement(
				targetTable,
				List.of( mock( Assignment.class ) ),
				null
		);
		final TestingMutationTranslator updateTranslator = createMutationTranslator( updateStatement, support );
		updateTranslator.visitUpdateStatement( updateStatement );
		assertThat( updateTranslator.renderedSql() ).isEqualTo( "update example_table set value=1 where true" );
	}

	@Test
	void returningTemplatesAreFinalAndOwnSelectionWrappingAndColumnTraversal() throws NoSuchMethodException {
		assertFinalMethod( "visitDeleteStatement", DeleteStatement.class );
		assertFinalMethod( "visitUpdateStatement", UpdateStatement.class );
		assertFinalMethod( "visitInsertStatement", InsertSelectStatement.class );
		assertFinalMethod( "visitStandardTableDelete", TableDeleteStandard.class );
		assertFinalMethod( "visitStandardTableUpdate", TableUpdateStandard.class );
		assertFinalMethod( "visitStandardTableInsert", TableInsertStandard.class );
		assertNoDeclaredMethod( "visitReturningColumns", List.class );

		final NamedTableReference targetTable = new NamedTableReference( "example_table", "t" );
		final ColumnReference id = new ColumnReference( targetTable, "id", mock( JdbcMapping.class ) );
		final DeleteStatement deleteStatement = new DeleteStatement( targetTable, null, List.of( id ) );

		final TestingMutationTranslator clauseTranslator =
				createMutationTranslator( deleteStatement, MutationSyntaxSupport.NONE );
		clauseTranslator.visitDeleteStatement( deleteStatement );
		assertThat( clauseTranslator.renderedSql() )
				.isEqualTo( "delete from example_table returning id" );

		final ReturningRenderingRequest[] capturedRequest = new ReturningRenderingRequest[1];
		final TestingMutationTranslator changeTableTranslator =
				createMutationTranslator( deleteStatement, MutationSyntaxSupport.NONE );
		changeTableTranslator.useReturningRenderingSupport( request -> {
			capturedRequest[0] = request;
			return new ReturningRenderingPlan.ChangeTable(
					org.hibernate.dialect.sql.ast.spi.ChangeTableKind.OLD
			);
		} );
		changeTableTranslator.visitDeleteStatement( deleteStatement );

		assertThat( changeTableTranslator.renderedSql() )
				.isEqualTo( "select id from old table (delete from example_table)" );
		assertThat( capturedRequest[0].mutationKind() ).isEqualTo( MutationKind.DELETE );
		assertThat( capturedRequest[0].source() ).isEqualTo( ReturningMutationSource.QUERY );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> capturedRequest[0].returningColumns().add( id ) );
	}

	@Test
	void nonePlanCannotSilentlyDiscardRequestedReturningColumns() {
		final NamedTableReference targetTable = new NamedTableReference( "example_table", "t" );
		final ColumnReference id = new ColumnReference( targetTable, "id", mock( JdbcMapping.class ) );
		final DeleteStatement deleteStatement = new DeleteStatement( targetTable, null, List.of( id ) );
		final TestingMutationTranslator translator =
				createMutationTranslator( deleteStatement, MutationSyntaxSupport.NONE );
		translator.useReturningRenderingSupport( request -> new ReturningRenderingPlan.None() );

		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> translator.visitDeleteStatement( deleteStatement ) )
				.withMessageContaining( "QUERY DELETE" );
		assertThat( translator.statementStackDepth() ).isOne();
	}

	@Test
	void insertConflictPlansOwnInsertSelectionSourceAliasingAndClauseRendering() throws NoSuchMethodException {
		assertNoDeclaredMethod( "visitInsertStatementOnly", InsertSelectStatement.class );
		assertNoDeclaredMethod( "visitConflictClause", ConflictClause.class );
		assertNoDeclaredMethod( "visitInsertSource", InsertSelectStatement.class );

		final NamedTableReference targetTable = new NamedTableReference( "example_table", "t" );
		final JdbcMapping jdbcMapping = mock( JdbcMapping.class );
		final ColumnReference id = new ColumnReference( targetTable, "id", jdbcMapping );
		final ColumnReference data = new ColumnReference( targetTable, "data", jdbcMapping );
		final ColumnReference excludedData = new ColumnReference(
				"excluded",
				"data",
				false,
				null,
				jdbcMapping
		);
		final Assignable assignable = mock( Assignable.class );
		when( assignable.getColumnReferences() ).thenReturn( List.of( data ) );
		final Assignment assignment = new Assignment( assignable, excludedData );

		final InsertSelectStatement standardStatement = insertStatement(
				targetTable,
				List.of( id, data ),
				new ConflictClause( null, List.of( "id" ), List.of( assignment ), null )
		);
		final TestingMutationTranslator standardTranslator =
				createMutationTranslator( standardStatement, MutationSyntaxSupport.NONE );
		standardTranslator.useInsertConflictRenderingSupport( StandardInsertConflictRenderingSupport.STANDARD );
		standardTranslator.useStandardSetAssignmentRendering();
		standardTranslator.visitInsertStatement( standardStatement );
		assertThat( standardTranslator.renderedSql() )
				.isEqualTo( "insert into example_table(id,data) values () on conflict(id) do update set data=excluded.data" );

		final InsertSelectStatement valuesFunctionStatement = insertStatement(
				targetTable,
				List.of( id, data ),
				new ConflictClause( null, List.of( "id" ), List.of( assignment ), null )
		);
		final TestingMutationTranslator valuesFunctionTranslator =
				createMutationTranslator( valuesFunctionStatement, MutationSyntaxSupport.NONE );
		valuesFunctionTranslator.useInsertConflictRenderingSupport(
				StandardInsertConflictRenderingSupport.ON_DUPLICATE_KEY_VALUES_FUNCTION
		);
		valuesFunctionTranslator.useStandardSetAssignmentRendering();
		valuesFunctionTranslator.visitInsertStatement( valuesFunctionStatement );
		assertThat( valuesFunctionTranslator.renderedSql() )
				.isEqualTo( "insert into example_table(id,data) values () on duplicate key update data=values(data)" );

		final InsertSelectStatement rowAliasStatement = insertStatement(
				targetTable,
				List.of( id, data ),
				new ConflictClause( null, List.of( "id" ), List.of( assignment ), null )
		);
		final TestingMutationTranslator rowAliasTranslator =
				createMutationTranslator( rowAliasStatement, MutationSyntaxSupport.NONE );
		rowAliasTranslator.useInsertConflictRenderingSupport(
				StandardInsertConflictRenderingSupport.ON_DUPLICATE_KEY_ROW_ALIAS
		);
		rowAliasTranslator.useStandardSetAssignmentRendering();
		rowAliasTranslator.visitInsertStatement( rowAliasStatement );
		assertThat( rowAliasTranslator.renderedSql() )
				.isEqualTo(
						"insert into example_table(id,data) values () as excluded(id,data)"
								+ " on duplicate key update data=excluded.data"
				);
	}

	@Test
	void invalidConflictPlansCannotSilentlyDiscardOrMisrepresentTheRequest() {
		final NamedTableReference targetTable = new NamedTableReference( "example_table", "t" );
		final InsertSelectStatement statement = insertStatement(
				targetTable,
				List.of( new ColumnReference( targetTable, "id", mock( JdbcMapping.class ) ) ),
				new ConflictClause( null, List.of(), List.of(), null )
		);
		final TestingMutationTranslator translator =
				createMutationTranslator( statement, MutationSyntaxSupport.NONE );
		translator.useInsertConflictRenderingSupport( request -> new InsertConflictRenderingPlan.None() );

		assertThatExceptionOfType( RuntimeException.class )
				.isThrownBy( () -> translator.visitInsertStatement( statement ) )
				.withMessageContaining( "cannot discard" );
	}

	@Test
	void insertValuesListDispatchIsFinalAndUsesTheDialectProfile() throws NoSuchMethodException {
		assertFinalMethod( "visitValuesList", List.class );
		final NamedTableReference targetTable = new NamedTableReference( "example_table", "t" );
		final InsertSelectStatement statement = insertStatement(
				targetTable,
				List.of( new ColumnReference( targetTable, "id", mock( JdbcMapping.class ) ) ),
				null
		);
		statement.setValuesList( List.of( new Values( List.of() ), new Values( List.of() ) ) );

		final TestingMutationTranslator nativeTranslator = createMutationTranslator(
				statement,
				MutationSyntaxSupport.NONE,
				ValuesListSupport.INSERT_ONLY
		);
		nativeTranslator.visitInsertStatement( statement );
		assertThat( nativeTranslator.renderedSql() )
				.isEqualTo( "insert into example_table(id) values (), ()" );

		final TestingMutationTranslator emulatingTranslator = createMutationTranslator(
				statement,
				MutationSyntaxSupport.NONE,
				ValuesListSupport.NONE
		);
		emulatingTranslator.visitInsertStatement( statement );
		assertThat( emulatingTranslator.renderedSql() )
				.isEqualTo( "insert into example_table(id) select null union all select null" );
	}

	private static InsertSelectStatement insertStatement(
			NamedTableReference targetTable,
			List<ColumnReference> targetColumns,
			ConflictClause conflictClause) {
		final InsertSelectStatement statement = new InsertSelectStatement( targetTable, List.of() );
		statement.addTargetColumnReferences( targetColumns );
		statement.setValuesList( List.of( new Values( List.of() ) ) );
		statement.setConflictClause( conflictClause );
		return statement;
	}

	@Test
	void mutationClauseTemplatesOwnAndRestoreTheClauseStack() {
		final NamedTableReference targetTable = new NamedTableReference( "example_table", "t" );

		final DeleteStatement deleteStatement = new DeleteStatement( targetTable, null );
		final TestingMutationTranslator deleteTranslator =
				createMutationTranslator( deleteStatement, MutationSyntaxSupport.NONE );
		deleteTranslator.failWhileRenderingDeleteClause();
		assertThatExceptionOfType( IllegalStateException.class )
				.isThrownBy( () -> deleteTranslator.visitDeleteStatement( deleteStatement ) );
		assertThat( deleteTranslator.deleteClauseWhileRendering() ).isEqualTo( Clause.DELETE );
		assertThat( deleteTranslator.clauseStackDepth() ).isZero();

		final UpdateStatement updateStatement = new UpdateStatement(
				targetTable,
				List.of( mock( Assignment.class ) ),
				null
		);
		final TestingMutationTranslator updateTranslator =
				createMutationTranslator( updateStatement, MutationSyntaxSupport.NONE );
		updateTranslator.failWhileRenderingUpdateClause();
		assertThatExceptionOfType( IllegalStateException.class )
				.isThrownBy( () -> updateTranslator.visitUpdateStatement( updateStatement ) );
		assertThat( updateTranslator.updateClauseWhileRendering() ).isEqualTo( Clause.UPDATE );
		assertThat( updateTranslator.clauseStackDepth() ).isZero();
	}

	@Test
	void setAssignmentTemplateOwnsBookkeepingAndRestoresTheClauseStack() {
		final NamedTableReference targetTable = new NamedTableReference( "example_table", "t" );
		final AffectedAssignable assignable = mock( AffectedAssignable.class );
		when( assignable.getAffectedTableName() ).thenReturn( "affected_table" );
		final Assignment assignment = mock( Assignment.class );
		when( assignment.getAssignable() ).thenReturn( assignable );
		final UpdateStatement updateStatement = new UpdateStatement(
				targetTable,
				List.of( assignment ),
				null
		);
		final TestingMutationTranslator translator =
				createMutationTranslator( updateStatement, MutationSyntaxSupport.NONE );
		translator.failWhileRenderingSetAssignment();

		assertThatExceptionOfType( IllegalStateException.class )
				.isThrownBy( () -> translator.visitUpdateStatement( updateStatement ) );
		assertThat( translator.setClauseWhileRenderingAssignment() ).isEqualTo( Clause.SET );
		assertThat( translator.getAffectedTableNames() ).contains( "affected_table" );
		assertThat( translator.clauseStackDepth() ).isZero();
	}

	@Test
	void targetAliasedScalarPlanOwnsTheUpdateTargetAndRestoresAssignmentContext() {
		final NamedTableReference targetTable = new NamedTableReference( "example_table", "t" );
		final Assignment assignment = mock( Assignment.class );
		final FromClause fromClause = new FromClause( 2 );
		fromClause.addRoot( mock( TableGroup.class ) );
		fromClause.addRoot( mock( TableGroup.class ) );
		final UpdateStatement updateStatement = new UpdateStatement(
				targetTable,
				fromClause,
				List.of( assignment ),
				null
		);
		final TestingMutationTranslator translator =
				createMutationTranslator( updateStatement, MutationSyntaxSupport.NONE );
		translator.useQueryMutationRenderingSupport(
				StandardQueryMutationRenderingSupport.withTargetAliasedScalarSubquery( "dml_target_" )
		);
		translator.failWhileRenderingSetAssignment();

		assertThatExceptionOfType( IllegalStateException.class )
				.isThrownBy( () -> translator.visitUpdateStatement( updateStatement ) );
		assertThat( translator.renderedSql() ).isEqualTo( "update example_table dml_target_ set " );
		assertThat( translator.setClauseWhileRenderingAssignment() ).isEqualTo( Clause.SET );
		assertThat( translator.clauseStackDepth() ).isZero();
		assertThatExceptionOfType( IllegalStateException.class )
				.isThrownBy( () -> translator.renderCorrelatedAssignment( assignment, updateStatement ) )
				.withMessageContaining( "target-aliased scalar-subquery plan" );
	}

	private static Expression renderingExpression(TestingTranslator translator, String sql) {
		final Expression expression = mock( Expression.class );
		doAnswer( invocation -> {
			translator.appendSql( sql );
			return null;
		} ).when( expression ).accept( translator );
		return expression;
	}

	private static FunctionTableReference functionTableReference(
			TestingTranslator translator,
			String sql,
			boolean rendersIdentifierVariable) {
		final FunctionExpression functionExpression = mock( FunctionExpression.class );
		doAnswer( invocation -> {
			translator.appendSql( sql );
			return null;
		} ).when( functionExpression ).accept( translator );
		return new FunctionTableReference(
				functionExpression,
				"f",
				List.of( "value", "ordinality" ),
				false,
				rendersIdentifierVariable,
				Set.of(),
				mock( SessionFactoryImplementor.class )
		);
	}

	private static void assertFinalMethod(String name, Class<?> parameterType) throws NoSuchMethodException {
		assertThat( Modifier.isFinal(
				AbstractSqlAstTranslator.class.getDeclaredMethod( name, parameterType ).getModifiers()
		) ).isTrue();
	}

	private static void assertFinalMethod(String name) throws NoSuchMethodException {
		assertThat( Modifier.isFinal( AbstractSqlAstTranslator.class.getDeclaredMethod( name ).getModifiers() ) )
				.isTrue();
	}

	private static void assertPrivateMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
		assertThat( Modifier.isPrivate(
				AbstractSqlAstTranslator.class.getDeclaredMethod( name, parameterTypes ).getModifiers()
		) ).isTrue();
	}

	private static void assertNoDeclaredMethod(String name, Class<?>... parameterTypes) {
		assertThatExceptionOfType( NoSuchMethodException.class )
				.isThrownBy( () -> AbstractSqlAstTranslator.class.getDeclaredMethod( name, parameterTypes ) );
	}

	private record TestingTableJoinRenderingRequest(
			TableJoinKind kind,
			SqlAstJoinType joinType,
			boolean lateral,
			boolean recursiveQueryPart,
			boolean predicatePresent) implements TableJoinRenderingRequest {
	}

	private record TestingSetReturningFunctionRenderingRequest(
			boolean ordinalityRequested,
			String ordinalityColumnName) implements SetReturningFunctionRenderingRequest {
	}

	private record TestingTableLockHintRequest(
			PessimisticLockKind lockKind,
			Timeout timeout,
			String tableExpression) implements TableLockHintRequest {
	}

	private record TestingPrimaryTableReferenceContext(
			PrimaryTableReferenceKind kind,
			boolean subqueryLike,
			boolean beginsNestedJoinGroup) implements PrimaryTableReferenceContext {
	}

	private static LockingSupport lockingSupport(TableLockHintRenderer renderer) {
		final LockingSupport support = mock( LockingSupport.class );
		when( support.getTableLockHintRenderer() ).thenReturn( renderer );
		return support;
	}

	private static String renderTableLockHint(
			LockingSupport support,
			PessimisticLockKind lockKind,
			Timeout timeout) {
		return support.getTableLockHintRenderer().render(
				new TestingTableLockHintRequest( lockKind, timeout, "example_table" )
		);
	}

	private static void renderNamedTableReference(
			TestingTranslator translator,
			NamedTableReference tableReference,
			LockMode lockMode) throws Exception {
		final Method method = AbstractSqlAstTranslator.class.getDeclaredMethod(
				"renderNamedTableReference",
				NamedTableReference.class,
				LockMode.class
		);
		method.setAccessible( true );
		method.invoke( translator, tableReference, lockMode );
	}

	private static void renderPrimaryTableReference(
			TestingTranslator translator,
			TableGroup tableGroup,
			LockMode lockMode,
			boolean beginsNestedJoinGroup) throws Exception {
		final Method method = AbstractSqlAstTranslator.class.getDeclaredMethod(
				"renderPrimaryTableReference",
				TableGroup.class,
				LockMode.class,
				boolean.class
		);
		method.setAccessible( true );
		method.invoke( translator, tableGroup, lockMode, beginsNestedJoinGroup );
	}

	private static TestingTranslator createTranslator() {
		return createTranslator( LockingSupportSimple.STANDARD_SUPPORT );
	}

	private static TestingTranslator createTranslator(QuerySpec querySpec) {
		return createTranslator( LockingSupportSimple.STANDARD_SUPPORT, querySpec );
	}

	private static TestingTranslator createTranslator(LockingSupport lockingSupport) {
		return createTranslator( lockingSupport, new QuerySpec( true ) );
	}

	private static TestingTranslator createTranslator(LockingSupport lockingSupport, QuerySpec querySpec) {
		final SessionFactoryImplementor sessionFactory = mock( SessionFactoryImplementor.class );
		final JdbcServices jdbcServices = mock( JdbcServices.class );
		when( sessionFactory.getJdbcServices() ).thenReturn( jdbcServices );
		final Dialect dialect = mock( Dialect.class );
		when( dialect.getCteSupport() ).thenReturn( CteSupport.STANDARD );
		when( dialect.getMutationSyntaxSupport() ).thenReturn( MutationSyntaxSupport.NONE );
		when( dialect.getSetOperationSupport() ).thenReturn( SetOperationSupport.STANDARD );
		when( dialect.getSingleRowTableSupport() ).thenReturn( SingleRowTableSupport.STANDARD );
		when( dialect.getSubquerySupport() ).thenReturn(
				SubquerySupport.builder().feature( SubquerySupport.Feature.LATERAL, true ).build()
		);
		when( dialect.getLockingSupport() ).thenReturn( lockingSupport );
		when( jdbcServices.getDialect() ).thenReturn( dialect );
		return new TestingTranslator(
				new SqlAstTranslationRequest.Select(
						sessionFactory,
						new SelectStatement( querySpec )
				)
		);
	}

	private static TestingH2Translator createH2Translator() {
		final SessionFactoryImplementor sessionFactory = mock( SessionFactoryImplementor.class );
		final JdbcServices jdbcServices = mock( JdbcServices.class );
		when( sessionFactory.getJdbcServices() ).thenReturn( jdbcServices );
		final Dialect dialect = mock( Dialect.class );
		when( dialect.getCteSupport() ).thenReturn( CteSupport.STANDARD );
		when( dialect.getMutationSyntaxSupport() ).thenReturn( MutationSyntaxSupport.NONE );
		when( dialect.getSetOperationSupport() ).thenReturn( SetOperationSupport.STANDARD );
		when( dialect.getSingleRowTableSupport() ).thenReturn( SingleRowTableSupport.STANDARD );
		when( dialect.getSubquerySupport() ).thenReturn( SubquerySupport.STANDARD );
		when( dialect.getLockingSupport() ).thenReturn( LockingSupportSimple.STANDARD_SUPPORT );
		when( jdbcServices.getDialect() ).thenReturn( dialect );
		return new TestingH2Translator( new SqlAstTranslationRequest.Select(
				sessionFactory,
				new SelectStatement( new QuerySpec( true ) )
		) );
	}

	private static TestingMutationTranslator createMutationTranslator(
			MutationStatement statement,
			MutationSyntaxSupport mutationSyntaxSupport) {
		return createMutationTranslator( statement, mutationSyntaxSupport, ValuesListSupport.INSERT_ONLY );
	}

	private static TestingMutationTranslator createMutationTranslator(
			MutationStatement statement,
			MutationSyntaxSupport mutationSyntaxSupport,
			ValuesListSupport valuesListSupport) {
		final SessionFactoryImplementor sessionFactory = mock( SessionFactoryImplementor.class );
		final JdbcServices jdbcServices = mock( JdbcServices.class );
		when( sessionFactory.getJdbcServices() ).thenReturn( jdbcServices );
		final Dialect dialect = mock( Dialect.class );
		when( dialect.getCteSupport() ).thenReturn( CteSupport.STANDARD );
		when( dialect.getMutationSyntaxSupport() ).thenReturn( mutationSyntaxSupport );
		when( dialect.getSetOperationSupport() ).thenReturn( SetOperationSupport.STANDARD );
		// Preserve the former unstubbed select-only fragment used by the separate
		// no-column-insert surface fixture; that rendering is outside this profile.
		when( dialect.getSingleRowTableSupport() ).thenReturn(
				SingleRowTableSupport.builder().selectOnlyFromClause( "null" ).build()
		);
		when( dialect.getSubquerySupport() ).thenReturn( SubquerySupport.STANDARD );
		when( dialect.getValuesListSupport() ).thenReturn( valuesListSupport );
		when( dialect.getLockingSupport() ).thenReturn( LockingSupportSimple.STANDARD_SUPPORT );
		when( jdbcServices.getDialect() ).thenReturn( dialect );
		return new TestingMutationTranslator(
				new SqlAstTranslationRequest.QueryMutation( sessionFactory, statement )
		);
	}

	private static TestingPostgreSQLFamilyTranslator createPostgreSQLFamilyTranslator(
			MutationStatement statement) {
		final SessionFactoryImplementor sessionFactory = mock( SessionFactoryImplementor.class );
		final JdbcServices jdbcServices = mock( JdbcServices.class );
		when( sessionFactory.getJdbcServices() ).thenReturn( jdbcServices );
		final Dialect dialect = mock( Dialect.class );
		when( dialect.getCteSupport() ).thenReturn( CteSupport.STANDARD );
		when( dialect.getMutationSyntaxSupport() ).thenReturn( MutationSyntaxSupport.NONE );
		when( dialect.getSetOperationSupport() ).thenReturn( SetOperationSupport.STANDARD );
		when( dialect.getSingleRowTableSupport() ).thenReturn( SingleRowTableSupport.STANDARD );
		when( dialect.getSubquerySupport() ).thenReturn( SubquerySupport.STANDARD );
		when( dialect.getLockingSupport() ).thenReturn( LockingSupportSimple.STANDARD_SUPPORT );
		when( jdbcServices.getDialect() ).thenReturn( dialect );
		return new TestingPostgreSQLFamilyTranslator(
				new SqlAstTranslationRequest.QueryMutation( sessionFactory, statement )
		);
	}

	private static class TestingH2Translator extends H2SqlAstTranslator<JdbcSelect> {
		private TestingH2Translator(SqlAstTranslationRequest.Select request) {
			super( request );
		}

		private void renderPrimaryPrefix(PrimaryTableReferenceContext context) {
			renderPrimaryTableReferencePrefix( context );
		}

		private String renderedSql() {
			return getSql();
		}
	}

	private static class TestingPostgreSQLFamilyTranslator
			extends PostgreSQLFamilySqlAstTranslator<JdbcOperationQueryMutation> {
		private TestingPostgreSQLFamilyTranslator(SqlAstTranslationRequest.QueryMutation request) {
			super( request );
		}

		private void renderAlias(TableReference tableReference) {
			renderTableReferenceIdentificationVariable( tableReference );
		}

		private String renderedSql() {
			return getSql();
		}
	}

	private static class TestingMutationTranslator extends StandardSqlAstTranslator<JdbcOperationQueryMutation> {
		private boolean failWhileRenderingDeleteClause;
		private boolean failWhileRenderingUpdateClause;
		private boolean failWhileRenderingSetAssignment;
		private Clause deleteClauseWhileRendering;
		private Clause updateClauseWhileRendering;
		private Clause setClauseWhileRenderingAssignment;
		private QueryMutationRenderingSupport queryMutationRenderingSupport;
		private ReturningRenderingSupport returningRenderingSupport;
		private InsertConflictRenderingSupport insertConflictRenderingSupport;
		private boolean useStandardSetAssignmentRendering;
		private TableReferenceAliasContext tableReferenceAliasContext;

		private TestingMutationTranslator(SqlAstTranslationRequest.QueryMutation request) {
			super( request );
		}

		private String renderedSql() {
			return getSql();
		}

		private void renderAlias(TableReference tableReference) {
			renderTableReferenceIdentificationVariable( tableReference );
		}

		private TableReferenceAliasContext tableReferenceAliasContext() {
			return tableReferenceAliasContext;
		}

		private void failWhileRenderingDeleteClause() {
			failWhileRenderingDeleteClause = true;
		}

		private void failWhileRenderingUpdateClause() {
			failWhileRenderingUpdateClause = true;
		}

		private void failWhileRenderingSetAssignment() {
			failWhileRenderingSetAssignment = true;
		}

		private void useQueryMutationRenderingSupport(QueryMutationRenderingSupport support) {
			queryMutationRenderingSupport = support;
		}

		private void useReturningRenderingSupport(ReturningRenderingSupport support) {
			returningRenderingSupport = support;
		}

		private void useInsertConflictRenderingSupport(InsertConflictRenderingSupport support) {
			insertConflictRenderingSupport = support;
		}

		private void useStandardSetAssignmentRendering() {
			useStandardSetAssignmentRendering = true;
		}

		private int statementStackDepth() {
			return getStatementStack().depth();
		}

		private Clause deleteClauseWhileRendering() {
			return deleteClauseWhileRendering;
		}

		private Clause updateClauseWhileRendering() {
			return updateClauseWhileRendering;
		}

		private Clause setClauseWhileRenderingAssignment() {
			return setClauseWhileRenderingAssignment;
		}

		private int clauseStackDepth() {
			return getClauseStack().depth();
		}

		private void renderCorrelatedAssignment(Assignment assignment, UpdateStatement statement) {
			renderSetAssignmentEmulateJoin( assignment, statement );
		}

		@Override
		protected QueryMutationRenderingSupport getQueryMutationRenderingSupport() {
			return queryMutationRenderingSupport == null
					? super.getQueryMutationRenderingSupport()
					: queryMutationRenderingSupport;
		}

		@Override
		protected ReturningRenderingSupport getReturningRenderingSupport() {
			return returningRenderingSupport == null
					? super.getReturningRenderingSupport()
					: returningRenderingSupport;
		}

		@Override
		protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
			return insertConflictRenderingSupport == null
					? super.getInsertConflictRenderingSupport()
					: insertConflictRenderingSupport;
		}

		@Override
		protected void renderTableReferenceAlias(String alias, TableReferenceAliasContext context) {
			tableReferenceAliasContext = context;
			super.renderTableReferenceAlias( alias, context );
		}

		@Override
		protected void renderDeleteClause(DeleteStatement statement) {
			deleteClauseWhileRendering = getClauseStack().getCurrent();
			if ( failWhileRenderingDeleteClause ) {
				throw new IllegalStateException( "Expected delete-clause failure" );
			}
			super.renderDeleteClause( statement );
		}

		@Override
		protected void renderUpdateClause(UpdateStatement statement) {
			updateClauseWhileRendering = getClauseStack().getCurrent();
			if ( failWhileRenderingUpdateClause ) {
				throw new IllegalStateException( "Expected update-clause failure" );
			}
			super.renderUpdateClause( statement );
		}

		@Override
		protected void renderSetAssignment(Assignment assignment) {
			if ( useStandardSetAssignmentRendering ) {
				super.renderSetAssignment( assignment );
				return;
			}
			setClauseWhileRenderingAssignment = getClauseStack().getCurrent();
			if ( failWhileRenderingSetAssignment ) {
				throw new IllegalStateException( "Expected set-assignment failure" );
			}
			appendSql( "value=1" );
		}
	}

	private interface AffectedAssignable extends Assignable, PathInterpretation<Object> {
	}

	private static class TestingTranslator extends StandardSqlAstTranslator<JdbcSelect> {
		private boolean supplyPriorRenderingMode;
		private boolean failWhileRenderingSelect;
		private boolean failWhileRenderingDerivedTable;
		private int selectStatementStackDepth;
		private SqlAstNodeRenderingMode selectRenderingMode;
		private List<SortSpecification> omittedGroupSortSpecifications;
		private boolean groupOrderByVisited;
		private int beforeQueryGroupCount;
		private int afterQueryGroupCount;
		private boolean failWhileRenderingSelectClause;
		private int selectClauseStackDepth;
		private boolean failWhileRenderingPartitionItem;
		private boolean failWhileRenderingFromClause;
		private int partitionItemClauseStackDepth;
		private Clause fromClauseWhileRenderingRoot;
		private SqlAstNodeRenderingMode partitionItemRenderingMode;
		private int havingClauseStackDepth;
		private int emptyOrderByCount;
		private int sortItemCount;
		private PaginationRenderingSupport paginationRenderingSupport;
		private QueryPart windowQueryPart;
		private boolean emulateWindowFetchClause;
		private int cteSelectHintCount;
		private boolean lateralDerivedTableWhileRendering;
		private boolean derivedTableKindWhileRendering;
		private boolean inlineCteKindWhileRendering;
		private SqlAstNodeRenderingMode derivedTableRenderingMode;
		private DerivedTableRenderingSupport derivedTableRenderingSupport;
		private SetReturningFunctionRenderingSupport setReturningFunctionRenderingSupport;
		private TableJoinRenderingSupport tableJoinRenderingSupport;
		private TableReferenceAliasContext tableReferenceAliasContext;
		private PrimaryTableReferenceContext primaryTableReferenceContext;

		private TestingTranslator(SqlAstTranslationRequest.Select request) {
			super( request );
		}

		private List<JdbcParameterBinder> parameterBinders() {
			return getParameterBinders();
		}

		private void renderPrefix(Consumer<SqlAppender> renderer) {
			renderStatementPrefix( renderer );
		}

		private String renderedSql() {
			return getSql();
		}

		private void renderAlias(TableReference tableReference) {
			renderTableReferenceIdentificationVariable( tableReference );
		}

		private TableReferenceAliasContext tableReferenceAliasContext() {
			return tableReferenceAliasContext;
		}

		private PrimaryTableReferenceContext primaryTableReferenceContext() {
			return primaryTableReferenceContext;
		}

		private void failWhileRenderingSelectWithPriorMode() {
			supplyPriorRenderingMode = true;
			failWhileRenderingSelect = true;
		}

		private void failWhileRenderingDerivedTable() {
			failWhileRenderingDerivedTable = true;
		}

		private void useDerivedTableRenderingSupport(DerivedTableRenderingSupport support) {
			derivedTableRenderingSupport = support;
		}

		private void useSetReturningFunctionRenderingSupport(SetReturningFunctionRenderingSupport support) {
			setReturningFunctionRenderingSupport = support;
		}

		private void useTableJoinRenderingSupport(TableJoinRenderingSupport support) {
			tableJoinRenderingSupport = support;
		}

		private void renderRootTableGroup(TableGroup tableGroup) {
			try {
				final Method method = AbstractSqlAstTranslator.class.getDeclaredMethod(
						"renderRootTableGroup",
						TableGroup.class,
						List.class
				);
				method.setAccessible( true );
				method.invoke( this, tableGroup, null );
			}
			catch (InvocationTargetException e) {
				if ( e.getCause() instanceof RuntimeException runtimeException ) {
					throw runtimeException;
				}
				throw new IllegalStateException( e.getCause() );
			}
			catch (ReflectiveOperationException e) {
				throw new IllegalStateException( e );
			}
		}

		private void renderWhere() {
			visitWhereClause( null );
		}

		private boolean lateralDerivedTableWhileRendering() {
			return lateralDerivedTableWhileRendering;
		}

		private boolean derivedTableKindWhileRendering() {
			return derivedTableKindWhileRendering;
		}

		private boolean inlineCteKindWhileRendering() {
			return inlineCteKindWhileRendering;
		}

		private SqlAstNodeRenderingMode derivedTableRenderingMode() {
			return derivedTableRenderingMode;
		}

		private boolean isInLateralDerivedTable() {
			return isRenderingLateralDerivedTable();
		}

		private boolean isInQueryPartDerivedTable() {
			return isRenderingDerivedTable( DerivedTableKind.QUERY_PART );
		}

		private boolean isInInlineCteDerivedTable() {
			return isRenderingDerivedTable( DerivedTableKind.INLINE_CTE );
		}

		private void inlineCteTableGroup(TableGroup tableGroup) {
			try {
				final Method method = AbstractSqlAstTranslator.class.getDeclaredMethod(
						"inlineCteTableGroup",
						TableGroup.class,
						LockMode.class,
						boolean.class
				);
				method.setAccessible( true );
				method.invoke( this, tableGroup, LockMode.NONE, false );
			}
			catch (InvocationTargetException e) {
				if ( e.getCause() instanceof RuntimeException runtimeException ) {
					throw runtimeException;
				}
				throw new IllegalStateException( e.getCause() );
			}
			catch (ReflectiveOperationException e) {
				throw new IllegalStateException( e );
			}
		}

		private void useLimit(Limit limit) {
			setLimitField( limit );
		}

		private Limit currentLimit() {
			return (Limit) getLimitField();
		}

		private void setLimitField(Limit limit) {
			try {
				final Field field = AbstractSqlAstTranslator.class.getDeclaredField( "limit" );
				field.setAccessible( true );
				field.set( this, limit );
			}
			catch (ReflectiveOperationException e) {
				throw new IllegalStateException( e );
			}
		}

		private Object getLimitField() {
			try {
				final Field field = AbstractSqlAstTranslator.class.getDeclaredField( "limit" );
				field.setAccessible( true );
				return field.get( this );
			}
			catch (ReflectiveOperationException e) {
				throw new IllegalStateException( e );
			}
		}

		private int selectStatementStackDepth() {
			return selectStatementStackDepth;
		}

		private SqlAstNodeRenderingMode selectRenderingMode() {
			return selectRenderingMode;
		}

		private int statementStackDepth() {
			return getStatementStack().depth();
		}

		private SqlAstNodeRenderingMode restoredRenderingMode() {
			return super.getParameterRenderingMode();
		}

		private void renderQueryGroupBody(QueryGroup queryGroup) {
			omittedGroupSortSpecifications = queryGroup.getSortSpecifications();
			renderQueryGroupWithoutOrderByAndOffsetFetch( queryGroup );
		}

		private boolean groupOrderByVisited() {
			return groupOrderByVisited;
		}

		private int beforeQueryGroupCount() {
			return beforeQueryGroupCount;
		}

		private int afterQueryGroupCount() {
			return afterQueryGroupCount;
		}

		private int queryPartStackDepth() {
			return getQueryPartStack().depth();
		}

		private void failWhileRenderingSelectClause() {
			failWhileRenderingSelectClause = true;
		}

		private int selectClauseStackDepth() {
			return selectClauseStackDepth;
		}

		private int clauseStackDepth() {
			return getClauseStack().depth();
		}

		private void failWhileRenderingPartitionItem() {
			failWhileRenderingPartitionItem = true;
		}

		private void failWhileRenderingFromClause() {
			failWhileRenderingFromClause = true;
		}

		private Clause fromClauseWhileRenderingRoot() {
			return fromClauseWhileRenderingRoot;
		}

		private void renderGroupBy(QuerySpec querySpec) {
			visitGroupByClause( querySpec, SelectItemReferenceStrategy.POSITION );
		}

		private void renderAliasedPartitionItem(Expression original, Expression resolved) {
			visitPartitionExpressions( List.of( original ), ignored -> resolved, true );
		}

		private int partitionItemClauseStackDepth() {
			return partitionItemClauseStackDepth;
		}

		private SqlAstNodeRenderingMode partitionItemRenderingMode() {
			return partitionItemRenderingMode;
		}

		@Override
		protected void renderPrimaryTableReferencePrefix(PrimaryTableReferenceContext context) {
			primaryTableReferenceContext = context;
			fromClauseWhileRenderingRoot = getClauseStack().getCurrent();
			if ( failWhileRenderingFromClause ) {
				throw new IllegalStateException( "Expected from-clause failure" );
			}
		}

		private void renderHaving(QuerySpec querySpec) {
			visitHavingClause( querySpec );
		}

		private void recordHavingClauseStackDepth() {
			havingClauseStackDepth = getClauseStack().depth();
		}

		private int havingClauseStackDepth() {
			return havingClauseStackDepth;
		}

		private void renderEmptyWindowOrderBy() {
			getClauseStack().push( Clause.OVER );
			try {
				renderOrderByClause( List.of() );
			}
			finally {
				getClauseStack().pop();
			}
		}

		private int emptyOrderByCount() {
			return emptyOrderByCount;
		}

		private int sortItemCount() {
			return sortItemCount;
		}

		private void usePaginationSupport(PaginationRenderingSupport paginationRenderingSupport) {
			this.paginationRenderingSupport = paginationRenderingSupport;
		}

		private void renderTrailingPagination(QueryPart queryPart) {
			visitOffsetFetchClause( queryPart );
		}

		private void renderSelectClause(QuerySpec querySpec) {
			getQueryPartStack().push( querySpec );
			try {
				visitSelectClause( querySpec.getSelectClause() );
			}
			finally {
				getQueryPartStack().pop();
			}
		}

		private QueryPart windowQueryPart() {
			return windowQueryPart;
		}

		private boolean emulateWindowFetchClause() {
			return emulateWindowFetchClause;
		}

		private int cteSelectHintCount() {
			return cteSelectHintCount;
		}

		@Override
		protected PaginationRenderingSupport getPaginationRenderingSupport() {
			return paginationRenderingSupport == null
					? super.getPaginationRenderingSupport()
					: paginationRenderingSupport;
		}

		@Override
		protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
			return derivedTableRenderingSupport == null
					? super.getDerivedTableRenderingSupport()
					: derivedTableRenderingSupport;
		}

		@Override
		protected SetReturningFunctionRenderingSupport getSetReturningFunctionRenderingSupport() {
			return setReturningFunctionRenderingSupport == null
					? super.getSetReturningFunctionRenderingSupport()
					: setReturningFunctionRenderingSupport;
		}

		@Override
		protected TableJoinRenderingSupport getTableJoinRenderingSupport() {
			return tableJoinRenderingSupport == null
					? super.getTableJoinRenderingSupport()
					: tableJoinRenderingSupport;
		}

		@Override
		protected void renderTableReferenceAlias(String alias, TableReferenceAliasContext context) {
			tableReferenceAliasContext = context;
			super.renderTableReferenceAlias( alias, context );
		}

		@Override
		protected void emulateFetchOffsetWithWindowFunctions(QueryPart queryPart, boolean emulateFetchClause) {
			windowQueryPart = queryPart;
			emulateWindowFetchClause = emulateFetchClause;
		}

		@Override
		protected SqlAstNodeRenderingMode getParameterRenderingMode() {
			if ( supplyPriorRenderingMode ) {
				supplyPriorRenderingMode = false;
				return SqlAstNodeRenderingMode.INLINE_ALL_PARAMETERS;
			}
			return super.getParameterRenderingMode();
		}

		@Override
		protected void renderSelectStatement(SelectStatement statement) {
			if ( failWhileRenderingDerivedTable ) {
				lateralDerivedTableWhileRendering = isRenderingLateralDerivedTable();
				derivedTableKindWhileRendering = isRenderingDerivedTable( DerivedTableKind.QUERY_PART );
				inlineCteKindWhileRendering = isRenderingDerivedTable( DerivedTableKind.INLINE_CTE );
				derivedTableRenderingMode = super.getParameterRenderingMode();
				throw new IllegalStateException( "Expected derived-table failure" );
			}
			if ( failWhileRenderingSelect ) {
				selectStatementStackDepth = getStatementStack().depth();
				selectRenderingMode = super.getParameterRenderingMode();
				throw new IllegalStateException( "Expected test failure" );
			}
			super.renderSelectStatement( statement );
		}

		@Override
		protected void renderSelectClause(SelectClause selectClause) {
			if ( failWhileRenderingSelectClause ) {
				selectClauseStackDepth = getClauseStack().depth();
				throw new IllegalStateException( "Expected test failure" );
			}
			super.renderSelectClause( selectClause );
		}

		@Override
		protected void renderCteSelectHint(CteStatement cte) {
			cteSelectHintCount++;
			appendSql( "/*cte*/ " );
		}

		@Override
		protected void renderPartitionItem(Expression expression) {
			if ( failWhileRenderingPartitionItem ) {
				partitionItemClauseStackDepth = getClauseStack().depth();
				partitionItemRenderingMode = super.getParameterRenderingMode();
				throw new IllegalStateException( "Expected test failure" );
			}
			super.renderPartitionItem( expression );
		}

		@Override
		protected void renderEmptyOrderBy() {
			emptyOrderByCount++;
			appendSql( "order by (select 0)" );
		}

		@Override
		protected void visitSortSpecification(
				Expression sortExpression,
				SortDirection sortOrder,
				Nulls nullPrecedence,
				boolean ignoreCase) {
			sortItemCount++;
		}

		@Override
		protected void visitOrderBy(List<SortSpecification> sortSpecifications) {
			if ( sortSpecifications == omittedGroupSortSpecifications ) {
				groupOrderByVisited = true;
			}
			super.visitOrderBy( sortSpecifications );
		}

		@Override
		protected void beforeQueryGroup(QueryGroup queryGroup, QueryPart currentQueryPart) {
			beforeQueryGroupCount++;
		}

		@Override
		protected void afterQueryGroup(QueryGroup queryGroup, QueryPart currentQueryPart) {
			afterQueryGroupCount++;
		}
	}
}
