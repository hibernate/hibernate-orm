/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.SingletonStack;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.internal.PerQuerySpecSqlExpressionResolver;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.spi.SqlAstProducerContext;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectToSqlAstConverter;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.EntityTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;

/**
 * Specialized SqmSelectToSqlAstConverter extension to help in generating the
 * SELECT for selection of an entity's identifier columns, here specifically
 * intended to be used as the SELECT portion of an INSERT-SELECT.
 *
 * @author Steve Ebersole
 */
public class IdSelectGenerator extends SqmSelectToSqlAstConverter {

	public static QuerySpec generateEntityIdSelect(
			EntityTypeDescriptor entityDescriptor,
			SqmDeleteOrUpdateStatement sourceSqmStatement,
			QueryOptions queryOptions,
			LoadQueryInfluencers loadQueryInfluencers,
			SessionFactoryImplementor sessionFactory) {
		final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();
		final QuerySpec entityIdSelection = new QuerySpec( true );

		final TableSpace entityIdSelectionTableSpace = entityIdSelection.getFromClause().makeTableSpace();

		// Ask the entity descriptor to create a TableGroup.  This TableGroup
		//		will contain all the individual TableReferences we need..
		final NavigablePath path = new NavigablePath( entityDescriptor.getEntityName() );
		final EntityTableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
				new TableGroupInfo() {
					@Override
					public String getUniqueIdentifier() {
						return sourceSqmStatement.getEntityFromElement().getUniqueIdentifier();
					}

					@Override
					public String getIdentificationVariable() {
						return sourceSqmStatement.getEntityFromElement().getIdentificationVariable();
					}

					@Override
					public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
						return entityDescriptor;
					}

					@Override
					public NavigablePath getNavigablePath() {
						return path;
					}
				},
				new RootTableGroupContext() {
					@Override
					public void addRestriction(Predicate predicate) {
						entityIdSelection.addRestriction( predicate );
					}

					@Override
					public QuerySpec getQuerySpec() {
						return entityIdSelection;
					}

					@Override
					public TableSpace getTableSpace() {
						return entityIdSelectionTableSpace;
					}

					@Override
					public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
						return sqlAliasBaseManager;
					}

					@Override
					public JoinType getTableReferenceJoinType() {
						return JoinType.INNER;
					}

