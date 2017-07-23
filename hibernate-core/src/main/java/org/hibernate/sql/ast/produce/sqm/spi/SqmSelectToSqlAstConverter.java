/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.AttributeNodeContainer;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.query.spi.EntityGraphQueryHint;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.consume.spi.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationArgument;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationTarget;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.ast.produce.internal.SqlSelectPlanImpl;
import org.hibernate.sql.ast.tree.spi.select.FetchParent;
import org.hibernate.sql.ast.tree.spi.select.QueryResult;
import org.hibernate.sql.ast.tree.spi.select.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlAstBuildingContext;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectInterpretation;
import org.hibernate.sql.ast.produce.sqm.internal.FetchGraphBuilder;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.SelectStatement;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiation;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.select.Selection;

import org.jboss.logging.Logger;

/**
 * Interprets an SqmSelectStatement as a SQL-AST SelectQuery.
 *
 * @author Steve Ebersole
 * @author John O'Hara
 */
@SuppressWarnings("unchecked")
public class SqmSelectToSqlAstConverter
		extends BaseSqmToSqlAstConverter
		implements QueryResultCreationContext {
	private static final Logger log = Logger.getLogger( SqmSelectToSqlAstConverter.class );

	// todo (6.0) : SqmSelectToSqlAstConverter needs to account for the EntityGraph hint
	private FetchGraphBuilder fetchGraphBuilder;

	/**
	 * Main entry point into SQM SelectStatement interpretation
	 *
	 * @param statement The SQM SelectStatement to interpret
	 * @param queryOptions The options to be applied to the interpretation
	 * @param sqlAstBuildingContext Contextual information ("parameter object") for
	 * information needed as a SQL AST is produced
	 *
	 * @return The interpretation
	 */
	public static SqlAstSelectInterpretation interpret(
			SqmSelectStatement statement,
			QueryOptions queryOptions,
			SqlAstBuildingContext sqlAstBuildingContext) {
		return new SqmSelectToSqlAstConverter( queryOptions, sqlAstBuildingContext )
				.interpret( statement );
	}

	private final Stack<Shallowness> shallownessStack = new Stack<>( Shallowness.NONE );
	private final Stack<NavigableReference> navigableReferenceStack = new Stack<>();

	private final List<QueryResult> queryResults = new ArrayList<>();

	private int counter;

	public String generateSqlAstNodeUid() {
		return "<uid(fetchgraph):" + counter++ + ">";
	}

	protected SqmSelectToSqlAstConverter(
			QueryOptions queryOptions,
			SqlAstBuildingContext sqlAstBuildingContext) {
		super( sqlAstBuildingContext, queryOptions );
		this.fetchDepthLimit = sqlAstBuildingContext.getSessionFactory().getSessionFactoryOptions().getMaximumFetchDepth();
		this.entityGraphQueryHintType = queryOptions.getEntityGraphQueryHint().getType();
	}

	private SqlAstSelectInterpretation interpret(SqmSelectStatement statement) {
		return new SqlSelectPlanImpl(
				visitSelectStatement( statement ),
				queryResults
		);
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return getSqlAstBuildingContext().getSessionFactory();
	}

	@Override
	public ColumnReferenceSource currentColumnReferenceSource() {
		return tableGroupStack.getCurrent();
	}

	@Override
	public NavigablePath currentNavigablePath() {
		return navigablePathStack.getCurrent();
	}

	@Override
	public boolean shouldCreateShallowEntityResult() {
		// todo (6.0) : we also need to vary this for ctor result based on ctor sigs + user option
		return shallownessStack.getCurrent() != Shallowness.NONE;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// walker


	@Override
	public Object visitUpdateStatement(SqmUpdateStatement statement) {
		throw new AssertionFailure( "Not expecting UpdateStatement" );
	}

	@Override
	public Object visitDeleteStatement(SqmDeleteStatement statement) {
		throw new AssertionFailure( "Not expecting DeleteStatement" );
	}

	@Override
	public Object visitInsertSelectStatement(SqmInsertSelectStatement statement) {
		throw new AssertionFailure( "Not expecting DeleteStatement" );
	}

	@Override
	public SelectStatement visitSelectStatement(SqmSelectStatement statement) {
		final QuerySpec querySpec = visitQuerySpec( statement.getQuerySpec() );

		return new SelectStatement( querySpec );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Fetches

	private final EntityGraphQueryHint.Type entityGraphQueryHintType;

	private final int fetchDepthLimit;

	private Stack<FetchParent> fetchParentStack = new Stack<>();
	private Stack<NavigablePath> navigablePathStack = new Stack<>();
	private final Stack<TableGroup> tableGroupStack = new Stack<>();
	private Stack<SqmFrom> sqmFromStack = new Stack<>();
	private Stack<AttributeNodeContainer> entityGraphNodeStack = new Stack<>();

	@Override
	public Selection visitSelection(SqmSelection sqmSelection) {
		final Selection selection = super.visitSelection( sqmSelection );

		// the call to Expression to resolve the ColumnReferenceResolver
		//		allows polymorphic input per Expression type.  E.g.
		//		a functions
		//
		// todo (6.0) : just pass TableGroupResolver into Selection#createQueryResult
		//		still allows access to TableGroup/ColumnReference resolution for
		//		Selection/Expression/Selectables that need it
		final QueryResult queryResult = selection.createQueryResult( this, this );
		queryResults.add( queryResult );
		applyFetches( queryResult );

		return selection;
	}

	protected void applyFetches(QueryResult queryReturn) {
		if ( !FetchParent.class.isInstance( queryReturn ) ) {
			return;
		}

		applyFetches( (FetchParent) queryReturn );
	}

	protected void applyFetches(FetchParent fetchParent) {
		new FetchGraphBuilder(
				getQuerySpecStack().getCurrent(),
				this,
				getQueryOptions().getEntityGraphQueryHint()
		).process( fetchParent );
	}

	@Override
	@SuppressWarnings("unchecked")
	public DynamicInstantiation visitDynamicInstantiation(SqmDynamicInstantiation dynamicInstantiation) {
		final Class target = interpret( dynamicInstantiation.getInstantiationTarget() );
		final DynamicInstantiation sqlTree = new DynamicInstantiation( target );

		for ( SqmDynamicInstantiationArgument argument : dynamicInstantiation.getArguments() ) {
			validateDynamicInstantiationArgument( target, argument );

			// generate the SqlSelections (if any) and get the SQL AST Expression
			final Expression expr = (Expression) argument.getExpression().accept( this );

			// now build the ArgumentReader and inject into the SQL AST DynamicInstantiation
			sqlTree.addArgument( argument.getAlias(), expr );
		}

		sqlTree.complete();

		return sqlTree;
	}

	@SuppressWarnings("unused")
	private void validateDynamicInstantiationArgument(Class target, SqmDynamicInstantiationArgument argument) {
		// validate use of aliases
		// todo : I think this ^^ is lready handled elsewhere
	}

	private Class interpret(SqmDynamicInstantiationTarget instantiationTarget) {
		if ( instantiationTarget.getNature() == SqmDynamicInstantiationTarget.Nature.LIST ) {
			return List.class;
		}
		if ( instantiationTarget.getNature() == SqmDynamicInstantiationTarget.Nature.MAP ) {
			return Map.class;
		}
		return instantiationTarget.getJavaType();
	}


//	@Override
//	public DomainReferenceExpression visitAttributeReferenceExpression(AttributeBinding attributeBinding) {
//		if ( attributeBinding instanceof PluralAttributeBinding ) {
//			return getCurrentDomainReferenceExpressionBuilder().buildPluralAttributeExpression(
//					this,
//					(PluralAttributeBinding) attributeBinding
//			);
//		}
//		else {
//			return getCurrentDomainReferenceExpressionBuilder().buildSingularAttributeExpression(
//					this,
//					(SingularAttributeBinding) attributeBinding
//			);
//		}
//	}
}
