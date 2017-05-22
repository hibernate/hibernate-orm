/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.internal;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.graph.spi.AttributeNodeContainer;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.query.spi.EntityGraphQueryHint;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.result.spi.Fetch;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.produce.spi.SqlAstBuildingContext;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.SingularAttributeReference;

import org.jboss.logging.Logger;

/**
 * Handles apply fetches to the "fetch graph" for an SQM-backed query.
 * Accounts for query-defined fetches as well as  EntityGraph-defined
 * fetches.
 *
 * @author Steve Ebersole
 */
public class FetchGraphBuilder {
	private static final Logger log = Logger.getLogger( FetchGraphBuilder.class );

	private final SqlAstBuildingContext sqlAstBuildingContext;
	private final SemanticQueryWalker sqmWalker;
	private final SqlSelectionResolver sqlSelectionResolver;
	private final QueryResultCreationContext queryResultCreationContext;


	public FetchGraphBuilder(
			SqlAstBuildingContext sqlAstBuildingContext,
			SemanticQueryWalker sqmWalker,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext queryResultCreationContext,
			FetchParent fetchParent,
			SqmFrom sqmFrom,
			EntityGraphQueryHint entityGraphQueryHint,
			NavigablePath navigablePath) {
		this.sqlAstBuildingContext = sqlAstBuildingContext;
		this.sqmWalker = sqmWalker;

		this.fetchDepthLimit = sqlAstBuildingContext.getSessionFactory().getSessionFactoryOptions().getMaximumFetchDepth();
		this.sqlSelectionResolver = sqlSelectionResolver;
		this.queryResultCreationContext = queryResultCreationContext;

		this.fetchParentStack.push( fetchParent );
		this.navigablePathStack.push( navigablePath );
		this.sqmFromStack.push( sqmFrom );

		this.entityGraphQueryHintType = entityGraphQueryHint.getType();

		if ( entityGraphQueryHintType != EntityGraphQueryHint.Type.NONE ) {
			this.entityGraphNodeStack.push( entityGraphQueryHint.getHintedGraph() );
		}
		else {
			if ( entityGraphQueryHint.getHintedGraph() == null ) {
				 log.debugf( "Encountered EntityGraph hint, but null EntityGraph" );
			}
		}
	}

	public void process() {
		sqmFromStack.getCurrent().accept( this );
	}

	@Override
	public Object visitQualifiedAttributeJoinFromElement(SqmAttributeJoin joinedFromElement) {
		if ( fetchParentStack.depth() + 1 > fetchDepthLimit ) {
			return null;
		}

		if ( joinedFromElement.isFetched() ) {
			makeFetch(  );
			return null;
		}


		joinedFromElement.getAttributeReference().getReferencedNavigable()

		final Fetchable navigable = (Fetchable) navigableReference.getNavigable();

		// todo (6.0) : actually for SQM this should be driven more by query-defined fetches
		//		this piece below would fit in the metamodel-walking version

		final FetchStrategy fetchStrategy = navigable.getMappedFetchStrategy();
		if ( fetchStrategy.getTiming() == FetchTiming.IMMEDIATE ) {
			if ( fetchStrategy.getStyle() == FetchStyle.JOIN ) {
				makeFetch( navigableReference );
				return;
			}
		}

		if ( entityGraphQueryHintType != EntityGraphQueryHint.Type.NONE ) {
			final AttributeNodeContainer currentGraphNode = entityGraphNodeStack.getCurrent();

			if ( currentGraphNode.containsAttribute( navigable.getNavigableName() ) ) {
				makeFetch( navigableReference );
			}
		}
		return null;
	}

	@Override
	public void visitSingularAttributeEntity(SingularPersistentAttributeEntity attribute) {
		final NavigablePath navigablePath = navigablePathStack.getCurrent().append( attribute.getNavigableName() );
		navigablePathStack.push( navigablePath );

		try {
			final SingularAttributeReference navigableReference = new SingularAttributeReference(
					fetchParentStack.getCurrent().getNavigableContainerReference(),
					attribute,
					navigablePath
			);
			visitNavigable( navigableReference );
		}
		finally {
			navigablePathStack.pop();
		}
	}



	private void visitNavigable(NavigableReference navigableReference) {
	}

	private static final FetchStrategy FETCH_NOW = new FetchStrategy( FetchTiming.IMMEDIATE, FetchStyle.JOIN );

	private void makeFetch(NavigableReference navigableReference) {
		final Fetchable navigable = (Fetchable) navigableReference.getNavigable();
		final Fetch fetch = navigable.generateFetch(
				fetchParentStack.getCurrent(),
				navigableReference,
				FETCH_NOW,
				null,
				sqlSelectionResolver,
				queryResultCreationContext
		);

		fetchParentStack.getCurrent().addFetch( fetch );

		if ( fetch instanceof FetchParent ) {
			processNext( (FetchParent) fetch );
		}
	}

	private void processNext(FetchParent fetchParent) {
		fetchParentStack.push( fetchParent );

		// todo (6.0) : finish hooking in entiyt-graphs.
		//		need to remember how all that works.  graphs/sub-graphs/kKey-sub-graphs/attributes
		//		makes my head spin

		try {
			fetchParent.getNavigableContainerReference().getNavigable().visitNavigable( this );
		}
		finally {
			fetchParentStack.pop();
		}
	}
}
