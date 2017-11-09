/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.model.domain.internal.BasicSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.CollectionKey;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierCompositeAggregated;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierCompositeNonAggregated;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierSimple;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.spi.RowIdDescriptor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.TenantDiscrimination;
import org.hibernate.metamodel.model.domain.spi.VersionDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.AssociationKey;
import org.hibernate.sql.ast.produce.metamodel.spi.AssociationKeyProducer;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.metamodel.spi.MetamodelDrivenSqlSelectPlanBuilder;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.FromClauseIndex;
import org.hibernate.sql.ast.produce.spi.JoinedTableGroupContext;
import org.hibernate.sql.ast.produce.spi.NavigablePathStack;
import org.hibernate.sql.ast.produce.spi.NonQualifiableSqlExpressable;
import org.hibernate.sql.ast.produce.spi.QualifiableSqlExpressable;
import org.hibernate.sql.ast.produce.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.spi.SqlAstBuildingContext;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.spi.TableGroupJoinProducer;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.predicate.RelationalPredicate;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.SqlSelection;

import org.jboss.logging.Logger;

/**
 * Abstract MetamodelDrivenSqlSelectPlanBuilder to help implementations
 * that need to walk the run-time metamodel and build a SqlSelectPlan (SQL AST for
 * SELECT queries).
 * <p/>
 * Specifically, this class helps builds the {@link SqlAstSelectDescriptor} and its single {@link QueryResult}
 *
 * @author Steve Ebersole
 */
