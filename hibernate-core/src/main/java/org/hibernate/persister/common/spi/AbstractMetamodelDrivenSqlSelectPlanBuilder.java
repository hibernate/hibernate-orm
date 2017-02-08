/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.build.internal.returns.CollectionReturnImpl;
import org.hibernate.loader.plan.build.internal.returns.EntityReturnImpl;
import org.hibernate.loader.plan.build.spi.ExpandingEntityIdentifierDescription;
import org.hibernate.loader.plan.build.spi.ExpandingFetchSource;
import org.hibernate.loader.plan.build.spi.ExpandingQuerySpaces;
import org.hibernate.loader.plan.spi.AttributeFetch;
import org.hibernate.loader.plan.spi.CollectionAttributeFetch;
import org.hibernate.loader.plan.spi.CollectionFetchableElement;
import org.hibernate.loader.plan.spi.CollectionFetchableIndex;
import org.hibernate.loader.plan.spi.CollectionReference;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.CompositeAttributeFetch;
import org.hibernate.loader.plan.spi.CompositeFetch;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.FetchSource;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.persister.collection.spi.CollectionElementBasic;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.spi.IdentifiableTypeImplementor;
import org.hibernate.persister.entity.spi.IdentifierDescriptor;
import org.hibernate.persister.walking.internal.FetchStrategyHelper;
import org.hibernate.persister.walking.spi.AnyMappingDefinition;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AssociationKey;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CollectionDefinition;
import org.hibernate.persister.walking.spi.CollectionElementDefinition;
import org.hibernate.persister.walking.spi.CollectionIndexDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.persister.walking.spi.EntityDefinition;
import org.hibernate.persister.walking.spi.EntityIdentifierDefinition;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.sql.ast.QuerySpec;
import org.hibernate.sql.ast.SelectQuery;
import org.hibernate.sql.ast.from.TableGroup;
import org.hibernate.sql.ast.from.TableSpace;
import org.hibernate.sql.convert.internal.FromClauseIndex;
import org.hibernate.sql.convert.internal.SqlAliasBaseManager;
import org.hibernate.sql.convert.internal.SqlSelectPlanImpl;
import org.hibernate.sql.convert.results.spi.FetchParent;
import org.hibernate.sql.convert.results.spi.ReturnResolutionContext;
import org.hibernate.sql.convert.spi.Callback;
import org.hibernate.sql.convert.spi.SqlSelectPlan;
import org.hibernate.sql.convert.spi.Stack;
import org.hibernate.type.spi.Type;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

/**
 * Abstract SqlSelectPlanBuilder implementation to help implementors.

 * A LoadPlanBuildingAssociationVisitationStrategy is a strategy for building a LoadPlan.
 * LoadPlanBuildingAssociationVisitationStrategy is also a AssociationVisitationStrategy, which is used in
 * conjunction with visiting associations via walking metamodel definitions.
 * <p/>
 * So this strategy defines a AssociationVisitationStrategy that walks the metamodel-defined associations afterQuery
 * which is can then build a LoadPlan based on the visited associations. {@link #determineFetchStrategy} is the
 * main decision point that determines if an association is walked.
 *
 * @author Steve Ebersole
 *
 * @see MetamodelDrivenSqlSelectPlanBuilder
 * @see NavigableVisitationStrategy
 */
