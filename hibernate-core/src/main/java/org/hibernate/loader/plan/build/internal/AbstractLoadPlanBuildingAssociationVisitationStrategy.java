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
package org.hibernate.loader.plan.build.internal;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.build.internal.returns.CollectionReturnImpl;
import org.hibernate.loader.plan.build.internal.returns.EntityReturnImpl;
import org.hibernate.loader.plan.build.internal.spaces.QuerySpacesImpl;
import org.hibernate.loader.plan.build.spi.ExpandingEntityIdentifierDescription;
import org.hibernate.loader.plan.build.spi.ExpandingFetchSource;
import org.hibernate.loader.plan.build.spi.ExpandingQuerySpaces;
import org.hibernate.loader.plan.build.spi.LoadPlanBuildingAssociationVisitationStrategy;
import org.hibernate.loader.plan.build.spi.LoadPlanBuildingContext;
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

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

/**
 * A LoadPlanBuildingAssociationVisitationStrategy is a strategy for building a LoadPlan.
 * LoadPlanBuildingAssociationVisitationStrategy is also a AssociationVisitationStrategy, which is used in
 * conjunction with visiting associations via walking metamodel definitions.
 * <p/>
 * So this strategy defines a AssociationVisitationStrategy that walks the metamodel-defined associations after
 * which is can then build a LoadPlan based on the visited associations. {@link #determineFetchStrategy} is the
 * main decision point that determines if an association is walked.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.loader.plan.build.spi.LoadPlanBuildingAssociationVisitationStrategy
 * @see org.hibernate.persister.walking.spi.AssociationVisitationStrategy
 */
public abstract class AbstractLoadPlanBuildingAssociationVisitationStrategy
		implements LoadPlanBuildingAssociationVisitationStrategy, LoadPlanBuildingContext {
	private static final Logger log = Logger.getLogger( AbstractLoadPlanBuildingAssociationVisitationStrategy.class );
	private static final String MDC_KEY = "hibernateLoadPlanWalkPath";

	private final SessionFactoryImplementor sessionFactory;
	private final QuerySpacesImpl querySpaces;

	private final PropertyPathStack propertyPathStack = new PropertyPathStack();

	private final ArrayDeque<ExpandingFetchSource> fetchSourceStack = new ArrayDeque<ExpandingFetchSource>();

	/**
	 * Constructs an AbstractLoadPlanBuildingAssociationVisitationStrategy.
	 *
	 * @param sessionFactory The session factory.
	 */
	protected AbstractLoadPlanBuildingAssociationVisitationStrategy(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		this.querySpaces = new QuerySpacesImpl( sessionFactory );
	}

	/**
	 * Gets the session factory.
	 *
	 * @return The session factory.
	 */
	protected SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

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

	// top-level AssociationVisitationStrategy hooks ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void start() {
		if ( ! fetchSourceStack.isEmpty() ) {
			throw new WalkingException(
					"Fetch owner stack was not empty on start; " +
							"be sure to not use LoadPlanBuilderStrategy instances concurrently"
			);
		}
		propertyPathStack.push( new PropertyPath() );
	}

	@Override
	public void finish() {
		propertyPathStack.pop();
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

		if ( indexType.isEntityType() || indexType.isComponentType() ) {
			if ( indexGraph == null ) {
				throw new WalkingException(
						"CollectionReference did not return an expected index graph : " +
								indexDefinition.getCollectionDefinition().getCollectionPersister().getRole()
				);
			}
			if ( !indexType.isAnyType() ) {
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

		if ( indexType.isAnyType() ) {
			// nothing to do because the index graph was not pushed in #startingCollectionIndex.
		}
		else if ( indexType.isEntityType() || indexType.isComponentType() ) {
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
			if ( !elementType.isAnyType() ) {
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

		if ( elementType.isAnyType() ) {
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
			if ( attributeType.isAnyType() ) {
				// Nothing to do because AnyFetch does not implement ExpandingFetchSource (i.e., it cannot be pushed/popped).
			}
			else if ( attributeType.isEntityType() ) {
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
			else if ( attributeType.isCollectionType() ) {
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
		if ( fetchStrategy.getTiming() != FetchTiming.IMMEDIATE ) {
			return false;
		}

		final ExpandingFetchSource currentSource = currentSource();
		currentSource.validateFetchPlan( fetchStrategy, attributeDefinition );

		final AssociationAttributeDefinition.AssociationNature nature = attributeDefinition.getAssociationNature();
		if ( nature == AssociationAttributeDefinition.AssociationNature.ANY ) {
			// for ANY mappings we need to build a Fetch:
			//		1) fetch type is SELECT
			//		2) (because the fetch cannot be a JOIN...) do not push it to the stack
			currentSource.buildAnyAttributeFetch(
					attributeDefinition,
					fetchStrategy
			);
			return false;
		}
		else if ( nature == AssociationAttributeDefinition.AssociationNature.ENTITY ) {
			EntityFetch fetch = currentSource.buildEntityAttributeFetch(
					attributeDefinition,
					fetchStrategy
			);
			if ( fetchStrategy.getStyle() == FetchStyle.JOIN ) {
				pushToStack( (ExpandingFetchSource) fetch );
				return true;
			}
			else {
				return false;
			}
		}
		else {
			// Collection
			CollectionAttributeFetch fetch = currentSource.buildCollectionAttributeFetch( attributeDefinition, fetchStrategy );
			if ( fetchStrategy.getStyle() == FetchStyle.JOIN ) {
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

	// LoadPlanBuildingContext impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory();
	}

	/**
	 * Maintains stack information for the property paths we are processing for logging purposes.  Because of the
	 * recursive calls it is often useful (while debugging) to be able to see the "property path" as part of the
	 * logging output.
	 */
	public static class PropertyPathStack {
		private ArrayDeque<PropertyPath> pathStack = new ArrayDeque<PropertyPath>();

		public void push(PropertyPath path) {
			pathStack.addFirst( path );
			MDC.put( MDC_KEY, extractFullPath( path ) );
		}

		private String extractFullPath(PropertyPath path) {
			return path == null ? "<no-path>" : path.getFullPath();
		}

		public void pop() {
			pathStack.removeFirst();
			PropertyPath newHead = pathStack.peekFirst();
			MDC.put( MDC_KEY, extractFullPath( newHead ) );
		}
	}
}
