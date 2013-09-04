/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader.plan2.build.spi;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import org.hibernate.HibernateException;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan2.build.internal.spaces.QuerySpacesImpl;
import org.hibernate.loader.plan2.build.internal.returns.CollectionReturnImpl;
import org.hibernate.loader.plan2.build.internal.returns.EntityReturnImpl;
import org.hibernate.loader.plan2.spi.CollectionFetch;
import org.hibernate.loader.plan2.spi.CollectionFetchableElement;
import org.hibernate.loader.plan2.spi.CollectionFetchableIndex;
import org.hibernate.loader.plan2.spi.CollectionReference;
import org.hibernate.loader.plan2.spi.CollectionReturn;
import org.hibernate.loader.plan2.spi.CompositeFetch;
import org.hibernate.loader.plan2.spi.EntityFetch;
import org.hibernate.loader.plan2.spi.EntityIdentifierDescription;
import org.hibernate.loader.plan2.spi.EntityReference;
import org.hibernate.loader.plan2.spi.FetchSource;
import org.hibernate.loader.plan2.spi.Return;
import org.hibernate.persister.entity.Joinable;
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
import org.hibernate.type.Type;

/**
 * A LoadPlanBuilderStrategy is a strategy for building a LoadPlan.  LoadPlanBuilderStrategy is also a
 * AssociationVisitationStrategy, which is used in conjunction with visiting associations via walking
 * metamodel definitions.
 * <p/>
 * So this strategy defines a AssociationVisitationStrategy that walks the metamodel defined associations after
 * which is can then build a LoadPlan based on the visited associations.  {@link #determineFetchStrategy} Is the
 * main decision point
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.loader.plan2.build.spi.LoadPlanBuildingAssociationVisitationStrategy
 * @see org.hibernate.persister.walking.spi.AssociationVisitationStrategy
 */
