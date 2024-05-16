/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;

import org.hibernate.FetchNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.WrongClassException;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.spi.HibernateMonitoringEvent;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.ast.internal.CacheEntityLoaderHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.DiscriminatorValueDetails;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UniqueKeyEntry;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.proxy.map.MapProxy;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.basic.BasicResultAssembler;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityLoadingLogging;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;
import org.hibernate.sql.results.graph.internal.AbstractInitializer;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.Type;

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.internal.log.LoggingHelper.toLoggableString;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * @author Andrea Boriero
 */
public class EntityInitializerImpl extends AbstractInitializer implements EntityInitializer {

	// NOTE : even though we only keep the EntityDescriptor here, rather than EntityResultGraphNode
	//		the "scope" of this initializer is a specific EntityReference.
	//
	//		The full EntityResultGraphNode is simply not needed here, and so we just keep
	//		the EntityDescriptor here to avoid chicken/egg issues in the creation of
	// 		these

	private final EntityValuedModelPart referencedModelPart;
	private final EntityPersister entityDescriptor;
	private final EntityPersister rootEntityDescriptor;
	private final NavigablePath navigablePath;
	private final LockMode lockMode;
	private final @Nullable InitializerParent parent;
	private final NotFoundAction notFoundAction;
	private final boolean isPartOfKey;
	private final boolean isResultInitializer;
	private final boolean hasKeyManyToOne;

	private final @Nullable DomainResultAssembler<?> keyAssembler;
	private final @Nullable DomainResultAssembler<?> identifierAssembler;
	private final @Nullable BasicResultAssembler<?> discriminatorAssembler;
	private final @Nullable DomainResultAssembler<?> versionAssembler;
	private final @Nullable DomainResultAssembler<Object> rowIdAssembler;

	private final DomainResultAssembler<?>[][] assemblers;
	private final Initializer[][] subInitializers;

	private boolean shallowCached;

	// per-row state
	private @Nullable EntityPersister concreteDescriptor;
	private @Nullable EntityKey entityKey;
	private @Nullable Object entityInstance;
	private @Nullable Object entityInstanceForNotify;
	private @Nullable EntityHolder entityHolder;

	public EntityInitializerImpl(
			EntityResultGraphNode resultDescriptor,
			LockMode lockMode,
			@Nullable Fetch identifierFetch,
			@Nullable Fetch discriminatorFetch,
			@Nullable DomainResult<?> keyResult,
			@Nullable DomainResult<Object> rowIdResult,
			NotFoundAction notFoundAction,
			@Nullable InitializerParent parent,
			boolean isResultInitializer,
			AssemblerCreationState creationState) {

		referencedModelPart = resultDescriptor.getEntityValuedModelPart();
		entityDescriptor = (EntityPersister) referencedModelPart.getEntityMappingType();

		final String rootEntityName = entityDescriptor.getRootEntityName();
		rootEntityDescriptor = rootEntityName == null || rootEntityName.equals( entityDescriptor.getEntityName() )
				? entityDescriptor
				: entityDescriptor.getRootEntityDescriptor().getEntityPersister();

		this.navigablePath = resultDescriptor.getNavigablePath();
		this.lockMode = lockMode;
		this.parent = parent;
		assert lockMode != null;
		this.isResultInitializer = isResultInitializer;
		this.isPartOfKey = Initializer.isPartOfKey( navigablePath, parent );

		assert identifierFetch != null || isResultInitializer : "Identifier must be fetched, unless this is a result initializer";
		if ( identifierFetch == null ) {
			identifierAssembler = null;
			hasKeyManyToOne = false;
		}
		else {
			identifierAssembler = identifierFetch.createAssembler( (InitializerParent) this, creationState );
			final Initializer initializer = identifierAssembler.getInitializer();
			// For now, assume key many to ones if the identifier has an initializer
			// todo: improve this
			hasKeyManyToOne = initializer != null;
		}

		assert entityDescriptor.hasSubclasses() == (discriminatorFetch != null) : "Discriminator should only be fetched if the entity has subclasses";
		discriminatorAssembler = discriminatorFetch != null
				? (BasicResultAssembler<?>) discriminatorFetch.createAssembler( (InitializerParent) this, creationState )
				: null;

		final EntityVersionMapping versionMapping = entityDescriptor.getVersionMapping();
		if ( versionMapping != null ) {
			final Fetch versionFetch = resultDescriptor.findFetch( versionMapping );
			// If there is a version mapping, there must be a fetch for it
			assert versionFetch != null;
			versionAssembler = versionFetch.createAssembler( (InitializerParent) this, creationState );
		}
		else {
			versionAssembler = null;
		}

		rowIdAssembler = rowIdResult != null
				? rowIdResult.createResultAssembler( (InitializerParent) this, creationState )
				: null;

		final Collection<EntityMappingType> subMappingTypes = rootEntityDescriptor.getSubMappingTypes();
		final DomainResultAssembler<?>[][] assemblers = new DomainResultAssembler[subMappingTypes.size() + 1][];
		final ArrayList<Initializer>[] subInitializers = new ArrayList[subMappingTypes.size() + 1];
		assemblers[rootEntityDescriptor.getSubclassId()] = new DomainResultAssembler[rootEntityDescriptor.getNumberOfFetchables()];

		for ( EntityMappingType subMappingType : subMappingTypes ) {
			assemblers[subMappingType.getSubclassId()] = new DomainResultAssembler[subMappingType.getNumberOfFetchables()];
		}

		final int size = entityDescriptor.getNumberOfFetchables();
		for ( int i = 0; i < size; i++ ) {
			final AttributeMapping attributeMapping = entityDescriptor.getFetchable( i ).asAttributeMapping();
			final Fetch fetch = resultDescriptor.findFetch( attributeMapping );
			final DomainResultAssembler<?> stateAssembler = fetch == null
					? new NullValueAssembler<>( attributeMapping.getMappedType().getMappedJavaType() )
					: fetch.createAssembler( (InitializerParent) this, creationState );

			final int stateArrayPosition = attributeMapping.getStateArrayPosition();
			final EntityMappingType declaringType = attributeMapping.getDeclaringType().asEntityMappingType();
			final int subclassId = declaringType.getSubclassId();

			final Initializer subInitializer = stateAssembler.getInitializer();
			if ( subInitializer != null ) {
				if ( subInitializers[subclassId] == null ) {
					subInitializers[subclassId] = new ArrayList<>();
				}
				subInitializers[subclassId].add( subInitializer );
			}

			assemblers[subclassId][stateArrayPosition] = stateAssembler;
			for ( EntityMappingType subMappingType : declaringType.getSubMappingTypes() ) {
				assemblers[subMappingType.getSubclassId()][stateArrayPosition] = stateAssembler;
				if ( subInitializer != null ) {
					if ( subInitializers[subMappingType.getSubclassId()] == null ) {
						subInitializers[subMappingType.getSubclassId()] = new ArrayList<>();
					}
					subInitializers[subMappingType.getSubclassId()].add( subInitializer );
				}
			}
		}
		final Initializer[][] subInitializersArray = new Initializer[subInitializers.length][];
		for ( int i = 0; i < subInitializers.length; i++ ) {
			final ArrayList<Initializer> subInitializerList = subInitializers[i];
			if ( subInitializerList == null || subInitializerList.isEmpty() ) {
				subInitializersArray[i] = Initializer.EMPTY_ARRAY;
			}
			else {
				subInitializersArray[i] = subInitializerList.toArray( Initializer.EMPTY_ARRAY );
			}
		}
		this.assemblers = assemblers;
		this.subInitializers = subInitializersArray;
		this.notFoundAction = notFoundAction;

		this.keyAssembler = keyResult == null ? null : keyResult.createResultAssembler( (InitializerParent) this, creationState );
	}

