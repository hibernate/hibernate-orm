/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity;

import java.util.Collection;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.WrongClassException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.ast.internal.CacheEntityLoaderHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping.DiscriminatorValueDetails;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.internal.PropertyAccessStrategyBackRefImpl;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.proxy.map.MapProxy;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.basic.BasicResultAssembler;
import org.hibernate.sql.results.graph.entity.internal.EntityResultInitializer;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;

import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.internal.log.LoggingHelper.toLoggableString;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * @author Steve Ebersole
 * @author Nathan Xu
 */
public abstract class AbstractEntityInitializer extends AbstractFetchParentAccess implements EntityInitializer {

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

	private final DomainResultAssembler<?> identifierAssembler;
	private final BasicResultAssembler<?> discriminatorAssembler;
	private final DomainResultAssembler<?> versionAssembler;
	private final DomainResultAssembler<Object> rowIdAssembler;

	private final DomainResultAssembler<?>[][] assemblers;

	// per-row state
	private EntityPersister concreteDescriptor;
	private EntityKey entityKey;
	private Object version;
	private Object entityInstance;
	private Object entityInstanceForNotify;
	protected boolean missing;
	boolean isInitialized;
	private boolean isOwningInitializer;
	private Object[] resolvedEntityState;

	// todo (6.0) : ^^ need a better way to track whether we are loading the entity state or if something else is/has

