/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.internal;

import java.util.HashSet;
import java.util.List;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.graph.spi.AttributeNodeContainer;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.EntityGraphQueryHint;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.JoinedTableGroupContext;
import org.hibernate.sql.ast.produce.spi.TableGroupJoinProducer;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectToSqlAstConverter;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;

import org.jboss.logging.Logger;

import static org.hibernate.engine.FetchStrategy.IMMEDIATE_JOIN;

/**
 * Handles apply fetches to the "fetch graph" for an SQM-backed query.
 * Accounts for query-defined fetches as well as  EntityGraph-defined
 * fetches.
 *
 * @author Steve Ebersole
 */
public class FetchGraphBuilder {
	private static final Logger log = Logger.getLogger( FetchGraphBuilder.class );

	private final QuerySpec querySpec;
	private final SqmSelectToSqlAstConverter builder;

	@SuppressWarnings("FieldCanBeLocal")
	private final EntityGraphQueryHint.Type entityGraphQueryHintType;
	private final AttributeNodeContainer rootGraphAttributeContainer;

	private final int fetchDepthLimit;

	private final FetchStrategy fetchStrategy = IMMEDIATE_JOIN;

	public FetchGraphBuilder(
			QuerySpec querySpec,
			SqmSelectToSqlAstConverter builder,
			EntityGraphQueryHint entityGraphQueryHint) {
		this.querySpec = querySpec;
		this.builder = builder;

		this.fetchDepthLimit = builder.getSessionFactory().getSessionFactoryOptions().getMaximumFetchDepth();

		if ( entityGraphQueryHint != null ) {
			this.entityGraphQueryHintType = entityGraphQueryHint.getType();
			this.rootGraphAttributeContainer = entityGraphQueryHint.getHintedGraph();
		}
		else {
			this.entityGraphQueryHintType = EntityGraphQueryHint.Type.NONE;
			this.rootGraphAttributeContainer = null;
		}

	}

	// todo (6.0) : need proper circularity checks here

	// todo (6.0) : need to decide who should "apply" fetches
	//		this means both (1) adding to the from-clause and (2) adding the the select-clause.


	public void process(FetchParent fetchParent) {
//		processFetchParent( fetchParent, rootGraphAttributeContainer, 0 );
	}