public abstract class AbstractMetamodelDrivenSqlSelectPlanBuilder
		implements MetamodelDrivenSqlSelectPlanBuilder, SqlSelectPlanBuildingContext, ReturnResolutionContext {
	private static final Logger log = Logger.getLogger( AbstractMetamodelDrivenSqlSelectPlanBuilder.class );
	private static final String MDC_KEY = "hibernateSqlSelectPlanWalkPath";

	private final SqlSelectPlanBuildingContext buildingContext;
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final LockOptions lockOptions;


	// todo (6.0) : see org.hibernate.sql.convert.spi.SqmSelectToSqlAstConverter.
	// todo (6.0) : would also be good to share as much as possible with SqmSelectToSqlAstConverter

	// essentially this needs to combine the ideas from SqmSelectToSqlAstConverter and the LoadPlan
	// 		builders.  May also need ideas from org.hibernate.sqm.parser.hql.internal.SemanticQueryBuilder
	//		from SQM (QuerySpecProcessingState, stacks, etc)

	// todo (6.0) : do we need a stack of QuerySpecs?

	private final FromClauseIndex fromClauseIndex = new FromClauseIndex();
	private final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();
//	private final Stack<Clause> currentClauseStack = new Stack<>();
//	private final Stack<QuerySpec> querySpecStack = new Stack<>();
//	private final Stack<NavigableSource<?>> navigableSourceStack = new Stack<>();
	private QuerySpec querySpec;
	private TableSpace tableSpace;

	private org.hibernate.sql.convert.results.spi.Return queryReturn;


	private final PropertyPathStack propertyPathStack = new PropertyPathStack();


	private final Stack<FetchParent> fetchParentStack = new Stack<>();
	private final Stack<TableGroup> fetchLhsTableGroupStack = new Stack<>();


	private final ArrayDeque<ExpandingFetchSource> fetchSourceStack = new ArrayDeque<ExpandingFetchSource>();

	/**
	 *
	 * @param buildingContext Parameter object providing access to information needede
	 * 		during plan building
	 * @param loadQueryInfluencers Any options that influence load queries (entity graphs, fetch
	 * 		profiles, filters, etc)
	 * @param lockOptions The requested locking profile
	 */
	public AbstractMetamodelDrivenSqlSelectPlanBuilder(
			SqlSelectPlanBuildingContext buildingContext,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions) {
		this.buildingContext = buildingContext;
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.lockOptions = lockOptions;

		this.querySpec = new QuerySpec();
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

	@Override
	public Callback getCallback() {
		return buildingContext.getCallback();
	}

	@Override
	public SqlSelectPlan buildSqlSelectPlan(NavigableSource rootNavigable) {
		// rootNavigable should be either
		// 		1) an IdentifiableTypeImplementor (Entity or MappedSuperclass)
		// 		2) a CollectionPersister
		assert rootNavigable instanceof IdentifiableTypeImplementor
				|| rootNavigable instanceof CollectionPersister;

		prepareForVisitation();

		try {
			propertyPathStack.push( rootNavigable );

			try {
				final TableGroup tableGroup = rootNavigable.buildTableGroup(
						tableSpace,
						sqlAliasBaseManager,
						fromClauseIndex
				);

				// * resolve SqlSelections
				// * create Return and Fetches


				queryReturn = rootNavigable.generateReturn( this, tableGroup );

				// finally visit any potential fetches
				visitNavigables( rootNavigable, (FetchParent) queryReturn, tableGroup );
			}
			finally {
				propertyPathStack.pop();
			}
		}
		finally {
			visitationComplete();
		}

		return new SqlSelectPlanImpl(
				new SelectQuery( querySpec ),
				Collections.singletonList( queryReturn )
		);
	}

	@Override
	public void prepareForVisitation() {
		if ( !fetchParentStack.isEmpty() || !fetchLhsTableGroupStack.isEmpty() ) {
			throw new IllegalStateException(
					"MetamodelDrivenSqlSelectPlanBuilder [" + this + "] is not in proper state to begin visitation"
			);
		}
	}

	@Override
	public void visitationComplete() {
		propertyPathStack.clear();

		if ( !fetchParentStack.isEmpty() ) {
			log.debug( "fetchParentStack was not empty upon completion of visitation; un-matched push and pop?" );
			fetchParentStack.clear();
		}
		if ( !fetchLhsTableGroupStack.isEmpty() ) {
			log.debug( "fetchLhsTableGroupStack was not empty upon completion of visitation; un-matched push and pop?" );
			fetchLhsTableGroupStack.clear();
		}
	}


	private void visitNavigables(
			NavigableSource navigableSource,
			FetchParent fetchParent,
			TableGroup tableGroup) {
		fetchParentStack.push( fetchParent );
		fetchLhsTableGroupStack.push( tableGroup );

		try {
			navigableSource.visitNavigables( this );
		}
		finally {
			fetchLhsTableGroupStack.pop();
			fetchParentStack.pop();
		}
	}

	interface CompositeEntityIdentifierVisitor {
		void visitKeyAttribute();
		void visitKeyManyToOne();
	}

	public void visitEntityIdentifier(IdentifierDescriptor entityIdentifier) {
		propertyPathStack.push( entityIdentifier );

		try {
			if ( entityIdentifier instanceof NavigableSource ) {

			}
		}
		finally {
			propertyPathStack.pop();
		}
	}

	public void visitCollectionElementBasic(CollectionElementBasic collectionElementBasic) {

	}

	protected abstract void addRootReturn(Return rootReturn);






	@Override
	public ExpandingQuerySpaces getQuerySpaces() {
		return querySpaces;
	}







	// stack management ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private void pushToStack(ExpandingFetchSource fetchSource) {
		log.trace( "Pushing fetch source to stack : " + fetchSource );
		propertyPathStack.push( fetchSource.getPropertyPath() );
		fetchSourceStack.addFirst( fetchSource );
	}

	private ExpandingFetchSource popFromStack() {
		final ExpandingFetchSource last = fetchSourceStack.removeFirst();
		log.trace( "Popped fetch owner from stack : " + last );
		propertyPathStack.pop();
		return last;
	}

	protected ExpandingFetchSource currentSource() {
		return fetchSourceStack.peekFirst();
	}


	// Entity-level AssociationVisitationStrategy hooks ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected boolean supportsRootEntityReturns() {
		return true;
	}

	// Entities  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void startingEntity(EntityDefinition entityDefinition) {
		// see if the EntityDefinition is a root...
		final boolean isRoot = fetchSourceStack.isEmpty();
		if ( ! isRoot ) {
			// if not, this call should represent a fetch which should have been handled in #startingAttribute
			return;
		}

		// if we get here, it is a root

		log.tracef(
				"%s Starting root entity : %s",
				StringHelper.repeat( ">>", fetchSourceStack.size() ),
				entityDefinition.getEntityPersister().getEntityName()
		);

		if ( !supportsRootEntityReturns() ) {
			throw new HibernateException( "This strategy does not support root entity returns" );
		}

		final EntityReturnImpl entityReturn = new EntityReturnImpl( entityDefinition, querySpaces );
		addRootReturn( entityReturn );
		pushToStack( entityReturn );

		// also add an AssociationKey for the root so we can later on recognize circular references back to the root.
		final Joinable entityPersister = (Joinable) entityDefinition.getEntityPersister();
		associationKeyRegistered(
				new AssociationKey( entityPersister.getTableName(), entityPersister.getKeyColumnNames() )
		);
	}

	@Override
	public void finishingEntity(EntityDefinition entityDefinition) {
		// Only process the entityDefinition if it is for the root return.
		final FetchSource currentSource = currentSource();
		final boolean isRoot = EntityReturn.class.isInstance( currentSource ) &&
				entityDefinition.getEntityPersister().equals( EntityReturn.class.cast( currentSource ).getEntityPersister() );
		if ( !isRoot ) {
			// if not, this call should represent a fetch which will be handled in #finishingAttribute
			return;
		}

		// if we get here, it is a root
		final ExpandingFetchSource popped = popFromStack();
		checkPoppedEntity( popped, entityDefinition );

		log.tracef(
				"%s Finished root entity : %s",
				StringHelper.repeat( "<<", fetchSourceStack.size() ),
				entityDefinition.getEntityPersister().getEntityName()
		);
	}

	private void checkPoppedEntity(ExpandingFetchSource fetchSource, EntityDefinition entityDefinition) {
		// make sure what we just fetchSource represents entityDefinition
		if ( ! EntityReference.class.isInstance( fetchSource ) ) {
			throw new WalkingException(
					String.format(
							"Mismatched FetchSource from stack on pop.  Expecting EntityReference(%s), but found %s",
							entityDefinition.getEntityPersister().getEntityName(),
							fetchSource
					)
			);
		}

		final EntityReference entityReference = (EntityReference) fetchSource;
		// NOTE : this is not the most exhaustive of checks because of hierarchical associations (employee/manager)
		if ( ! entityReference.getEntityPersister().equals( entityDefinition.getEntityPersister() ) ) {
			throw new WalkingException( "Mismatched FetchSource from stack on pop" );
		}
	}


	// entity identifiers ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void startingEntityIdentifier(EntityIdentifierDefinition entityIdentifierDefinition) {
		log.tracef(
				"%s Starting entity identifier : %s",
				StringHelper.repeat( ">>", fetchSourceStack.size() ),
				entityIdentifierDefinition.getEntityDefinition().getEntityPersister().getEntityName()
		);

		final EntityReference entityReference = (EntityReference) currentSource();

		// perform some stack validation
		if ( ! entityReference.getEntityPersister().equals( entityIdentifierDefinition.getEntityDefinition().getEntityPersister() ) ) {
			throw new WalkingException(
					String.format(
							"Encountered unexpected fetch owner [%s] in stack while processing entity identifier for [%s]",
							entityReference.getEntityPersister().getEntityName(),
							entityIdentifierDefinition.getEntityDefinition().getEntityPersister().getEntityName()
					)
			);
		}

		if ( ExpandingEntityIdentifierDescription.class.isInstance( entityReference.getIdentifierDescription() ) ) {
			pushToStack( (ExpandingEntityIdentifierDescription) entityReference.getIdentifierDescription() );
		}
	}

	@Override
	public void finishingEntityIdentifier(EntityIdentifierDefinition entityIdentifierDefinition) {
		// only pop from stack if the current source is ExpandingEntityIdentifierDescription..
		final ExpandingFetchSource currentSource = currentSource();
		if ( ! ExpandingEntityIdentifierDescription.class.isInstance( currentSource ) ) {
			// in this case, the current source should be the entity that owns entityIdentifierDefinition
			if ( ! EntityReference.class.isInstance( currentSource ) ) {
				throw new WalkingException( "Unexpected state in FetchSource stack" );
			}
			final EntityReference entityReference = (EntityReference) currentSource;
			if ( entityReference.getEntityPersister().getEntityKeyDefinition() != entityIdentifierDefinition ) {
				throw new WalkingException(
						String.format(
								"Encountered unexpected fetch owner [%s] in stack while processing entity identifier for [%s]",
								entityReference.getEntityPersister().getEntityName(),
								entityIdentifierDefinition.getEntityDefinition().getEntityPersister().getEntityName()
						)
				);
			}
			return;
		}

		// the current source is ExpandingEntityIdentifierDescription...
		final ExpandingEntityIdentifierDescription identifierDescription =
				(ExpandingEntityIdentifierDescription) popFromStack();

		// and then on the node beforeQuery it (which should be the entity that owns the identifier being described)
		final ExpandingFetchSource entitySource = currentSource();
		if ( ! EntityReference.class.isInstance( entitySource ) ) {
			throw new WalkingException( "Unexpected state in FetchSource stack" );
		}
		final EntityReference entityReference = (EntityReference) entitySource;
		if ( entityReference.getIdentifierDescription() != identifierDescription ) {
			throw new WalkingException(
					String.format(
							"Encountered unexpected fetch owner [%s] in stack while processing entity identifier for [%s]",
							entityReference.getEntityPersister().getEntityName(),
							entityIdentifierDefinition.getEntityDefinition().getEntityPersister().getEntityName()
					)
			);
		}

		log.tracef(
				"%s Finished entity identifier : %s",
				StringHelper.repeat( "<<", fetchSourceStack.size() ),
				entityIdentifierDefinition.getEntityDefinition().getEntityPersister().getEntityName()
		);
	}
	// Collections ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private ArrayDeque<CollectionReference> collectionReferenceStack = new ArrayDeque<CollectionReference>();

	private void pushToCollectionStack(CollectionReference collectionReference) {
		log.trace( "Pushing collection reference to stack : " + collectionReference );
		propertyPathStack.push( collectionReference.getPropertyPath() );
		collectionReferenceStack.addFirst( collectionReference );
	}

	private CollectionReference popFromCollectionStack() {
		final CollectionReference last = collectionReferenceStack.removeFirst();
		log.trace( "Popped collection reference from stack : " + last );
		propertyPathStack.pop();
		return last;
	}

	private CollectionReference currentCollection() {
		return collectionReferenceStack.peekFirst();
	}

	@Override
	public void startingCollection(CollectionDefinition collectionDefinition) {
		// see if the EntityDefinition is a root...
		final boolean isRoot = fetchSourceStack.isEmpty();
		if ( ! isRoot ) {
			// if not, this call should represent a fetch which should have been handled in #startingAttribute
			return;
		}

		log.tracef(
				"%s Starting root collection : %s",
				StringHelper.repeat( ">>", fetchSourceStack.size() ),
				collectionDefinition.getCollectionPersister().getRole()
		);

		// if we get here, it is a root
		if ( ! supportsRootCollectionReturns() ) {
			throw new HibernateException( "This strategy does not support root collection returns" );
		}

		final CollectionReturn collectionReturn = new CollectionReturnImpl( collectionDefinition, querySpaces );
		pushToCollectionStack( collectionReturn );
		addRootReturn( collectionReturn );

		associationKeyRegistered(
				new AssociationKey(
						( (Joinable) collectionDefinition.getCollectionPersister() ).getTableName(),
						( (Joinable) collectionDefinition.getCollectionPersister() ).getKeyColumnNames()
				)
		);
	}

	protected boolean supportsRootCollectionReturns() {
		return true;
	}

	@Override
	public void finishingCollection(CollectionDefinition collectionDefinition) {
		final boolean isRoot = fetchSourceStack.isEmpty() && collectionReferenceStack.size() == 1;
		if ( !isRoot ) {
			// if not, this call should represent a fetch which will be handled in #finishingAttribute
			return;
		}

		final CollectionReference popped = popFromCollectionStack();
		checkedPoppedCollection( popped, collectionDefinition );

		log.tracef(
				"%s Finished root collection : %s",
				StringHelper.repeat( "<<", fetchSourceStack.size() ),
				collectionDefinition.getCollectionPersister().getRole()
		);
	}

	private void checkedPoppedCollection(CollectionReference poppedCollectionReference, CollectionDefinition collectionDefinition) {
		// make sure what we just poppedCollectionReference represents collectionDefinition.
		if ( ! poppedCollectionReference.getCollectionPersister().equals( collectionDefinition.getCollectionPersister() ) ) {
			throw new WalkingException( "Mismatched CollectionReference from stack on pop" );
		}
	}

	@Override
	public void startingCollectionIndex(CollectionIndexDefinition indexDefinition) {
		final Type indexType = indexDefinition.getType();
		log.tracef(
				"%s Starting collection index graph : %s",
				StringHelper.repeat( ">>", fetchSourceStack.size() ),
				indexDefinition.getCollectionDefinition().getCollectionPersister().getRole()
		);

		final CollectionReference collectionReference = currentCollection();
		final CollectionFetchableIndex indexGraph = collectionReference.getIndexGraph();

		if ( indexType.getClassification().equals( Type.Classification.ENTITY ) || indexType.isComponentType() ) {
			if ( indexGraph == null ) {
				throw new WalkingException(
						"CollectionReference did not return an expected index graph : " +
								indexDefinition.getCollectionDefinition().getCollectionPersister().getRole()
				);
			}
			if ( !indexType.getClassification().equals( Type.Classification.ANY ) ) {
				pushToStack( (ExpandingFetchSource) indexGraph );
			}
		}
		else {
			if ( indexGraph != null ) {
				throw new WalkingException(
						"CollectionReference returned an unexpected index graph : " +
								indexDefinition.getCollectionDefinition().getCollectionPersister().getRole()
				);
			}
		}
	}

	@Override
	public void finishingCollectionIndex(CollectionIndexDefinition indexDefinition) {
		final Type indexType = indexDefinition.getType();

		if ( indexType.getClassification().equals( Type.Classification.ANY ) ) {
			// nothing to do because the index graph was not pushed in #startingCollectionIndex.
		}
		else if ( indexType.getClassification().equals( Type.Classification.ENTITY ) || indexType.isComponentType() ) {
			// todo : validate the stack?
			final ExpandingFetchSource fetchSource = popFromStack();
			if ( !CollectionFetchableIndex.class.isInstance( fetchSource ) ) {
				throw new WalkingException(
						"CollectionReference did not return an expected index graph : " +
								indexDefinition.getCollectionDefinition().getCollectionPersister().getRole()
				);
			}
		}

		log.tracef(
				"%s Finished collection index graph : %s",
				StringHelper.repeat( "<<", fetchSourceStack.size() ),
				indexDefinition.getCollectionDefinition().getCollectionPersister().getRole()
		);
	}

	@Override
	public void startingCollectionElements(CollectionElementDefinition elementDefinition) {
		final Type elementType = elementDefinition.getType();
		log.tracef(
				"%s Starting collection element graph : %s",
				StringHelper.repeat( ">>", fetchSourceStack.size() ),
				elementDefinition.getCollectionDefinition().getCollectionPersister().getRole()
		);

		final CollectionReference collectionReference = currentCollection();
		final CollectionFetchableElement elementGraph = collectionReference.getElementGraph();

		if ( elementType.isAssociationType() || elementType.isComponentType() ) {
			if ( elementGraph == null ) {
				throw new IllegalStateException(
						"CollectionReference did not return an expected element graph : " +
								elementDefinition.getCollectionDefinition().getCollectionPersister().getRole()
				);
			}
			if ( !elementType.getClassification().equals( Type.Classification.ANY ) ) {
				pushToStack( (ExpandingFetchSource) elementGraph );
			}
		}
		else {
			if ( elementGraph != null ) {
				throw new IllegalStateException(
						"CollectionReference returned an unexpected element graph : " +
								elementDefinition.getCollectionDefinition().getCollectionPersister().getRole()
				);
			}
		}
	}

	@Override
	public void finishingCollectionElements(CollectionElementDefinition elementDefinition) {
		final Type elementType = elementDefinition.getType();

		if ( elementType.getClassification().equals( Type.Classification.ANY ) ) {
			// nothing to do because the element graph was not pushed in #startingCollectionElement..
		}
		else if ( elementType.isComponentType() || elementType.isAssociationType()) {
			// pop it from the stack
			final ExpandingFetchSource popped = popFromStack();

			// validation
			if ( ! CollectionFetchableElement.class.isInstance( popped ) ) {
				throw new WalkingException( "Mismatched FetchSource from stack on pop" );
			}
		}

		log.tracef(
				"%s Finished collection element graph : %s",
				StringHelper.repeat( "<<", fetchSourceStack.size() ),
				elementDefinition.getCollectionDefinition().getCollectionPersister().getRole()
		);
	}

	@Override
	public void startingComposite(CompositionDefinition compositionDefinition) {
		log.tracef(
				"%s Starting composite : %s",
				StringHelper.repeat( ">>", fetchSourceStack.size() ),
				compositionDefinition.getName()
		);

		if ( fetchSourceStack.isEmpty() && collectionReferenceStack.isEmpty() ) {
			throw new HibernateException( "A component cannot be the root of a walk nor a graph" );
		}

		// No need to push anything here; it should have been pushed by
		// #startingAttribute, #startingCollectionElements, #startingCollectionIndex, or #startingEntityIdentifier
		final FetchSource currentSource = currentSource();
		if ( !CompositeFetch.class.isInstance( currentSource ) &&
				!CollectionFetchableElement.class.isInstance( currentSource ) &&
				!CollectionFetchableIndex.class.isInstance( currentSource ) &&
				!ExpandingEntityIdentifierDescription.class.isInstance( currentSource ) ) {
			throw new WalkingException( "Mismatched FetchSource from stack on pop" );
		}
	}

	@Override
	public void finishingComposite(CompositionDefinition compositionDefinition) {
		// No need to pop anything here; it will be popped by
		// #finishingAttribute, #finishingCollectionElements, #finishingCollectionIndex, or #finishingEntityIdentifier

		log.tracef(
				"%s Finishing composite : %s",
				StringHelper.repeat( "<<", fetchSourceStack.size() ),
				compositionDefinition.getName()
		);
	}

	protected PropertyPath currentPropertyPath = new PropertyPath( "" );

	@Override
	public boolean startingAttribute(AttributeDefinition attributeDefinition) {
		log.tracef(
				"%s Starting attribute %s",
				StringHelper.repeat( ">>", fetchSourceStack.size() ),
				attributeDefinition
		);

		final Type attributeType = attributeDefinition.getType();

		final boolean isComponentType = attributeType.isComponentType();
		final boolean isAssociationType = attributeType.isAssociationType();
		final boolean isBasicType = ! ( isComponentType || isAssociationType );
		currentPropertyPath = currentPropertyPath.append( attributeDefinition.getName() );
		if ( isBasicType ) {
			return true;
		}
		else if ( isAssociationType ) {
			// also handles any type attributes...
			return handleAssociationAttribute( (AssociationAttributeDefinition) attributeDefinition );
		}
		else {
			return handleCompositeAttribute( attributeDefinition );
		}
	}

	@Override
	public void finishingAttribute(AttributeDefinition attributeDefinition) {
		final Type attributeType = attributeDefinition.getType();

		if ( attributeType.isAssociationType() ) {
			final AssociationAttributeDefinition associationAttributeDefinition =
					(AssociationAttributeDefinition) attributeDefinition;
			if ( attributeType.getClassification().equals( Type.Classification.ANY ) ) {
				// Nothing to do because AnyFetch does not implement ExpandingFetchSource (i.e., it cannot be pushed/popped).
			}
			else if ( attributeType.getClassification().equals( Type.Classification.ENTITY ) ) {
				final ExpandingFetchSource source = currentSource();
				// One way to find out if the fetch was pushed is to check the fetch strategy; rather than recomputing
				// the fetch strategy, simply check if current source's fetched attribute definition matches
				// associationAttributeDefinition.
				if ( AttributeFetch.class.isInstance( source ) &&
						associationAttributeDefinition.equals( AttributeFetch.class.cast( source ).getFetchedAttributeDefinition() ) ) {
					final ExpandingFetchSource popped = popFromStack();
					checkPoppedEntity( popped, associationAttributeDefinition.toEntityDefinition() );
				}
			}
			else if ( attributeType.getClassification().equals( Type.Classification.COLLECTION ) ) {
				final CollectionReference currentCollection = currentCollection();
				// One way to find out if the fetch was pushed is to check the fetch strategy; rather than recomputing
				// the fetch strategy, simply check if current collection's fetched attribute definition matches
				// associationAttributeDefinition.
				if ( AttributeFetch.class.isInstance( currentCollection ) &&
						associationAttributeDefinition.equals( AttributeFetch.class.cast( currentCollection ).getFetchedAttributeDefinition() ) ) {
					final CollectionReference popped = popFromCollectionStack();
					checkedPoppedCollection( popped, associationAttributeDefinition.toCollectionDefinition() );
				}
			}
		}
		else if ( attributeType.isComponentType() ) {
			// CompositeFetch is always pushed, during #startingAttribute(),
			// so pop the current fetch owner, and make sure what we just popped represents this composition
			final ExpandingFetchSource popped = popFromStack();
			if ( !CompositeAttributeFetch.class.isInstance( popped ) ) {
				throw new WalkingException(
						String.format(
								"Mismatched FetchSource from stack on pop; expected: CompositeAttributeFetch; actual: [%s]",
								popped
						)
				);
			}
			final CompositeAttributeFetch poppedAsCompositeAttributeFetch = (CompositeAttributeFetch) popped;
			if ( !attributeDefinition.equals( poppedAsCompositeAttributeFetch.getFetchedAttributeDefinition() ) ) {
				throw new WalkingException(
						String.format(
								"Mismatched CompositeAttributeFetch from stack on pop; expected fetch for attribute: [%s]; actual: [%s]",
								attributeDefinition,
								poppedAsCompositeAttributeFetch.getFetchedAttributeDefinition()
						)
				);
			}
		}

		log.tracef(
				"%s Finishing up attribute : %s",
				StringHelper.repeat( "<<", fetchSourceStack.size() ),
				attributeDefinition
		);
		currentPropertyPath = currentPropertyPath.getParent();
	}

	private Map<AssociationKey,FetchSource> fetchedAssociationKeySourceMap = new HashMap<AssociationKey, FetchSource>();

	@Override
	public boolean isDuplicateAssociationKey(AssociationKey associationKey) {
		return fetchedAssociationKeySourceMap.containsKey( associationKey );
	}

	@Override
	public void associationKeyRegistered(AssociationKey associationKey) {
		// todo : use this information to maintain a map of AssociationKey->FetchSource mappings (associationKey + current FetchSource stack entry)
		//		that mapping can then be used in #foundCircularAssociationKey to build the proper BiDirectionalEntityFetch
		//		based on the mapped owner
		log.tracef(
				"%s Registering AssociationKey : %s -> %s",
				StringHelper.repeat( "..", fetchSourceStack.size() ),
				associationKey,
				currentSource()
		);
		fetchedAssociationKeySourceMap.put( associationKey, currentSource() );
	}

	@Override
	public FetchSource registeredFetchSource(AssociationKey associationKey) {
		return fetchedAssociationKeySourceMap.get( associationKey );
	}

	@Override
	public void foundCircularAssociation(AssociationAttributeDefinition attributeDefinition) {
		final FetchStrategy fetchStrategy = determineFetchStrategy( attributeDefinition );
		if ( fetchStrategy.getStyle() != FetchStyle.JOIN ) {
			return; // nothing to do
		}

		final AssociationKey associationKey = attributeDefinition.getAssociationKey();

		// go ahead and build the bidirectional fetch
		if ( attributeDefinition.getAssociationNature() == AssociationAttributeDefinition.AssociationNature.ENTITY ) {
			final Joinable currentEntityPersister = (Joinable) currentSource().resolveEntityReference().getEntityPersister();
			final AssociationKey currentEntityReferenceAssociationKey =
					new AssociationKey( currentEntityPersister.getTableName(), currentEntityPersister.getKeyColumnNames() );
			// if associationKey is equal to currentEntityReferenceAssociationKey
			// that means that the current EntityPersister has a single primary key attribute
			// (i.e., derived attribute) which is mapped by attributeDefinition.
			// This is not a bidirectional association.
			// TODO: AFAICT, to avoid an overflow, the associated entity must already be loaded into the session, or
			// it must be loaded when the ID for the dependent entity is resolved. Is there some other way to
			// deal with this???
			final FetchSource registeredFetchSource = registeredFetchSource( associationKey );
			if ( registeredFetchSource != null && ! associationKey.equals( currentEntityReferenceAssociationKey ) ) {
				currentSource().buildBidirectionalEntityReference(
						attributeDefinition,
						fetchStrategy,
						registeredFetchSource( associationKey ).resolveEntityReference()
				);
			}
		}
		else {
			// Do nothing for collection
		}
	}