	@Override
	public void resolveKey() {
		resolveKey( rowProcessingState, false );
	}

	@Override
	public @Nullable EntityKey resolveEntityKeyOnly(RowProcessingState rowProcessingState) {
		resolveKey( rowProcessingState, true );
		if ( state == State.MISSING ) {
			return null;
		}
		if ( entityKey == null ) {
			assert identifierAssembler != null;
			final Object id = identifierAssembler.assemble( rowProcessingState );
			if ( id == null ) {
				setMissing( rowProcessingState );
				return null;
			}
			resolveEntityKey( rowProcessingState, id );
		}
		return entityKey;
	}

	public void resolveKey(RowProcessingState rowProcessingState, boolean entityKeyOnly) {
		// todo (6.0) : atm we do not handle sequential selects
		// 		- see AbstractEntityPersister#hasSequentialSelect and
		//			AbstractEntityPersister#getSequentialSelect in 5.2
		if ( state != State.UNINITIALIZED ) {
			return;
		}
		state = State.KEY_RESOLVED;

		// reset row state
		concreteDescriptor = null;
		entityKey = null;
		entityInstance = null;
		entityInstanceForNotify = null;
		entityHolder = null;

		if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isTraceEnabled() ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.tracef(
					"(%s) Beginning Initializer#resolveKey process for entity : %s",
					StringHelper.collapse( this.getClass().getName() ),
					getNavigablePath()
			);
		}

		final Object id;
		if ( identifierAssembler == null ) {
			id = rowProcessingState.getEntityId();
			assert id != null : "Initializer requires a not null id for loading";
		}
		else {
			final Initializer initializer = identifierAssembler.getInitializer();
			if ( initializer != null ) {
				initializer.resolveKey();
				if ( initializer.getState() == State.MISSING ) {
					setMissing( rowProcessingState );
					return;
				}
				else {
					concreteDescriptor = determineConcreteEntityDescriptor(
							rowProcessingState,
							discriminatorAssembler,
							entityDescriptor
					);
					assert concreteDescriptor != null;
					if ( hasKeyManyToOne ) {
						if ( !shallowCached && !entityKeyOnly ) {
							resolveKeySubInitializers( rowProcessingState );
						}
						return;
					}
				}
			}
			id = identifierAssembler.assemble( rowProcessingState );
			if ( id == null ) {
				setMissing( rowProcessingState );
				return;
			}
		}