	private void processFetchParent(
			FetchParent fetchParent,
			AttributeNodeContainer attributeNodeContainer,
			int depth) {
		if ( depth > fetchDepthLimit ) {
			return;
		}

		final HashSet<String> processedAttributeNames = new HashSet<>();

		NavigableContainerReference parentContainerReference =
				(NavigableContainerReference) builder.getFromClauseIndex().findResolvedNavigableReference( fetchParent.getNavigablePath() );


		// todo (6.0) : need a better approach to tracking FetchParent
		final String parentUid = builder.getFromClauseIndex().getTableGroupUidForNavigableReference(
				parentContainerReference
		);
		final TableGroup parentTableGroup = builder.getFromClauseIndex().resolveTableGroup( parentUid );

		final List<SqmNavigableJoin> fetchedJoins = builder.getFromClauseIndex().findFetchesByParentUniqueIdentifier( parentUid );
		for ( SqmNavigableJoin fetchedJoin : fetchedJoins ) {
			assert fetchedJoin.isFetched();

			final String fetchedAttributeName = fetchedJoin.getAttributeReference()
					.getReferencedNavigable()
					.getAttributeName();

			final TableGroup tableGroup = builder.getFromClauseIndex().resolveTableGroup( fetchedJoin.getUniqueIdentifier() );
			assert tableGroup != null;

			final AttributeNodeImplementor attributeNode = attributeNodeContainer.findAttributeNode( fetchedAttributeName );
			final NavigableReference fetchedNavigableReference = tableGroup.getNavigableReference();

			processedAttributeNames.add( fetchedNavigableReference.getNavigable().getNavigableName() );

			assert fetchedNavigableReference.getNavigable() instanceof Fetchable;
			final Fetch fetch = ( (Fetchable) fetchedNavigableReference.getNavigable() ).generateFetch(
					fetchParent,
					tableGroup,
					fetchStrategy,
					fetchedJoin.getIdentificationVariable(),
					builder
			);
			processFetch( fetchParent, fetch, attributeNode, depth );
		}

		for ( AttributeNodeImplementor<?> attributeNode : attributeNodeContainer.attributeNodes() ) {
			if ( processedAttributeNames.contains( attributeNode.getAttributeName() ) ) {
				continue;
			}

			final PersistentAttribute persistentAttribute = (PersistentAttribute) fetchParent.getFetchContainer()
					.findNavigable( attributeNode.getAttributeName() );

			NavigableReference navigableReference = parentContainerReference.findNavigableReference( persistentAttribute.getNavigableName() );

			if ( navigableReference == null ) {
				// 1) Find the TableGroupJoinProducer and generate the TableGroupJoin
				assert persistentAttribute instanceof TableGroupJoinProducer;
				final String fetchJoinTableGroupUid = builder.generateSqlAstNodeUid();
				final TableGroupJoin fetchTableGroupJoin = ( (TableGroupJoinProducer) persistentAttribute ).createTableGroupJoin(
						new TableGroupInfo() {
							@Override
							public String getUniqueIdentifier() {
								return fetchJoinTableGroupUid;
							}

							@Override
							public String getIdentificationVariable() {
								return null;
							}

							@Override
							public EntityDescriptor getIntrinsicSubclassEntityMetadata() {
								return null;
							}
						},
						JoinType.LEFT,
						new JoinedTableGroupContext() {
							@Override
							public TableGroup getLhs() {
								return parentTableGroup;
							}

							@Override
							public ColumnReferenceQualifier getColumnReferenceQualifier() {
								return parentTableGroup;
							}

							@Override
							public NavigablePath getNavigablePath() {
								// todo (6.0) whose NavigablePath?
								return null;
							}

							@Override
							public QuerySpec getQuerySpec() {
								return querySpec;
							}

							@Override
							public TableSpace getTableSpace() {
								return parentTableGroup.getTableSpace();
							}

							@Override
							public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
								return builder.getSqlAliasBaseManager();
							}

							@Override
							public JoinType getTableReferenceJoinType() {
								return JoinType.INNER;
							}

							@Override
							public QueryOptions getQueryOptions() {
								return builder.getQueryOptions();
							}
						}
				);
				parentTableGroup.getTableSpace().addJoinedTableGroup( fetchTableGroupJoin );

				navigableReference = fetchTableGroupJoin.getJoinedGroup().getNavigableReference();
				( (NavigableContainerReference) parentTableGroup.getNavigableReference() ).addNavigableReference( navigableReference );
			}

			final Fetch fetch = ( (Fetchable) persistentAttribute ).generateFetch(
					fetchParent,
					// todo (6.0) : wtf?
					null,
					fetchStrategy,
					null,
					builder
			);

			processFetch( fetchParent, fetch, attributeNode, depth );
		}

	}

	@SuppressWarnings("unchecked")
	private void processFetch(
			FetchParent fetchParent,
			Fetch fetch,
			AttributeNodeImplementor fetchedAttributeNode,
			int depth) {
		fetchParent.addFetch( fetch );

		if ( !FetchParent.class.isInstance( fetch ) ) {
			return;
		}

		final FetchParent fetchAsFetchParent = (FetchParent) fetch;

		assert fetch.getFetchedNavigable() instanceof PersistentAttribute;
		final PersistentAttribute persistentAttribute = (PersistentAttribute) fetch.getFetchedNavigable();

		final SubGraphImplementor subGraph = fetchedAttributeNode.extractSubGraph( persistentAttribute );

		processFetchParent( fetchAsFetchParent, subGraph, depth + 1 );
	}
}
