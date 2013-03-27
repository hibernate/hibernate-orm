/*
 * jDocBook, processing of DocBook sources
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
package org.hibernate.loader.plan.spi;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.DefaultEntityAliases;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.GeneratedCollectionAliases;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.internal.LoadPlanBuildingHelper;
import org.hibernate.loader.spi.ResultSetProcessingContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.spi.HydratedCompoundValueHandler;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CollectionDefinition;
import org.hibernate.persister.walking.spi.CollectionElementDefinition;
import org.hibernate.persister.walking.spi.CollectionIndexDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.persister.walking.spi.EntityDefinition;
import org.hibernate.persister.walking.spi.EntityIdentifierDefinition;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.type.Type;

import static org.hibernate.loader.spi.ResultSetProcessingContext.IdentifierResolutionContext;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractLoadPlanBuilderStrategy implements LoadPlanBuilderStrategy, LoadPlanBuildingContext {
	private static final Logger log = Logger.getLogger( AbstractLoadPlanBuilderStrategy.class );
	private static final String MDC_KEY = "hibernateLoadPlanWalkPath";

	private final SessionFactoryImplementor sessionFactory;

	private ArrayDeque<FetchOwner> fetchOwnerStack = new ArrayDeque<FetchOwner>();
	private ArrayDeque<CollectionReference> collectionReferenceStack = new ArrayDeque<CollectionReference>();

	protected AbstractLoadPlanBuilderStrategy(SessionFactoryImplementor sessionFactory, int suffixSeed) {
		this.sessionFactory = sessionFactory;
		this.currentSuffixBase = suffixSeed;
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
			identifierAttributeCollector = new EncapsulatedIdentifierAttributeCollector( entityReference );
		}
		else {
			identifierAttributeCollector = new NonEncapsulatedIdentifierAttributeCollector( entityReference );
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
		final boolean isBasicType = ! ( isComponentType || attributeType.isAssociationType() );

		if ( isBasicType ) {
			return true;
		}
		else if ( isComponentType ) {
			return handleCompositeAttribute( (CompositionDefinition) attributeDefinition );
		}
		else {
			return handleAssociationAttribute( (AssociationAttributeDefinition) attributeDefinition );
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

	protected boolean handleCompositeAttribute(CompositionDefinition attributeDefinition) {
		final FetchOwner fetchOwner = currentFetchOwner();
		final CompositeFetch fetch = fetchOwner.buildCompositeFetch( attributeDefinition, this );
		pushToStack( fetch );
		return true;
	}

	protected boolean handleAssociationAttribute(AssociationAttributeDefinition attributeDefinition) {
		final FetchStrategy fetchStrategy = determineFetchPlan( attributeDefinition );
		if ( fetchStrategy.getTiming() != FetchTiming.IMMEDIATE ) {
			return false;
		}

		final FetchOwner fetchOwner = currentFetchOwner();
		fetchOwner.validateFetchPlan( fetchStrategy );

		final Fetch associationFetch;
		if ( attributeDefinition.isCollection() ) {
			associationFetch = fetchOwner.buildCollectionFetch( attributeDefinition, fetchStrategy, this );
		}
		else {
			associationFetch = fetchOwner.buildEntityFetch( attributeDefinition, fetchStrategy, this );
		}

		if ( FetchOwner.class.isInstance( associationFetch ) ) {
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

	private int currentSuffixBase;
	private int implicitAliasUniqueness = 0;

	private String createImplicitAlias() {
		return "ia" + implicitAliasUniqueness++;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory();
	}

	@Override
	public EntityAliases resolveEntityColumnAliases(AssociationAttributeDefinition attributeDefinition) {
		return generateEntityColumnAliases( attributeDefinition.toEntityDefinition().getEntityPersister() );
	}

	protected EntityAliases generateEntityColumnAliases(EntityPersister persister) {
		return new DefaultEntityAliases( (Loadable) persister, Integer.toString( currentSuffixBase++ ) + '_' );
	}

	@Override
	public CollectionAliases resolveCollectionColumnAliases(AssociationAttributeDefinition attributeDefinition) {
		return generateCollectionColumnAliases( attributeDefinition.toCollectionDefinition().getCollectionPersister() );
	}

	protected CollectionAliases generateCollectionColumnAliases(CollectionPersister persister) {
		return new GeneratedCollectionAliases( persister, Integer.toString( currentSuffixBase++ ) + '_' );
	}

	@Override
	public String resolveRootSourceAlias(EntityDefinition definition) {
		return createImplicitAlias();
	}

	@Override
	public String resolveRootSourceAlias(CollectionDefinition definition) {
		return createImplicitAlias();
	}

	@Override
	public String resolveFetchSourceAlias(AssociationAttributeDefinition attributeDefinition) {
		return createImplicitAlias();
	}

	@Override
	public String resolveFetchSourceAlias(CompositionDefinition compositionDefinition) {
		return createImplicitAlias();
	}

	public static interface FetchStackAware {
		public void poppedFromStack();
	}

	protected static abstract class AbstractIdentifierAttributeCollector
			implements FetchOwner, EntityReference, FetchStackAware {

		protected final EntityReference entityReference;
		private final PropertyPath propertyPath;

		protected final List<EntityFetch> identifierFetches = new ArrayList<EntityFetch>();
		protected final Map<EntityFetch,HydratedCompoundValueHandler> fetchToHydratedStateExtractorMap
				= new HashMap<EntityFetch, HydratedCompoundValueHandler>();

		public AbstractIdentifierAttributeCollector(EntityReference entityReference) {
			this.entityReference = entityReference;
			this.propertyPath = ( (FetchOwner) entityReference ).getPropertyPath().append( "<id>" );
		}

		@Override
		public String getAlias() {
			return entityReference.getAlias();
		}

		@Override
		public LockMode getLockMode() {
			return entityReference.getLockMode();
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
		public EntityFetch buildEntityFetch(
				AssociationAttributeDefinition attributeDefinition,
				FetchStrategy fetchStrategy,
				LoadPlanBuildingContext loadPlanBuildingContext) {
			// we have a key-many-to-one
			//
			// IMPL NOTE: we pass ourselves as the FetchOwner which will route the fetch back throw our #addFetch
			// 		impl.  We collect them there and later build the IdentifierDescription
			final EntityFetch fetch = LoadPlanBuildingHelper.buildStandardEntityFetch(
					this,
					attributeDefinition,
					fetchStrategy,
					loadPlanBuildingContext
			);
			fetchToHydratedStateExtractorMap.put( fetch, attributeDefinition.getHydratedCompoundValueExtractor() );

			return fetch;
		}

		@Override
		public CompositeFetch buildCompositeFetch(
				CompositionDefinition attributeDefinition, LoadPlanBuildingContext loadPlanBuildingContext) {
			// nested composition.  Unusual, but not disallowed.
			//
			// IMPL NOTE: we pass ourselves as the FetchOwner which will route the fetch back throw our #addFetch
			// 		impl.  We collect them there and later build the IdentifierDescription
			return LoadPlanBuildingHelper.buildStandardCompositeFetch(
					this,
					attributeDefinition,
					loadPlanBuildingContext
			);
		}

		@Override
		public void poppedFromStack() {
			final IdentifierDescription identifierDescription = buildIdentifierDescription();
			entityReference.injectIdentifierDescription( identifierDescription );
		}

		protected abstract IdentifierDescription buildIdentifierDescription();

		@Override
		public void addFetch(Fetch fetch) {
			identifierFetches.add( (EntityFetch) fetch );
		}

		@Override
		public Fetch[] getFetches() {
			return ( (FetchOwner) entityReference ).getFetches();
		}

		@Override
		public void validateFetchPlan(FetchStrategy fetchStrategy) {
			( (FetchOwner) entityReference ).validateFetchPlan( fetchStrategy );
		}

		@Override
		public EntityPersister retrieveFetchSourcePersister() {
			return ( (FetchOwner) entityReference ).retrieveFetchSourcePersister();
		}

		@Override
		public PropertyPath getPropertyPath() {
			return propertyPath;
		}

		@Override
		public EntityAliases getEntityAliases() {
			return entityReference.getEntityAliases();
		}

		@Override
		public void injectIdentifierDescription(IdentifierDescription identifierDescription) {
			throw new WalkingException(
					"IdentifierDescription collector should not get injected with IdentifierDescription"
			);
		}
	}

	protected static class EncapsulatedIdentifierAttributeCollector extends AbstractIdentifierAttributeCollector {
		public EncapsulatedIdentifierAttributeCollector(EntityReference entityReference) {
			super( entityReference );
		}

		@Override
		protected IdentifierDescription buildIdentifierDescription() {
			return new IdentifierDescriptionImpl(
					entityReference,
					identifierFetches.toArray( new EntityFetch[ identifierFetches.size() ] ),
					null
			);
		}
	}

	protected static class NonEncapsulatedIdentifierAttributeCollector extends AbstractIdentifierAttributeCollector {
		public NonEncapsulatedIdentifierAttributeCollector(EntityReference entityReference) {
			super( entityReference );
		}

		@Override
		protected IdentifierDescription buildIdentifierDescription() {
			return new IdentifierDescriptionImpl(
					entityReference,
					identifierFetches.toArray( new EntityFetch[ identifierFetches.size() ] ),
					fetchToHydratedStateExtractorMap
			);
		}
	}

	private static class IdentifierDescriptionImpl implements IdentifierDescription {
		private final EntityReference entityReference;
		private final EntityFetch[] identifierFetches;
		private final Map<EntityFetch,HydratedCompoundValueHandler> fetchToHydratedStateExtractorMap;

		private IdentifierDescriptionImpl(
				EntityReference entityReference, EntityFetch[] identifierFetches,
				Map<EntityFetch, HydratedCompoundValueHandler> fetchToHydratedStateExtractorMap) {
			this.entityReference = entityReference;
			this.identifierFetches = identifierFetches;
			this.fetchToHydratedStateExtractorMap = fetchToHydratedStateExtractorMap;
		}

		@Override
		public Fetch[] getFetches() {
			return identifierFetches;
		}

		@Override
		public void hydrate(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
			final IdentifierResolutionContext ownerIdentifierResolutionContext =
					context.getIdentifierResolutionContext( entityReference );
			final Object ownerIdentifierHydratedState = ownerIdentifierResolutionContext.getHydratedForm();

			if ( ownerIdentifierHydratedState != null ) {
				for ( EntityFetch fetch : identifierFetches ) {
					final IdentifierResolutionContext identifierResolutionContext =
							context.getIdentifierResolutionContext( fetch );
					// if the identifier was already hydrated, nothing to do
					if ( identifierResolutionContext.getHydratedForm() != null ) {
						continue;
					}

					// try to extract the sub-hydrated value from the owners tuple array
					if ( fetchToHydratedStateExtractorMap != null && ownerIdentifierHydratedState != null ) {
						Serializable extracted = (Serializable) fetchToHydratedStateExtractorMap.get( fetch )
								.extract( ownerIdentifierHydratedState );
						identifierResolutionContext.registerHydratedForm( extracted );
						continue;
					}

					// if we can't, then read from result set
					fetch.hydrate( resultSet, context );
				}
				return;
			}

			final Object hydratedIdentifierState = entityReference.getEntityPersister().getIdentifierType().hydrate(
					resultSet,
					entityReference.getEntityAliases().getSuffixedKeyAliases(),
					context.getSession(),
					null
			);
			context.getIdentifierResolutionContext( entityReference ).registerHydratedForm( hydratedIdentifierState );
		}

		@Override
		public EntityKey resolve(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
			for ( EntityFetch fetch : identifierFetches ) {
				final IdentifierResolutionContext identifierResolutionContext =
						context.getIdentifierResolutionContext( fetch );
				if ( identifierResolutionContext.getEntityKey() != null ) {
					continue;
				}

				EntityKey fetchKey = fetch.resolveInIdentifier( resultSet, context );
				identifierResolutionContext.registerEntityKey( fetchKey );
			}

			final IdentifierResolutionContext ownerIdentifierResolutionContext =
					context.getIdentifierResolutionContext( entityReference );
			Object hydratedState = ownerIdentifierResolutionContext.getHydratedForm();
			Serializable resolvedId = (Serializable) entityReference.getEntityPersister()
					.getIdentifierType()
					.resolve( hydratedState, context.getSession(), null );
			return context.getSession().generateEntityKey( resolvedId, entityReference.getEntityPersister() );
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