	protected AbstractEntityInitializer(
			EntityResultGraphNode resultDescriptor,
			NavigablePath navigablePath,
			LockMode lockMode,
			Fetch identifierFetch,
			Fetch discriminatorFetch,
			DomainResult<Object> rowIdResult,
			AssemblerCreationState creationState) {
		super();

		referencedModelPart = resultDescriptor.getEntityValuedModelPart();
		entityDescriptor = (EntityPersister) referencedModelPart.getEntityMappingType();

		final String rootEntityName = entityDescriptor.getRootEntityName();
		rootEntityDescriptor = rootEntityName == null || rootEntityName.equals( entityDescriptor.getEntityName() )
				? entityDescriptor
				: entityDescriptor.getRootEntityDescriptor().getEntityPersister();

		this.navigablePath = navigablePath;
		this.lockMode = lockMode;
		assert lockMode != null;

		identifierAssembler = identifierFetch != null
				? identifierFetch.createAssembler( this, creationState )
				: null;

		discriminatorAssembler = discriminatorFetch != null
				? (BasicResultAssembler<?>) discriminatorFetch.createAssembler( this, creationState )
				: null;

		final EntityVersionMapping versionMapping = entityDescriptor.getVersionMapping();
		if ( versionMapping != null ) {
			final Fetch versionFetch = resultDescriptor.findFetch( versionMapping );
			// If there is a version mapping, there must be a fetch for it
			assert versionFetch != null;
			versionAssembler = versionFetch.createAssembler( this, creationState );
		}
		else {
			versionAssembler = null;
		}

		rowIdAssembler = rowIdResult != null
				? rowIdResult.createResultAssembler( this, creationState )
				: null;

		final Collection<EntityMappingType> subMappingTypes = rootEntityDescriptor.getSubMappingTypes();
		assemblers = new DomainResultAssembler[subMappingTypes.size() + 1][];
		assemblers[rootEntityDescriptor.getSubclassId()] = new DomainResultAssembler[rootEntityDescriptor.getNumberOfFetchables()];

		for ( EntityMappingType subMappingType : subMappingTypes ) {
			assemblers[subMappingType.getSubclassId()] = new DomainResultAssembler[subMappingType.getNumberOfFetchables()];
		}

		final int size = entityDescriptor.getNumberOfFetchables();
		for ( int i = 0; i < size; i++ ) {
			final AttributeMapping attributeMapping = entityDescriptor.getFetchable( i ).asAttributeMapping();
			// todo (6.0) : somehow we need to track whether all state is loaded/resolved
			//		note that lazy proxies or uninitialized collections count against
			//		that in the affirmative
			final Fetch fetch = resultDescriptor.findFetch( attributeMapping );
			final DomainResultAssembler<?> stateAssembler = fetch == null
					? new NullValueAssembler<>( attributeMapping.getMappedType().getMappedJavaType() )
					: fetch.createAssembler( this, creationState );

			final int stateArrayPosition = attributeMapping.getStateArrayPosition();
			final EntityMappingType declaringType = (EntityMappingType) attributeMapping.getDeclaringType();
			assemblers[declaringType.getSubclassId()][stateArrayPosition] = stateAssembler;
			for ( EntityMappingType subMappingType : declaringType.getSubMappingTypes() ) {
				assemblers[subMappingType.getSubclassId()][stateArrayPosition] = stateAssembler;
			}
		}
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

	/**
	 * Simple class name of this initializer for logging
	 */
	protected abstract String getSimpleConcreteImplName();

	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	protected DomainResultAssembler<?> getIdentifierAssembler() {
		return identifierAssembler;
	}

	protected boolean isMissing() {
		return missing;
	}

	protected void setMissing(boolean missing) {
		this.missing = missing;
	}

	protected abstract boolean isEntityReturn();

	@Override
	public EntityPersister getEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public Object getEntityInstance() {
		return entityInstance;
	}

	@SuppressWarnings("unused")
	public Object getKeyValue() {
		return entityKey == null ? null : entityKey.getIdentifier();
	}

	@Override
	public EntityKey getEntityKey() {
		return entityKey;
	}

	@Override
	public Object getParentKey() {
		return getKeyValue();
	}

	@Override
	public void registerResolutionListener(Consumer<Object> listener) {
		if ( entityInstanceForNotify != null ) {
			listener.accept( entityInstanceForNotify );
		}
		else {
			super.registerResolutionListener( listener );
		}
	}

	// todo (6.0) : how to best handle possibility of null association?

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		// todo (6.0) : atm we do not handle sequential selects
		// 		- see AbstractEntityPersister#hasSequentialSelect and
		//			AbstractEntityPersister#getSequentialSelect in 5.2

		if ( entityKey == null ) {
			if ( EntityLoadingLogging.TRACE_ENABLED ) {
				EntityLoadingLogging.ENTITY_LOADING_LOGGER.tracef(
						"(%s) Beginning Initializer#resolveKey process for entity : %s",
						StringHelper.collapse( this.getClass().getName() ),
						getNavigablePath()
				);
			}

			concreteDescriptor = determineConcreteEntityDescriptor( rowProcessingState );
			if ( concreteDescriptor == null ) {
				missing = true;
			}
			else {
				resolveEntityKey( rowProcessingState );
				if ( entityKey == null ) {
					EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
							"(%s) EntityKey (%s) is null",
							getSimpleConcreteImplName(),
							getNavigablePath()
					);
					assert missing;
				}
				else {
					if ( EntityLoadingLogging.DEBUG_ENABLED ) {
						EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
								"(%s) Hydrated EntityKey (%s): %s",
								getSimpleConcreteImplName(),
								getNavigablePath(),
								entityKey.getIdentifier()
						);
					}
				}
			}
		}
	}

	private EntityPersister determineConcreteEntityDescriptor(RowProcessingState rowProcessingState)
			throws WrongClassException {
		if ( discriminatorAssembler == null ) {
			return entityDescriptor;
		}
		else {
			final EntityDiscriminatorMapping discriminatorMapping = entityDescriptor.getDiscriminatorMapping();
			assert discriminatorMapping != null;
			final Object discriminator = discriminatorAssembler.extractRawValue( rowProcessingState );
			final DiscriminatorValueDetails discriminatorDetails =
					discriminatorMapping.resolveDiscriminatorValue( discriminator );
			if ( discriminatorDetails == null ) {
				return entityDescriptor;
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

	protected void resolveEntityKey(RowProcessingState rowProcessingState) {
		if ( entityKey == null ) {
			// it has not yet been resolved
			// 1) resolve the hydrated identifier value(s) into its identifier representation
			final Object id = initializeIdentifier( rowProcessingState );
			if ( id == null ) {
				missing = true;
			}
			else {
				// 2) build the EntityKey
				entityKey = new EntityKey( id, concreteDescriptor );
				// 3) schedule the EntityKey for batch loading, if possible
				if ( concreteDescriptor.isBatchLoadable() ) {
					final PersistenceContext persistenceContext =
							rowProcessingState.getSession().getPersistenceContextInternal();
					if ( !persistenceContext.containsEntity( entityKey ) ) {
						persistenceContext.getBatchFetchQueue().addBatchLoadableEntityKey( entityKey );
					}
				}
			}
		}
	}

	private Object initializeIdentifier(RowProcessingState rowProcessingState) {
		final JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState =
				rowProcessingState.getJdbcValuesSourceProcessingState();
		final Object id = jdbcValuesSourceProcessingState.getProcessingOptions().getEffectiveOptionalId();
		if ( useEmbeddedIdentifierInstanceAsEntity( id ) ) {
			entityInstance = id;
			return id;
		}
		else if ( identifierAssembler == null ) {
			return id;
		}
		else {
			return identifierAssembler.assemble(
					rowProcessingState,
					jdbcValuesSourceProcessingState.getProcessingOptions()
			);
		}
	}

	private boolean useEmbeddedIdentifierInstanceAsEntity(Object id) {
		return id != null
			&& id.getClass().equals( concreteDescriptor.getJavaType().getJavaType() );
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		if ( !missing && !isInitialized ) {
			if ( shouldSkipResolveInstance( rowProcessingState ) ) {
				missing = true;
				return;
			}
			// Special case map proxy to avoid stack overflows
			// We know that a map proxy will always be of "the right type" so just use that object
			final LoadingEntityEntry existingLoadingEntry =
					rowProcessingState.getSession().getPersistenceContextInternal().getLoadContexts()
							.findLoadingEntityEntry( entityKey );
			setIsOwningInitializer( entityKey.getIdentifier(), existingLoadingEntry );

			if ( entityInstance == null ) {
				resolveEntityInstance( rowProcessingState, existingLoadingEntry, entityKey.getIdentifier() );
			}
			else if ( existingLoadingEntry != null && existingLoadingEntry.getEntityInitializer() != this ) {
				isInitialized = true;
			}
		}
	}

	protected boolean shouldSkipResolveInstance(RowProcessingState rowProcessingState) {
		if ( navigablePath.getParent() != null ) {
			Initializer parentInitializer = rowProcessingState.resolveInitializer( navigablePath.getParent() );
			if ( parentInitializer != null ) {
				ModelPart modelPart = referencedModelPart;
				NavigablePath currentNavigablePath = navigablePath;
				// Walk back initializers until we find an EntityInitializer
				while ( parentInitializer != null && !parentInitializer.isEntityInitializer() ) {
					modelPart = parentInitializer.getInitializedPart();
					currentNavigablePath = currentNavigablePath.getParent();
					parentInitializer = rowProcessingState.resolveInitializer( currentNavigablePath.getParent() );
				}
				if ( parentInitializer != null && parentInitializer.asEntityInitializer()
						.getEntityDescriptor()
						.getEntityMetamodel()
						.isPolymorphic() ) {
					parentInitializer.resolveKey( rowProcessingState );
					return isReferencedModelPartInConcreteParent(
							modelPart,
							currentNavigablePath,
							parentInitializer
					);
				}
			}
		}
		return false;
	}

	private boolean isReferencedModelPartInConcreteParent(
			ModelPart modelPart,
			NavigablePath partNavigablePath,
			Initializer parentInitializer) {
		final EntityPersister parentConcreteDescriptor = parentInitializer.asEntityInitializer()
				.getConcreteDescriptor();
		if ( parentConcreteDescriptor != null && parentConcreteDescriptor.getEntityMetamodel().isPolymorphic() ) {
			final ModelPart concreteModelPart = parentConcreteDescriptor.findByPath( partNavigablePath.getLocalName() );
			if ( concreteModelPart == null
					|| !modelPart.getJavaType().getJavaTypeClass()
					.isAssignableFrom( concreteModelPart.getJavaType().getJavaTypeClass() ) ) {
		/*
			Given:

			class Message{

				@ManyToOne
				Address address;
			}

			@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
			class Address{
			}

			class AddressA extends Address {
				@OneToOne(mappedBy = "addressA")
				UserA userA;
			}

			class AddressB extends Address{
			@OneToOne(mappedBy = "addressB")
				UserB userB;
			}

			when we try to initialize the messages of Message.address,
			there will be one EntityJoinedFetchInitializer for UserA and one for UserB so
			when resolving AddressA we have to skip resolving the EntityJoinedFetchInitializer of UserB
			and for AddressB skip resolving the EntityJoinedFetchInitializer of UserA

		 */
				return true;
			}
		}
		return false;
	}

	protected void resolveEntityInstance(
			RowProcessingState rowProcessingState,
			LoadingEntityEntry existingLoadingEntry,
			Object entityIdentifier) {

		if ( EntityLoadingLogging.TRACE_ENABLED ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.tracef(
					"(%s) Beginning Initializer#resolveInstance process for entity (%s) : %s",
					StringHelper.collapse( this.getClass().getName() ),
					getNavigablePath(),
					entityIdentifier
			);
		}

		final PersistenceContext persistenceContext = rowProcessingState.getSession().getPersistenceContextInternal();
		final Object proxy = getProxy( persistenceContext );
		final Object entityInstanceFromExecutionContext =
				rowProcessingState.getJdbcValuesSourceProcessingState().getExecutionContext().getEntityInstance();
		if ( isProxyInstance( proxy ) ) {
			if ( useEntityInstanceFromExecutionContext( entityInstanceFromExecutionContext, persistenceContext.getSession() ) ) {
				entityInstance = entityInstanceFromExecutionContext;
				registerLoadingEntity( rowProcessingState, entityInstance );
			}
			else {
				entityInstance = proxy;
			}
		}
		else {
			final Object existingEntity = persistenceContext.getEntity( entityKey );
			if ( existingEntity != null ) {
				entityInstance = existingEntity;
				if ( existingLoadingEntry == null && isExistingEntityInitialized( existingEntity ) ) {
					notifyResolutionListeners( entityInstance );
					this.isInitialized = true;
				}
			}
			else if ( useEntityInstanceFromExecutionContext( entityInstanceFromExecutionContext, persistenceContext.getSession() ) ) {
				entityInstance = entityInstanceFromExecutionContext;
				registerLoadingEntity( rowProcessingState, entityInstance );
			}
			else {
				// look to see if another initializer from a parent load context or an earlier
				// initializer is already loading the entity
				entityInstance = resolveInstance( entityIdentifier, existingLoadingEntry, rowProcessingState );
			}

			upgradeLockMode( rowProcessingState );
		}
	}

	private boolean useEntityInstanceFromExecutionContext(
			Object entityInstanceFromExecutionContext,
			SharedSessionContractImplementor session) {
		return this instanceof EntityResultInitializer
			&& entityInstanceFromExecutionContext != null
			&& entityKey.getIdentifier()
				.equals( entityDescriptor.getIdentifier( entityInstanceFromExecutionContext, session ) );
	}

	private void upgradeLockMode(RowProcessingState rowProcessingState) {
		if ( lockMode != LockMode.NONE ) {
			final EntityEntry entry =
					rowProcessingState.getSession().getPersistenceContextInternal()
							.getEntry( entityInstance );
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
		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( entityInstance );
		if ( lazyInitializer != null ) {
			return !lazyInitializer.isUninitialized();
		}
		else if ( isPersistentAttributeInterceptable( existingEntity ) ) {
			final PersistentAttributeInterceptor persistentAttributeInterceptor =
					asPersistentAttributeInterceptable( entityInstance ).$$_hibernate_getInterceptor();
			return persistentAttributeInterceptor != null
				&& !( persistentAttributeInterceptor instanceof EnhancementAsProxyLazinessInterceptor );
		}

		return true;
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

	protected Object getProxy(PersistenceContext persistenceContext) {
		return persistenceContext.getProxy( entityKey );
	}

	private void setIsOwningInitializer(Object entityIdentifier,LoadingEntityEntry existingLoadingEntry) {
		if ( existingLoadingEntry != null ) {
			if ( EntityLoadingLogging.DEBUG_ENABLED ) {
				EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
						"(%s) Found existing loading entry [%s] - using loading instance",
						getSimpleConcreteImplName(),
						toLoggableString( getNavigablePath(), entityIdentifier )
				);
			}
			if ( existingLoadingEntry.getEntityInitializer() == this ) {
				isOwningInitializer = true;
			}
		}
		else {
			isOwningInitializer = true;
		}
	}

	protected boolean isOwningInitializer() {
		return isOwningInitializer;
	}

	private Object resolveInstance(
			Object entityIdentifier,
			LoadingEntityEntry existingLoadingEntry,
			RowProcessingState rowProcessingState) {
		if ( isOwningInitializer ) {
			assert existingLoadingEntry == null || existingLoadingEntry.getEntityInstance() == null;
			return resolveEntityInstance( entityIdentifier, rowProcessingState );
		}
		else {
			// the entity is already being loaded elsewhere
			if ( EntityLoadingLogging.DEBUG_ENABLED ) {
				EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
						"(%s) Entity [%s] being loaded by another initializer [%s] - skipping processing",
						getSimpleConcreteImplName(),
						toLoggableString( getNavigablePath(), entityIdentifier ),
						existingLoadingEntry.getEntityInitializer()
				);
			}
			return existingLoadingEntry.getEntityInstance();
		}
	}

	protected Object resolveEntityInstance(Object entityIdentifier, RowProcessingState rowProcessingState) {
		final Object resolved = resolveToOptionalInstance( rowProcessingState );
		if ( resolved != null ) {
			registerLoadingEntity( rowProcessingState, resolved );
			return resolved;
		}
		else {
			// We have to query the second level cache if reference cache entries are used
			if ( entityDescriptor.canUseReferenceCacheEntries() ) {
				final Object cached = resolveInstanceFromCache( rowProcessingState );
				if ( cached != null ) {
					// EARLY EXIT!!!
					// because the second level cache has reference cache entries, the entity is initialized
					isInitialized = true;
					return cached;
				}
			}
			final Object instance = instantiateEntity( entityIdentifier, rowProcessingState.getSession() );
			registerLoadingEntity( rowProcessingState, instance );
			return instance;
		}
	}

	protected Object instantiateEntity(Object entityIdentifier, SharedSessionContractImplementor session) {
		final Object instance = session.instantiate( concreteDescriptor.getEntityName(), entityKey.getIdentifier() );
		if ( EntityLoadingLogging.DEBUG_ENABLED ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
					"(%s) Created new entity instance [%s] : %s",
					getSimpleConcreteImplName(),
					toLoggableString( getNavigablePath(), entityIdentifier),
					instance
			);
		}
		return instance;
	}

	private Object resolveToOptionalInstance(RowProcessingState rowProcessingState) {
		if ( isEntityReturn() ) {
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

	private void registerLoadingEntity(RowProcessingState rowProcessingState, Object instance) {
		rowProcessingState.getJdbcValuesSourceProcessingState()
				.registerLoadingEntity(
						entityKey,
						new LoadingEntityEntry( this, entityKey, concreteDescriptor, instance )
				);
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		if ( !missing && !isInitialized ) {
			preLoad( rowProcessingState );

			final LazyInitializer lazyInitializer = extractLazyInitializer( entityInstance );
			final SharedSessionContractImplementor session = rowProcessingState.getSession();
			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			if ( lazyInitializer != null ) {
				Object instance = persistenceContext.getEntity( entityKey );
				if ( instance == null ) {
					instance = resolveInstance(
							entityKey.getIdentifier(),
							persistenceContext.getLoadContexts().findLoadingEntityEntry( entityKey ),
							rowProcessingState
					);
					initializeEntity( instance, rowProcessingState );
				}
				lazyInitializer.setImplementation( instance );
				entityInstanceForNotify = instance;
			}
			else {
				if ( entityDescriptor.canReadFromCache() ) {
					/*
						@Cache
						class Child {

							@ManyToOne
							private Parent parent;
						}

						@Cache
						class Parent {
							@OneToOne
							private Parent parent;

						}

						when the query "select c from Child c" is executed and the second level cache (2LC) contains
						an instance of Child and Parent
						then when the EntitySelectFetchInitializer#initializeInstance() is executed before the EntityResultInitializer one
						the persistence context will contain the instances retrieved form the 2LC
					 */
					final Object entity = persistenceContext.getEntity( entityKey );
					if ( entity != null ) {
						entityInstance = entity;
						registerLoadingEntity( rowProcessingState, entityInstance );
						initializeEntityInstance( entityInstance, rowProcessingState );
					}
					else {
						initializeEntity( entityInstance, rowProcessingState );
					}
					entityInstanceForNotify = entityInstance;
				}
				else {
					initializeEntity( entityInstance, rowProcessingState );
					entityInstanceForNotify = entityInstance;
				}
			}

			notifyResolutionListeners( entityInstanceForNotify );

			isInitialized = true;
		}
	}

	private void initializeEntity(Object toInitialize, RowProcessingState rowProcessingState) {
		if ( !skipInitialization( toInitialize, rowProcessingState ) ) {
			assert consistentInstance( toInitialize, rowProcessingState );
			initializeEntityInstance( toInitialize, rowProcessingState );
		}
	}

	protected boolean consistentInstance(Object toInitialize, RowProcessingState rowProcessingState) {
		final PersistenceContext persistenceContextInternal =
				rowProcessingState.getSession().getPersistenceContextInternal();
		// Only call PersistenceContext#getEntity within the assert expression, as it is costly
		return !persistenceContextInternal.containsEntity( entityKey )
			|| persistenceContextInternal.getEntity( entityKey ) == toInitialize;
	}

	private void initializeEntityInstance(Object toInitialize, RowProcessingState rowProcessingState) {
		final Object entityIdentifier = entityKey.getIdentifier();
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();

		if ( EntityLoadingLogging.TRACE_ENABLED ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.tracef(
					"(%s) Beginning Initializer#initializeInstance process for entity %s",
					getSimpleConcreteImplName(),
					toLoggableString( getNavigablePath(), entityIdentifier )
			);
		}

		entityDescriptor.setIdentifier( toInitialize, entityIdentifier, session );

		resolvedEntityState = extractConcreteTypeStateValues( rowProcessingState );

		if ( isPersistentAttributeInterceptable(toInitialize) ) {
			PersistentAttributeInterceptor persistentAttributeInterceptor =
					asPersistentAttributeInterceptable( toInitialize ).$$_hibernate_getInterceptor();
			if ( persistentAttributeInterceptor == null
					|| persistentAttributeInterceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
				// if we do this after the entity has been initialized the
				// BytecodeLazyAttributeInterceptor#isAttributeLoaded(String fieldName) would return false;
				concreteDescriptor.getBytecodeEnhancementMetadata()
						.injectInterceptor( toInitialize, entityIdentifier, session );
			}
		}
		concreteDescriptor.setPropertyValues( toInitialize, resolvedEntityState );

		persistenceContext.addEntity( entityKey, toInitialize );

		// Also register possible unique key entries
		registerPossibleUniqueKeyEntries( toInitialize, session );

		version = versionAssembler != null ? versionAssembler.assemble( rowProcessingState ) : null;
		final Object rowId = rowIdAssembler != null ? rowIdAssembler.assemble( rowProcessingState ) : null;

		// from the perspective of Hibernate, an entity is read locked as soon as it is read
		// so regardless of the requested lock mode, we upgrade to at least the read level
		final LockMode lockModeToAcquire = lockMode == LockMode.NONE ? LockMode.READ : lockMode;

		final EntityEntry entityEntry = persistenceContext.addEntry(
				toInitialize,
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

		registerNaturalIdResolution( persistenceContext, entityIdentifier );

		takeSnapshot( rowProcessingState, session, persistenceContext, entityEntry );

		concreteDescriptor.afterInitialize( toInitialize, session );

		if ( EntityLoadingLogging.DEBUG_ENABLED ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
					"(%s) Done materializing entityInstance : %s",
					getSimpleConcreteImplName(),
					toLoggableString( getNavigablePath(), entityIdentifier )
			);
		}

		final StatisticsImplementor statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			if ( !rowProcessingState.isQueryCacheHit() ) {
				statistics.loadEntity( concreteDescriptor.getEntityName() );
			}
		}
	}

	protected void updateCaches(
			Object toInitialize,
			RowProcessingState rowProcessingState,
			SharedSessionContractImplementor session,
			PersistenceContext persistenceContext,
			Object entityIdentifier,
			Object version) {
		// No need to put into the entity cache if this is coming from the query cache already
		final EntityDataAccess cacheAccess = concreteDescriptor.getCacheAccessStrategy();
		if ( !rowProcessingState.isQueryCacheHit() && cacheAccess != null && session.getCacheMode().isPutEnabled() ) {
			putInCache( toInitialize, session, persistenceContext, entityIdentifier, version, cacheAccess );
		}
	}

	protected void registerNaturalIdResolution(PersistenceContext persistenceContext, Object entityIdentifier) {
		if ( entityDescriptor.getNaturalIdMapping() != null ) {
			final Object naturalId =
					entityDescriptor.getNaturalIdMapping().extractNaturalIdFromEntityState( resolvedEntityState );
			persistenceContext.getNaturalIdResolutions()
					.cacheResolutionFromLoad( entityIdentifier, naturalId, entityDescriptor );
		}
	}

	protected void takeSnapshot(
			RowProcessingState rowProcessingState,
			SharedSessionContractImplementor session,
			PersistenceContext persistenceContext,
			EntityEntry entityEntry) {
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
			Object toInitialize,
			SharedSessionContractImplementor session,
			PersistenceContext persistenceContext,
			Object entityIdentifier,
			Object version,
			EntityDataAccess cacheAccess) {
		final SessionFactoryImplementor factory = session.getFactory();

		if ( EntityLoadingLogging.DEBUG_ENABLED ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
					"(%S) Adding entityInstance to second-level cache: %s",
					getSimpleConcreteImplName(),
					toLoggableString( getNavigablePath(), entityIdentifier)
			);
		}

		final CacheEntry cacheEntry = concreteDescriptor.buildCacheEntry( toInitialize, resolvedEntityState, version, session );
		final Object cacheKey = cacheAccess.generateCacheKey(
				entityIdentifier,
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
		if ( persistenceContext.wasInsertedDuringTransaction( concreteDescriptor, entityIdentifier) ) {
			cacheAccess.update(
					session,
					cacheKey,
					rootEntityDescriptor.getCacheEntryStructure().structure( cacheEntry ),
					version,
					version
			);
		}
		else {
			final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
			try {
				eventListenerManager.cachePutStart();
				final boolean put = cacheAccess.putFromLoad(
						session,
						cacheKey,
						rootEntityDescriptor.getCacheEntryStructure().structure( cacheEntry ),
						version,
						//useMinimalPuts( session, entityEntry )
						false
				);

				final StatisticsImplementor statistics = factory.getStatistics();
				if ( put && statistics.isStatisticsEnabled() ) {
					statistics.entityCachePut( rootEntityDescriptor.getNavigableRole(), cacheAccess.getRegion().getName() );
				}
			}
			finally {
				eventListenerManager.cachePutEnd();
			}
		}
	}

	protected void registerPossibleUniqueKeyEntries(Object toInitialize, SharedSessionContractImplementor session) {
		for ( Type propertyType : concreteDescriptor.getPropertyTypes() ) {
			if ( propertyType instanceof AssociationType ) {
				final AssociationType associationType = (AssociationType) propertyType;
				final String ukName = associationType.getLHSPropertyName();
				if ( ukName != null ) {
					final int index = concreteDescriptor.findAttributeMapping( ukName ).getStateArrayPosition();
					final Type type = concreteDescriptor.getPropertyTypes()[index];

					// polymorphism not really handled completely correctly,
					// perhaps...well, actually its ok, assuming that the
					// entity name used in the lookup is the same as the
					// one used here, which it will be

					if ( resolvedEntityState[index] != null ) {
						final EntityUniqueKey euk = new EntityUniqueKey(
								concreteDescriptor.getRootEntityDescriptor().getEntityName(),
								//polymorphism comment above
								ukName,
								resolvedEntityState[index],
								type,
								session.getFactory()
						);
						session.getPersistenceContextInternal().addEntity( euk, toInitialize );
					}
				}
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

	protected boolean skipInitialization(Object toInitialize, RowProcessingState rowProcessingState) {
		final EntityEntry entry =
				rowProcessingState.getSession().getPersistenceContextInternal().getEntry( toInitialize );
		if ( entry == null ) {
			return false;
		}
		// todo (6.0): do we really need this check ?
		else if ( entry.getStatus().isDeletedOrGone() ) {
			return true;
		}
		else if ( isOwningInitializer ) {
			if ( isPersistentAttributeInterceptable( toInitialize ) ) {
				final PersistentAttributeInterceptor interceptor =
						asPersistentAttributeInterceptable( toInitialize ).$$_hibernate_getInterceptor();
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
				return !isEntityReturn()
					|| rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions()
							.getEffectiveOptionalObject() != toInitialize;
			}
			else {
				return false;
			}
		}
		else {
			return true;
		}
	}

	private boolean isReadOnly(RowProcessingState rowProcessingState, SharedSessionContractImplementor persistenceContext) {
		final Boolean readOnly = rowProcessingState.getQueryOptions().isReadOnly();
		return readOnly == null ? persistenceContext.isDefaultReadOnly() : readOnly;
	}

	protected void preLoad(RowProcessingState rowProcessingState) {
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		if ( session.isEventSource() ) {
			final PreLoadEvent preLoadEvent = rowProcessingState.getJdbcValuesSourceProcessingState().getPreLoadEvent();
			assert preLoadEvent != null;

			preLoadEvent.reset();

			preLoadEvent.setEntity( entityInstance )
					.setId( entityKey.getIdentifier() )
					.setPersister( concreteDescriptor );

			session.getFactory()
					.getFastSessionServices()
					.eventListenerGroup_PRE_LOAD
					.fireEventOnEachListener( preLoadEvent, PreLoadEventListener::onPreLoad );
		}
	}

	@Override
	public boolean isEntityInitialized() {
		return isInitialized;
	}

	protected void setEntityInitialized(boolean isInitialized) {
		this.isInitialized = isInitialized;
	}

	protected void setEntityInstanceForNotify(Object entityInstanceForNotify) {
		this.entityInstanceForNotify = entityInstanceForNotify;
	}

	protected Object getEntityInstanceForNotify() {
		return entityInstanceForNotify;
	}

	protected void setResolvedEntityState(Object[] resolvedEntityState) {
		this.resolvedEntityState = resolvedEntityState;
	}

	protected Object[] getResolvedEntityState() {
		return resolvedEntityState;
	}

	@Override
	public EntityPersister getConcreteDescriptor() {
		return concreteDescriptor == null ? entityDescriptor : concreteDescriptor;
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

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		if ( resolvedEntityState != null ) {
			updateCaches(
					entityInstanceForNotify,
					rowProcessingState,
					session,
					session.getPersistenceContext(),
					entityKey.getIdentifier(),
					version
			);
		}

		// reset row state
		isOwningInitializer = false;
		concreteDescriptor = null;
		entityKey = null;
		version = null;
		entityInstance = null;
		entityInstanceForNotify = null;
		missing = false;
		resolvedEntityState = null;
		isInitialized = false;
		clearResolutionListeners();
	}
}
