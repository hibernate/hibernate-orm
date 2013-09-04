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
package org.hibernate.loader.plan.spi.build;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.spi.AbstractFetchOwner;
import org.hibernate.loader.plan.spi.AnyFetch;
import org.hibernate.loader.plan.spi.BidirectionalEntityFetch;
import org.hibernate.loader.plan.spi.CollectionFetch;
import org.hibernate.loader.plan.spi.CollectionReference;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.CompositeFetch;
import org.hibernate.loader.plan.spi.CopyContext;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityPersisterBasedSqlSelectFragmentResolver;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.plan.spi.FetchOwner;
import org.hibernate.loader.plan.spi.IdentifierDescription;
import org.hibernate.loader.plan.spi.KeyManyToOneBidirectionalEntityFetch;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.loader.plan.spi.SqlSelectFragmentResolver;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.spi.HydratedCompoundValueHandler;
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
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractLoadPlanBuilderStrategy implements LoadPlanBuilderStrategy, LoadPlanBuildingContext {
	private static final Logger log = Logger.getLogger( AbstractLoadPlanBuilderStrategy.class );
	private static final String MDC_KEY = "hibernateLoadPlanWalkPath";

	private final SessionFactoryImplementor sessionFactory;

	private ArrayDeque<FetchOwner> fetchOwnerStack = new ArrayDeque<FetchOwner>();
	private ArrayDeque<CollectionReference> collectionReferenceStack = new ArrayDeque<CollectionReference>();

	protected AbstractLoadPlanBuilderStrategy(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

	protected FetchOwner currentFetchOwner() {
		return fetchOwnerStack.peekFirst();
	}

	@Override
	public void start() {
		if ( ! fetchOwnerStack.isEmpty() ) {
			throw new WalkingException(
					"Fetch owner stack was not empty on start; " +
							"be sure to not use LoadPlanBuilderStrategy instances concurrently"
			);
		}
		if ( ! collectionReferenceStack.isEmpty() ) {
			throw new WalkingException(
					"Collection reference stack was not empty on start; " +
							"be sure to not use LoadPlanBuilderStrategy instances concurrently"
			);
		}
		MDC.put( MDC_KEY, new MDCStack() );
	}

	@Override
	public void finish() {
		MDC.remove( MDC_KEY );
		fetchOwnerStack.clear();
		collectionReferenceStack.clear();
	}

	@Override
	public void startingEntity(EntityDefinition entityDefinition) {
		log.tracef(
				"%s Starting entity : %s",
				StringHelper.repeat( ">>", fetchOwnerStack.size() ),
				entityDefinition.getEntityPersister().getEntityName()
		);

		if ( fetchOwnerStack.isEmpty() ) {
			// this is a root...
			if ( ! supportsRootEntityReturns() ) {
				throw new HibernateException( "This strategy does not support root entity returns" );
			}
			final EntityReturn entityReturn = buildRootEntityReturn( entityDefinition );
			addRootReturn( entityReturn );
			pushToStack( entityReturn );
		}
		// otherwise this call should represent a fetch which should have been handled in #startingAttribute
	}

	protected boolean supportsRootEntityReturns() {
		return false;
	}

	protected abstract void addRootReturn(Return rootReturn);

	@Override
	public void finishingEntity(EntityDefinition entityDefinition) {
		// pop the current fetch owner, and make sure what we just popped represents this entity
		final FetchOwner poppedFetchOwner = popFromStack();

		if ( ! EntityReference.class.isInstance( poppedFetchOwner ) ) {
			throw new WalkingException( "Mismatched FetchOwner from stack on pop" );
		}

		final EntityReference entityReference = (EntityReference) poppedFetchOwner;
		// NOTE : this is not the most exhaustive of checks because of hierarchical associations (employee/manager)
		if ( ! entityReference.getEntityPersister().equals( entityDefinition.getEntityPersister() ) ) {
			throw new WalkingException( "Mismatched FetchOwner from stack on pop" );
		}

		log.tracef(
				"%s Finished entity : %s",
				StringHelper.repeat( "<<", fetchOwnerStack.size() ),
				entityDefinition.getEntityPersister().getEntityName()
		);
	}

	@Override
	public void startingEntityIdentifier(EntityIdentifierDefinition entityIdentifierDefinition) {
		log.tracef(
				"%s Starting entity identifier : %s",
				StringHelper.repeat( ">>", fetchOwnerStack.size() ),
				entityIdentifierDefinition.getEntityDefinition().getEntityPersister().getEntityName()
		);

		final EntityReference entityReference = (EntityReference) currentFetchOwner();

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

		final FetchOwner identifierAttributeCollector;
		if ( entityIdentifierDefinition.isEncapsulated() ) {
			identifierAttributeCollector = new EncapsulatedIdentifierAttributeCollector( sessionFactory, entityReference );
		}
		else {
			identifierAttributeCollector = new NonEncapsulatedIdentifierAttributeCollector( sessionFactory, entityReference );
		}
		pushToStack( identifierAttributeCollector );
	}

	@Override
	public void finishingEntityIdentifier(EntityIdentifierDefinition entityIdentifierDefinition) {
		// perform some stack validation on exit, first on the current stack element we want to pop
		{
			final FetchOwner poppedFetchOwner = popFromStack();

			if ( ! AbstractIdentifierAttributeCollector.class.isInstance( poppedFetchOwner ) ) {
				throw new WalkingException( "Unexpected state in FetchOwner stack" );
			}

			final EntityReference entityReference = (EntityReference) poppedFetchOwner;
			if ( ! entityReference.getEntityPersister().equals( entityIdentifierDefinition.getEntityDefinition().getEntityPersister() ) ) {
				throw new WalkingException(
						String.format(
								"Encountered unexpected fetch owner [%s] in stack while processing entity identifier for [%s]",
								entityReference.getEntityPersister().getEntityName(),
								entityIdentifierDefinition.getEntityDefinition().getEntityPersister().getEntityName()
						)
				);
			}
		}

		// and then on the element before it
		{
			final FetchOwner currentFetchOwner = currentFetchOwner();
			if ( ! EntityReference.class.isInstance( currentFetchOwner ) ) {
				throw new WalkingException( "Unexpected state in FetchOwner stack" );
			}
			final EntityReference entityReference = (EntityReference) currentFetchOwner;
			if ( ! entityReference.getEntityPersister().equals( entityIdentifierDefinition.getEntityDefinition().getEntityPersister() ) ) {
				throw new WalkingException(
						String.format(
								"Encountered unexpected fetch owner [%s] in stack while processing entity identifier for [%s]",
								entityReference.getEntityPersister().getEntityName(),
								entityIdentifierDefinition.getEntityDefinition().getEntityPersister().getEntityName()
						)
				);
			}
		}

		log.tracef(
				"%s Finished entity identifier : %s",
				StringHelper.repeat( "<<", fetchOwnerStack.size() ),
				entityIdentifierDefinition.getEntityDefinition().getEntityPersister().getEntityName()
		);
	}

	@Override
	public void startingCollection(CollectionDefinition collectionDefinition) {
		log.tracef(
				"%s Starting collection : %s",
				StringHelper.repeat( ">>", fetchOwnerStack.size() ),
				collectionDefinition.getCollectionPersister().getRole()
		);

		if ( fetchOwnerStack.isEmpty() ) {
			// this is a root...
			if ( ! supportsRootCollectionReturns() ) {
				throw new HibernateException( "This strategy does not support root collection returns" );
			}
			final CollectionReturn collectionReturn = buildRootCollectionReturn( collectionDefinition );
			addRootReturn( collectionReturn );
			pushToCollectionStack( collectionReturn );
		}
	}

	protected boolean supportsRootCollectionReturns() {
		return false;
	}

	@Override
	public void startingCollectionIndex(CollectionIndexDefinition collectionIndexDefinition) {
		final Type indexType = collectionIndexDefinition.getType();
		if ( indexType.isAssociationType() || indexType.isComponentType() ) {
			final CollectionReference collectionReference = collectionReferenceStack.peekFirst();
			final FetchOwner indexGraph = collectionReference.getIndexGraph();
			if ( indexGraph == null ) {
				throw new WalkingException( "Collection reference did not return index handler" );
			}
			pushToStack( indexGraph );
		}
	}

	@Override
	public void finishingCollectionIndex(CollectionIndexDefinition collectionIndexDefinition) {
		// nothing to do here
		// 	- the element graph pushed while starting would be popped in finishing/Entity/finishingComposite
	}

	@Override
	public void startingCollectionElements(CollectionElementDefinition elementDefinition) {
		if ( elementDefinition.getType().isAssociationType() || elementDefinition.getType().isComponentType() ) {
			final CollectionReference collectionReference = collectionReferenceStack.peekFirst();
			final FetchOwner elementGraph = collectionReference.getElementGraph();
			if ( elementGraph == null ) {
				throw new WalkingException( "Collection reference did not return element handler" );
			}
			pushToStack( elementGraph );
		}
	}

	@Override
	public void finishingCollectionElements(CollectionElementDefinition elementDefinition) {
		// nothing to do here
		// 	- the element graph pushed while starting would be popped in finishing/Entity/finishingComposite
	}

//	@Override
//	public void startingCompositeCollectionElement(CompositeCollectionElementDefinition compositeElementDefinition) {
//		log.tracef(
//				"%s Starting composite collection element for (%s)",
//				StringHelper.repeat( ">>", fetchOwnerStack.size() ),
//				compositeElementDefinition.getCollectionDefinition().getCollectionPersister().getRole()
//		);
//	}
//
//	@Override
//	public void finishingCompositeCollectionElement(CompositeCollectionElementDefinition compositeElementDefinition) {
//		// pop the current fetch owner, and make sure what we just popped represents this composition
//		final FetchOwner poppedFetchOwner = popFromStack();
//
//		if ( ! CompositeElementGraph.class.isInstance( poppedFetchOwner ) ) {
//			throw new WalkingException( "Mismatched FetchOwner from stack on pop" );
//		}
//
//		// NOTE : not much else we can really check here atm since on the walking spi side we do not have path
//
//		log.tracef(
//				"%s Finished composite element for  : %s",
//				StringHelper.repeat( "<<", fetchOwnerStack.size() ),
//				compositeElementDefinition.getCollectionDefinition().getCollectionPersister().getRole()
//		);
//	}

	@Override
	public void finishingCollection(CollectionDefinition collectionDefinition) {
		// pop the current fetch owner, and make sure what we just popped represents this collection
		final CollectionReference collectionReference = popFromCollectionStack();
		if ( ! collectionReference.getCollectionPersister().equals( collectionDefinition.getCollectionPersister() ) ) {
			throw new WalkingException( "Mismatched FetchOwner from stack on pop" );
		}

		log.tracef(
				"%s Finished collection : %s",
				StringHelper.repeat( "<<", fetchOwnerStack.size() ),
				collectionDefinition.getCollectionPersister().getRole()
		);
	}

	@Override
	public void startingComposite(CompositionDefinition compositionDefinition) {
		log.tracef(
				"%s Starting composition : %s",
				StringHelper.repeat( ">>", fetchOwnerStack.size() ),
				compositionDefinition.getName()
		);

		if ( fetchOwnerStack.isEmpty() ) {
			throw new HibernateException( "A component cannot be the root of a walk nor a graph" );
		}
	}

	@Override
	public void finishingComposite(CompositionDefinition compositionDefinition) {
		// pop the current fetch owner, and make sure what we just popped represents this composition
		final FetchOwner poppedFetchOwner = popFromStack();

		if ( ! CompositeFetch.class.isInstance( poppedFetchOwner ) ) {
			throw new WalkingException( "Mismatched FetchOwner from stack on pop" );
		}

		// NOTE : not much else we can really check here atm since on the walking spi side we do not have path

		log.tracef(
				"%s Finished composition : %s",
				StringHelper.repeat( "<<", fetchOwnerStack.size() ),
				compositionDefinition.getName()
		);
	}

	@Override
	public boolean startingAttribute(AttributeDefinition attributeDefinition) {
		log.tracef(
				"%s Starting attribute %s",
				StringHelper.repeat( ">>", fetchOwnerStack.size() ),
				attributeDefinition
		);

		final Type attributeType = attributeDefinition.getType();

		final boolean isComponentType = attributeType.isComponentType();
		final boolean isAssociationType = attributeType.isAssociationType();
		final boolean isBasicType = ! ( isComponentType || isAssociationType );

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
				StringHelper.repeat( "<<", fetchOwnerStack.size() ),
				attributeDefinition
		);
	}

	private Map<AssociationKey,FetchOwner> fetchedAssociationKeyOwnerMap = new HashMap<AssociationKey, FetchOwner>();

	@Override
	public boolean isDuplicateAssociationKey(AssociationKey associationKey) {
		return fetchedAssociationKeyOwnerMap.containsKey( associationKey );
	}

	@Override
	public void associationKeyRegistered(AssociationKey associationKey) {
		// todo : use this information to maintain a map of AssociationKey->FetchOwner mappings (associationKey + current fetchOwner stack entry)
		//		that mapping can then be used in #foundCircularAssociationKey to build the proper BiDirectionalEntityFetch
		//		based on the mapped owner
		fetchedAssociationKeyOwnerMap.put( associationKey, currentFetchOwner() );
	}

	@Override
	public void foundCircularAssociation(AssociationAttributeDefinition attributeDefinition) {
		// todo : use this information to create the BiDirectionalEntityFetch instances
		final AssociationKey associationKey = attributeDefinition.getAssociationKey();
		final FetchOwner fetchOwner = fetchedAssociationKeyOwnerMap.get( associationKey );
		if ( fetchOwner == null ) {
			throw new IllegalStateException(
					String.format(
							"Expecting AssociationKey->FetchOwner mapping for %s",
							associationKey.toString()
					)
			);
		}

		currentFetchOwner().addFetch( new CircularFetch( currentFetchOwner(), fetchOwner, attributeDefinition ) );
	}

	public static class CircularFetch implements BidirectionalEntityFetch, EntityReference, Fetch {
		private final FetchOwner circularFetchOwner;
		private final FetchOwner associationOwner;
		private final AttributeDefinition attributeDefinition;

		private final EntityReference targetEntityReference;

		private final FetchStrategy fetchStrategy = new FetchStrategy(
				FetchTiming.IMMEDIATE,
				FetchStyle.JOIN
		);

		public CircularFetch(FetchOwner circularFetchOwner, FetchOwner associationOwner, AttributeDefinition attributeDefinition) {
			this.circularFetchOwner = circularFetchOwner;
			this.associationOwner = associationOwner;
			this.attributeDefinition = attributeDefinition;
			this.targetEntityReference = resolveEntityReference( associationOwner );
		}

		@Override
		public EntityReference getTargetEntityReference() {
			return targetEntityReference;
		}

		protected static EntityReference resolveEntityReference(FetchOwner owner) {
			if ( EntityReference.class.isInstance( owner ) ) {
				return (EntityReference) owner;
			}
			if ( CompositeFetch.class.isInstance( owner ) ) {
				return resolveEntityReference( ( (CompositeFetch) owner ).getOwner() );
			}
			// todo : what others?

			throw new UnsupportedOperationException(
					"Unexpected FetchOwner type [" + owner + "] encountered trying to build circular fetch"
			);

		}

		@Override
		public FetchOwner getOwner() {
			return circularFetchOwner;
		}

		@Override
		public PropertyPath getPropertyPath() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public Type getFetchedType() {
			return attributeDefinition.getType();
		}

		@Override
		public FetchStrategy getFetchStrategy() {
			return fetchStrategy;
		}

		@Override
		public boolean isNullable() {
			return attributeDefinition.isNullable();
		}

		@Override
		public String getAdditionalJoinConditions() {
			return null;
		}

		@Override
		public String[] toSqlSelectFragments(String alias) {
			return new String[0];
		}

		@Override
		public Fetch makeCopy(CopyContext copyContext, FetchOwner fetchOwnerCopy) {
			// todo : will need this implemented
			return null;
		}

		@Override
		public LockMode getLockMode() {
			return targetEntityReference.getLockMode();
		}

		@Override
		public EntityReference getEntityReference() {
			return targetEntityReference;
		}

		@Override
		public EntityPersister getEntityPersister() {
			return targetEntityReference.getEntityPersister();
		}

		@Override
		public IdentifierDescription getIdentifierDescription() {
			return targetEntityReference.getIdentifierDescription();
		}

		@Override
		public void injectIdentifierDescription(IdentifierDescription identifierDescription) {
			throw new IllegalStateException( "IdentifierDescription should never be injected from circular fetch side" );
		}
	}

	@Override
	public void foundAny(AssociationAttributeDefinition attributeDefinition, AnyMappingDefinition anyDefinition) {
		// for ANY mappings we need to build a Fetch:
		//		1) fetch type is SELECT, timing might be IMMEDIATE or DELAYED depending on whether it was defined as lazy
		//		2) (because the fetch cannot be a JOIN...) do not push it to the stack
		final FetchStrategy fetchStrategy = determineFetchPlan( attributeDefinition );

		final FetchOwner fetchOwner = currentFetchOwner();
		fetchOwner.validateFetchPlan( fetchStrategy, attributeDefinition );

		fetchOwner.buildAnyFetch(
				attributeDefinition,
				anyDefinition,
				fetchStrategy,
				this
		);
	}

	protected boolean handleCompositeAttribute(CompositionDefinition attributeDefinition) {
		final FetchOwner fetchOwner = currentFetchOwner();
		final CompositeFetch fetch = fetchOwner.buildCompositeFetch( attributeDefinition, this );
		pushToStack( fetch );
		return true;
	}

	protected boolean handleAssociationAttribute(AssociationAttributeDefinition attributeDefinition) {
		// todo : this seems to not be correct for one-to-one
		final FetchStrategy fetchStrategy = determineFetchPlan( attributeDefinition );
		if ( fetchStrategy.getStyle() != FetchStyle.JOIN ) {
			return false;
		}
//		if ( fetchStrategy.getTiming() != FetchTiming.IMMEDIATE ) {
//			return false;
//		}

		final FetchOwner fetchOwner = currentFetchOwner();
		fetchOwner.validateFetchPlan( fetchStrategy, attributeDefinition );

		final Fetch associationFetch;
		final AssociationAttributeDefinition.AssociationNature nature = attributeDefinition.getAssociationNature();
		if ( nature == AssociationAttributeDefinition.AssociationNature.ANY ) {
			return false;
//			throw new NotYetImplementedException( "AnyType support still in progress" );
		}
		else if ( nature == AssociationAttributeDefinition.AssociationNature.ENTITY ) {
			associationFetch = fetchOwner.buildEntityFetch(
					attributeDefinition,
					fetchStrategy,
					this
			);
		}
		else {
			associationFetch = fetchOwner.buildCollectionFetch( attributeDefinition, fetchStrategy, this );
			pushToCollectionStack( (CollectionReference) associationFetch );
		}

		if ( FetchOwner.class.isInstance( associationFetch) ) {
			pushToStack( (FetchOwner) associationFetch );
		}

		return true;
	}

	protected abstract FetchStrategy determineFetchPlan(AssociationAttributeDefinition attributeDefinition);

	protected int currentDepth() {
		return fetchOwnerStack.size();
	}

	protected boolean isTooManyCollections() {
		return false;
	}

	private void pushToStack(FetchOwner fetchOwner) {
		log.trace( "Pushing fetch owner to stack : " + fetchOwner );
		mdcStack().push( fetchOwner.getPropertyPath() );
		fetchOwnerStack.addFirst( fetchOwner );
	}

	private MDCStack mdcStack() {
		return (MDCStack) MDC.get( MDC_KEY );
	}

	private FetchOwner popFromStack() {
		final FetchOwner last = fetchOwnerStack.removeFirst();
		log.trace( "Popped fetch owner from stack : " + last );
		mdcStack().pop();
		if ( FetchStackAware.class.isInstance( last ) ) {
			( (FetchStackAware) last ).poppedFromStack();
		}
		return last;
	}

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

	protected abstract EntityReturn buildRootEntityReturn(EntityDefinition entityDefinition);

	protected abstract CollectionReturn buildRootCollectionReturn(CollectionDefinition collectionDefinition);



	// LoadPlanBuildingContext impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory();
	}

	public static interface FetchStackAware {
		public void poppedFromStack();
	}

	protected static abstract class AbstractIdentifierAttributeCollector
			extends AbstractFetchOwner
			implements FetchOwner, EntityReference, FetchStackAware {
		protected final EntityReference entityReference;
		private final EntityPersisterBasedSqlSelectFragmentResolver sqlSelectFragmentResolver;
		protected final Map<Fetch,HydratedCompoundValueHandler> fetchToHydratedStateExtractorMap
				= new HashMap<Fetch, HydratedCompoundValueHandler>();

		public AbstractIdentifierAttributeCollector(SessionFactoryImplementor sessionFactory, EntityReference entityReference) {
			super( sessionFactory );
			this.entityReference = entityReference;
			this.sqlSelectFragmentResolver = new EntityPersisterBasedSqlSelectFragmentResolver(
					(Queryable) entityReference.getEntityPersister()
			);
		}

		@Override
		public LockMode getLockMode() {
			return entityReference.getLockMode();
		}

		@Override
		public EntityReference getEntityReference() {
			return this;
		}

		@Override
		public EntityPersister getEntityPersister() {
			return entityReference.getEntityPersister();
		}

		@Override
		public IdentifierDescription getIdentifierDescription() {
			return entityReference.getIdentifierDescription();
		}

		@Override
		public CollectionFetch buildCollectionFetch(
				AssociationAttributeDefinition attributeDefinition,
				FetchStrategy fetchStrategy,
				LoadPlanBuildingContext loadPlanBuildingContext) {
			throw new WalkingException( "Entity identifier cannot contain persistent collections" );
		}

		@Override
		public AnyFetch buildAnyFetch(
				AttributeDefinition attribute,
				AnyMappingDefinition anyDefinition,
				FetchStrategy fetchStrategy,
				LoadPlanBuildingContext loadPlanBuildingContext) {
			throw new WalkingException( "Entity identifier cannot contain ANY type mappings" );
		}

		@Override
		public EntityFetch buildEntityFetch(
				AssociationAttributeDefinition attributeDefinition,
				FetchStrategy fetchStrategy,
				LoadPlanBuildingContext loadPlanBuildingContext) {
			// we have a key-many-to-one
			//
			// IMPL NOTE: we pass ourselves as the FetchOwner which will route the fetch back through our #addFetch
			// 		impl.  We collect them there and later build the IdentifierDescription

			// if `this` is a fetch and its owner is "the same" (bi-directionality) as the attribute to be join fetched
			// we should wrap our FetchOwner as an EntityFetch.  That should solve everything except for the alias
			// context lookups because of the different instances (because of wrapping).  So somehow the consumer of this
			// needs to be able to unwrap it to do the alias lookup, and would have to know to do that.
			//
			//
			// we are processing the EntityReference(Address) identifier.  we come across its key-many-to-one reference
			// to Person.  Now, if EntityReference(Address) is an instance of EntityFetch(Address) there is a strong
			// likelihood that we have a bi-directionality and need to handle that specially.
			//
			// how to best (a) find the bi-directionality and (b) represent that?

			if ( EntityFetch.class.isInstance( entityReference ) ) {
				// we just confirmed that EntityReference(Address) is an instance of EntityFetch(Address),
				final EntityFetch entityFetch = (EntityFetch) entityReference;
				final FetchOwner entityFetchOwner = entityFetch.getOwner();
				// so at this point we need to see if entityFetchOwner and attributeDefinition refer to the
				// "same thing".  "same thing" == "same type" && "same column(s)"?
				//
				// i make assumptions here that that the attribute type is the EntityType, is that always valid?
				final EntityType attributeDefinitionTypeAsEntityType = (EntityType) attributeDefinition.getType();

				final boolean sameType = attributeDefinitionTypeAsEntityType.getAssociatedEntityName().equals(
						entityFetchOwner.retrieveFetchSourcePersister().getEntityName()
				);

				if ( sameType ) {
					// check same columns as well?

					return new KeyManyToOneBidirectionalEntityFetch(
							sessionFactory(),
							//ugh
							LockMode.READ,
							this,
							attributeDefinition,
							(EntityReference) entityFetchOwner,
							fetchStrategy
					);
				}
			}

			final EntityFetch fetch = super.buildEntityFetch( attributeDefinition, fetchStrategy, loadPlanBuildingContext );

			// pretty sure this HydratedCompoundValueExtractor stuff is not needed...
			fetchToHydratedStateExtractorMap.put( fetch, attributeDefinition.getHydratedCompoundValueExtractor() );

			return fetch;
		}


		@Override
		public Type getType(Fetch fetch) {
			return fetch.getFetchedType();
		}

		@Override
		public boolean isNullable(Fetch fetch) {
			return  fetch.isNullable();
		}

		@Override
		public String[] toSqlSelectFragments(Fetch fetch, String alias) {
			return fetch.toSqlSelectFragments( alias );
		}

		@Override
		public SqlSelectFragmentResolver toSqlSelectFragmentResolver() {
			return sqlSelectFragmentResolver;
		}

		@Override
		public void poppedFromStack() {
			final IdentifierDescription identifierDescription = buildIdentifierDescription();
			entityReference.injectIdentifierDescription( identifierDescription );
		}

		protected abstract IdentifierDescription buildIdentifierDescription();

		@Override
		public void validateFetchPlan(FetchStrategy fetchStrategy, AttributeDefinition attributeDefinition) {
			( (FetchOwner) entityReference ).validateFetchPlan( fetchStrategy, attributeDefinition );
		}

		@Override
		public EntityPersister retrieveFetchSourcePersister() {
			return ( (FetchOwner) entityReference ).retrieveFetchSourcePersister();
		}

		@Override
		public void injectIdentifierDescription(IdentifierDescription identifierDescription) {
			throw new WalkingException(
					"IdentifierDescription collector should not get injected with IdentifierDescription"
			);
		}
	}

	protected static class EncapsulatedIdentifierAttributeCollector extends AbstractIdentifierAttributeCollector {
		private final PropertyPath propertyPath;

		public EncapsulatedIdentifierAttributeCollector(
				final SessionFactoryImplementor sessionFactory,
				final EntityReference entityReference) {
			super( sessionFactory, entityReference );
			this.propertyPath = ( (FetchOwner) entityReference ).getPropertyPath();
		}

		@Override
		protected IdentifierDescription buildIdentifierDescription() {
			return new IdentifierDescriptionImpl(
					entityReference,
					getFetches(),
					null
			);
		}

		@Override
		public PropertyPath getPropertyPath() {
			return propertyPath;
		}
	}

	protected static class NonEncapsulatedIdentifierAttributeCollector extends AbstractIdentifierAttributeCollector {
		private final PropertyPath propertyPath;

		public NonEncapsulatedIdentifierAttributeCollector(
				final SessionFactoryImplementor sessionfactory,
				final EntityReference entityReference) {
			super( sessionfactory, entityReference );
			this.propertyPath = ( (FetchOwner) entityReference ).getPropertyPath().append( "<id>" );
		}

		@Override
		protected IdentifierDescription buildIdentifierDescription() {
			return new IdentifierDescriptionImpl(
					entityReference,
					getFetches(),
					fetchToHydratedStateExtractorMap
			);
		}

		@Override
		public PropertyPath getPropertyPath() {
			return propertyPath;
		}
	}

	private static class IdentifierDescriptionImpl implements IdentifierDescription {
		private final EntityReference entityReference;
		private final Fetch[] identifierFetches;
		private final Map<Fetch,HydratedCompoundValueHandler> fetchToHydratedStateExtractorMap;

		private IdentifierDescriptionImpl(
				EntityReference entityReference,
				Fetch[] identifierFetches,
				Map<Fetch, HydratedCompoundValueHandler> fetchToHydratedStateExtractorMap) {
			this.entityReference = entityReference;
			this.identifierFetches = identifierFetches;
			this.fetchToHydratedStateExtractorMap = fetchToHydratedStateExtractorMap;
		}

		@Override
		public Fetch[] getFetches() {
			return identifierFetches;
		}
	}


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