// TODO: is the following still useful???
//	@Override
//	public void foundCircularAssociationKey(AssociationKey associationKey, AttributeDefinition attributeDefinition) {
//		// use this information to create the bi-directional EntityReference (as EntityFetch) instances
//		final FetchSource owningFetchSource = fetchedAssociationKeySourceMap.get( associationKey );
//		if ( owningFetchSource == null ) {
//			throw new IllegalStateException(
//					String.format(
//							"Expecting AssociationKey->FetchSource mapping for %s",
//							associationKey.toString()
//					)
//			);
//		}
//
//		final FetchSource currentFetchSource = currentSource();
//		( (ExpandingFetchSource) currentFetchSource ).addCircularFetch( new CircularFetch(  ))
//
//		currentFetchOwner().addFetch( new CircularFetch( currentSource(), fetchSource, attributeDefinition ) );
//	}
//
//	public static class CircularFetch implements EntityFetch, EntityReference {
//		private final FetchOwner circularFetchOwner;
//		private final FetchOwner associationOwner;
//		private final AttributeDefinition attributeDefinition;
//
//		private final EntityReference targetEntityReference;
//
//		private final FetchStrategy fetchStrategy = new FetchStrategy(
//				FetchTiming.IMMEDIATE,
//				FetchStyle.JOIN
//		);
//
//		public CircularFetch(FetchOwner circularFetchOwner, FetchOwner associationOwner, AttributeDefinition attributeDefinition) {
//			this.circularFetchOwner = circularFetchOwner;
//			this.associationOwner = associationOwner;
//			this.attributeDefinition = attributeDefinition;
//			this.targetEntityReference = resolveEntityReference( associationOwner );
//		}
//
//		@Override
//		public EntityReference getTargetEntityReference() {
//			return targetEntityReference;
//		}
//
//		protected static EntityReference resolveEntityReference(FetchOwner owner) {
//			if ( EntityReference.class.isInstance( owner ) ) {
//				return (EntityReference) owner;
//			}
//			if ( CompositeFetch.class.isInstance( owner ) ) {
//				return resolveEntityReference( ( (CompositeFetch) owner ).getOwner() );
//			}
//			// todo : what others?
//
//			throw new UnsupportedOperationException(
//					"Unexpected FetchOwner type [" + owner + "] encountered trying to build circular fetch"
//			);
//
//		}
//
//		@Override
//		public FetchOwner getSource() {
//			return circularFetchOwner;
//		}
//
//		@Override
//		public PropertyPath getPropertyPath() {
//			return null;  //To change body of implemented methods use File | Settings | File Templates.
//		}
//
//		@Override
//		public Type getFetchedType() {
//			return attributeDefinition.getType();
//		}
//
//		@Override
//		public FetchStrategy getFetchStrategy() {
//			return fetchStrategy;
//		}
//
//		@Override
//		public boolean isNullable() {
//			return attributeDefinition.isNullable();
//		}
//
//		@Override
//		public String getAdditionalJoinConditions() {
//			return null;
//		}
//
//		@Override
//		public String[] toSqlSelectFragments(String alias) {
//			return new String[0];
//		}
//
//		@Override
//		public Fetch makeCopy(CopyContext copyContext, FetchOwner fetchSourceCopy) {
//			// todo : will need this implemented
//			return null;
//		}
//
//		@Override
//		public LockMode getHibernateFlushMode() {
//			return targetEntityReference.getHibernateFlushMode();
//		}
//
//		@Override
//		public EntityReference getEntityReference() {
//			return targetEntityReference;
//		}
//
//		@Override
//		public EntityPersister getEntityPersister() {
//			return targetEntityReference.getEntityPersister();
//		}
//
//		@Override
//		public IdentifierDescription getIdentifierDescription() {
//			return targetEntityReference.getIdentifierDescription();
//		}
//
//		@Override
//		public void injectIdentifierDescription(IdentifierDescription identifierDescription) {
//			throw new IllegalStateException( "IdentifierDescription should never be injected from circular fetch side" );
//		}
//	}

	@Override
	public void foundAny(AnyMappingDefinition anyDefinition) {
		// do nothing.
	}

	protected boolean handleCompositeAttribute(AttributeDefinition attributeDefinition) {
		final CompositeFetch compositeFetch = currentSource().buildCompositeAttributeFetch( attributeDefinition );
		pushToStack( (ExpandingFetchSource) compositeFetch );
		return true;
	}

	protected boolean handleAssociationAttribute(AssociationAttributeDefinition attributeDefinition) {
		// todo : this seems to not be correct for one-to-one
		final FetchStrategy fetchStrategy = determineFetchStrategy( attributeDefinition );

		final ExpandingFetchSource currentSource = currentSource();
		currentSource.validateFetchPlan( fetchStrategy, attributeDefinition );

		final AssociationAttributeDefinition.AssociationNature nature = attributeDefinition.getAssociationNature();
		if ( nature == AssociationAttributeDefinition.AssociationNature.ANY ) {
			// for ANY mappings we need to build a Fetch:
			//		1) fetch type is SELECT
			//		2) (because the fetch cannot be a JOIN...) do not push it to the stack
			// regardless of the fetch style, build the fetch
			currentSource.buildAnyAttributeFetch(
					attributeDefinition,
					fetchStrategy
			);
			return false;
		}
		else if ( nature == AssociationAttributeDefinition.AssociationNature.ENTITY ) {
			// regardless of the fetch style, build the fetch
			EntityFetch fetch = currentSource.buildEntityAttributeFetch(
					attributeDefinition,
					fetchStrategy
			);
			if ( FetchStrategyHelper.isJoinFetched( fetchStrategy ) ) {
				// only push to the stack if join fetched
				pushToStack( (ExpandingFetchSource) fetch );
				return true;
			}
			else {
				return false;
			}
		}
		else {
			// Collection
			// regardless of the fetch style, build the fetch
			CollectionAttributeFetch fetch = currentSource.buildCollectionAttributeFetch(
					attributeDefinition,
					fetchStrategy
			);
			if ( FetchStrategyHelper.isJoinFetched( fetchStrategy ) ) {
				// only push to the stack if join fetched
				pushToCollectionStack( fetch );
				return true;
			}
			else {
				return false;
			}
		}
	}

	protected abstract FetchStrategy determineFetchStrategy(AssociationAttributeDefinition attributeDefinition);

	protected int currentDepth() {
		return fetchSourceStack.size();
	}

	protected boolean isTooManyCollections() {
		return false;
	}


	/**
	 * Specialized stack implementation which simultaneously manages the stack of
	 * PropertyPath references as well as the logging MDC value based on the current
	 * PropertyPath node.
	 * <p/>
	 * Due to the recursive calls needed for processing a Navigable graph it is generally
	 * beneficial to see exactly where we are in the graph walking as part of log messages.
	 * This MDC hook provides this capability.
	 */
	public static class PropertyPathStack {
		private ArrayDeque<PropertyPath> internalStack = new ArrayDeque<>();

		public void push(Navigable navigable) {
			assert navigable != null;

			final PropertyPath propertyPath;
			if ( internalStack.isEmpty() ) {
				propertyPath = new PropertyPath( navigable.getNavigableName() );
			}
			else {
				propertyPath = internalStack.peekFirst().append( navigable.getNavigableName() );
			}
			internalStack.addFirst( propertyPath );

			MDC.put( MDC_KEY, propertyPath.getFullPath() );
		}

		public void pop() {
			assert !internalStack.isEmpty();

			internalStack.removeFirst();

			final PropertyPath newHead = internalStack.peekFirst();
			final String mdcRep = newHead == null ? "<no-path>" : newHead.getFullPath();
			MDC.put( MDC_KEY, mdcRep );
		}

		public void clear() {
			MDC.remove( MDC_KEY );

			if ( !internalStack.isEmpty() ) {
				log.debug( "propertyPathStack not empty upon completion of visitation; mis-matched push/pop?" );
				internalStack.clear();
			}
		}
	}
}
