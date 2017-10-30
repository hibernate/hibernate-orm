/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.AttributeNodeContainer;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.EntityGraphQueryHint;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.consume.spi.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.ast.produce.internal.SqlAstSelectDescriptorImpl;
import org.hibernate.sql.ast.produce.spi.SqlAstBuildingContext;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.sqm.internal.FetchGraphBuilder;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.SelectStatement;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.QueryResultProducer;
import org.hibernate.sql.results.spi.SqlSelection;

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

	private final Stack<Shallowness> shallownessStack = new Stack<>( Shallowness.NONE );
	private final Stack<NavigableReference> navigableReferenceStack = new Stack<>();
	private final Stack<Expression> currentSelectedExpression = new Stack<>();

	private final Map<Expression,SqlSelection> sqlSelectionByExpressionMap = new HashMap<>();

	private final List<QueryResult> queryResults = new ArrayList<>();

	private int counter;

	public String generateSqlAstNodeUid() {
		return "<uid(fetchgraph):" + counter++ + ">";
	}

	public SqmSelectToSqlAstConverter(
			QueryOptions queryOptions,
			SqlAstBuildingContext sqlAstBuildingContext) {
		super( sqlAstBuildingContext, queryOptions );
		this.fetchDepthLimit = sqlAstBuildingContext.getSessionFactory().getSessionFactoryOptions().getMaximumFetchDepth();
		this.entityGraphQueryHintType = queryOptions.getEntityGraphQueryHint() == null
				? EntityGraphQueryHint.Type.NONE
				:  queryOptions.getEntityGraphQueryHint().getType();
	}

	public SqlAstSelectDescriptor interpret(SqmSelectStatement statement) {
		return new SqlAstSelectDescriptorImpl(
				visitSelectStatement( statement ),
				queryResults,
				affectedTableNames()
		);
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return getSqlAstBuildingContext().getSessionFactory();
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
	public Void visitSelection(SqmSelection sqmSelection) {
		final QueryResultProducer resultProducer = (QueryResultProducer) sqmSelection.getSelectableNode().accept( this );

		if ( getQuerySpecStack().depth() > 1 ) {
			// we only need the QueryResults if we are in the top-level select-clause.
			return null;
		}

		final QueryResult queryResult = resultProducer.createQueryResult(
				sqmSelection.getAlias(),
				this
		);

		queryResults.add( queryResult );
		applyFetches( queryResult );

		return null;
	}

	@SuppressWarnings("WeakerAccess")
	protected void applyFetches(QueryResult queryReturn) {
		if ( !FetchParent.class.isInstance( queryReturn ) ) {
			return;
		}

		applyFetches( (FetchParent) queryReturn );
	}

	@SuppressWarnings("WeakerAccess")
	protected void applyFetches(FetchParent fetchParent) {
		new FetchGraphBuilder(
				getQuerySpecStack().getCurrent(),
				this,
				getQueryOptions().getEntityGraphQueryHint()
		).process( fetchParent );
	}

	@Override
	public SqlExpressionResolver getSqlSelectionResolver() {
		return this;
	}

//	@Override
//	public SqlSelection resolveSqlSelection(Expression expression) {
//		return sqlSelectionByExpressionMap.get( expression );
//	}

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