					@Override
					public LockOptions getLockOptions() {
						return queryOptions.getLockOptions();
					}
				}
		);

		entityIdSelectionTableSpace.setRootTableGroup( rootTableGroup );

		final PerQuerySpecSqlExpressionResolver sqlExpressionResolver = new PerQuerySpecSqlExpressionResolver(
				sessionFactory,
				() -> entityIdSelection,
				expression -> expression,
				(expression, selection) -> {}
		);

		final Stack<ColumnReferenceQualifier> tableGroupStack = new SingletonStack<>( rootTableGroup );
		final Stack<NavigableReference> navRefStack = new SingletonStack<>( rootTableGroup.getNavigableReference() );

		final DomainResultCreationContext domainResultCreationContext = new DomainResultCreationContext() {
			@Override
			public SessionFactoryImplementor getSessionFactory() {
				return sessionFactory;
			}
		};

		final DomainResultCreationState domainResultCreationState = new DomainResultCreationState() {
			@Override
			public SqlExpressionResolver getSqlExpressionResolver() {
				return sqlExpressionResolver;
			}

			@Override
			public Stack<ColumnReferenceQualifier> getColumnReferenceQualifierStack() {
				return tableGroupStack;
			}

			@Override
			public Stack<NavigableReference> getNavigableReferenceStack() {
				return navRefStack;
			}

			@Override
			public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
				throw new NotYetImplementedFor6Exception( getClass() );
			}

			@Override
			public boolean fetchAllAttributes() {
				return false;
			}

			@Override
			public List<Fetch> visitFetches(FetchParent fetchParent) {
				final List<Fetch> fetches = new ArrayList<>();
				final Consumer<Fetchable> fetchableConsumer = fetchable -> {
					if ( fetchParent.findFetch( fetchable.getNavigableName() ) != null ) {
						return;
					}

					fetches.add(
							fetchable.generateFetch(
									fetchParent,
									FetchTiming.IMMEDIATE,
									true,
									LockMode.NONE,
									null,
									this,
									domainResultCreationContext
							)
					);
				};

				try {
					NavigableContainer navigableContainer = fetchParent.getNavigableContainer();
					navigableContainer.visitKeyFetchables( fetchableConsumer );
					navigableContainer.visitFetchables( fetchableConsumer );
				}
				finally {

				}

				return fetches;
			}

			@Override
			public TableSpace getCurrentTableSpace() {
				return entityIdSelectionTableSpace;
			}

			@Override
			public LockMode determineLockMode(String identificationVariable) {
				return null;
			}
		};

		// NOTE : we ignore the created DomainResult - we only care about the generated/resolved SqlSelections
		entityDescriptor.getIdentifierDescriptor().createDomainResult(
				rootTableGroup.getNavigableReference(),
				// no domain alias
				null,
				domainResultCreationState,
				() -> sessionFactory
		);

		applyQueryRestrictions(
				entityDescriptor,
				entityIdSelection,
				entityIdSelectionTableSpace,
				sourceSqmStatement,
				queryOptions,
				sqlAliasBaseManager,
				sessionFactory
		);

		return entityIdSelection;
	}

	private static void applyQueryRestrictions(
			EntityTypeDescriptor entityDescriptor,
			QuerySpec entityIdSelection,
			TableSpace entityIdSelectionTableSpace,
			SqmDeleteOrUpdateStatement sourceSqmStatement,
			QueryOptions queryOptions,
			SqlAliasBaseManager sqlAliasBaseManager,
			SessionFactoryImplementor factory) {

		final SqmWhereClause whereClause = sourceSqmStatement.getWhereClause();
		if ( whereClause == null ) {
			return;
		}

		final SqmPredicate predicate = whereClause.getPredicate();
		if ( predicate == null ) {
			return;
		}

		if ( predicate instanceof Junction ) {
			final Junction junction = (Junction) predicate;
			if ( junction.isEmpty() ) {
				// todo (6.0) : ? - should it matter a conjunction versus a disjunction in terms of all versus none like we do in other places?
				return;
			}
		}

		final IdSelectGenerator walker = new IdSelectGenerator(
				entityDescriptor,
				entityIdSelection,
				entityIdSelectionTableSpace,
				sourceSqmStatement,
				sqlAliasBaseManager,
				queryOptions,
				factory
		);

		predicate.accept( walker );
	}


	private final EntityTypeDescriptor entityDescriptor;

	private final QuerySpec idSelectQuerySpec;
	private final TableSpace idSelectTableSpace;

	private final SqmDeleteOrUpdateStatement sourceSqmStatement;

	private final SqlAliasBaseManager sqlAliasBaseManager;
	private final SessionFactoryImplementor sessionFactory;

	private IdSelectGenerator(
			EntityTypeDescriptor entityDescriptor,
			QuerySpec idSelectQuerySpec,
			TableSpace idSelectTableSpace,
			SqmDeleteOrUpdateStatement sourceSqmStatement,
			SqlAliasBaseManager sqlAliasBaseManager,
			QueryOptions queryOptions,
			SessionFactoryImplementor sessionFactory) {
		super(
				queryOptions,
				new SqlAstProducerContext() {
					@Override
					public SessionFactoryImplementor getSessionFactory() {
						return sessionFactory;
					}

					@Override
					public LoadQueryInfluencers getLoadQueryInfluencers() {
						return LoadQueryInfluencers.NONE;
					}

					@Override
					public Callback getCallback() {
						return afterLoadAction -> {
						};
					}
				}
		);
		this.entityDescriptor = entityDescriptor;

		this.idSelectQuerySpec = idSelectQuerySpec;
		this.idSelectTableSpace = idSelectTableSpace;
		this.sqlAliasBaseManager = sqlAliasBaseManager;
		this.sourceSqmStatement = sourceSqmStatement;
		this.sessionFactory = sessionFactory;

		primeQuerySpecStack( idSelectQuerySpec );
		primeNavigableReferenceStack( idSelectTableSpace.getRootTableGroup().getNavigableReference() );

		// this allows where-clause restrictions from the source query to
		// 	resolve to this TableGroup for the entity for the id select
		getFromClauseIndex().crossReference( sourceSqmStatement.getEntityFromElement(), idSelectTableSpace.getRootTableGroup() );
	}


	@Override
	public SqlAliasBaseManager getSqlAliasBaseManager() {
		return sqlAliasBaseManager;
	}


	@Override
	protected QuerySpec currentQuerySpec() {
		return idSelectQuerySpec;
	}

}