		resolveEntityKey( rowProcessingState, id );
		if ( !entityKeyOnly ) {
			// Resolve the entity instance early as we have no key many-to-one
			resolveInstance();
			if ( !shallowCached ) {
				if ( state == State.INITIALIZED ) {
					if ( entityHolder.getEntityInitializer() == null ) {
						// The entity is already part of the persistence context,
						// so let's figure out the loaded state and only run sub-initializers if necessary
						resolveInstanceSubInitializers( rowProcessingState );
					}
					// If the entity is initialized and getEntityInitializer() == this,
					// we already processed a row for this entity before,
					// but we still have to call resolveKeySubInitializers to activate sub-initializers,
					// because a row might contain data that sub-initializers want to consume
					else {
						// todo: try to diff the eagerness of the sub-initializers to avoid further processing
						resolveKeySubInitializers( rowProcessingState );
					}
				}
				else {
					resolveKeySubInitializers( rowProcessingState );
				}
			}
		}
	}

	protected void resolveInstanceSubInitializers(RowProcessingState rowProcessingState) {
		final Initializer[] initializers = subInitializers[concreteDescriptor.getSubclassId()];
		if ( initializers.length == 0 ) {
			return;
		}
		final EntityEntry entityEntry = rowProcessingState.getSession()
				.getPersistenceContextInternal()
				.getEntry( entityInstanceForNotify );
		final Object[] loadedState = entityEntry.getLoadedState();
		final Object[] state;
		if ( loadedState == null ) {
			if ( entityEntry.getStatus() == Status.READ_ONLY ) {
				state = concreteDescriptor.getValues( entityInstanceForNotify );
			}
			else {
				// This branch is entered when a load happens while a cache entry is assembling.
				// The EntityEntry has the LOADING state, but the loaded state is still empty.
				assert entityEntry.getStatus() == Status.LOADING;
				// Just skip any initialization in this case as the cache entry assembling will take care of it
				return;
			}
		}
		else {
			state = loadedState;
		}
		for ( Initializer initializer : initializers ) {
			final AttributeMapping attribute = initializer.getInitializedPart().asAttributeMapping();
			final Object subInstance = state[attribute.getStateArrayPosition()];
			if ( subInstance == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
				// Go through the normal initializer process
				initializer.resolveKey();
			}
			else {
				initializer.resolveInstance( subInstance );
			}
		}
	}

	private void resolveKeySubInitializers(RowProcessingState rowProcessingState) {
		for ( Initializer initializer : subInitializers[concreteDescriptor.getSubclassId()] ) {
			initializer.resolveKey();
		}
	}

	@EnsuresNonNull( "entityKey" )
	protected void resolveEntityKey(RowProcessingState rowProcessingState, Object id) {
		if ( concreteDescriptor == null ) {
			concreteDescriptor = determineConcreteEntityDescriptor(
					rowProcessingState,
					discriminatorAssembler,
					entityDescriptor
			);
			assert concreteDescriptor != null;
		}
		entityKey = new EntityKey( id, concreteDescriptor );
		if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isDebugEnabled() ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
					"(%s) Hydrated EntityKey (%s): %s",
					getSimpleConcreteImplName(),
					getNavigablePath(),
					entityKey.getIdentifier()
			);
		}
	}

	protected void setMissing(RowProcessingState rowProcessingState) {
		entityKey = null;
		concreteDescriptor = null;
		entityInstance = null;
		entityInstanceForNotify = null;
		entityHolder = null;
		state = State.MISSING;

		// super processes the foreign-key target column.  here we
		// need to also look at the foreign-key value column to check
		// for a dangling foreign-key

		if ( keyAssembler != null ) {
			final Object fkKeyValue = keyAssembler.assemble( rowProcessingState );
			if ( fkKeyValue != null ) {
				if ( notFoundAction != NotFoundAction.IGNORE ) {
					throw new FetchNotFoundException(
							getEntityDescriptor().getEntityName(),
							fkKeyValue
					);
				}
				else {
					EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
							"Ignoring dangling foreign-key due to `@NotFound(IGNORE); association will be null - %s",
							getNavigablePath()
					);
				}
			}
		}
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance) {
		final AttributeMapping attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance = attributeMapping != null
				? attributeMapping.getValue( parentInstance )
				: parentInstance;
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		if ( instance == null ) {
			setMissing( rowProcessingState );
		}
		else {
			entityInstance = instance;
			entityInstanceForNotify = Hibernate.unproxy( instance );
			concreteDescriptor = session.getEntityPersister( null, entityInstanceForNotify );
			resolveEntityKey(
					rowProcessingState,
					concreteDescriptor.getIdentifier( entityInstanceForNotify, session )
			);
			entityHolder = session.getPersistenceContextInternal().getEntityHolder( entityKey );
			state = State.INITIALIZED;
			initializeSubInstancesFromParent( rowProcessingState );
		}
	}

	protected String getSimpleConcreteImplName() {
		return "EntityInitializerImpl";
	}

	@Override
	public boolean isResultInitializer() {
		return isResultInitializer;
	}

	private static void deepCopy(ManagedMappingType containerDescriptor, Object[] source, Object[] target) {
		final int numberOfAttributeMappings = containerDescriptor.getNumberOfAttributeMappings();
		for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
			final AttributeMapping attributeMapping = containerDescriptor.getAttributeMapping( i );
			final AttributeMetadata attributeMetadata = attributeMapping.getAttributeMetadata();
			if ( attributeMetadata.isUpdatable() ) {
				final int position = attributeMapping.getStateArrayPosition();
				target[position] = copy( attributeMetadata, source[position] );
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static Object copy(AttributeMetadata attributeMetadata, Object sourceValue) {
		return sourceValue == LazyPropertyInitializer.UNFETCHED_PROPERTY
				|| sourceValue == PropertyAccessStrategyBackRefImpl.UNKNOWN
				? sourceValue
				: attributeMetadata.getMutabilityPlan().deepCopy( sourceValue );
	}

	@Override
	public ModelPart getInitializedPart() {
		return referencedModelPart;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	protected DomainResultAssembler<?> getIdentifierAssembler() {
		return identifierAssembler;
	}

	@Override
	public EntityPersister getEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public @Nullable EntityKey getEntityKey() {
		return entityKey;
	}

	@Override
	public Object getEntityInstance() {
		return entityInstance;
	}

	protected void setEntityInstance(Object entityInstance) {
		this.entityInstance = entityInstance;
	}

	@Override
	public Object getTargetInstance() {
		return entityInstanceForNotify;
	}

	@Override
	public FetchParentAccess getFetchParentAccess() {
		return (FetchParentAccess) parent;
	}

	@Override
	public @Nullable InitializerParent getParent() {
		return parent;
	}

	@Override
	public void startLoading(RowProcessingState rowProcessingState) {
		if ( rowProcessingState.isQueryCacheHit() && entityDescriptor.useShallowQueryCacheLayout() ) {
			shallowCached = true;
		}
		super.startLoading( rowProcessingState );
	}

	public static @Nullable EntityPersister determineConcreteEntityDescriptor(
			RowProcessingState rowProcessingState,
			BasicResultAssembler<?> discriminatorAssembler,
			EntityPersister entityDescriptor)
			throws WrongClassException {
		if ( discriminatorAssembler == null
				|| rowProcessingState.isQueryCacheHit() && !entityDescriptor.storeDiscriminatorInShallowQueryCacheLayout() ) {
			return entityDescriptor;
		}
		else {
			assert entityDescriptor.hasSubclasses() : "Reading a discriminator from a result set should only happen if the entity has subclasses";
			final EntityDiscriminatorMapping discriminatorMapping = entityDescriptor.getDiscriminatorMapping();
			assert discriminatorMapping != null;
			final Object discriminator = discriminatorAssembler.extractRawValue( rowProcessingState );
			final DiscriminatorValueDetails discriminatorDetails =
					discriminatorMapping.resolveDiscriminatorValue( discriminator );
			if ( discriminatorDetails == null ) {
				assert discriminator == null : "Discriminator details should only be null for null values";
				return null;
			}
			else {
				final EntityMappingType indicatedEntity = discriminatorDetails.getIndicatedEntity();
				if ( indicatedEntity.isTypeOrSuperType( entityDescriptor ) ) {
					return indicatedEntity.getEntityPersister();
				}
				else {
					throw new WrongClassException(
							indicatedEntity.getEntityName(),
							null,
							entityDescriptor.getEntityName(),
							discriminator
					);
				}
			}
		}
	}

	private boolean useEmbeddedIdentifierInstanceAsEntity(Object id, RowProcessingState rowProcessingState) {
		return id != null && isResultInitializer()
				// The id can only be the entity instance if this is a non-aggregated id that has no containing class
				&& entityDescriptor.getIdentifierMapping() instanceof CompositeIdentifierMapping
				&& !( (CompositeIdentifierMapping) entityDescriptor.getIdentifierMapping() ).hasContainingClass()
				&& ( concreteDescriptor = determineConcreteEntityDescriptor( rowProcessingState, discriminatorAssembler, entityDescriptor ) ) != null
				&& concreteDescriptor.isInstance( id );
	}

	@Override
	public void resolveInstance(Object instance) {
		if ( instance == null ) {
			setMissing( rowProcessingState );
			return;
		}
		entityInstance = instance;
		final LazyInitializer lazyInitializer = extractLazyInitializer( entityInstance );
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		if ( lazyInitializer == null ) {
			// Entity is most probably initialized
			entityInstanceForNotify = entityInstance;
			concreteDescriptor = session.getEntityPersister( null, entityInstance );
			resolveEntityKey(
					rowProcessingState,
					concreteDescriptor.getIdentifier( entityInstance, session )
			);
			entityHolder = session.getPersistenceContextInternal().getEntityHolder( entityKey );
			// If the entity initializer is null, we know the entity is fully initialized,
			// otherwise it will be initialized by some other initializer
			state = entityHolder.getEntityInitializer() == null ? State.INITIALIZED : State.RESOLVED;
		}
		else if ( lazyInitializer.isUninitialized() ) {
			state = State.RESOLVED;
			// Read the discriminator from the result set if necessary
			concreteDescriptor = discriminatorAssembler == null
					? entityDescriptor
					: determineConcreteEntityDescriptor( rowProcessingState, discriminatorAssembler, entityDescriptor );
			assert concreteDescriptor != null;
			resolveEntityKey( rowProcessingState, lazyInitializer.getIdentifier() );
			entityHolder = session.getPersistenceContextInternal().claimEntityHolderIfPossible(
					entityKey,
					null,
					rowProcessingState.getJdbcValuesSourceProcessingState(),
					this
			);
			// Resolve and potentially create the entity instance
			entityInstanceForNotify = resolveEntityInstance( rowProcessingState );
			lazyInitializer.setImplementation( entityInstanceForNotify );
			registerLoadingEntity( rowProcessingState, entityInstanceForNotify );
		}
		else {
			state = State.INITIALIZED;
			entityInstanceForNotify = lazyInitializer.getImplementation();
			concreteDescriptor = session.getEntityPersister( null, entityInstanceForNotify );
			resolveEntityKey( rowProcessingState, lazyInitializer.getIdentifier() );
			entityHolder = session.getPersistenceContextInternal().getEntityHolder( entityKey );
		}
		if ( identifierAssembler != null ) {
			final Initializer initializer = identifierAssembler.getInitializer();
			if ( initializer != null ) {
				initializer.resolveInstance( entityKey.getIdentifier() );
			}
		}
		upgradeLockMode( rowProcessingState );
		if ( state == State.INITIALIZED ) {
			registerReloadedEntity( rowProcessingState );
			resolveInstanceSubInitializers( rowProcessingState );
			if ( !rowProcessingState.isQueryCacheHit() && rowProcessingState.getQueryOptions().isResultCachingEnabled() == Boolean.TRUE ) {
				// We need to read result set values to correctly populate the query cache
				resolveState( rowProcessingState );
			}
		}
		else {
			resolveKeySubInitializers( rowProcessingState );
		}
	}

	@Override
	public void resolveInstance() {
		if ( state != State.KEY_RESOLVED ) {
			return;
		}
		state = State.RESOLVED;
		if ( entityKey == null ) {
			assert identifierAssembler != null;
			final Object id = identifierAssembler.assemble( rowProcessingState );
			if ( id == null ) {
				setMissing( rowProcessingState );
				return;
			}
			resolveEntityKey( rowProcessingState, id );
		}
		final PersistenceContext persistenceContext = rowProcessingState.getSession()
				.getPersistenceContextInternal();
		entityHolder = persistenceContext.claimEntityHolderIfPossible(
				entityKey,
				null,
				rowProcessingState.getJdbcValuesSourceProcessingState(),
				this
		);

		if ( useEmbeddedIdentifierInstanceAsEntity( rowProcessingState.getEntityId(), rowProcessingState ) ) {
			entityInstance = entityInstanceForNotify = rowProcessingState.getEntityId();
		}
		else {
			resolveEntityInstance1( rowProcessingState );
			if ( isResultInitializer() ) {
				final String uniqueKeyAttributePath = rowProcessingState.getEntityUniqueKeyAttributePath();
				if ( uniqueKeyAttributePath != null ) {
					final SharedSessionContractImplementor session = rowProcessingState.getSession();
					final EntityUniqueKey euk = new EntityUniqueKey(
							getConcreteDescriptor().getEntityName(),
							uniqueKeyAttributePath,
							rowProcessingState.getEntityUniqueKey(),
							getConcreteDescriptor().getPropertyType( uniqueKeyAttributePath ),
							session.getFactory()
					);
					session.getPersistenceContextInternal().addEntity( euk, getEntityInstance() );
				}
			}
		}

		if ( entityInstance != null ) {
			upgradeLockMode( rowProcessingState );
			if ( state == State.INITIALIZED ) {
				registerReloadedEntity( rowProcessingState );
				if ( !rowProcessingState.isQueryCacheHit() && rowProcessingState.getQueryOptions().isResultCachingEnabled() == Boolean.TRUE ) {
					// We need to read result set values to correctly populate the query cache
					resolveState( rowProcessingState );
				}
			}
			if ( shallowCached ) {
				initializeSubInstancesFromParent( rowProcessingState );
			}
		}
	}

	protected void resolveEntityInstance1(RowProcessingState rowProcessingState) {
		if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isTraceEnabled() ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.tracef(
					"(%s) Beginning Initializer#resolveInstance process for entity (%s) : %s",
					StringHelper.collapse( this.getClass().getName() ),
					getNavigablePath(),
					entityKey.getIdentifier()
			);
		}

		final Object proxy = entityHolder.getProxy();
		final boolean unwrapProxy = proxy != null && referencedModelPart instanceof ToOneAttributeMapping
				&& ( (ToOneAttributeMapping) referencedModelPart ).isUnwrapProxy()
				&& getConcreteDescriptor().getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
		final Object entityFromExecutionContext;
		if ( !unwrapProxy && isProxyInstance( proxy ) ) {
			if ( ( entityFromExecutionContext = getEntityFromExecutionContext( rowProcessingState ) ) != null ) {
				entityInstance = entityInstanceForNotify = entityFromExecutionContext;
				// If the entity comes from the execution context, it is treated as not initialized
				// so that we can refresh the data as requested
				registerReloadedEntity( rowProcessingState );
			}
			else {
				entityInstance = proxy;
				if ( Hibernate.isInitialized( entityInstance ) ) {
					state = State.INITIALIZED;
					entityInstanceForNotify = Hibernate.unproxy( entityInstance );
				}
				else {
					final LazyInitializer lazyInitializer = extractLazyInitializer( entityInstance );
					assert lazyInitializer != null;
					entityInstanceForNotify = resolveInstance( entityHolder, rowProcessingState );
					lazyInitializer.setImplementation( entityInstanceForNotify );
				}
			}
		}
		else {
			final Object existingEntity = entityHolder.getEntity();
			if ( existingEntity != null ) {
				entityInstance = entityInstanceForNotify = existingEntity;
				if ( entityHolder.getEntityInitializer() == null ) {
					if ( isExistingEntityInitialized( existingEntity ) ) {
						state = State.INITIALIZED;
					}
					else if ( isResultInitializer() ) {
						registerLoadingEntity( rowProcessingState, entityInstance );
					}
				}
				else if ( entityHolder.getEntityInitializer() != this ) {
					state = State.INITIALIZED;
				}
			}
			else if ( ( entityFromExecutionContext = getEntityFromExecutionContext( rowProcessingState ) ) != null ) {
				// This is the entity to refresh, so don't set the state to initialized
				entityInstance = entityInstanceForNotify = entityFromExecutionContext;
				if ( isResultInitializer() ) {
					registerLoadingEntity( rowProcessingState, entityInstance );
				}
			}
			else {
				assert entityHolder.getEntityInitializer() == this;
				// look to see if another initializer from a parent load context or an earlier
				// initializer is already loading the entity
				entityInstance = entityInstanceForNotify = resolveInstance( entityHolder, rowProcessingState );
				final Initializer idInitializer;
				if ( entityHolder.getEntityInitializer() == this && !isEntityInitialized()
						&& identifierAssembler != null
						&& ( idInitializer = identifierAssembler.getInitializer() ) != null ) {
					// If this is the owning initializer and the returned object is not initialized,
					// this means that the entity instance was just instantiated.
					// In this case, we want to call "assemble" and hence "initializeInstance" on the initializer
					// for possibly non-aggregated identifier mappings, so inject the virtual id representation
					idInitializer.initializeInstance();
				}
			}
		}
		// todo: ensure we initialize the entity
		assert !shallowCached || state == State.INITIALIZED : "Forgot to initialize the entity";
	}

	protected Object getEntityFromExecutionContext(RowProcessingState rowProcessingState) {
		final ExecutionContext executionContext = rowProcessingState.getJdbcValuesSourceProcessingState()
				.getExecutionContext();
		if ( rootEntityDescriptor == executionContext.getRootEntityDescriptor()
				&& entityKey.getIdentifier().equals( executionContext.getEntityId() ) ) {
			return executionContext.getEntityInstance();
		}
		return null;
	}

	private void upgradeLockMode(RowProcessingState rowProcessingState) {
		if ( lockMode != LockMode.NONE && rowProcessingState.upgradeLocks() ) {
			final EntityEntry entry =
					rowProcessingState.getSession().getPersistenceContextInternal()
							.getEntry( entityInstanceForNotify );
			if ( entry != null && entry.getLockMode().lessThan( lockMode ) ) {
				//we only check the version when _upgrading_ lock modes
				if ( versionAssembler != null && entry.getLockMode() != LockMode.NONE ) {
					checkVersion( entry, rowProcessingState );
				}
				//we need to upgrade the lock mode to the mode requested
				entry.setLockMode( lockMode );
			}
		}
	}

	private boolean isProxyInstance(Object proxy) {
		return proxy != null
				&& ( proxy instanceof MapProxy || entityDescriptor.getJavaType().getJavaTypeClass().isInstance( proxy ) );
	}

	private boolean isExistingEntityInitialized(Object existingEntity) {
		return Hibernate.isInitialized( existingEntity );
	}

	/**
	 * Check the version of the object in the {@code RowProcessingState} against
	 * the object version in the session cache, throwing an exception
	 * if the version numbers are different
	 */
	private void checkVersion(EntityEntry entry, final RowProcessingState rowProcessingState) throws HibernateException {
		final Object version = entry.getVersion();
		if ( version != null ) {
			// null version means the object is in the process of being loaded somewhere else in the ResultSet
			final Object currentVersion = versionAssembler.assemble( rowProcessingState );
			if ( !concreteDescriptor.getVersionType().isEqual( version, currentVersion ) ) {
				final StatisticsImplementor statistics = rowProcessingState.getSession().getFactory().getStatistics();
				if ( statistics.isStatisticsEnabled() ) {
					statistics.optimisticFailure( concreteDescriptor.getEntityName() );
				}
				throw new StaleObjectStateException( concreteDescriptor.getEntityName(), entry.getId() );
			}
		}

	}

	/**
	 * Used by Hibernate Reactive
	 */
	protected Object resolveInstance(EntityHolder holder, RowProcessingState rowProcessingState) {
		if ( entityHolder.getEntityInitializer() == this ) {
			assert holder.getEntity() == null;
			return resolveEntityInstance( rowProcessingState );
		}
		else {
			// the entity is already being loaded elsewhere
			if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isDebugEnabled() ) {
				EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
						"(%s) Entity [%s] being loaded by another initializer [%s] - skipping processing",
						getSimpleConcreteImplName(),
						toLoggableString( getNavigablePath(), entityKey.getIdentifier() ),
						holder.getEntityInitializer()
				);
			}
			return holder.getEntity();
		}
	}

	protected Object resolveEntityInstance(RowProcessingState rowProcessingState) {
		final Object resolved = resolveToOptionalInstance( rowProcessingState );
		if ( resolved != null ) {
			registerLoadingEntity( rowProcessingState, resolved );
			return resolved;
		}
		else {
			if ( rowProcessingState.isQueryCacheHit() && entityDescriptor.useShallowQueryCacheLayout() ) {
				// We must load the entity this way, because the query cache entry contains only the primary key
				state = State.INITIALIZED;
				final SharedSessionContractImplementor session = rowProcessingState.getSession();
				assert entityHolder.getEntityInitializer() == this;
				// If this initializer owns the entity, we have to remove the entity holder,
				// because the subsequent loading process will claim the entity
				session.getPersistenceContextInternal().removeEntityHolder( entityKey );
				return session.internalLoad(
						concreteDescriptor.getEntityName(),
						entityKey.getIdentifier(),
						true,
						false
				);
			}
			// We have to query the second level cache if reference cache entries are used
			else if ( entityDescriptor.canUseReferenceCacheEntries() ) {
				final Object cached = resolveInstanceFromCache( rowProcessingState );
				if ( cached != null ) {
					// EARLY EXIT!!!
					// because the second level cache has reference cache entries, the entity is initialized
					state = State.INITIALIZED;
					return cached;
				}
			}
			final Object instance = instantiateEntity( rowProcessingState.getSession() );
			registerLoadingEntity( rowProcessingState, instance );
			return instance;
		}
	}

	protected Object instantiateEntity(SharedSessionContractImplementor session) {
		final Object instance = session.instantiate( concreteDescriptor, entityKey.getIdentifier() );
		if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isDebugEnabled() ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
					"(%s) Created new entity instance [%s] : %s",
					getSimpleConcreteImplName(),
					toLoggableString( getNavigablePath(), entityKey.getIdentifier() ),
					System.identityHashCode( instance )
			);
		}
		return instance;
	}

	private Object resolveToOptionalInstance(RowProcessingState rowProcessingState) {
		if ( isResultInitializer() ) {
			// this isEntityReturn bit is just for entity loaders, not hql/criteria
			final JdbcValuesSourceProcessingOptions processingOptions =
					rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions();
			return matchesOptionalInstance( processingOptions ) ? processingOptions.getEffectiveOptionalObject() : null;
		}
		else {
			return null;
		}
	}

	private boolean matchesOptionalInstance(JdbcValuesSourceProcessingOptions processingOptions) {
		final Object optionalEntityInstance = processingOptions.getEffectiveOptionalObject();
		final Object requestedEntityId = processingOptions.getEffectiveOptionalId();
		return requestedEntityId != null
				&& optionalEntityInstance != null
				&& requestedEntityId.equals( entityKey.getIdentifier() );
	}

	private Object resolveInstanceFromCache(RowProcessingState rowProcessingState) {
		return CacheEntityLoaderHelper.INSTANCE.loadFromSecondLevelCache(
				rowProcessingState.getSession().asEventSource(),
				null,
				lockMode,
				entityDescriptor,
				entityKey
		);
	}

	protected void registerLoadingEntity(RowProcessingState rowProcessingState, Object instance) {
		rowProcessingState.getSession().getPersistenceContextInternal().claimEntityHolderIfPossible(
				entityKey,
				instance,
				rowProcessingState.getJdbcValuesSourceProcessingState(),
				this
		);
	}

	protected void registerReloadedEntity(RowProcessingState rowProcessingState) {
		if ( rowProcessingState.hasCallbackActions() ) {
			// This is only needed for follow-on locking, so skip registering the entity if there is no callback
			entityHolder.markAsReloaded( rowProcessingState.getJdbcValuesSourceProcessingState() );
		}
	}

	@Override
	public void initializeInstance() {
		if ( state != State.RESOLVED ) {
			return;
		}
		if ( !skipInitialization( rowProcessingState ) ) {
			assert consistentInstance( rowProcessingState );
			initializeEntityInstance( rowProcessingState );
		}
		state = State.INITIALIZED;
	}

	protected boolean consistentInstance(RowProcessingState rowProcessingState) {
		final PersistenceContext persistenceContextInternal =
				rowProcessingState.getSession().getPersistenceContextInternal();
		// Only call PersistenceContext#getEntity within the assert expression, as it is costly
		final Object entity = persistenceContextInternal.getEntity( entityKey );
		return entity == null || entity == entityInstanceForNotify;
	}

	private void initializeEntityInstance(RowProcessingState rowProcessingState) {
		final Object entityIdentifier = entityKey.getIdentifier();
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();

		if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isTraceEnabled() ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.tracef(
					"(%s) Beginning Initializer#initializeInstance process for entity %s",
					getSimpleConcreteImplName(),
					toLoggableString( getNavigablePath(), entityIdentifier )
			);
		}

		final Object[] resolvedEntityState = extractConcreteTypeStateValues( rowProcessingState );

		preLoad( rowProcessingState, resolvedEntityState );

		if ( isPersistentAttributeInterceptable( entityInstanceForNotify ) ) {
			final PersistentAttributeInterceptor persistentAttributeInterceptor =
					asPersistentAttributeInterceptable( entityInstanceForNotify ).$$_hibernate_getInterceptor();
			if ( persistentAttributeInterceptor == null
					|| persistentAttributeInterceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
				// if we do this after the entity has been initialized the
				// BytecodeLazyAttributeInterceptor#isAttributeLoaded(String fieldName) would return false;
				concreteDescriptor.getBytecodeEnhancementMetadata()
						.injectInterceptor( entityInstanceForNotify, entityIdentifier, session );
			}
		}
		concreteDescriptor.setPropertyValues( entityInstanceForNotify, resolvedEntityState );

		persistenceContext.addEntity( entityKey, entityInstanceForNotify );

		// Also register possible unique key entries
		registerPossibleUniqueKeyEntries( resolvedEntityState, session );

		final Object version = versionAssembler != null ? versionAssembler.assemble( rowProcessingState ) : null;
		final Object rowId = rowIdAssembler != null ? rowIdAssembler.assemble( rowProcessingState ) : null;

		// from the perspective of Hibernate, an entity is read locked as soon as it is read
		// so regardless of the requested lock mode, we upgrade to at least the read level
		final LockMode lockModeToAcquire = lockMode == LockMode.NONE ? LockMode.READ : lockMode;

		final EntityEntry entityEntry = persistenceContext.addEntry(
				entityInstanceForNotify,
				Status.LOADING,
				resolvedEntityState,
				rowId,
				entityKey.getIdentifier(),
				version,
				lockModeToAcquire,
				true,
				concreteDescriptor,
				false
		);

		registerNaturalIdResolution( persistenceContext, resolvedEntityState );

		takeSnapshot( rowProcessingState, session, persistenceContext, entityEntry, resolvedEntityState );

		concreteDescriptor.afterInitialize( entityInstanceForNotify, session );

		if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isDebugEnabled() ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
					"(%s) Done materializing entityInstance : %s",
					getSimpleConcreteImplName(),
					toLoggableString( getNavigablePath(), entityIdentifier )
			);
		}

		assert concreteDescriptor.getIdentifier( entityInstanceForNotify, session ) != null;

		final StatisticsImplementor statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			if ( !rowProcessingState.isQueryCacheHit() ) {
				statistics.loadEntity( concreteDescriptor.getEntityName() );
			}
		}
		updateCaches(
				rowProcessingState,
				session,
				session.getPersistenceContext(),
				resolvedEntityState,
				version
		);
	}

	protected void updateCaches(
			RowProcessingState rowProcessingState,
			SharedSessionContractImplementor session,
			PersistenceContext persistenceContext,
			Object[] resolvedEntityState,
			Object version) {
		if ( concreteDescriptor.canWriteToCache()
				// No need to put into the entity cache if this is coming from the query cache already
				&& !rowProcessingState.isQueryCacheHit()
				&& session.getCacheMode().isPutEnabled() ) {
			final EntityDataAccess cacheAccess = concreteDescriptor.getCacheAccessStrategy();
			if ( cacheAccess != null  ) {
				putInCache( session, persistenceContext, resolvedEntityState, version, cacheAccess );
			}
		}
	}

	protected void registerNaturalIdResolution(PersistenceContext persistenceContext, Object[] resolvedEntityState) {
		if ( entityDescriptor.getNaturalIdMapping() != null ) {
			final Object naturalId =
					entityDescriptor.getNaturalIdMapping().extractNaturalIdFromEntityState( resolvedEntityState );
			persistenceContext.getNaturalIdResolutions()
					.cacheResolutionFromLoad( entityKey.getIdentifier(), naturalId, entityDescriptor );
		}
	}

	protected void takeSnapshot(
			RowProcessingState rowProcessingState,
			SharedSessionContractImplementor session,
			PersistenceContext persistenceContext,
			EntityEntry entityEntry,
			Object[] resolvedEntityState) {
		if ( isReallyReadOnly( rowProcessingState, session ) ) {
			//no need to take a snapshot - this is a
			//performance optimization, but not really
			//important, except for entities with huge
			//mutable property values
			persistenceContext.setEntryStatus( entityEntry, Status.READ_ONLY );
		}
		else {
			//take a snapshot
			deepCopy( concreteDescriptor, resolvedEntityState, resolvedEntityState );
			persistenceContext.setEntryStatus( entityEntry, Status.MANAGED );
		}
	}

	private boolean isReallyReadOnly(RowProcessingState rowProcessingState, SharedSessionContractImplementor session) {
		if ( !concreteDescriptor.isMutable() ) {
			return true;
		}
		else {
			final LazyInitializer lazyInitializer = extractLazyInitializer( entityInstance );
			if ( lazyInitializer != null ) {
				// there is already a proxy for this impl
				// only set the status to read-only if the proxy is read-only
				return lazyInitializer.isReadOnly();
			}
			else {
				return isReadOnly( rowProcessingState, session );
			}
		}
	}

	private void putInCache(
			SharedSessionContractImplementor session,
			PersistenceContext persistenceContext,
			Object[] resolvedEntityState,
			Object version,
			EntityDataAccess cacheAccess) {
		final SessionFactoryImplementor factory = session.getFactory();

		if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isDebugEnabled() ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
					"(%S) Adding entityInstance to second-level cache: %s",
					getSimpleConcreteImplName(),
					toLoggableString( getNavigablePath(), entityKey.getIdentifier() )
			);
		}

		final CacheEntry cacheEntry = concreteDescriptor.buildCacheEntry(
				entityInstanceForNotify,
				resolvedEntityState,
				version,
				session
		);
		final Object cacheKey = cacheAccess.generateCacheKey(
				entityKey.getIdentifier(),
				rootEntityDescriptor,
				factory,
				session.getTenantIdentifier()
		);

		// explicit handling of caching for rows just inserted and then somehow forced to be read
		// from the database *within the same transaction*.  usually this is done by
		// 		1) Session#refresh, or
		// 		2) Session#clear + some form of load
		//
		// we need to be careful not to clobber the lock here in the cache so that it can be rolled back if need be
		final EventManager eventManager = session.getEventManager();
		if ( persistenceContext.wasInsertedDuringTransaction( concreteDescriptor, entityKey.getIdentifier() ) ) {
			boolean update = false;
			final HibernateMonitoringEvent cachePutEvent = eventManager.beginCachePutEvent();
			try {
				update = cacheAccess.update(
						session,
						cacheKey,
						concreteDescriptor.getCacheEntryStructure().structure( cacheEntry ),
						version,
						version
				);
			}
			finally {
				eventManager.completeCachePutEvent(
						cachePutEvent,
						session,
						cacheAccess,
						concreteDescriptor,
						update,
						EventManager.CacheActionDescription.ENTITY_UPDATE
				);
			}
		}
		else {
			final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
			boolean put = false;
			final HibernateMonitoringEvent cachePutEvent = eventManager.beginCachePutEvent();
			try {
				eventListenerManager.cachePutStart();
				put = cacheAccess.putFromLoad(
						session,
						cacheKey,
						concreteDescriptor.getCacheEntryStructure().structure( cacheEntry ),
						version,
						//useMinimalPuts( session, entityEntry )
						false
				);
			}
			finally {
				eventManager.completeCachePutEvent(
						cachePutEvent,
						session,
						cacheAccess,
						concreteDescriptor,
						put,
						EventManager.CacheActionDescription.ENTITY_LOAD
				);
				final StatisticsImplementor statistics = factory.getStatistics();
				if ( put && statistics.isStatisticsEnabled() ) {
					statistics.entityCachePut( rootEntityDescriptor.getNavigableRole(), cacheAccess.getRegion().getName() );
				}
				eventListenerManager.cachePutEnd();
			}
		}
	}

	protected void registerPossibleUniqueKeyEntries(
			Object[] resolvedEntityState,
			final SharedSessionContractImplementor session) {
		for ( UniqueKeyEntry entry : concreteDescriptor.uniqueKeyEntries() ) {
			final String ukName = entry.getUniqueKeyName();
			final int index = entry.getStateArrayPosition();
			final Type type = entry.getPropertyType();

			// polymorphism not really handled completely correctly,
			// perhaps...well, actually its ok, assuming that the
			// entity name used in the lookup is the same as the
			// one used here, which it will be

			if ( resolvedEntityState[index] != null ) {
				final EntityUniqueKey entityUniqueKey = new EntityUniqueKey(
						concreteDescriptor.getRootEntityDescriptor().getEntityName(),
						//polymorphism comment above
						ukName,
						resolvedEntityState[index],
						type,
						session.getFactory()
				);
				session.getPersistenceContextInternal().addEntity( entityUniqueKey, entityInstanceForNotify );
			}
		}
	}

	protected Object[] extractConcreteTypeStateValues(RowProcessingState rowProcessingState) {
		final Object[] values = new Object[concreteDescriptor.getNumberOfAttributeMappings()];
		final DomainResultAssembler<?>[] concreteAssemblers = assemblers[concreteDescriptor.getSubclassId()];
		for ( int i = 0; i < values.length; i++ ) {
			final DomainResultAssembler<?> assembler = concreteAssemblers[i];
			values[i] = assembler == null ? UNFETCHED_PROPERTY : assembler.assemble( rowProcessingState );
		}
		return values;
	}

	private void resolveState(RowProcessingState rowProcessingState) {
		for ( final DomainResultAssembler<?> assembler : assemblers[concreteDescriptor.getSubclassId()] ) {
			if ( assembler != null ) {
				assembler.resolveState( rowProcessingState );
			}
		}
	}

	protected boolean skipInitialization(RowProcessingState rowProcessingState) {
		if ( entityHolder.getEntityInitializer() != this ) {
			return true;
		}
		final EntityEntry entry =
				rowProcessingState.getSession().getPersistenceContextInternal().getEntry( entityInstanceForNotify );
		if ( entry == null ) {
			return false;
		}
		// todo (6.0): do we really need this check ?
		else if ( entry.getStatus().isDeletedOrGone() ) {
			return true;
		}
		else {
			if ( isPersistentAttributeInterceptable( entityInstanceForNotify ) ) {
				final PersistentAttributeInterceptor interceptor =
						asPersistentAttributeInterceptable( entityInstanceForNotify ).$$_hibernate_getInterceptor();
				if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
					// Avoid loading the same entity proxy twice for the same result set: it could lead to errors,
					// because some code writes to its input (ID in hydrated state replaced by the loaded entity, in particular).
					return entry.getStatus() == Status.LOADING;
				}
			}

			// If the instance to initialize is the main entity, we can't skip this.
			// This can happen if we initialize an enhanced proxy.
			if ( entry.getStatus() != Status.LOADING ) {
				// If the instance to initialize is the main entity, we can't skip this.
				// This can happen if we initialize an enhanced proxy.
				return rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions()
						.getEffectiveOptionalObject() != entityInstanceForNotify;
			}
			else {
				return false;
			}
		}
	}

	private boolean isReadOnly(RowProcessingState rowProcessingState, SharedSessionContractImplementor persistenceContext) {
		final Boolean readOnly = rowProcessingState.getQueryOptions().isReadOnly();
		return readOnly == null ? persistenceContext.isDefaultReadOnly() : readOnly;
	}

	protected void preLoad(RowProcessingState rowProcessingState, Object[] resolvedEntityState) {
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		if ( session.isEventSource() ) {
			final PreLoadEvent preLoadEvent = rowProcessingState.getJdbcValuesSourceProcessingState().getPreLoadEvent();
			assert preLoadEvent != null;

			preLoadEvent.reset();

			preLoadEvent.setEntity( entityInstance )
					.setState( resolvedEntityState )
					.setId( entityKey.getIdentifier() )
					.setPersister( concreteDescriptor );

			session.getFactory()
					.getFastSessionServices()
					.eventListenerGroup_PRE_LOAD
					.fireEventOnEachListener( preLoadEvent, PreLoadEventListener::onPreLoad );
		}
	}

	@Override
	public boolean isPartOfKey() {
		return isPartOfKey;
	}

	@Override
	public boolean isEntityInitialized() {
		return state == State.INITIALIZED;
	}

	@Override
	public EntityPersister getConcreteDescriptor() {
		assert state != State.UNINITIALIZED;
		return concreteDescriptor == null ? entityDescriptor : concreteDescriptor;
	}

	protected void initializeSubInstancesFromParent(RowProcessingState rowProcessingState) {
		if ( entityInstanceForNotify != null ) {
			for ( Initializer initializer : subInitializers[concreteDescriptor.getSubclassId()] ) {
				initializer.initializeInstanceFromParent( entityInstanceForNotify );
			}
		}
	}

	@Override
	protected <X> void forEachSubInitializer(BiConsumer<Initializer, X> consumer, X arg) {
		if ( identifierAssembler != null ) {
			final Initializer initializer = identifierAssembler.getInitializer();
			if ( initializer != null ) {
				consumer.accept( initializer, arg );
			}
		}
		if ( concreteDescriptor == null ) {
			for ( Initializer[] initializers : subInitializers ) {
				for ( Initializer initializer : initializers ) {
					consumer.accept( initializer, arg );
				}
			}
		}
		else {
			for ( Initializer initializer : subInitializers[concreteDescriptor.getSubclassId()] ) {
				consumer.accept( initializer, arg );
			}
		}
	}

	@Override
	public void endLoading(ExecutionContext executionContext) {
		super.endLoading( executionContext );
		shallowCached = false;
	}

	@Override
	public String toString() {
		return "EntityJoinedFetchInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}

	//#########################
	// For Hibernate Reactive
	//#########################

	protected void setEntityInstanceForNotify(Object entityInstanceForNotify) {
		this.entityInstanceForNotify = entityInstanceForNotify;
	}

	protected Object getEntityInstanceForNotify() {
		return entityInstanceForNotify;
	}

	protected DomainResultAssembler<?> getVersionAssembler() {
		return versionAssembler;
	}

	protected DomainResultAssembler<Object> getRowIdAssembler() {
		return rowIdAssembler;
	}

	protected LockMode getLockMode() {
		return lockMode;
	}

	protected DomainResultAssembler<?>[][] getAssemblers() {
		return assemblers;
	}

}