public abstract class AbstractLoadPlanBuildingAssociationVisitationStrategy
		implements LoadPlanBuildingAssociationVisitationStrategy, LoadPlanBuildingContext {
	private static final Logger log = Logger.getLogger( AbstractLoadPlanBuildingAssociationVisitationStrategy.class );
	private static final String MDC_KEY = "hibernateLoadPlanWalkPath";

	private final SessionFactoryImplementor sessionFactory;
	private final QuerySpacesImpl querySpaces;

	private final ArrayDeque<ExpandingFetchSource> fetchSourceStack = new ArrayDeque<ExpandingFetchSource>();

	protected AbstractLoadPlanBuildingAssociationVisitationStrategy(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		this.querySpaces = new QuerySpacesImpl( sessionFactory );
	}

	public SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

	@Override
	public ExpandingQuerySpaces getQuerySpaces() {
		return querySpaces;
	}


	// stack management ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static interface FetchStackAware {
		public void poppedFromStack();
	}

	private void pushToStack(ExpandingFetchSource fetchSource) {
		log.trace( "Pushing fetch source to stack : " + fetchSource );
		mdcStack().push( fetchSource.getPropertyPath() );
		fetchSourceStack.addFirst( fetchSource );
	}

	private MDCStack mdcStack() {
		return (MDCStack) MDC.get( MDC_KEY );
	}

	private ExpandingFetchSource popFromStack() {
		final ExpandingFetchSource last = fetchSourceStack.removeFirst();
		log.trace( "Popped fetch owner from stack : " + last );
		mdcStack().pop();
		if ( FetchStackAware.class.isInstance( last ) ) {
			( (FetchStackAware) last ).poppedFromStack();
		}

		return last;
	}

	private ExpandingFetchSource currentSource() {
		return fetchSourceStack.peekFirst();
	}

	// top-level AssociationVisitationStrategy hooks ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void start() {
		if ( ! fetchSourceStack.isEmpty() ) {
			throw new WalkingException(
					"Fetch owner stack was not empty on start; " +
							"be sure to not use LoadPlanBuilderStrategy instances concurrently"
			);
		}
		MDC.put( MDC_KEY, new MDCStack() );
	}

	@Override
	public void finish() {
		MDC.remove( MDC_KEY );
		fetchSourceStack.clear();
	}


	protected abstract void addRootReturn(Return rootReturn);


	// Entity-level AssociationVisitationStrategy hooks ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected boolean supportsRootEntityReturns() {
		return true;
	}

	// Entities  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void startingEntity(EntityDefinition entityDefinition) {
		log.tracef(
				"%s Starting entity : %s",
				StringHelper.repeat( ">>", fetchSourceStack.size() ),
				entityDefinition.getEntityPersister().getEntityName()
		);

		// see if the EntityDefinition is a root...
		final boolean isRoot = fetchSourceStack.isEmpty();
		if ( ! isRoot ) {
			// if not, this call should represent a fetch which should have been handled in #startingAttribute
			return;
		}

		// if we get here, it is a root
		if ( !supportsRootEntityReturns() ) {
			throw new HibernateException( "This strategy does not support root entity returns" );
		}

		final EntityReturnImpl entityReturn = new EntityReturnImpl( entityDefinition, this );
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
		// pop the current fetch owner, and make sure what we just popped represents this entity
		final ExpandingFetchSource fetchSource = popFromStack();

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

		log.tracef(
				"%s Finished entity : %s",
				StringHelper.repeat( "<<", fetchSourceStack.size() ),
				entityDefinition.getEntityPersister().getEntityName()
		);
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

		// todo : handle AssociationKeys here?  is that why we get the duplicate joins and fetches?

		if ( ExpandingEntityIdentifierDescription.class.isInstance( entityReference.getIdentifierDescription() ) ) {
			pushToStack( (ExpandingEntityIdentifierDescription) entityReference.getIdentifierDescription() );
		}
	}

	@Override
	public void finishingEntityIdentifier(EntityIdentifierDefinition entityIdentifierDefinition) {
		// peek at the current stack element...
		final ExpandingFetchSource current = currentSource();
		if ( ! EntityIdentifierDescription.class.isInstance( current ) ) {
			return;
		}

		final ExpandingFetchSource popped = popFromStack();

		// perform some stack validation on exit, first on the current stack element we want to pop
		if ( ! ExpandingEntityIdentifierDescription.class.isInstance( popped ) ) {
			throw new WalkingException( "Unexpected state in FetchSource stack" );
		}

		final ExpandingEntityIdentifierDescription identifierDescription = (ExpandingEntityIdentifierDescription) popped;

		// and then on the node before it (which should be the entity that owns the identifier being described)
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
		mdcStack().push( collectionReference.getPropertyPath() );
		collectionReferenceStack.addFirst( collectionReference );
	}

	private CollectionReference popFromCollectionStack() {
		final CollectionReference last = collectionReferenceStack.removeFirst();
		log.trace( "Popped collection reference from stack : " + last );
		mdcStack().pop();
		if ( FetchStackAware.class.isInstance( last ) ) {
			( (FetchStackAware) last ).poppedFromStack();
		}
		return last;
	}

	@Override
	public void startingCollection(CollectionDefinition collectionDefinition) {
		log.tracef(
				"%s Starting collection : %s",
				StringHelper.repeat( ">>", fetchSourceStack.size() ),
				collectionDefinition.getCollectionPersister().getRole()
		);

		// see if the EntityDefinition is a root...
		final boolean isRoot = fetchSourceStack.isEmpty();
		if ( ! isRoot ) {
			// if not, this call should represent a fetch which should have been handled in #startingAttribute
			return;
		}

		// if we get here, it is a root
		if ( ! supportsRootCollectionReturns() ) {
			throw new HibernateException( "This strategy does not support root collection returns" );
		}

		final CollectionReturn collectionReturn = new CollectionReturnImpl( collectionDefinition, this );
		pushToCollectionStack( collectionReturn );
		addRootReturn( collectionReturn );

		// also add an AssociationKey for the root so we can later on recognize circular references back to the root.
		// for a collection, the circularity would always be to an entity element...
		if ( collectionReturn.getElementGraph() != null ) {
			if ( EntityReference.class.isInstance( collectionReturn.getElementGraph() ) ) {
				final EntityReference entityReference = (EntityReference) collectionReturn.getElementGraph();
				final Joinable entityPersister = (Joinable) entityReference.getEntityPersister();
				associationKeyRegistered(
						new AssociationKey( entityPersister.getTableName(), entityPersister.getKeyColumnNames() )
				);
			}
		}
	}

	protected boolean supportsRootCollectionReturns() {
		return true;
	}

	@Override
	public void finishingCollection(CollectionDefinition collectionDefinition) {
		// pop the current fetch owner, and make sure what we just popped represents this collection
		final CollectionReference collectionReference = popFromCollectionStack();
		if ( ! collectionReference.getCollectionPersister().equals( collectionDefinition.getCollectionPersister() ) ) {
			throw new WalkingException( "Mismatched FetchSource from stack on pop" );
		}

		log.tracef(
				"%s Finished collection : %s",
				StringHelper.repeat( "<<", fetchSourceStack.size() ),
				collectionDefinition.getCollectionPersister().getRole()
		);
	}

	@Override
	public void startingCollectionIndex(CollectionIndexDefinition indexDefinition) {
		final Type indexType = indexDefinition.getType();
		if ( indexType.isAnyType() ) {
			throw new WalkingException( "CollectionIndexDefinition reported any-type mapping as map index" );
		}

		log.tracef(
				"%s Starting collection index graph : %s",
				StringHelper.repeat( ">>", fetchSourceStack.size() ),
				indexDefinition.getCollectionDefinition().getCollectionPersister().getRole()
		);

		final CollectionReference collectionReference = collectionReferenceStack.peekFirst();
		final CollectionFetchableIndex indexGraph = collectionReference.getIndexGraph();

		if ( indexType.isEntityType() || indexType.isComponentType() ) {
			if ( indexGraph == null ) {
				throw new WalkingException(
						"CollectionReference did not return an expected index graph : " +
								indexDefinition.getCollectionDefinition().getCollectionPersister().getRole()
				);
			}
			pushToStack( (ExpandingFetchSource) indexGraph );
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
		if ( indexType.isComponentType() ) {
			// todo : validate the stack?
			popFromStack();

		}

		// entity indexes would be popped during finishingEntity processing

		log.tracef(
				"%s Finished collection index graph : %s",
				StringHelper.repeat( "<<", fetchSourceStack.size() ),
				indexDefinition.getCollectionDefinition().getCollectionPersister().getRole()
		);
	}

	@Override
	public void startingCollectionElements(CollectionElementDefinition elementDefinition) {
		final Type elementType = elementDefinition.getType();
		if ( elementType.isAnyType() ) {
			return;
		}

		log.tracef(
				"%s Starting collection element graph : %s",
				StringHelper.repeat( ">>", fetchSourceStack.size() ),
				elementDefinition.getCollectionDefinition().getCollectionPersister().getRole()
		);

		final CollectionReference collectionReference = collectionReferenceStack.peekFirst();
		final CollectionFetchableElement elementGraph = collectionReference.getElementGraph();

		if ( elementType.isAssociationType() || elementType.isComponentType() ) {
			if ( elementGraph == null ) {
				throw new IllegalStateException(
						"CollectionReference did not return an expected element graph : " +
								elementDefinition.getCollectionDefinition().getCollectionPersister().getRole()
				);
			}

			pushToStack( (ExpandingFetchSource) elementGraph );
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
		if ( elementType.isComponentType() ) {
			// pop it from the stack
			final ExpandingFetchSource popped = popFromStack();

			// validation

			if ( ! CollectionFetchableElement.class.isInstance( popped ) ) {
				throw new WalkingException( "Mismatched FetchSource from stack on pop" );
			}
		}

		// entity indexes would be popped during finishingEntity processing

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

		if ( fetchSourceStack.isEmpty() ) {
			throw new HibernateException( "A component cannot be the root of a walk nor a graph" );
		}

		final CompositeFetch compositeFetch = currentSource().buildCompositeFetch( compositionDefinition, this );
		pushToStack( (ExpandingFetchSource) compositeFetch );
	}

	@Override
	public void finishingComposite(CompositionDefinition compositionDefinition) {
		// pop the current fetch owner, and make sure what we just popped represents this composition
		final ExpandingFetchSource popped = popFromStack();

		if ( ! CompositeFetch.class.isInstance( popped ) ) {
			throw new WalkingException( "Mismatched FetchSource from stack on pop" );
		}

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
			return handleAssociationAttribute( (AssociationAttributeDefinition) attributeDefinition );
		}
		else {
			return handleCompositeAttribute( (CompositionDefinition) attributeDefinition );
		}
	}

	@Override
	public void finishingAttribute(AttributeDefinition attributeDefinition) {
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

		// go ahead and build the bidirectional fetch
		if ( attributeDefinition.getAssociationNature() == AssociationAttributeDefinition.AssociationNature.ENTITY ) {
			currentSource().buildEntityFetch(
					attributeDefinition,
					fetchStrategy,
					this
			);
		}
		else {
			// Collection
			currentSource().buildCollectionFetch( attributeDefinition, fetchStrategy, this );
		}
	}

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
//		public LockMode getLockMode() {
//			return targetEntityReference.getLockMode();
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
	public void foundAny(AssociationAttributeDefinition attributeDefinition, AnyMappingDefinition anyDefinition) {
		// for ANY mappings we need to build a Fetch:
		//		1) fetch type is SELECT, timing might be IMMEDIATE or DELAYED depending on whether it was defined as lazy
		//		2) (because the fetch cannot be a JOIN...) do not push it to the stack
		final FetchStrategy fetchStrategy = determineFetchStrategy( attributeDefinition );

//		final FetchOwner fetchSource = currentFetchOwner();
//		fetchOwner.validateFetchPlan( fetchStrategy, attributeDefinition );
//
//		fetchOwner.buildAnyFetch(
//				attributeDefinition,
//				anyDefinition,
//				fetchStrategy,
//				this
//		);
	}

	protected boolean handleCompositeAttribute(CompositionDefinition attributeDefinition) {
//		final ExpandingFetchSource currentSource = currentSource();
//		final CompositeFetch fetch = currentSource.buildCompositeFetch( attributeDefinition, this );
//		pushToStack( (ExpandingFetchSource) fetch );
		return true;
	}

	protected boolean handleAssociationAttribute(AssociationAttributeDefinition attributeDefinition) {
		// todo : this seems to not be correct for one-to-one
		final FetchStrategy fetchStrategy = determineFetchStrategy( attributeDefinition );
		if ( fetchStrategy.getStyle() != FetchStyle.JOIN ) {
			return false;
		}
//		if ( fetchStrategy.getTiming() != FetchTiming.IMMEDIATE ) {
//			return false;
//		}

		final ExpandingFetchSource currentSource = currentSource();
		currentSource.validateFetchPlan( fetchStrategy, attributeDefinition );

		final AssociationAttributeDefinition.AssociationNature nature = attributeDefinition.getAssociationNature();
		if ( nature == AssociationAttributeDefinition.AssociationNature.ANY ) {
			return false;
		}

		if ( nature == AssociationAttributeDefinition.AssociationNature.ENTITY ) {
			EntityFetch fetch = currentSource.buildEntityFetch(
					attributeDefinition,
					fetchStrategy,
					this
			);
			pushToStack( (ExpandingFetchSource) fetch );
		}
		else {
			// Collection
			CollectionFetch fetch = currentSource.buildCollectionFetch( attributeDefinition, fetchStrategy, this );
			pushToCollectionStack( fetch );
		}
		return true;
	}

	protected abstract FetchStrategy determineFetchStrategy(AssociationAttributeDefinition attributeDefinition);

	protected int currentDepth() {
		return fetchSourceStack.size();
	}

	protected boolean isTooManyCollections() {
		return false;
	}

//	protected abstract EntityReturn buildRootEntityReturn(EntityDefinition entityDefinition);
//
//	protected abstract CollectionReturn buildRootCollectionReturn(CollectionDefinition collectionDefinition);



	// LoadPlanBuildingContext impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory();
	}

	/**
	 * Used as the MDC object for logging purposes.  Because of the recursive calls it is often useful (while debugging)
	 * to be able to see the "property path" as part of the logging output.  This class helps fulfill that role
	 * here by acting as the object that gets put into the logging libraries underlying MDC.
	 */
	public static class MDCStack {
		private ArrayDeque<PropertyPath> pathStack = new ArrayDeque<PropertyPath>();

		public void push(PropertyPath path) {
			pathStack.addFirst( path );
		}

		public void pop() {
			pathStack.removeFirst();
		}

		public String toString() {
			final PropertyPath path = pathStack.peekFirst();
			return path == null ? "<no-path>" : path.getFullPath();
		}
	}
}