public abstract class AbstractMetamodelDrivenSqlSelectPlanBuilder
		implements MetamodelDrivenSqlSelectPlanBuilder, RootTableGroupContext, JoinedTableGroupContext,
		QueryResultCreationContext, SqlAstBuildingContext, SqlExpressionResolver {

	private static final Logger log = Logger.getLogger( AbstractMetamodelDrivenSqlSelectPlanBuilder.class );

	private final SqlAstBuildingContext buildingContext;
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final LockOptions lockOptions;

	private final FromClauseIndex fromClauseIndex = new FromClauseIndex();
	private final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();

	// todo (6.0) : populate this set everytime we encounter a "joinable" and check every time (b4) we continue processing the joinable
	private final Set<AssociationKey> associationKeys = new HashSet<>();

	//	private final Stack<Clause> currentClauseStack = new Stack<>();
//	private final Stack<QuerySpec> querySpecStack = new Stack<>();
//	private final Stack<NavigableSource<?>> navigableSourceStack = new Stack<>();


	private final NavigablePathStack navigablePathStack = new NavigablePathStack();
	private final Stack<FetchParent> fetchParentStack = new Stack<>();
	private final Stack<NavigableContainerReferenceInfoImpl> navigableContainerInfoStack = new Stack<>();
	private final Stack<TableGroup> tableGroupStack = new Stack<>();

	private final Map<FetchParent,NavigableContainerReference> fetchParentNavigableContainerReferenceMap = new HashMap<>();
	private final Map<NavigableContainerReference,FetchParent>  navigableContainerReferenceFetchParentHashMap= new HashMap<>();

	private QuerySpec querySpec;
	private TableSpace tableSpace;
	private TableGroup rootTableGroup;
	private HashMap<SqlExpressable,SqlSelection> sqlSelectionMap = new HashMap<>();

	/**
	 *
	 * @param buildingContext Parameter object providing access to information needede
	 * 		during plan building
	 * @param loadQueryInfluencers Any options that influence load queries (entity graphs, fetch
	 * 		profiles, filters, etc)
	 * @param lockOptions The requested locking profile
	 */
	public AbstractMetamodelDrivenSqlSelectPlanBuilder(
			SqlAstBuildingContext buildingContext,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions) {
		this.buildingContext = buildingContext;
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.lockOptions = lockOptions;

		this.querySpec = new QuerySpec( true );
		this.tableSpace = querySpec.getFromClause().makeTableSpace();
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return buildingContext.getSessionFactory();
	}

	/**
	 * @deprecated Use {@link #getSessionFactory()} instead.
	 */
	@Deprecated
	protected SessionFactoryImplementor sessionFactory() {
		return getSessionFactory();
	}

	public FromClauseIndex getFromClauseIndex() {
		return fromClauseIndex;
	}

	@Override
	public Callback getCallback() {
		return buildingContext.getCallback();
	}

	private NavigableReferenceInfoImpl createNavigableRefInfo(Navigable navigable) {
		if ( navigable instanceof NavigableContainer ) {
			return new NavigableContainerReferenceInfoImpl(
					navigableContainerInfoStack.getCurrent(),
					(NavigableContainer) navigable,
					navigablePathStack.getCurrent(),
					generateSqlAstNodeUid(),
					null,
					null
			);
		}
		else {
			return new NavigableReferenceInfoImpl(
					navigableContainerInfoStack.getCurrent(),
					navigable,
					navigablePathStack.getCurrent(),
					generateSqlAstNodeUid(),
					null,
					null
			);
		}
	}

	private int counter;

	private String generateSqlAstNodeUid() {
		return "uid" + counter++;
	}

	@Override
	public void prepareForVisitation() {
		if ( !fetchParentStack.isEmpty() || !tableGroupStack.isEmpty() ) {
			throw new IllegalStateException(
					"MetamodelDrivenSqlSelectPlanBuilder [" + this + "] is not in proper state to begin visitation"
			);
		}

		querySpec = new QuerySpec( true );
		// when generating a SELECT from the metamodel there is only ever 1 TableSpace, so make it here
		tableSpace = querySpec.getFromClause().makeTableSpace();
	}

	@Override
	public void visitationComplete() {
		querySpec = null;
		tableSpace = null;
		rootTableGroup = null;

		navigablePathStack.clear();

		if ( !fetchParentStack.isEmpty() ) {
			log.debug( "fetchParentStack was not empty upon completion of visitation; un-matched push and pop?" );
			fetchParentStack.clear();
		}
		if ( !tableGroupStack.isEmpty() ) {
			log.debug( "fetchLhsTableGroupStack was not empty upon completion of visitation; un-matched push and pop?" );
			tableGroupStack.clear();
		}
	}

	@Override
	public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
		return sqlAliasBaseManager;
	}


	protected Predicate generateRestriction() {
		final Expression restrictionExpression = generateRestrictionExpression();
		return new RelationalPredicate(
				RelationalPredicate.Operator.EQUAL,
				restrictionExpression,
				new LoadIdParameter( (AllowableParameterType) restrictionExpression.getType() )
		);
	}

	protected abstract Expression generateRestrictionExpression();

	private boolean shouldContinue(Navigable navigable) {
		if ( navigable instanceof AssociationKeyProducer ) {
			final AssociationKeyProducer producer = (AssociationKeyProducer) navigable;
			final AssociationKey associationKey = producer.getAssociationKey();
			return associationKeys.add( associationKey );
		}

		return true;
	}

	@Override
	public void visitEntity(EntityDescriptor entity) {
		// the root entity is handled in #buildSqlSelectPlan and entity fetches are
		//		handled in the actual "joinable navigable".  So nothing to do here
		//		except propagate to the entity's navigables.
		entity.visitNavigables( this );
	}

	@Override
	public void visitSingularAttributeEmbedded(SingularPersistentAttributeEmbedded attribute) {
		processFetchableSingularAttribute( attribute );
	}

	private void processFetchableSingularAttribute(SingularPersistentAttribute attribute) {
		assert attribute instanceof Fetchable;
		final Fetchable fetchable = (Fetchable) attribute;

		if ( !shouldContinue( attribute ) ) {
			log.debugf( "Stopping walking of Navigable model at [%s]", attribute );
			return;
		}

		// todo (6.0) : why are we creating both a NavigableReferenceInfo and a NavigableReference?
		//		maybe this is needed from SQM conversion - check
		//		otherwise this should be reworked:
		//			1) I think we may still need "TableGroupSourceInfo", but not sure NavigableReferenceInfo
		//				should extend that
		//			2) either
		// 				a) have NavigableReference implement NavigableReferenceInfo, and
		//					NavigableContainerReference implement NavigableContainerReferenceInfo
		//				b) simply use NavigableReference/NavigableContainerReference in place of
		//					NavigableReferenceInfo/NavigableContainerReferenceInfo
		//
		//		I'd prefer 2.b.  The only time this NavigableReferenceInfo/NavigableContainerReferenceInfo
		// 			stuff is (possibly) useful is in processing SQM so would be great to localize it there
		//			*if* SQM processing even needs it.
		//
		//		^^ NavigableReferenceInfo/NavigableContainerReferenceInfo were initially developed to
		//			help in isolating persisters and SQL AST production.  But a lot has changed since in
		//			those things individually and I think we no longer need these.

		navigablePathStack.push( attribute );
		final NavigableContainerReferenceInfoImpl navigableRefInfo = (NavigableContainerReferenceInfoImpl) createNavigableRefInfo( attribute );
		navigableContainerInfoStack.push( navigableRefInfo );

		try {
//			final NavigableReference navigableReference = new SingularAttributeReference(
//					fetchParentNavigableContainerReferenceMap.get( fetchParentStack.getCurrent() ),
//					attribute,
//					currentNavigablePath()
//			);


			if ( attribute instanceof TableGroupJoinProducer ) {
				final TableGroupJoinProducer tableGroupJoinProducer = (TableGroupJoinProducer) attribute;
				TableGroupJoin tableGroupJoin = tableGroupJoinProducer.createTableGroupJoin(
						navigableRefInfo,
						// todo (6.0) : join type.  use LEFT OUTER for now
						JoinType.LEFT,
						this
				);

				tableGroupStack.push( tableGroupJoin.getJoinedGroup() );
			}

			try {
				final Fetch fetch = fetchable.generateFetch(
						fetchParentStack.getCurrent(),
						tableGroupStack.getCurrent(),
						// todo (6.0) : auto-generate SQL alias base?
						null,
						// todo (6.0) : this needs to be a QueryResultCreationContext
						null,
						this

				);
				fetchParentStack.getCurrent().addFetch( fetch );
				fetchParentStack.push( (FetchParent) fetch );
				try {
					( (NavigableContainer) attribute ).visitNavigables( this );
				}
				finally {
					fetchParentStack.pop();
				}
			}
			finally {
				if ( attribute instanceof TableGroupJoinProducer ) {
					tableGroupStack.pop();
				}
			}
		}
		finally {
			navigablePathStack.pop();
				navigableContainerInfoStack.pop();
		}
	}

	@Override
	public void visitSingularAttributeEntity(SingularPersistentAttributeEntity attribute) {
		processFetchableSingularAttribute( attribute );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlSelectionResolver
	//
	//		See impls on SqmSelectToSqlAstConverter


	@Override
	public SqlExpressionResolver getSqlSelectionResolver() {
		return this;
	}

	@Override
	public Expression resolveSqlExpression(
			ColumnReferenceQualifier qualifier,
			QualifiableSqlExpressable sqlSelectable) {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public Expression resolveSqlExpression(NonQualifiableSqlExpressable sqlSelectable) {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public SqlSelection resolveSqlSelection(Expression expression) {
		throw new NotYetImplementedFor6Exception(  );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// TableGroupContext

	@Override
	public QuerySpec getQuerySpec() {
		return querySpec;
	}

	@Override
	public TableSpace getTableSpace() {
		return tableSpace;
	}

	@Override
	public TableGroup getLhs() {
		return tableGroupStack.getCurrent();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// QueryResultCreationContext

//	@Override
//	public ColumnReferenceSource currentColumnReferenceSource() {
//		return tableGroupStack.getCurrent();
//	}
//
	private NavigablePath currentNavigablePath() {
		return navigablePathStack.getCurrent();
	}

	@Override
	public boolean shouldCreateShallowEntityResult() {
		return false;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// RootTableGroupContext

	@Override
	public void addRestriction(Predicate predicate) {
		log.debugf( "Query restriction being added through RootTableGroupContext#addRestriction" );
		querySpec.addRestriction( predicate );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// no-op impls

	@Override
	public void visitSimpleIdentifier(EntityIdentifierSimple identifier) {
	}

	@Override
	public void visitAggregateCompositeIdentifier(EntityIdentifierCompositeAggregated identifier) {
	}

	@Override
	public void visitNonAggregateCompositeIdentifier(EntityIdentifierCompositeNonAggregated identifier) {
	}

	@Override
	public void visitDiscriminator(DiscriminatorDescriptor discriminator) {
	}

	@Override
	public void visitVersion(VersionDescriptor version) {
	}

	@Override
	public void visitRowIdDescriptor(RowIdDescriptor rowIdDescriptor) {
	}

	@Override
	public void visitTenantTenantDiscrimination(TenantDiscrimination tenantDiscrimination) {
	}

	@Override
	public void visitSingularAttributeBasic(BasicSingularPersistentAttribute attribute) {
	}

	@Override
	public void visitCollectionForeignKey(CollectionKey collectionKey) {
	}












//
//	interface CompositeEntityIdentifierVisitor {
//		void visitKeyAttribute();
//		void visitKeyManyToOne();
//	}
//
//	public void visitEntityIdentifier(EntityIdentifier entityIdentifier) {
//		navigablePathStack.push( entityIdentifier );
//
//		try {
//			if ( entityIdentifier instanceof NavigableContainer ) {
//
//			}
//		}
//		finally {
//			navigablePathStack.pop();
//		}
//	}
//
//	public void visitCollectionElementBasic(CollectionElementBasic collectionElementBasic) {
//
//	}
//
//	protected abstract void addRootReturn(Return rootReturn);
//
//
//
//
//
//
//	@Override
//	public ExpandingQuerySpaces getQuerySpaces() {
//		return querySpaces;
//	}
//
//
//
//
//
//
//
//	// stack management ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
//	private void pushToStack(ExpandingFetchSource fetchSource) {
//		log.trace( "Pushing fetch source to stack : " + fetchSource );
//		navigablePathStack.push( fetchSource );
//		fetchSourceStack.addFirst( fetchSource );
//	}
//
//	private ExpandingFetchSource popFromStack() {
//		final ExpandingFetchSource last = fetchSourceStack.removeFirst();
//		log.trace( "Popped fetch owner from stack : " + last );
//		navigablePathStack.pop();
//		return last;
//	}
//
//	protected ExpandingFetchSource currentSource() {
//		return fetchSourceStack.peekFirst();
//	}
//
//
//	// Entity-level AssociationVisitationStrategy hooks ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
//	protected boolean supportsRootEntityReturns() {
//		return true;
//	}
//
//	// Entities  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
//	@Override
//	public void startingEntity(EntityDefinition entityDefinition) {
//		// see if the EntityDefinition is a root...
//		final boolean isRoot = fetchSourceStack.isEmpty();
//		if ( ! isRoot ) {
//			// if not, this call should represent a fetch which should have been handled in #startingAttribute
//			return;
//		}
//
//		// if we get here, it is a root
//
//		log.tracef(
//				"%s Starting root entity : %s",
//				StringHelper.repeat( ">>", fetchSourceStack.size() ),
//				entityDefinition.getEntityDescriptor().getEntityName()
//		);
//
//		if ( !supportsRootEntityReturns() ) {
//			throw new HibernateException( "This strategy does not support root entity returns" );
//		}
//
//		final EntityReturnImpl entityReturn = new EntityReturnImpl( entityDefinition, querySpaces );
//		addRootReturn( entityReturn );
//		pushToStack( entityReturn );
//
//		// also add an AssociationKey for the root so we can later on recognize circular references back to the root.
//		final Joinable entityPersister = (Joinable) entityDefinition.getEntityDescriptor();
//		associationKeyRegistered(
//				new AssociationKey( entityPersister.getTableName(), entityPersister.getKeyColumnNames() )
//		);
//	}
//
//	@Override
//	public void finishingEntity(EntityDefinition entityDefinition) {
//		// Only process the entityDefinition if it is for the root return.
//		final FetchSource currentSource = currentSource();
//		final boolean isRoot = EntityReturn.class.isInstance( currentSource ) &&
//				entityDefinition.getEntityDescriptor().equals( EntityReturn.class.cast( currentSource ).getEntityDescriptor() );
//		if ( !isRoot ) {
//			// if not, this call should represent a fetch which will be handled in #finishingAttribute
//			return;
//		}
//
//		// if we get here, it is a root
//		final ExpandingFetchSource popped = popFromStack();
//		checkPoppedEntity( popped, entityDefinition );
//
//		log.tracef(
//				"%s Finished root entity : %s",
//				StringHelper.repeat( "<<", fetchSourceStack.size() ),
//				entityDefinition.getEntityDescriptor().getEntityName()
//		);
//	}
//
//	private void checkPoppedEntity(ExpandingFetchSource fetchSource, EntityDefinition entityDefinition) {
//		// make sure what we just fetchSource represents entityDefinition
//		if ( ! EntityReference.class.isInstance( fetchSource ) ) {
//			throw new WalkingException(
//					String.format(
//							"Mismatched FetchSource from stack on pop.  Expecting EntityReference(%s), but found %s",
//							entityDefinition.getEntityDescriptor().getEntityName(),
//							fetchSource
//					)
//			);
//		}
//
//		final EntityReference entityReference = (EntityReference) fetchSource;
//		// NOTE : this is not the most exhaustive of checks because of hierarchical associations (employee/manager)
//		if ( ! entityReference.getEntityDescriptor().equals( entityDefinition.getEntityDescriptor() ) ) {
//			throw new WalkingException( "Mismatched FetchSource from stack on pop" );
//		}
//	}
//
//
//	// entity identifiers ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
//	@Override
//	public void startingEntityIdentifier(EntityIdentifierDefinition entityIdentifierDefinition) {
//		log.tracef(
//				"%s Starting entity identifier : %s",
//				StringHelper.repeat( ">>", fetchSourceStack.size() ),
//				entityIdentifierDefinition.getEntityDefinition().getEntityDescriptor().getEntityName()
//		);
//
//		final EntityReference entityReference = (EntityReference) currentSource();
//
//		// perform some stack validation
//		if ( ! entityReference.getEntityDescriptor().equals( entityIdentifierDefinition.getEntityDefinition().getEntityDescriptor() ) ) {
//			throw new WalkingException(
//					String.format(
//							"Encountered unexpected fetch owner [%s] in stack while processing entity identifier for [%s]",
//							entityReference.getEntityDescriptor().getEntityName(),
//							entityIdentifierDefinition.getEntityDefinition().getEntityDescriptor().getEntityName()
//					)
//			);
//		}
//
//		if ( ExpandingEntityIdentifierDescription.class.isInstance( entityReference.getIdentifierDescription() ) ) {
//			pushToStack( (ExpandingEntityIdentifierDescription) entityReference.getIdentifierDescription() );
//		}
//	}
//
//	@Override
//	public void finishingEntityIdentifier(EntityIdentifierDefinition entityIdentifierDefinition) {
//		// only pop from stack if the current source is ExpandingEntityIdentifierDescription..
//		final ExpandingFetchSource currentSource = currentSource();
//		if ( ! ExpandingEntityIdentifierDescription.class.isInstance( currentSource ) ) {
//			// in this case, the current source should be the entity that owns entityIdentifierDefinition
//			if ( ! EntityReference.class.isInstance( currentSource ) ) {
//				throw new WalkingException( "Unexpected state in FetchSource stack" );
//			}
//			final EntityReference entityReference = (EntityReference) currentSource;
//			if ( entityReference.getEntityDescriptor().getEntityKeyDefinition() != entityIdentifierDefinition ) {
//				throw new WalkingException(
//						String.format(
//								"Encountered unexpected fetch owner [%s] in stack while processing entity identifier for [%s]",
//								entityReference.getEntityDescriptor().getEntityName(),
//								entityIdentifierDefinition.getEntityDefinition().getEntityDescriptor().getEntityName()
//						)
//				);
//			}
//			return;
//		}
//
//		// the current source is ExpandingEntityIdentifierDescription...
//		final ExpandingEntityIdentifierDescription identifierDescription =
//				(ExpandingEntityIdentifierDescription) popFromStack();
//
//		// and then on the node beforeQuery it (which should be the entity that owns the identifier being described)
//		final ExpandingFetchSource entitySource = currentSource();
//		if ( ! EntityReference.class.isInstance( entitySource ) ) {
//			throw new WalkingException( "Unexpected state in FetchSource stack" );
//		}
//		final EntityReference entityReference = (EntityReference) entitySource;
//		if ( entityReference.getIdentifierDescription() != identifierDescription ) {
//			throw new WalkingException(
//					String.format(
//							"Encountered unexpected fetch owner [%s] in stack while processing entity identifier for [%s]",
//							entityReference.getEntityDescriptor().getEntityName(),
//							entityIdentifierDefinition.getEntityDefinition().getEntityDescriptor().getEntityName()
//					)
//			);
//		}
//
//		log.tracef(
//				"%s Finished entity identifier : %s",
//				StringHelper.repeat( "<<", fetchSourceStack.size() ),
//				entityIdentifierDefinition.getEntityDefinition().getEntityDescriptor().getEntityName()
//		);
//	}
//	// Collections ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
//	private ArrayDeque<CollectionReference> collectionReferenceStack = new ArrayDeque<CollectionReference>();
//
//	private void pushToCollectionStack(CollectionReference collectionReference) {
//		log.trace( "Pushing collection reference to stack : " + collectionReference );
//		navigablePathStack.push( collectionReference.getPropertyPath() );
//		collectionReferenceStack.addFirst( collectionReference );
//	}
//
//	private CollectionReference popFromCollectionStack() {
//		final CollectionReference last = collectionReferenceStack.removeFirst();
//		log.trace( "Popped collection reference from stack : " + last );
//		navigablePathStack.pop();
//		return last;
//	}
//
//	private CollectionReference currentCollection() {
//		return collectionReferenceStack.peekFirst();
//	}
//
//	@Override
//	public void startingCollection(CollectionDefinition collectionDefinition) {
//		// see if the EntityDefinition is a root...
//		final boolean isRoot = fetchSourceStack.isEmpty();
//		if ( ! isRoot ) {
//			// if not, this call should represent a fetch which should have been handled in #startingAttribute
//			return;
//		}
//
//		log.tracef(
//				"%s Starting root collection : %s",
//				StringHelper.repeat( ">>", fetchSourceStack.size() ),
//				collectionDefinition.getCollectionDescriptor().getRole()
//		);
//
//		// if we get here, it is a root
//		if ( ! supportsRootCollectionReturns() ) {
//			throw new HibernateException( "This strategy does not support root collection returns" );
//		}
//
//		final CollectionReturn collectionReturn = new CollectionReturnImpl( collectionDefinition, querySpaces );
//		pushToCollectionStack( collectionReturn );
//		addRootReturn( collectionReturn );
//
//		associationKeyRegistered(
//				new AssociationKey(
//						( (Joinable) collectionDefinition.getCollectionDescriptor() ).getTableName(),
//						( (Joinable) collectionDefinition.getCollectionDescriptor() ).getKeyColumnNames()
//				)
//		);
//	}
//
//	protected boolean supportsRootCollectionReturns() {
//		return true;
//	}
//
//	@Override
//	public void finishingCollection(CollectionDefinition collectionDefinition) {
//		final boolean isRoot = fetchSourceStack.isEmpty() && collectionReferenceStack.size() == 1;
//		if ( !isRoot ) {
//			// if not, this call should represent a fetch which will be handled in #finishingAttribute
//			return;
//		}
//
//		final CollectionReference popped = popFromCollectionStack();
//		checkedPoppedCollection( popped, collectionDefinition );
//
//		log.tracef(
//				"%s Finished root collection : %s",
//				StringHelper.repeat( "<<", fetchSourceStack.size() ),
//				collectionDefinition.getCollectionDescriptor().getRole()
//		);
//	}
//
//	private void checkedPoppedCollection(CollectionReference poppedCollectionReference, CollectionDefinition collectionDefinition) {
//		// make sure what we just poppedCollectionReference represents collectionDefinition.
//		if ( ! poppedCollectionReference.getCollectionDescriptor().equals( collectionDefinition.getCollectionDescriptor() ) ) {
//			throw new WalkingException( "Mismatched CollectionReference from stack on pop" );
//		}
//	}
//
//	@Override
//	public void startingCollectionIndex(CollectionIndexDefinition indexDefinition) {
//		final Type indexType = indexDefinition.getType();
//		log.tracef(
//				"%s Starting collection index graph : %s",
//				StringHelper.repeat( ">>", fetchSourceStack.size() ),
//				indexDefinition.getCollectionDefinition().getCollectionDescriptor().getRole()
//		);
//
//		final CollectionReference collectionReference = currentCollection();
//		final CollectionFetchableIndex indexGraph = collectionReference.getIndexGraph();
//
//		if ( indexType.getClassification().equals( Type.Classification.ENTITY ) || indexType.isComponentType() ) {
//			if ( indexGraph == null ) {
//				throw new WalkingException(
//						"CollectionReference did not return an expected index graph : " +
//								indexDefinition.getCollectionDefinition().getCollectionDescriptor().getRole()
//				);
//			}
//			if ( !indexType.getClassification().equals( Type.Classification.ANY ) ) {
//				pushToStack( (ExpandingFetchSource) indexGraph );
//			}
//		}
//		else {
//			if ( indexGraph != null ) {
//				throw new WalkingException(
//						"CollectionReference returned an unexpected index graph : " +
//								indexDefinition.getCollectionDefinition().getCollectionDescriptor().getRole()
//				);
//			}
//		}
//	}
//
//	@Override
//	public void finishingCollectionIndex(CollectionIndexDefinition indexDefinition) {
//		final Type indexType = indexDefinition.getType();
//
//		if ( indexType.getClassification().equals( Type.Classification.ANY ) ) {
//			// nothing to do because the index graph was not pushed in #startingCollectionIndex.
//		}
//		else if ( indexType.getClassification().equals( Type.Classification.ENTITY ) || indexType.isComponentType() ) {
//			// todo : validate the stack?
//			final ExpandingFetchSource fetchSource = popFromStack();
//			if ( !CollectionFetchableIndex.class.isInstance( fetchSource ) ) {
//				throw new WalkingException(
//						"CollectionReference did not return an expected index graph : " +
//								indexDefinition.getCollectionDefinition().getCollectionDescriptor().getRole()
//				);
//			}
//		}
//
//		log.tracef(
//				"%s Finished collection index graph : %s",
//				StringHelper.repeat( "<<", fetchSourceStack.size() ),
//				indexDefinition.getCollectionDefinition().getCollectionDescriptor().getRole()
//		);
//	}
//
//	@Override
//	public void startingCollectionElements(CollectionElementDefinition elementDefinition) {
//		final Type elementType = elementDefinition.getType();
//		log.tracef(
//				"%s Starting collection element graph : %s",
//				StringHelper.repeat( ">>", fetchSourceStack.size() ),
//				elementDefinition.getCollectionDefinition().getCollectionDescriptor().getRole()
//		);
//
//		final CollectionReference collectionReference = currentCollection();
//		final CollectionFetchableElement elementGraph = collectionReference.getElementGraph();
//
//		if ( elementType.isAssociationType() || elementType.isComponentType() ) {
//			if ( elementGraph == null ) {
//				throw new IllegalStateException(
//						"CollectionReference did not return an expected element graph : " +
//								elementDefinition.getCollectionDefinition().getCollectionDescriptor().getRole()
//				);
//			}
//			if ( !elementType.getClassification().equals( Type.Classification.ANY ) ) {
//				pushToStack( (ExpandingFetchSource) elementGraph );
//			}
//		}
//		else {
//			if ( elementGraph != null ) {
//				throw new IllegalStateException(
//						"CollectionReference returned an unexpected element graph : " +
//								elementDefinition.getCollectionDefinition().getCollectionDescriptor().getRole()
//				);
//			}
//		}
//	}
//
//	@Override
//	public void finishingCollectionElements(CollectionElementDefinition elementDefinition) {
//		final Type elementType = elementDefinition.getType();
//
//		if ( elementType.getClassification().equals( Type.Classification.ANY ) ) {
//			// nothing to do because the element graph was not pushed in #startingCollectionElement..
//		}
//		else if ( elementType.isComponentType() || elementType.isAssociationType()) {
//			// pop it from the stack
//			final ExpandingFetchSource popped = popFromStack();
//
//			// validation
//			if ( ! CollectionFetchableElement.class.isInstance( popped ) ) {
//				throw new WalkingException( "Mismatched FetchSource from stack on pop" );
//			}
//		}
//
//		log.tracef(
//				"%s Finished collection element graph : %s",
//				StringHelper.repeat( "<<", fetchSourceStack.size() ),
//				elementDefinition.getCollectionDefinition().getCollectionDescriptor().getRole()
//		);
//	}
//
//	@Override
//	public void startingComposite(CompositionDefinition compositionDefinition) {
//		log.tracef(
//				"%s Starting composite : %s",
//				StringHelper.repeat( ">>", fetchSourceStack.size() ),
//				compositionDefinition.getName()
//		);
//
//		if ( fetchSourceStack.isEmpty() && collectionReferenceStack.isEmpty() ) {
//			throw new HibernateException( "A component cannot be the root of a walk nor a graph" );
//		}
//
//		// No need to push anything here; it should have been pushed by
//		// #startingAttribute, #startingCollectionElements, #startingCollectionIndex, or #startingEntityIdentifier
//		final FetchSource currentSource = currentSource();
//		if ( !CompositeFetch.class.isInstance( currentSource ) &&
//				!CollectionFetchableElement.class.isInstance( currentSource ) &&
//				!CollectionFetchableIndex.class.isInstance( currentSource ) &&
//				!ExpandingEntityIdentifierDescription.class.isInstance( currentSource ) ) {
//			throw new WalkingException( "Mismatched FetchSource from stack on pop" );
//		}
//	}
//
//	@Override
//	public void finishingComposite(CompositionDefinition compositionDefinition) {
//		// No need to pop anything here; it will be popped by
//		// #finishingAttribute, #finishingCollectionElements, #finishingCollectionIndex, or #finishingEntityIdentifier
//
//		log.tracef(
//				"%s Finishing composite : %s",
//				StringHelper.repeat( "<<", fetchSourceStack.size() ),
//				compositionDefinition.getName()
//		);
//	}
//
//	protected PropertyPath currentPropertyPath = new PropertyPath( "" );
//
//	@Override
//	public boolean startingAttribute(AttributeDefinition attributeDefinition) {
//		log.tracef(
//				"%s Starting attribute %s",
//				StringHelper.repeat( ">>", fetchSourceStack.size() ),
//				attributeDefinition
//		);
//
//		final Type attributeType = attributeDefinition.getType();
//
//		final boolean isComponentType = attributeType.isComponentType();
//		final boolean isAssociationType = attributeType.isAssociationType();
//		final boolean isBasicType = ! ( isComponentType || isAssociationType );
//		currentPropertyPath = currentPropertyPath.append( attributeDefinition.getName() );
//		if ( isBasicType ) {
//			return true;
//		}
//		else if ( isAssociationType ) {
//			// also handles any type attributes...
//			return handleAssociationAttribute( (AssociationAttributeDefinition) attributeDefinition );
//		}
//		else {
//			return handleCompositeAttribute( attributeDefinition );
//		}
//	}
//
//	@Override
//	public void finishingAttribute(AttributeDefinition attributeDefinition) {
//		final Type attributeType = attributeDefinition.getType();
//
//		if ( attributeType.isAssociationType() ) {
//			final AssociationAttributeDefinition associationAttributeDefinition =
//					(AssociationAttributeDefinition) attributeDefinition;
//			if ( attributeType.getClassification().equals( Type.Classification.ANY ) ) {
//				// Nothing to do because AnyFetch does not implement ExpandingFetchSource (i.e., it cannot be pushed/popped).
//			}
//			else if ( attributeType.getClassification().equals( Type.Classification.ENTITY ) ) {
//				final ExpandingFetchSource source = currentSource();
//				// One way to find out if the fetch was pushed is to check the fetch strategy; rather than recomputing
//				// the fetch strategy, simply check if current source's fetched attribute definition matches
//				// associationAttributeDefinition.
//				if ( AttributeFetch.class.isInstance( source ) &&
//						associationAttributeDefinition.equals( AttributeFetch.class.cast( source ).getFetchedAttributeDefinition() ) ) {
//					final ExpandingFetchSource popped = popFromStack();
//					checkPoppedEntity( popped, associationAttributeDefinition.toEntityDefinition() );
//				}
//			}
//			else if ( attributeType.getClassification().equals( Type.Classification.COLLECTION ) ) {
//				final CollectionReference currentCollection = currentCollection();
//				// One way to find out if the fetch was pushed is to check the fetch strategy; rather than recomputing
//				// the fetch strategy, simply check if current collection's fetched attribute definition matches
//				// associationAttributeDefinition.
//				if ( AttributeFetch.class.isInstance( currentCollection ) &&
//						associationAttributeDefinition.equals( AttributeFetch.class.cast( currentCollection ).getFetchedAttributeDefinition() ) ) {
//					final CollectionReference popped = popFromCollectionStack();
//					checkedPoppedCollection( popped, associationAttributeDefinition.toCollectionDefinition() );
//				}
//			}
//		}
//		else if ( attributeType.isComponentType() ) {
//			// CompositeFetch is always pushed, during #startingAttribute(),
//			// so pop the current fetch owner, and make sure what we just popped represents this composition
//			final ExpandingFetchSource popped = popFromStack();
//			if ( !CompositeAttributeFetch.class.isInstance( popped ) ) {
//				throw new WalkingException(
//						String.format(
//								"Mismatched FetchSource from stack on pop; expected: CompositeAttributeFetch; actual: [%s]",
//								popped
//						)
//				);
//			}
//			final CompositeAttributeFetch poppedAsCompositeAttributeFetch = (CompositeAttributeFetch) popped;
//			if ( !attributeDefinition.equals( poppedAsCompositeAttributeFetch.getFetchedAttributeDefinition() ) ) {
//				throw new WalkingException(
//						String.format(
//								"Mismatched CompositeAttributeFetch from stack on pop; expected fetch for attribute: [%s]; actual: [%s]",
//								attributeDefinition,
//								poppedAsCompositeAttributeFetch.getFetchedAttributeDefinition()
//						)
//				);
//			}
//		}
//
//		log.tracef(
//				"%s Finishing up attribute : %s",
//				StringHelper.repeat( "<<", fetchSourceStack.size() ),
//				attributeDefinition
//		);
//		currentPropertyPath = currentPropertyPath.getParent();
//	}
//
//	private Map<AssociationKey,FetchSource> fetchedAssociationKeySourceMap = new HashMap<AssociationKey, FetchSource>();
//
//	@Override
//	public boolean isDuplicateAssociationKey(AssociationKey associationKey) {
//		return fetchedAssociationKeySourceMap.containsKey( associationKey );
//	}
//
//	@Override
//	public void associationKeyRegistered(AssociationKey associationKey) {
//		// todo : use this information to maintain a map of AssociationKey->FetchSource mappings (associationKey + current FetchSource stack entry)
//		//		that mapping can then be used in #foundCircularAssociationKey to build the proper BiDirectionalEntityFetch
//		//		based on the mapped owner
//		log.tracef(
//				"%s Registering AssociationKey : %s -> %s",
//				StringHelper.repeat( "..", fetchSourceStack.size() ),
//				associationKey,
//				currentSource()
//		);
//		fetchedAssociationKeySourceMap.put( associationKey, currentSource() );
//	}
//
//	@Override
//	public FetchSource registeredFetchSource(AssociationKey associationKey) {
//		return fetchedAssociationKeySourceMap.get( associationKey );
//	}
//
//	@Override
//	public void foundCircularAssociation(AssociationAttributeDefinition attributeDefinition) {
//		final FetchStrategy fetchStrategy = determineFetchStrategy( attributeDefinition );
//		if ( fetchStrategy.getStyle() != FetchStyle.JOIN ) {
//			return; // nothing to do
//		}
//
//		final AssociationKey associationKey = attributeDefinition.getAssociationKey();
//
//		// go ahead and build the bidirectional fetch
//		if ( attributeDefinition.getAssociationNature() == AssociationAttributeDefinition.AssociationNature.ENTITY ) {
//			final Joinable currentEntityPersister = (Joinable) currentSource().resolveEntityReference().getEntityDescriptor();
//			final AssociationKey currentEntityReferenceAssociationKey =
//					new AssociationKey( currentEntityPersister.getTableName(), currentEntityPersister.getKeyColumnNames() );
//			// if associationKey is equal to currentEntityReferenceAssociationKey
//			// that means that the current EntityPersister has a single primary key attribute
//			// (i.e., derived attribute) which is mapped by attributeDefinition.
//			// This is not a bidirectional association.
//			// TODO: AFAICT, to avoid an overflow, the associated entity must already be loaded into the session, or
//			// it must be loaded when the ID for the dependent entity is resolved. Is there some other way to
//			// deal with this???
//			final FetchSource registeredFetchSource = registeredFetchSource( associationKey );
//			if ( registeredFetchSource != null && ! associationKey.equals( currentEntityReferenceAssociationKey ) ) {
//				currentSource().buildBidirectionalEntityReference(
//						attributeDefinition,
//						fetchStrategy,
//						registeredFetchSource( associationKey ).resolveEntityReference()
//				);
//			}
//		}
//		else {
//			// Do nothing for collection
//		}
//	}
//
//// TODO: is the following still useful???
////	@Override
////	public void foundCircularAssociationKey(AssociationKey associationKey, AttributeDefinition attributeDefinition) {
////		// use this information to create the bi-directional EntityReference (as EntityFetch) instances
////		final FetchSource owningFetchSource = fetchedAssociationKeySourceMap.get( associationKey );
////		if ( owningFetchSource == null ) {
////			throw new IllegalStateException(
////					String.format(
////							"Expecting AssociationKey->FetchSource mapping for %s",
////							associationKey.toString()
////					)
////			);
////		}
////
////		final FetchSource currentFetchSource = currentSource();
////		( (ExpandingFetchSource) currentFetchSource ).addCircularFetch( new CircularFetch(  ))
////
////		currentFetchOwner().addFetch( new CircularFetch( currentSource(), fetchSource, attributeDefinition ) );
////	}
////
////	public static class CircularFetch implements EntityFetch, EntityReference {
////		private final FetchOwner circularFetchOwner;
////		private final FetchOwner associationOwner;
////		private final AttributeDefinition attributeDefinition;
////
////		private final EntityReference targetEntityReference;
////
////		private final FetchStrategy fetchStrategy = new FetchStrategy(
////				FetchTiming.IMMEDIATE,
////				FetchStyle.JOIN
////		);
////
////		public CircularFetch(FetchOwner circularFetchOwner, FetchOwner associationOwner, AttributeDefinition attributeDefinition) {
////			this.circularFetchOwner = circularFetchOwner;
////			this.associationOwner = associationOwner;
////			this.attributeDefinition = attributeDefinition;
////			this.targetEntityReference = resolveEntityReference( associationOwner );
////		}
////
////		@Override
////		public EntityReference getTargetEntityReference() {
////			return targetEntityReference;
////		}
////
////		protected static EntityReference resolveEntityReference(FetchOwner owner) {
////			if ( EntityReference.class.isInstance( owner ) ) {
////				return (EntityReference) owner;
////			}
////			if ( CompositeFetch.class.isInstance( owner ) ) {
////				return resolveEntityReference( ( (CompositeFetch) owner ).getOwner() );
////			}
////			// todo : what others?
////
////			throw new UnsupportedOperationException(
////					"Unexpected FetchOwner type [" + owner + "] encountered trying to build circular fetch"
////			);
////
////		}
////
////		@Override
////		public FetchOwner getContainer() {
////			return circularFetchOwner;
////		}
////
////		@Override
////		public PropertyPath getNavigablePath() {
////			return null;  //To change body of implemented methods use File | Settings | File Templates.
////		}
////
////		@Override
////		public Type getFetchedType() {
////			return attributeDefinition.getType();
////		}
////
////		@Override
////		public FetchStrategy getMappedFetchStrategy() {
////			return fetchStrategy;
////		}
////
////		@Override
////		public boolean isNullable() {
////			return attributeDefinition.isNullable();
////		}
////
////		@Override
////		public String getAdditionalJoinConditions() {
////			return null;
////		}
////
////		@Override
////		public String[] toSqlSelectFragments(String alias) {
////			return new String[0];
////		}
////
////		@Override
////		public Fetch makeCopy(CopyContext copyContext, FetchOwner fetchSourceCopy) {
////			// todo : will need this implemented
////			return null;
////		}
////
////		@Override
////		public LockMode getHibernateFlushMode() {
////			return targetEntityReference.getHibernateFlushMode();
////		}
////
////		@Override
////		public EntityReference getEntityReference() {
////			return targetEntityReference;
////		}
////
////		@Override
////		public EntityPersister getEntityDescriptor() {
////			return targetEntityReference.getEntityDescriptor();
////		}
////
////		@Override
////		public IdentifierDescription getIdentifierDescription() {
////			return targetEntityReference.getIdentifierDescription();
////		}
////
////		@Override
////		public void injectIdentifierDescription(IdentifierDescription identifierDescription) {
////			throw new IllegalStateException( "IdentifierDescription should never be injected from circular fetch side" );
////		}
////	}
//
//	@Override
//	public void foundAny(AnyMappingDefinition anyDefinition) {
//		// do nothing.
//	}
//
//	protected boolean handleCompositeAttribute(AttributeDefinition attributeDefinition) {
//		final CompositeFetch compositeFetch = currentSource().buildCompositeAttributeFetch( attributeDefinition );
//		pushToStack( (ExpandingFetchSource) compositeFetch );
//		return true;
//	}
//
//	protected boolean handleAssociationAttribute(AssociationAttributeDefinition attributeDefinition) {
//		// todo : this seems to not be correct for one-to-one
//		final FetchStrategy fetchStrategy = determineFetchStrategy( attributeDefinition );
//
//		final ExpandingFetchSource currentSource = currentSource();
//		currentSource.validateFetchPlan( fetchStrategy, attributeDefinition );
//
//		final AssociationAttributeDefinition.AssociationNature nature = attributeDefinition.getAssociationNature();
//		if ( nature == AssociationAttributeDefinition.AssociationNature.ANY ) {
//			// for ANY mappings we need to build a Fetch:
//			//		1) fetch type is SELECT
//			//		2) (because the fetch cannot be a JOIN...) do not push it to the stack
//			// regardless of the fetch style, build the fetch
//			currentSource.buildAnyAttributeFetch(
//					attributeDefinition,
//					fetchStrategy
//			);
//			return false;
//		}
//		else if ( nature == AssociationAttributeDefinition.AssociationNature.ENTITY ) {
//			// regardless of the fetch style, build the fetch
//			EntityFetch fetch = currentSource.buildEntityAttributeFetch(
//					attributeDefinition,
//					fetchStrategy
//			);
//			if ( FetchStrategyHelper.isJoinFetched( fetchStrategy ) ) {
//				// only push to the stack if join fetched
//				pushToStack( (ExpandingFetchSource) fetch );
//				return true;
//			}
//			else {
//				return false;
//			}
//		}
//		else {
//			// Collection
//			// regardless of the fetch style, build the fetch
//			CollectionAttributeFetch fetch = currentSource.buildCollectionAttributeFetch(
//					attributeDefinition,
//					fetchStrategy
//			);
//			if ( FetchStrategyHelper.isJoinFetched( fetchStrategy ) ) {
//				// only push to the stack if join fetched
//				pushToCollectionStack( fetch );
//				return true;
//			}
//			else {
//				return false;
//			}
//		}
//	}
//
//	protected abstract FetchStrategy determineFetchStrategy(AssociationAttributeDefinition attributeDefinition);
//
//	protected int currentDepth() {
//		return fetchSourceStack.size();
//	}
//
//	protected boolean isTooManyCollections() {
//		return false;
//	}
//

}
