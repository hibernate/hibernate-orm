/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.Collection;
import java.util.function.BiConsumer;

import org.hibernate.EntityFilterException;
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
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.basic.BasicResultAssembler;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
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
import static org.hibernate.metamodel.mapping.ForeignKeyDescriptor.Nature.TARGET;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * @author Andrea Boriero
 */
public class EntityInitializerImpl extends AbstractInitializer<EntityInitializerImpl.EntityInitializerData>
		implements EntityInitializer<EntityInitializerImpl.EntityInitializerData> {

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
	private final String sourceAlias;
	private final @Nullable InitializerParent<?> parent;
	private final NotFoundAction notFoundAction;
	private final boolean affectedByFilter;
	private final boolean isPartOfKey;
	private final boolean isResultInitializer;
	private final boolean hasKeyManyToOne;

	private final @Nullable DomainResultAssembler<?> keyAssembler;
	private final @Nullable DomainResultAssembler<?> identifierAssembler;
	private final @Nullable BasicResultAssembler<?> discriminatorAssembler;
	private final @Nullable DomainResultAssembler<?> versionAssembler;
	private final @Nullable DomainResultAssembler<Object> rowIdAssembler;

	private final DomainResultAssembler<?>[][] assemblers;
	private final Initializer<?>[][] subInitializers;

	public static class EntityInitializerData extends InitializerData {

		protected boolean shallowCached;
		protected LockMode lockMode;
		protected String uniqueKeyAttributePath;
		protected Type[] uniqueKeyPropertyTypes;
		protected boolean canUseEmbeddedIdentifierInstanceAsEntity;
		protected boolean hasCallbackActions;

		// per-row state
		protected @Nullable EntityPersister concreteDescriptor;
		protected @Nullable EntityKey entityKey;
		protected @Nullable Object entityInstanceForNotify;
		protected @Nullable EntityHolder entityHolder;

		public EntityInitializerData(RowProcessingState rowProcessingState) {
			super( rowProcessingState );
		}
	}

	public EntityInitializerImpl(
			EntityResultGraphNode resultDescriptor,
			String sourceAlias,
			@Nullable Fetch identifierFetch,
			@Nullable Fetch discriminatorFetch,
			@Nullable DomainResult<?> keyResult,
			@Nullable DomainResult<Object> rowIdResult,
			NotFoundAction notFoundAction,
			boolean affectedByFilter,
			@Nullable InitializerParent<?> parent,
			boolean isResultInitializer,
			AssemblerCreationState creationState) {
		super( creationState );

		referencedModelPart = resultDescriptor.getEntityValuedModelPart();
		entityDescriptor = (EntityPersister) referencedModelPart.getEntityMappingType();

		final String rootEntityName = entityDescriptor.getRootEntityName();
		rootEntityDescriptor = rootEntityName == null || rootEntityName.equals( entityDescriptor.getEntityName() )
				? entityDescriptor
				: entityDescriptor.getRootEntityDescriptor().getEntityPersister();

		this.navigablePath = resultDescriptor.getNavigablePath();
		this.sourceAlias = sourceAlias;
		this.parent = parent;
		this.isResultInitializer = isResultInitializer;
		this.isPartOfKey = Initializer.isPartOfKey( navigablePath, parent );

		assert identifierFetch != null || isResultInitializer : "Identifier must be fetched, unless this is a result initializer";
		if ( identifierFetch == null ) {
			identifierAssembler = null;
			hasKeyManyToOne = false;
		}
		else {
			identifierAssembler = identifierFetch.createAssembler( this, creationState );
			final Initializer<?> initializer = identifierAssembler.getInitializer();
			// For now, assume key many to ones if the identifier has an initializer
			// todo: improve this
			hasKeyManyToOne = initializer != null;
		}

		assert entityDescriptor.hasSubclasses() == (discriminatorFetch != null) : "Discriminator should only be fetched if the entity has subclasses";
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
		final DomainResultAssembler<?>[][] assemblers = new DomainResultAssembler[subMappingTypes.size() + 1][];
		final Initializer<?>[][] subInitializers = new Initializer<?>[subMappingTypes.size() + 1][];
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
					: fetch.createAssembler( this, creationState );

			final int stateArrayPosition = attributeMapping.getStateArrayPosition();
			final EntityMappingType declaringType = attributeMapping.getDeclaringType().asEntityMappingType();
			final int subclassId = declaringType.getSubclassId();

			final Initializer<?> subInitializer = stateAssembler.getInitializer();
			if ( subInitializer != null ) {
				if ( subInitializers[subclassId] == null ) {
					subInitializers[subclassId] = new Initializer<?>[size];
				}
				subInitializers[subclassId][stateArrayPosition] = subInitializer;
			}

			assemblers[subclassId][stateArrayPosition] = stateAssembler;
			for ( EntityMappingType subMappingType : declaringType.getSubMappingTypes() ) {
				assemblers[subMappingType.getSubclassId()][stateArrayPosition] = stateAssembler;
				if ( subInitializer != null ) {
					if ( subInitializers[subMappingType.getSubclassId()] == null ) {
						subInitializers[subMappingType.getSubclassId()] = new Initializer<?>[size];
					}
					subInitializers[subMappingType.getSubclassId()][stateArrayPosition] = subInitializer;
				}
			}
		}
		OUTER: for ( int i = 0; i < subInitializers.length; i++ ) {
			if ( subInitializers[i] != null ) {
				for ( Initializer<?> initializer : subInitializers[i] ) {
					if ( initializer != null ) {
						continue OUTER;
					}
				}
			}
			subInitializers[i] = Initializer.EMPTY_ARRAY;
		}

		this.assemblers = assemblers;
		this.subInitializers = subInitializers;
		this.notFoundAction = notFoundAction;

		this.keyAssembler = keyResult == null ? null : keyResult.createResultAssembler( this, creationState );
		this.affectedByFilter = affectedByFilter;
	}

	@Override
	protected EntityInitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new EntityInitializerData( rowProcessingState );
	}

	@Override
	public void resolveKey(EntityInitializerData data) {
		resolveKey( data, false );
	}

	@Override
	public @Nullable Object getEntityIdentifier(EntityInitializerData data) {
		return data.entityKey == null ? null : data.entityKey.getIdentifier();
	}

	@Override
	public @Nullable EntityKey resolveEntityKeyOnly(RowProcessingState rowProcessingState) {
		final EntityInitializerData data = getData( rowProcessingState );
		resolveKey( data, true );
		if ( data.getState() == State.MISSING ) {
			return null;
		}
		if ( data.entityKey == null ) {
			assert identifierAssembler != null;
			final Object id = identifierAssembler.assemble( rowProcessingState );
			if ( id == null ) {
				setMissing( data );
				return null;
			}
			resolveEntityKey( data, id );
		}
		return data.entityKey;
	}

	protected void resolveKey(EntityInitializerData data, boolean entityKeyOnly) {
		// todo (6.0) : atm we do not handle sequential selects
		// 		- see AbstractEntityPersister#hasSequentialSelect and
		//			AbstractEntityPersister#getSequentialSelect in 5.2
		if ( data.getState() != State.UNINITIALIZED ) {
			return;
		}
		data.setState( State.KEY_RESOLVED );

		// reset row state
		data.concreteDescriptor = null;
		data.entityKey = null;
		data.setInstance( null );
		data.entityInstanceForNotify = null;
		data.entityHolder = null;

		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final Object id;
		if ( identifierAssembler == null ) {
			id = rowProcessingState.getEntityId();
			assert id != null : "Initializer requires a not null id for loading";
		}
		else {
			//noinspection unchecked
			final Initializer<InitializerData> initializer = (Initializer<InitializerData>) identifierAssembler.getInitializer();
			if ( initializer != null ) {
				final InitializerData subData = initializer.getData( rowProcessingState );
				initializer.resolveKey( subData );
				if ( subData.getState() == State.MISSING ) {
					setMissing( data );
					return;
				}
				else {
					data.concreteDescriptor = determineConcreteEntityDescriptor(
							rowProcessingState,
							discriminatorAssembler,
							entityDescriptor
					);
					assert data.concreteDescriptor != null;
					if ( hasKeyManyToOne ) {
						if ( !data.shallowCached && !entityKeyOnly ) {
							resolveKeySubInitializers( data );
						}
						return;
					}
				}
			}
			id = identifierAssembler.assemble( rowProcessingState );
			if ( id == null ) {
				setMissing( data );
				return;
			}
		}

		resolveEntityKey( data, id );
		if ( !entityKeyOnly ) {
			// Resolve the entity instance early as we have no key many-to-one
			resolveInstance( data );
			if ( !data.shallowCached ) {
				if ( data.getState() == State.INITIALIZED ) {
					if ( data.entityHolder.getEntityInitializer() == null ) {
						// The entity is already part of the persistence context,
						// so let's figure out the loaded state and only run sub-initializers if necessary
						resolveInstanceSubInitializers( data );
					}
					// If the entity is initialized and getEntityInitializer() == this,
					// we already processed a row for this entity before,
					// but we still have to call resolveKeySubInitializers to activate sub-initializers,
					// because a row might contain data that sub-initializers want to consume
					else {
						// todo: try to diff the eagerness of the sub-initializers to avoid further processing
						resolveKeySubInitializers( data );
					}
				}
				else {
					resolveKeySubInitializers( data );
				}
			}
		}
	}

	protected void resolveInstanceSubInitializers(EntityInitializerData data) {
		final Initializer<?>[] initializers = subInitializers[data.concreteDescriptor.getSubclassId()];
		if ( initializers.length == 0 ) {
			return;
		}
		final EntityEntry entityEntry = data.entityHolder.getEntityEntry();
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		assert entityEntry == rowProcessingState.getSession()
				.getPersistenceContextInternal()
				.getEntry( data.entityInstanceForNotify );
		final Object[] loadedState = entityEntry.getLoadedState();
		final Object[] state;
		if ( loadedState == null ) {
			if ( entityEntry.getStatus() == Status.READ_ONLY ) {
				state = data.concreteDescriptor.getValues( data.entityInstanceForNotify );
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
		for ( int i = 0; i < initializers.length; i++ ) {
			final Initializer<?> initializer = initializers[i];
			if ( initializer != null ) {
				final Object subInstance = state[i];
				if ( subInstance == UNFETCHED_PROPERTY ) {
					// Go through the normal initializer process
					initializer.resolveKey( rowProcessingState );
				}
				else {
					initializer.resolveInstance( subInstance, rowProcessingState );
				}
			}
		}
	}

	private void resolveKeySubInitializers(EntityInitializerData data) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		for ( Initializer<?> initializer : subInitializers[data.concreteDescriptor.getSubclassId()] ) {
			if ( initializer != null ) {
				initializer.resolveKey( rowProcessingState );
			}
		}
	}

	@EnsuresNonNull( "entityKey" )
	protected void resolveEntityKey(EntityInitializerData data, Object id) {
		if ( data.concreteDescriptor == null ) {
			data.concreteDescriptor = determineConcreteEntityDescriptor(
					data.getRowProcessingState(),
					discriminatorAssembler,
					entityDescriptor
			);
			assert data.concreteDescriptor != null;
		}
		data.entityKey = new EntityKey( id, data.concreteDescriptor );
	}

	protected void setMissing(EntityInitializerData data) {
		data.entityKey = null;
		data.concreteDescriptor = null;
		data.setInstance( null );
		data.entityInstanceForNotify = null;
		data.entityHolder = null;
		data.setState( State.MISSING );

		// super processes the foreign-key target column.  here we
		// need to also look at the foreign-key value column to check
		// for a dangling foreign-key

		if ( keyAssembler != null ) {
			final Object fkKeyValue = keyAssembler.assemble( data.getRowProcessingState() );
			if ( fkKeyValue != null ) {
				if ( notFoundAction != NotFoundAction.IGNORE ) {
					if ( affectedByFilter ) {
						throw new EntityFilterException(
								getEntityDescriptor().getEntityName(),
								fkKeyValue,
								referencedModelPart.getNavigableRole().getFullPath()
						);
					}
					throw new FetchNotFoundException(
							getEntityDescriptor().getEntityName(),
							fkKeyValue
					);
				}
			}
		}
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance, EntityInitializerData data) {
		final AttributeMapping attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance = attributeMapping != null
				? attributeMapping.getValue( parentInstance )
				: parentInstance;
		final SharedSessionContractImplementor session = data.getRowProcessingState().getSession();
		if ( instance == null ) {
			setMissing( data );
		}
		else {
			data.setInstance( instance );
			data.entityInstanceForNotify = Hibernate.unproxy( instance );
			data.concreteDescriptor = session.getEntityPersister( null, data.entityInstanceForNotify );
			resolveEntityKey(
					data,
					data.concreteDescriptor.getIdentifier( data.entityInstanceForNotify, session )
			);
			data.entityHolder = session.getPersistenceContextInternal().getEntityHolder( data.entityKey );
			data.setState( State.INITIALIZED );
			initializeSubInstancesFromParent( data );
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
	public Object getEntityInstance(EntityInitializerData data) {
		return data.getInstance();
	}

	@Override
	public Object getTargetInstance(EntityInitializerData data) {
		return data.entityInstanceForNotify;
	}

	@Override
	public @Nullable InitializerParent<?> getParent() {
		return parent;
	}

	@Override
	public void startLoading(RowProcessingState rowProcessingState) {
		final EntityInitializerData data = createInitializerData( rowProcessingState );
		rowProcessingState.setInitializerData( initializerId, data );
		if ( rowProcessingState.isQueryCacheHit() && entityDescriptor.useShallowQueryCacheLayout() ) {
			data.shallowCached = true;
		}
		data.lockMode = rowProcessingState.determineEffectiveLockMode( sourceAlias );
		if ( isResultInitializer() ) {
			data.uniqueKeyAttributePath = rowProcessingState.getEntityUniqueKeyAttributePath();
			if ( data.uniqueKeyAttributePath != null ) {
				data.uniqueKeyPropertyTypes = getParentEntityAttributeTypes( data.uniqueKeyAttributePath );
			}
			else {
				data.uniqueKeyPropertyTypes = null;
			}
			data.canUseEmbeddedIdentifierInstanceAsEntity = data.getRowProcessingState().getEntityId() != null
					// The id can only be the entity instance if this is a non-aggregated id that has no containing class
					&& entityDescriptor.getIdentifierMapping() instanceof CompositeIdentifierMapping
					&& !( (CompositeIdentifierMapping) entityDescriptor.getIdentifierMapping() ).hasContainingClass();
		}
		else {
			data.uniqueKeyAttributePath = null;
			data.uniqueKeyPropertyTypes = null;
			data.canUseEmbeddedIdentifierInstanceAsEntity = false;
		}
		data.hasCallbackActions = rowProcessingState.hasCallbackActions();
		forEachSubInitializer( Initializer::startLoading, data );
	}

	protected Type[] getParentEntityAttributeTypes(String attributeName) {
		final Type[] attributeTypes = new Type[
				entityDescriptor.getRootEntityDescriptor()
						.getSubclassEntityNames()
						.size()
				];
		initializeAttributeType( attributeTypes, entityDescriptor, attributeName );
		for ( EntityMappingType subMappingType : entityDescriptor.getSubMappingTypes() ) {
			initializeAttributeType( attributeTypes, subMappingType.getEntityPersister(), attributeName );
		}
		return attributeTypes;
	}

	protected void initializeAttributeType(Type[] attributeTypes, EntityPersister entityDescriptor, String attributeName) {
		if ( entityDescriptor.findByPath( attributeName ) != null ) {
			attributeTypes[entityDescriptor.getSubclassId()] = entityDescriptor.getPropertyType( attributeName );
		}
	}

	public static @Nullable EntityPersister determineConcreteEntityDescriptor(
			RowProcessingState rowProcessingState,
			BasicResultAssembler<?> discriminatorAssembler,
			EntityPersister entityDescriptor)
			throws WrongClassException {
		if ( discriminatorAssembler == null
				|| rowProcessingState.isQueryCacheHit() && entityDescriptor.useShallowQueryCacheLayout() && !entityDescriptor.storeDiscriminatorInShallowQueryCacheLayout() ) {
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

	private boolean useEmbeddedIdentifierInstanceAsEntity(EntityInitializerData data) {
		return data.canUseEmbeddedIdentifierInstanceAsEntity
				&& ( data.concreteDescriptor = determineConcreteEntityDescriptor( data.getRowProcessingState(), discriminatorAssembler, entityDescriptor ) ) != null
				&& data.concreteDescriptor.isInstance( data.getRowProcessingState().getEntityId() );
	}

	@Override
	public void resolveInstance(Object instance, EntityInitializerData data) {
		if ( instance == null ) {
			setMissing( data );
			return;
		}
		data.setInstance( instance );
		final LazyInitializer lazyInitializer = extractLazyInitializer( data.getInstance() );
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		if ( lazyInitializer == null ) {
			// Entity is most probably initialized
			data.entityInstanceForNotify = data.getInstance();
			data.concreteDescriptor = session.getEntityPersister( null, data.getInstance() );
			resolveEntityKey(
					data,
					data.concreteDescriptor.getIdentifier( data.getInstance(), session )
			);
			data.entityHolder = session.getPersistenceContextInternal().getEntityHolder( data.entityKey );
			if ( data.entityHolder == null ) {
				// Entity was most probably removed in the same session without setting the reference to null
				resolveKey( data );
				assert data.getState() == State.MISSING;
				assert referencedModelPart instanceof ToOneAttributeMapping
						&& ( (ToOneAttributeMapping) referencedModelPart ).getSideNature() == TARGET;
				return;
			}
			// If the entity initializer is null, we know the entity is fully initialized,
			// otherwise it will be initialized by some other initializer
			data.setState( data.entityHolder.getEntityInitializer() == null ? State.INITIALIZED : State.RESOLVED );
		}
		else if ( lazyInitializer.isUninitialized() ) {
			data.setState( State.RESOLVED );
			// Read the discriminator from the result set if necessary
			data.concreteDescriptor = discriminatorAssembler == null
					? entityDescriptor
					: determineConcreteEntityDescriptor( rowProcessingState, discriminatorAssembler, entityDescriptor );
			assert data.concreteDescriptor != null;
			resolveEntityKey( data, lazyInitializer.getIdentifier() );
			data.entityHolder = session.getPersistenceContextInternal().claimEntityHolderIfPossible(
					data.entityKey,
					null,
					rowProcessingState.getJdbcValuesSourceProcessingState(),
					this
			);
			// Resolve and potentially create the entity instance
			data.entityInstanceForNotify = resolveEntityInstance( data );
			lazyInitializer.setImplementation( data.entityInstanceForNotify );
			registerLoadingEntity( data, data.entityInstanceForNotify );
		}
		else {
			data.setState( State.INITIALIZED );
			data.entityInstanceForNotify = lazyInitializer.getImplementation();
			data.concreteDescriptor = session.getEntityPersister( null, data.entityInstanceForNotify );
			resolveEntityKey( data, lazyInitializer.getIdentifier() );
			data.entityHolder = session.getPersistenceContextInternal().getEntityHolder( data.entityKey );
		}
		if ( identifierAssembler != null ) {
			final Initializer<?> initializer = identifierAssembler.getInitializer();
			if ( initializer != null ) {
				initializer.resolveInstance( data.entityKey.getIdentifier(), rowProcessingState );
			}
		}
		upgradeLockMode( data );
		if ( data.getState() == State.INITIALIZED ) {
			registerReloadedEntity( data );
			resolveInstanceSubInitializers( data );
			if ( rowProcessingState.needsResolveState() ) {
				// We need to read result set values to correctly populate the query cache
				resolveState( data );
			}
		}
		else {
			resolveKeySubInitializers( data );
		}
	}

	@Override
	public void resolveInstance(EntityInitializerData data) {
		if ( data.getState() != State.KEY_RESOLVED ) {
			return;
		}
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		data.setState( State.RESOLVED );
		if ( data.entityKey == null ) {
			assert identifierAssembler != null;
			final Object id = identifierAssembler.assemble( rowProcessingState );
			if ( id == null ) {
				setMissing( data );
				return;
			}
			resolveEntityKey( data, id );
		}
		final PersistenceContext persistenceContext = rowProcessingState.getSession()
				.getPersistenceContextInternal();
		data.entityHolder = persistenceContext.claimEntityHolderIfPossible(
				data.entityKey,
				null,
				rowProcessingState.getJdbcValuesSourceProcessingState(),
				this
		);

		if ( useEmbeddedIdentifierInstanceAsEntity( data ) ) {
			data.setInstance( data.entityInstanceForNotify = rowProcessingState.getEntityId() );
		}
		else {
			resolveEntityInstance1( data );
			if ( data.uniqueKeyAttributePath != null ) {
				final SharedSessionContractImplementor session = rowProcessingState.getSession();
				final EntityPersister concreteDescriptor = getConcreteDescriptor( data );
				final EntityUniqueKey euk = new EntityUniqueKey(
						concreteDescriptor.getEntityName(),
						data.uniqueKeyAttributePath,
						rowProcessingState.getEntityUniqueKey(),
						data.uniqueKeyPropertyTypes[concreteDescriptor.getSubclassId()],
						session.getFactory()
				);
				session.getPersistenceContextInternal().addEntity( euk, getEntityInstance( data ) );
			}
		}

		if ( data.getInstance() != null ) {
			upgradeLockMode( data );
			if ( data.getState() == State.INITIALIZED ) {
				registerReloadedEntity( data );
				if ( rowProcessingState.needsResolveState() ) {
					// We need to read result set values to correctly populate the query cache
					resolveState( data );
				}
			}
			if ( data.shallowCached ) {
				initializeSubInstancesFromParent( data );
			}
		}
	}

	protected void resolveEntityInstance1(EntityInitializerData data) {
		final Object proxy = data.entityHolder.getProxy();
		final boolean unwrapProxy = proxy != null && referencedModelPart instanceof ToOneAttributeMapping
				&& ( (ToOneAttributeMapping) referencedModelPart ).isUnwrapProxy()
				&& getConcreteDescriptor( data ).getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
		final Object entityFromExecutionContext;
		if ( !unwrapProxy && isProxyInstance( proxy ) ) {
			if ( ( entityFromExecutionContext = getEntityFromExecutionContext( data ) ) != null ) {
				data.setInstance( data.entityInstanceForNotify = entityFromExecutionContext );
				// If the entity comes from the execution context, it is treated as not initialized
				// so that we can refresh the data as requested
				registerReloadedEntity( data );
			}
			else {
				data.setInstance( proxy );
				if ( Hibernate.isInitialized( data.getInstance() ) ) {
					data.setState( State.INITIALIZED );
					data.entityInstanceForNotify = Hibernate.unproxy( data.getInstance() );
				}
				else {
					final LazyInitializer lazyInitializer = extractLazyInitializer( data.getInstance() );
					assert lazyInitializer != null;
					data.entityInstanceForNotify = resolveEntityInstance2( data );
					lazyInitializer.setImplementation( data.entityInstanceForNotify );
				}
			}
		}
		else {
			final Object existingEntity = data.entityHolder.getEntity();
			if ( existingEntity != null ) {
				data.setInstance( data.entityInstanceForNotify = existingEntity );
				if ( data.entityHolder.getEntityInitializer() == null ) {
					assert data.entityHolder.isInitialized() == isExistingEntityInitialized( existingEntity );
					if ( data.entityHolder.isInitialized() ) {
						data.setState( State.INITIALIZED );
					}
					else if ( isResultInitializer() ) {
						registerLoadingEntity( data, data.getInstance() );
					}
				}
				else if ( data.entityHolder.getEntityInitializer() != this ) {
					data.setState( State.INITIALIZED );
				}
			}
			else if ( ( entityFromExecutionContext = getEntityFromExecutionContext( data ) ) != null ) {
				// This is the entity to refresh, so don't set the state to initialized
				data.setInstance( data.entityInstanceForNotify = entityFromExecutionContext );
				if ( isResultInitializer() ) {
					registerLoadingEntity( data, data.getInstance() );
				}
			}
			else {
				assert data.entityHolder.getEntityInitializer() == this;
				// look to see if another initializer from a parent load context or an earlier
				// initializer is already loading the entity
				data.setInstance( data.entityInstanceForNotify = resolveEntityInstance2( data ) );
				final Initializer<?> idInitializer;
				if ( data.entityHolder.getEntityInitializer() == this && data.getState() != State.INITIALIZED
						&& identifierAssembler != null
						&& ( idInitializer = identifierAssembler.getInitializer() ) != null ) {
					// If this is the owning initializer and the returned object is not initialized,
					// this means that the entity instance was just instantiated.
					// In this case, we want to call "assemble" and hence "initializeInstance" on the initializer
					// for possibly non-aggregated identifier mappings, so inject the virtual id representation
					idInitializer.initializeInstance( data.getRowProcessingState() );
				}
			}
		}
		// todo: ensure we initialize the entity
		assert !data.shallowCached || data.getState() == State.INITIALIZED : "Forgot to initialize the entity";
	}

	protected Object getEntityFromExecutionContext(EntityInitializerData data) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final ExecutionContext executionContext = rowProcessingState.getJdbcValuesSourceProcessingState()
				.getExecutionContext();
		if ( rootEntityDescriptor == executionContext.getRootEntityDescriptor()
				&& data.entityKey.getIdentifier().equals( executionContext.getEntityId() ) ) {
			return executionContext.getEntityInstance();
		}
		return null;
	}

	private void upgradeLockMode(EntityInitializerData data) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		if ( data.lockMode != LockMode.NONE && rowProcessingState.upgradeLocks() ) {
			final EntityEntry entry = data.entityHolder.getEntityEntry();
			assert entry == rowProcessingState.getSession().getPersistenceContextInternal()
							.getEntry( data.entityInstanceForNotify );
			if ( entry != null && entry.getLockMode().lessThan( data.lockMode ) ) {
				//we only check the version when _upgrading_ lock modes
				if ( versionAssembler != null && entry.getLockMode() != LockMode.NONE ) {
					checkVersion( data, entry, rowProcessingState );
				}
				//we need to upgrade the lock mode to the mode requested
				entry.setLockMode( data.lockMode );
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
	private void checkVersion(
			EntityInitializerData data,
			EntityEntry entry,
			final RowProcessingState rowProcessingState) throws HibernateException {
		final Object version = entry.getVersion();
		if ( version != null ) {
			// null version means the object is in the process of being loaded somewhere else in the ResultSet
			final Object currentVersion = versionAssembler.assemble( rowProcessingState );
			if ( !data.concreteDescriptor.getVersionType().isEqual( version, currentVersion ) ) {
				final StatisticsImplementor statistics = rowProcessingState.getSession().getFactory().getStatistics();
				if ( statistics.isStatisticsEnabled() ) {
					statistics.optimisticFailure( data.concreteDescriptor.getEntityName() );
				}
				throw new StaleObjectStateException( data.concreteDescriptor.getEntityName(), entry.getId() );
			}
		}

	}

	/**
	 * Used by Hibernate Reactive
	 */
	protected Object resolveEntityInstance2(EntityInitializerData data) {
		if ( data.entityHolder.getEntityInitializer() == this ) {
			assert data.entityHolder.getEntity() == null;
			return resolveEntityInstance( data );
		}
		else {
			// the entity is already being loaded elsewhere
			return data.entityHolder.getEntity();
		}
	}

	protected Object resolveEntityInstance(EntityInitializerData data) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final Object resolved = resolveToOptionalInstance( data );
		if ( resolved != null ) {
			registerLoadingEntity( data, resolved );
			return resolved;
		}
		else {
			if ( rowProcessingState.isQueryCacheHit() && entityDescriptor.useShallowQueryCacheLayout() ) {
				// We must load the entity this way, because the query cache entry contains only the primary key
				data.setState( State.INITIALIZED );
				final SharedSessionContractImplementor session = rowProcessingState.getSession();
				assert data.entityHolder.getEntityInitializer() == this;
				// If this initializer owns the entity, we have to remove the entity holder,
				// because the subsequent loading process will claim the entity
				rowProcessingState.getJdbcValuesSourceProcessingState().getLoadingEntityHolders().remove( data.entityHolder );
				session.getPersistenceContextInternal().removeEntityHolder( data.entityKey );
				return session.internalLoad(
						data.concreteDescriptor.getEntityName(),
						data.entityKey.getIdentifier(),
						true,
						false
				);
			}
			// We have to query the second level cache if reference cache entries are used
			else if ( entityDescriptor.canUseReferenceCacheEntries() ) {
				final Object cached = resolveInstanceFromCache( data );
				if ( cached != null ) {
					// EARLY EXIT!!!
					// because the second level cache has reference cache entries, the entity is initialized
					data.setState( State.INITIALIZED );
					return cached;
				}
			}
			final Object instance = instantiateEntity( data );
			registerLoadingEntity( data, instance );
			return instance;
		}
	}

	protected Object instantiateEntity(EntityInitializerData data) {
		final Object instance = data.getRowProcessingState().getSession()
				.instantiate( data.concreteDescriptor, data.entityKey.getIdentifier() );
		return instance;
	}

	private Object resolveToOptionalInstance(EntityInitializerData data) {
		if ( isResultInitializer() ) {
			// this isEntityReturn bit is just for entity loaders, not hql/criteria
			final JdbcValuesSourceProcessingOptions processingOptions =
					data.getRowProcessingState().getJdbcValuesSourceProcessingState().getProcessingOptions();
			return matchesOptionalInstance( data, processingOptions ) ? processingOptions.getEffectiveOptionalObject() : null;
		}
		else {
			return null;
		}
	}

	private boolean matchesOptionalInstance(
			EntityInitializerData data,
			JdbcValuesSourceProcessingOptions processingOptions) {
		final Object optionalEntityInstance = processingOptions.getEffectiveOptionalObject();
		final Object requestedEntityId = processingOptions.getEffectiveOptionalId();
		return requestedEntityId != null
				&& optionalEntityInstance != null
				&& requestedEntityId.equals( data.entityKey.getIdentifier() );
	}

	private Object resolveInstanceFromCache(EntityInitializerData data) {
		return CacheEntityLoaderHelper.INSTANCE.loadFromSecondLevelCache(
				data.getRowProcessingState().getSession().asEventSource(),
				null,
				data.lockMode,
				entityDescriptor,
				data.entityKey
		);
	}

	protected void registerLoadingEntity(EntityInitializerData data, Object instance) {
		data.getRowProcessingState().getSession().getPersistenceContextInternal().claimEntityHolderIfPossible(
				data.entityKey,
				instance,
				data.getRowProcessingState().getJdbcValuesSourceProcessingState(),
				this
		);
	}

	protected void registerReloadedEntity(EntityInitializerData data) {
		if ( data.hasCallbackActions ) {
			// This is only needed for follow-on locking, so skip registering the entity if there is no callback
			data.entityHolder.markAsReloaded( data.getRowProcessingState().getJdbcValuesSourceProcessingState() );
		}
	}

	@Override
	public void initializeInstance(EntityInitializerData data) {
		if ( data.getState() != State.RESOLVED ) {
			return;
		}
		if ( !skipInitialization( data ) ) {
			assert consistentInstance( data );
			initializeEntityInstance( data );
		}
		data.setState( State.INITIALIZED );
	}

	protected boolean consistentInstance(EntityInitializerData data) {
		final PersistenceContext persistenceContextInternal =
				data.getRowProcessingState().getSession().getPersistenceContextInternal();
		// Only call PersistenceContext#getEntity within the assert expression, as it is costly
		final Object entity = persistenceContextInternal.getEntity( data.entityKey );
		return entity == null || entity == data.entityInstanceForNotify;
	}

	private void initializeEntityInstance(EntityInitializerData data) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final Object entityIdentifier = data.entityKey.getIdentifier();
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();

		final Object[] resolvedEntityState = extractConcreteTypeStateValues( data );

		preLoad( data, resolvedEntityState );

		if ( isPersistentAttributeInterceptable( data.entityInstanceForNotify ) ) {
			final PersistentAttributeInterceptor persistentAttributeInterceptor =
					asPersistentAttributeInterceptable( data.entityInstanceForNotify ).$$_hibernate_getInterceptor();
			if ( persistentAttributeInterceptor == null
					|| persistentAttributeInterceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
				// if we do this after the entity has been initialized the
				// BytecodeLazyAttributeInterceptor#isAttributeLoaded(String fieldName) would return false;
				data.concreteDescriptor.getBytecodeEnhancementMetadata()
						.injectInterceptor( data.entityInstanceForNotify, entityIdentifier, session );
			}
		}
		data.concreteDescriptor.setPropertyValues( data.entityInstanceForNotify, resolvedEntityState );

		persistenceContext.addEntity( data.entityKey, data.entityInstanceForNotify );

		// Also register possible unique key entries
		registerPossibleUniqueKeyEntries( data, resolvedEntityState, session );

		final Object version = versionAssembler != null ? versionAssembler.assemble( rowProcessingState ) : null;
		final Object rowId = rowIdAssembler != null ? rowIdAssembler.assemble( rowProcessingState ) : null;

		// from the perspective of Hibernate, an entity is read locked as soon as it is read
		// so regardless of the requested lock mode, we upgrade to at least the read level
		final LockMode lockModeToAcquire = data.lockMode == LockMode.NONE ? LockMode.READ : data.lockMode;

		final EntityEntry entityEntry = persistenceContext.addEntry(
				data.entityInstanceForNotify,
				Status.LOADING,
				resolvedEntityState,
				rowId,
				data.entityKey.getIdentifier(),
				version,
				lockModeToAcquire,
				true,
				data.concreteDescriptor,
				false
		);
		data.entityHolder.setEntityEntry( entityEntry );

		registerNaturalIdResolution( data, persistenceContext, resolvedEntityState );

		takeSnapshot( data, session, persistenceContext, entityEntry, resolvedEntityState );

		data.concreteDescriptor.afterInitialize( data.entityInstanceForNotify, session );

		assert data.concreteDescriptor.getIdentifier( data.entityInstanceForNotify, session ) != null;

		final StatisticsImplementor statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			if ( !rowProcessingState.isQueryCacheHit() ) {
				statistics.loadEntity( data.concreteDescriptor.getEntityName() );
			}
		}
		updateCaches(
				data,
				session,
				session.getPersistenceContextInternal(),
				resolvedEntityState,
				version
		);
	}

	protected void updateCaches(
			EntityInitializerData data,
			SharedSessionContractImplementor session,
			PersistenceContext persistenceContext,
			Object[] resolvedEntityState,
			Object version) {
		if ( data.concreteDescriptor.canWriteToCache()
				// No need to put into the entity cache if this is coming from the query cache already
				&& !data.getRowProcessingState().isQueryCacheHit()
				&& session.getCacheMode().isPutEnabled() ) {
			final EntityDataAccess cacheAccess = data.concreteDescriptor.getCacheAccessStrategy();
			if ( cacheAccess != null  ) {
				putInCache( data, session, persistenceContext, resolvedEntityState, version, cacheAccess );
			}
		}
	}

	protected void registerNaturalIdResolution(
			EntityInitializerData data,
			PersistenceContext persistenceContext,
			Object[] resolvedEntityState) {
		if ( entityDescriptor.getNaturalIdMapping() != null ) {
			final Object naturalId =
					entityDescriptor.getNaturalIdMapping().extractNaturalIdFromEntityState( resolvedEntityState );
			persistenceContext.getNaturalIdResolutions()
					.cacheResolutionFromLoad( data.entityKey.getIdentifier(), naturalId, entityDescriptor );
		}
	}

	protected void takeSnapshot(
			EntityInitializerData data,
			SharedSessionContractImplementor session,
			PersistenceContext persistenceContext,
			EntityEntry entityEntry,
			Object[] resolvedEntityState) {
		if ( isReallyReadOnly( data, session ) ) {
			//no need to take a snapshot - this is a
			//performance optimization, but not really
			//important, except for entities with huge
			//mutable property values
			persistenceContext.setEntryStatus( entityEntry, Status.READ_ONLY );
		}
		else {
			//take a snapshot
			deepCopy( data.concreteDescriptor, resolvedEntityState, resolvedEntityState );
			persistenceContext.setEntryStatus( entityEntry, Status.MANAGED );
		}
	}

	private boolean isReallyReadOnly(EntityInitializerData data, SharedSessionContractImplementor session) {
		if ( !data.concreteDescriptor.isMutable() ) {
			return true;
		}
		else {
			final LazyInitializer lazyInitializer = extractLazyInitializer( data.getInstance() );
			if ( lazyInitializer != null ) {
				// there is already a proxy for this impl
				// only set the status to read-only if the proxy is read-only
				return lazyInitializer.isReadOnly();
			}
			else {
				return isReadOnly( data.getRowProcessingState(), session );
			}
		}
	}

	private void putInCache(
			EntityInitializerData data,
			SharedSessionContractImplementor session,
			PersistenceContext persistenceContext,
			Object[] resolvedEntityState,
			Object version,
			EntityDataAccess cacheAccess) {
		final SessionFactoryImplementor factory = session.getFactory();

		final CacheEntry cacheEntry = data.concreteDescriptor.buildCacheEntry(
				data.entityInstanceForNotify,
				resolvedEntityState,
				version,
				session
		);
		final Object cacheKey = cacheAccess.generateCacheKey(
				data.entityKey.getIdentifier(),
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
		if ( persistenceContext.wasInsertedDuringTransaction( data.concreteDescriptor, data.entityKey.getIdentifier() ) ) {
			boolean update = false;
			final HibernateMonitoringEvent cachePutEvent = eventManager.beginCachePutEvent();
			try {
				update = cacheAccess.update(
						session,
						cacheKey,
						data.concreteDescriptor.getCacheEntryStructure().structure( cacheEntry ),
						version,
						version
				);
			}
			finally {
				eventManager.completeCachePutEvent(
						cachePutEvent,
						session,
						cacheAccess,
						data.concreteDescriptor,
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
						data.concreteDescriptor.getCacheEntryStructure().structure( cacheEntry ),
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
						data.concreteDescriptor,
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
			EntityInitializerData data,
			Object[] resolvedEntityState,
			final SharedSessionContractImplementor session) {
		for ( UniqueKeyEntry entry : data.concreteDescriptor.uniqueKeyEntries() ) {
			final String ukName = entry.getUniqueKeyName();
			final int index = entry.getStateArrayPosition();
			final Type type = entry.getPropertyType();

			// polymorphism not really handled completely correctly,
			// perhaps...well, actually its ok, assuming that the
			// entity name used in the lookup is the same as the
			// one used here, which it will be

			if ( resolvedEntityState[index] != null ) {
				final EntityUniqueKey entityUniqueKey = new EntityUniqueKey(
						data.concreteDescriptor.getRootEntityDescriptor().getEntityName(),
						//polymorphism comment above
						ukName,
						resolvedEntityState[index],
						type,
						session.getFactory()
				);
				session.getPersistenceContextInternal().addEntity( entityUniqueKey, data.entityInstanceForNotify );
			}
		}
	}

	protected Object[] extractConcreteTypeStateValues(EntityInitializerData data) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final Object[] values = new Object[data.concreteDescriptor.getNumberOfAttributeMappings()];
		final DomainResultAssembler<?>[] concreteAssemblers = assemblers[data.concreteDescriptor.getSubclassId()];
		for ( int i = 0; i < values.length; i++ ) {
			final DomainResultAssembler<?> assembler = concreteAssemblers[i];
			values[i] = assembler == null ? UNFETCHED_PROPERTY : assembler.assemble( rowProcessingState );
		}
		return values;
	}

	private void resolveState(EntityInitializerData data) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		for ( final DomainResultAssembler<?> assembler : assemblers[data.concreteDescriptor.getSubclassId()] ) {
			if ( assembler != null ) {
				assembler.resolveState( rowProcessingState );
			}
		}
	}

	protected boolean skipInitialization(EntityInitializerData data) {
		if ( data.entityHolder.getEntityInitializer() != this ) {
			return true;
		}
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final EntityEntry entry = data.entityHolder.getEntityEntry();
		assert entry == rowProcessingState.getSession().getPersistenceContextInternal().getEntry( data.entityInstanceForNotify );
		if ( entry == null ) {
			return false;
		}
		// todo (6.0): do we really need this check ?
		else if ( entry.getStatus().isDeletedOrGone() ) {
			return true;
		}
		else {
			if ( isPersistentAttributeInterceptable( data.entityInstanceForNotify ) ) {
				final PersistentAttributeInterceptor interceptor =
						asPersistentAttributeInterceptable( data.entityInstanceForNotify ).$$_hibernate_getInterceptor();
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
						.getEffectiveOptionalObject() != data.entityInstanceForNotify;
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

	protected void preLoad(EntityInitializerData data, Object[] resolvedEntityState) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		if ( session.isEventSource() ) {
			final PreLoadEvent preLoadEvent = rowProcessingState.getJdbcValuesSourceProcessingState().getPreLoadEvent();
			assert preLoadEvent != null;

			preLoadEvent.reset();

			preLoadEvent.setEntity( data.getInstance() )
					.setState( resolvedEntityState )
					.setId( data.entityKey.getIdentifier() )
					.setPersister( data.concreteDescriptor );

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
	public EntityPersister getConcreteDescriptor(EntityInitializerData data) {
		assert data.getState() != State.UNINITIALIZED;
		return data.concreteDescriptor == null ? entityDescriptor : data.concreteDescriptor;
	}

	protected void initializeSubInstancesFromParent(EntityInitializerData data) {
		if ( data.entityInstanceForNotify != null ) {
			for ( Initializer<?> initializer : subInitializers[data.concreteDescriptor.getSubclassId()] ) {
				if (initializer != null) {
					initializer.initializeInstanceFromParent( data.entityInstanceForNotify, data.getRowProcessingState() );
				}
			}
		}
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		if ( keyAssembler != null ) {
			final Initializer<?> initializer = keyAssembler.getInitializer();
			if ( initializer != null ) {
				consumer.accept( initializer, rowProcessingState );
			}
		}
		if ( identifierAssembler != null ) {
			final Initializer<?> initializer = identifierAssembler.getInitializer();
			if ( initializer != null ) {
				consumer.accept( initializer, rowProcessingState );
			}
		}
		final EntityInitializerData entityInitializerData = (EntityInitializerData) data;
		if ( entityInitializerData.concreteDescriptor == null ) {
			for ( Initializer<?>[] initializers : subInitializers ) {
				for ( Initializer<?> initializer : initializers ) {
					if ( initializer != null ) {
						consumer.accept( initializer, rowProcessingState );
					}
				}
			}
		}
		else {
			for ( Initializer<?> initializer : subInitializers[entityInitializerData.concreteDescriptor.getSubclassId()] ) {
				if ( initializer != null ) {
					consumer.accept( initializer, rowProcessingState );
				}
			}
		}
	}

	@Override
	public void endLoading(EntityInitializerData data) {
		super.endLoading( data );
		data.shallowCached = false;
	}

	@Override
	public String toString() {
		return "EntityJoinedFetchInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}

	//#########################
	// For Hibernate Reactive
	//#########################

	protected DomainResultAssembler<?> getVersionAssembler() {
		return versionAssembler;
	}

	protected DomainResultAssembler<Object> getRowIdAssembler() {
		return rowIdAssembler;
	}

	protected DomainResultAssembler<?>[][] getAssemblers() {
		return assemblers;
	}

}
