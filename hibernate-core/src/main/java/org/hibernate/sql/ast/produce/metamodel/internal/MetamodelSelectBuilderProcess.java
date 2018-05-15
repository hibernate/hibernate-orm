/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.metamodel.model.domain.spi.BasicValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.produce.internal.UniqueIdGenerator;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.internal.SqlAstSelectDescriptorImpl;
import org.hibernate.sql.ast.produce.internal.StandardSqlExpressionResolver;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.NavigablePathStack;
import org.hibernate.sql.ast.produce.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.spi.RootTableGroupProducer;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlAstProducerContext;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.spi.SqlQueryOptions;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.SelectStatement;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.SqlTuple;
import org.hibernate.sql.ast.tree.spi.expression.domain.BasicValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.EmbeddableValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.predicate.RelationalPredicate;
import org.hibernate.sql.exec.internal.StandardJdbcParameterImpl;
import org.hibernate.sql.results.spi.CircularFetchDetector;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class MetamodelSelectBuilderProcess
		implements SqlAstProducerContext, SqlAstCreationContext, SqlQueryOptions, DomainResultCreationState {

	private static final Logger log = Logger.getLogger( MetamodelSelectBuilderProcess.class );

	@SuppressWarnings("WeakerAccess")
	public static SqlAstSelectDescriptor createSelect(
			SessionFactoryImplementor sessionFactory,
			NavigableContainer rootNavigableContainer,
			List<Navigable<?>> navigablesToSelect,
			Navigable restrictedNavigable,
			DomainResult domainResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions) {
		final MetamodelSelectBuilderProcess process = new MetamodelSelectBuilderProcess(
				sessionFactory,
				rootNavigableContainer,
				navigablesToSelect,
				restrictedNavigable,
				domainResult,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions
		);

		return process.execute();
	}

	private final SessionFactoryImplementor sessionFactory;
	private final NavigableContainer rootNavigableContainer;
	private final List<Navigable<?>> navigablesToSelect;
	private final Navigable restrictedNavigable;
	private final DomainResult domainResult;
	private final int numberOfKeysToLoad;
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final LockOptions lockOptions;


	private final Stack<TableSpace> tableSpaceStack = new StandardStack<>();
	private final Stack<TableGroup> tableGroupStack = new StandardStack<>();
	private final NavigablePathStack navigablePathStack = new NavigablePathStack();
	private final Stack<NavigableReference> navigableReferenceStack = new StandardStack<>();

	private final Set<String> affectedTables = new HashSet<>();

	private final QuerySpec rootQuerySpec = new QuerySpec( true );

	private final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();

	private final StandardSqlExpressionResolver sqlExpressionResolver;

	private final Stack<ColumnReferenceQualifier> columnReferenceQualifierStack = new StandardStack<>();

	private MetamodelSelectBuilderProcess(
			SessionFactoryImplementor sessionFactory,
			NavigableContainer rootNavigableContainer,
			List<Navigable<?>> navigablesToSelect,
			Navigable restrictedNavigable,
			DomainResult domainResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions) {
		this.sessionFactory = sessionFactory;
		this.rootNavigableContainer = rootNavigableContainer;
		this.navigablesToSelect = navigablesToSelect;
		this.restrictedNavigable = restrictedNavigable;
		this.domainResult = domainResult;
		this.numberOfKeysToLoad = numberOfKeysToLoad;
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.lockOptions = lockOptions != null ? lockOptions : LockOptions.NONE;

		this.sqlExpressionResolver = new StandardSqlExpressionResolver(
				() -> rootQuerySpec,
				expression -> expression,
				(expression, selection) -> {}
		);
	}

	private SqlAstSelectDescriptor execute() {
		navigablePathStack.push( rootNavigableContainer );

		final SelectStatement selectStatement = new SelectStatement( rootQuerySpec );

		final UniqueIdGenerator uidGenerator = new UniqueIdGenerator();
		final String uid = uidGenerator.generateUniqueId();

		final TableSpace rootTableSpace = rootQuerySpec.getFromClause().makeTableSpace();
		tableSpaceStack.push( rootTableSpace );

		final TableGroup rootTableGroup = makeRootTableGroup( uid, rootQuerySpec, rootTableSpace, getSqlAliasBaseGenerator() );
		rootTableSpace.setRootTableGroup( rootTableGroup );
		tableGroupStack.push( rootTableGroup );

		columnReferenceQualifierStack.push( rootTableGroup );
		navigableReferenceStack.push( rootTableGroup.getNavigableReference() );

		final List<DomainResult> domainResults;

		if ( navigablesToSelect != null && ! navigablesToSelect.isEmpty() ) {
			domainResults = new ArrayList<>();
			for ( Navigable navigable : navigablesToSelect ) {
				final NavigableReference navigableReference = makeNavigableReference( rootTableGroup, navigable );
				domainResults.add(
						navigable.createDomainResult(
								navigableReference,
								null,
								this, this
						)
				);
			}
		}
		else {
			// use the one passed to the constructor or create one (maybe always create and pass?)
			//		allows re-use as they can be re-used to save on memory - they
			//		do not share state between
			final DomainResult domainResult;
			if ( this.domainResult != null ) {
				// used the one passed to the constructor
				domainResult = this.domainResult;
			}
			else {
				// create one
				final NavigableReference rootNavigableReference = rootTableGroup.getNavigableReference();
				domainResult = rootNavigableContainer.createDomainResult(
						rootNavigableReference,
						null,
						this,
						this
				);
			}

			domainResults = Collections.singletonList( domainResult );
		}


		// add the id/uk/fk restriction

		final List<ColumnReference> keyColumnReferences = new ArrayList<>();
		final List<StandardJdbcParameterImpl> keyParameterReferences = new ArrayList<>();

		restrictedNavigable.visitColumns(
				new BiConsumer<SqlExpressableType, Column>() {
					@Override
					public void accept(SqlExpressableType type, Column column) {
						keyColumnReferences.add(
								(ColumnReference) sqlExpressionResolver.resolveSqlExpression( rootTableGroup, column )
						);

						keyParameterReferences.add(
								new StandardJdbcParameterImpl(
										keyParameterReferences.size(),
										type,
										Clause.WHERE,
										getSessionFactory().getTypeConfiguration()
								)
						);
					}
				},
				Clause.WHERE,
				getSessionFactory().getTypeConfiguration()
		);

		final Expression keyColumnExpression;
		final Expression keyParameterExpression;

		if ( keyColumnReferences.size() == 1 ) {
			keyColumnExpression = keyColumnReferences.get( 0 );
			keyParameterExpression = keyParameterReferences.get( 0 );
		}
		else {
			keyColumnExpression = new SqlTuple( keyColumnReferences );
			keyParameterExpression = new SqlTuple( keyParameterReferences );
		}

		if ( numberOfKeysToLoad <= 1 ) {
			rootQuerySpec.addRestriction(
					new RelationalPredicate(
							RelationalPredicate.Operator.EQUAL,
							keyColumnExpression,
							keyParameterExpression
					)
			);
		}
		else {
			final InListPredicate predicate = new InListPredicate( keyColumnExpression );
			for ( int i = 0; i < numberOfKeysToLoad; i++ ) {
				predicate.addExpression( keyParameterExpression );
			}
			rootQuerySpec.addRestriction( predicate );
		}


		return new SqlAstSelectDescriptorImpl(
				selectStatement,
				domainResults,
				affectedTables
		);
	}

	public static NavigableReference makeNavigableReference(TableGroup rootTableGroup, Navigable navigable) {
		if ( navigable instanceof BasicValuedNavigable ) {
			return new BasicValuedNavigableReference(
					(NavigableContainerReference) rootTableGroup.getNavigableReference(),
					(BasicValuedNavigable) navigable,
					rootTableGroup.getNavigableReference().getNavigablePath().append( navigable.getNavigableName() )
			);
		}

		if ( navigable instanceof EmbeddedValuedNavigable ) {
			// todo (6.0) : join?
			return new EmbeddableValuedNavigableReference(
					(NavigableContainerReference) rootTableGroup.getNavigableReference(),
					(EmbeddedValuedNavigable) navigable,
					rootTableGroup.getNavigableReference().getNavigablePath().append( navigable.getNavigableName() ),
					LockMode.NONE
			);
		}

		if ( navigable instanceof EntityValuedNavigable ) {
			// todo (6.0) : join?
			return new EntityValuedNavigableReference(
					(NavigableContainerReference) rootTableGroup.getNavigableReference(),
					(EntityValuedNavigable) navigable,
					rootTableGroup.getNavigableReference().getNavigablePath().append( navigable.getNavigableName() ),
					rootTableGroup,
					LockMode.NONE
			);
		}

		throw new NotYetImplementedFor6Exception();
	}

	private TableGroup makeRootTableGroup(
			String uid,
			QuerySpec querySpec,
			TableSpace rootTableSpace,
			SqlAliasBaseGenerator aliasBaseManager) {
		// todo (6.0) : alias?
		final NavigablePath path = new NavigablePath( rootNavigableContainer.getNavigableName() );
		return ( (RootTableGroupProducer) rootNavigableContainer ).createRootTableGroup(
				new TableGroupInfo() {
					@Override
					public String getUniqueIdentifier() {
						return uid;
					}

					@Override
					public String getIdentificationVariable() {
						return "this";
					}

					@Override
					public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
						return null;
					}

					@Override
					public NavigablePath getNavigablePath() {
						return path;
					}
				},
				new RootTableGroupContext() {
					@Override
					public void addRestriction(Predicate predicate) {
						querySpec.addRestriction( predicate );
					}

					@Override
					public QuerySpec getQuerySpec() {
						return querySpec;
					}

					@Override
					public TableSpace getTableSpace() {
						return rootTableSpace;
					}

					@Override
					public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
						return aliasBaseManager;
					}

					@Override
					public JoinType getTableReferenceJoinType() {
						return null;
					}

					@Override
					public LockOptions getLockOptions() {
						return lockOptions;
					}
				}
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainResultCreationState

	/**
	 * todo (6.0) : consider removing - I believe this can be handled using `#navigableReferenceStack` + `NavigableReference#getColumnReferenceQualifier`
	 * 		save runtime memory.  of course see note below about simply passing NavigableReference
	 */
	@Override
	public Stack<ColumnReferenceQualifier> getColumnReferenceQualifierStack() {
		return columnReferenceQualifierStack;
	}

	@Override
	public Stack<NavigableReference> getNavigableReferenceStack() {
		return navigableReferenceStack;
	}

	@Override
	public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
		return sqlAliasBaseManager;
	}

	// todo (6.0) : seems like this entire thing can be centralized and used in all SQL AST generation processes to handle fetches
	//		variations (arguments) include:
	//			1) FetchParent
	//			2) determining the FetchStrategy - functional interface returning the FetchStrategy (Function, BiFunction, etc)?
	//			3) LockMode - currently not handled very well here; should consult `#getLockOptions`
	//					 - perhaps a functional interface accepting the FetchParent and Fetchable and returning the LockMode
	//
	//		so something like:
	//			List<Fetch> visitFetches(
	// 					FetchParent fetchParent,
	// 					BiFunction<FetchParent,Fetchable,FetchStrategy> fetchStrategyResolver,
	// 					BiFunction<FetchParent,Fetchable,LockMode> lockModeResolver)
	//
	//		and called from here as, e.g. (`fetchState` being that centralized logic):
	//			fetchState.visitFetches(
	// 					fetchParent,
	//					// todo (6.0) : this should account for EntityGraph
	//					(fetchParent,fetchable) -> fetchable.getMappedFetchStrategy(),
	//					(fetchParent,fetchable) -> LockMode.READ
	//			);
	//
	// todo (6.0) : also consider passing along NavigableReference rather than using context+stack
	//		when calling `Fetchable#generateFetch`


	private final CircularFetchDetector circularFetchDetector = new CircularFetchDetector();

	@Override
	public List<Fetch> visitFetches(FetchParent fetchParent) {
		log.tracef( "Starting visitation of FetchParent's Fetchables : %s", fetchParent.getNavigablePath() );

		// will hold both:
		//
		// ... join fetch a.parent.child.parent
		//
		// and
		//
		// ... join fetch a.parent
		//
		// but `a.parent` points to the "real" `Fetch`, while `a.parent.child.parent` points to
		// the `BiDirFetchReference`
		//

		final List<Fetch> fetches = new ArrayList<>();

		final Consumer<Fetchable> fetchableConsumer = fetchable -> {

			final Fetch biDirectionalFetch = circularFetchDetector.findBiDirectionalFetch(
					fetchParent,
					fetchable
			);

			if ( biDirectionalFetch != null ) {
				fetches.add( biDirectionalFetch );
				return;
			}


			// todo (6.0) : move this code into `fetchable.generateFetch` call
			//		this can mean duplicated code, but is more appropriate
			//		because returning null is actually inaccurate, it should
			//		so a "later" fetch (lazy, n+1, ...) - and those would
			//		be specific to each Fetchable anyway (DelayedEntityFetch,
			//		DelayedCollectionFetch, ...)

			LockMode lockMode = LockMode.READ;
			FetchTiming fetchTiming = fetchable.getMappedFetchStrategy().getTiming();
			boolean joined = fetchable.getMappedFetchStrategy().getStyle() == FetchStyle.JOIN;

			final Integer maximumFetchDepth = getSessionFactory().getSessionFactoryOptions().getMaximumFetchDepth();
			// minus one because the root is not a fetch
			final int fetchDepth = getNavigableReferenceStack().depth() - 1;

			if ( fetchDepth == maximumFetchDepth ) {
				joined = false;
			}
			else if ( fetchDepth > maximumFetchDepth ) {
				return;
			}

			Fetch fetch = fetchable.generateFetch( fetchParent, fetchTiming, joined, lockMode, null, this, this );
			fetches.add( fetch );
		};

		NavigableContainer navigableContainer = fetchParent.getNavigableContainer();
		navigableContainer.visitKeyFetchables( fetchableConsumer );
		navigableContainer.visitFetchables( fetchableConsumer );

		return fetches;
	}


	@Override
	public TableSpace getCurrentTableSpace() {
		return tableSpaceStack.getCurrent();
	}

	@Override
	public LockMode determineLockMode(String identificationVariable) {
		return identificationVariable == null
				? getLockOptions().getLockMode()
				: getLockOptions().getEffectiveLockMode( identificationVariable );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlAstBuildingContext

	private List<AfterLoadAction> afterLoadActions;

	private final Callback sqlAstCreationCallback = new Callback() {
		@Override
		public void registerAfterLoadAction(AfterLoadAction afterLoadAction) {
			if ( afterLoadActions == null ) {
				afterLoadActions = new ArrayList<>();
			}
			afterLoadActions.add( afterLoadAction );
		}
	};

	@Override
	public Callback getCallback() {
		return sqlAstCreationCallback;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return loadQueryInfluencers;
	}

	@Override
	public SqlExpressionResolver getSqlSelectionResolver() {
		return sqlExpressionResolver;
	}

	@Override
	public boolean shouldCreateShallowEntityResult() {
		return false;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlQueryOptions

	@Override
	public Integer getFirstRow() {
		return null;
	}

	@Override
	public Integer getMaxRows() {
		return null;
	}

	@Override
	public String getComment() {
		// todo (6.0) : add generated sql comment based on Navigable + type-of-select
		return null;
	}

	@Override
	public List<String> getDatabaseHints() {
		return null;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlQueryOptions && QueryResultCreationContext

	@Override
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Override
	public SqlExpressionResolver getSqlExpressionResolver() {
		return sqlExpressionResolver;
	}

	@Override
	public boolean fetchAllAttributes() {
		return false;
	}
}
