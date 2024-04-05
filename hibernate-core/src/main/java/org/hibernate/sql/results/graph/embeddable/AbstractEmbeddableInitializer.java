/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EmbeddableDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.VirtualModelPart;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResultAssembler;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.sql.results.graph.entity.internal.BatchEntityInsideEmbeddableSelectFetchInitializer.BATCH_PROPERTY;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEmbeddableInitializer extends AbstractFetchParentAccess
		implements EmbeddableInitializer, ValueAccess {

	private final NavigablePath navigablePath;
	private final EmbeddableValuedModelPart embedded;
	private final FetchParentAccess fetchParentAccess;
	private final FetchParentAccess owningParent;
	private final EntityMappingType ownedModelPartDeclaringType;
	private final boolean isPartOfKey;
	private final boolean createEmptyCompositesEnabled;
	private final SessionFactoryImplementor sessionFactory;

	protected final DomainResultAssembler<?>[] assemblers;
	private final BasicResultAssembler<?> discriminatorAssembler;

	// per-row state
	private final Object[] rowState;
	private State state = State.INITIAL;
	protected Object compositeInstance;
	private Object discriminatorValue;
	private RowProcessingState wrappedProcessingState;

	public AbstractEmbeddableInitializer(
			EmbeddableResultGraphNode resultDescriptor,
			FetchParentAccess parentAccess,
			BasicFetch<?> discriminatorFetch,
			AssemblerCreationState creationState) {
		this.navigablePath = resultDescriptor.getNavigablePath();
		this.embedded = resultDescriptor.getReferencedMappingContainer();
		this.fetchParentAccess = parentAccess;

		final EmbeddableMappingType embeddableTypeDescriptor = embedded.getEmbeddableTypeDescriptor();
		final int size = embeddableTypeDescriptor.getNumberOfFetchables();
		this.rowState = new Object[ size ];

		this.isPartOfKey = embedded.isEntityIdentifierMapping() || Initializer.isPartOfKey( navigablePath, parentAccess );
		this.owningParent = FetchParentAccess.determineOwningParent( parentAccess );
		this.ownedModelPartDeclaringType = FetchParentAccess.determineOwnedModelPartDeclaringType( embedded, parentAccess, owningParent );
		// We never want to create empty composites for the FK target or PK, otherwise collections would break
		this.createEmptyCompositesEnabled = !isPartOfKey && embeddableTypeDescriptor.isCreateEmptyCompositesEnabled();
		this.sessionFactory = creationState.getSqlAstCreationContext().getSessionFactory();
		this.assemblers = createAssemblers( resultDescriptor, creationState, embeddableTypeDescriptor );
		discriminatorAssembler = discriminatorFetch != null ?
				(BasicResultAssembler<?>) discriminatorFetch.createAssembler( parentAccess, creationState ) :
				null;
	}

	protected DomainResultAssembler<?>[] createAssemblers(
			EmbeddableResultGraphNode resultDescriptor,
			AssemblerCreationState creationState,
			EmbeddableMappingType embeddableTypeDescriptor) {
		final int size = embeddableTypeDescriptor.getNumberOfFetchables();
		final DomainResultAssembler<?>[] assemblers = new DomainResultAssembler[size];
		for ( int i = 0; i < size; i++ ) {
			final Fetchable stateArrayContributor = embeddableTypeDescriptor.getFetchable( i );
			final Fetch fetch = resultDescriptor.findFetch( stateArrayContributor );

			final DomainResultAssembler<?> stateAssembler = fetch == null
					? new NullValueAssembler<>( stateArrayContributor.getJavaType() )
					: fetch.createAssembler( this, creationState );

			assemblers[i] = stateAssembler;
		}
		return assemblers;
	}

	@Override
	public EmbeddableValuedModelPart getInitializedPart() {
		return embedded;
	}

	@Override
	public FetchParentAccess getFetchParentAccess() {
		return fetchParentAccess;
	}

	@Override
	public @Nullable FetchParentAccess getOwningParent() {
		return owningParent;
	}

	@Override
	public @Nullable EntityMappingType getOwnedModelPartDeclaringType() {
		return ownedModelPartDeclaringType;
	}

	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public Object getCompositeInstance() {
		return state == State.NULL ? null : compositeInstance;
	}

	@Override
	public boolean isPartOfKey() {
		return isPartOfKey;
	}

	@Override
	public FetchParentAccess findFirstEntityDescriptorAccess() {
		if ( fetchParentAccess == null || embedded instanceof CollectionPart ) {
			return null;
		}
		return fetchParentAccess.findFirstEntityDescriptorAccess();
	}

	@Override
	public EntityInitializer findFirstEntityInitializer() {
		final FetchParentAccess firstEntityDescriptorAccess = findFirstEntityDescriptorAccess();
		if ( firstEntityDescriptorAccess == null ) {
			return null;
		}
		return firstEntityDescriptorAccess.findFirstEntityInitializer();
	}

	@Override
	public void resolveKey(RowProcessingState processingState) {
		// We need to possibly wrap the processing state if the embeddable is within an aggregate
		if ( wrappedProcessingState == null ) {
			wrappedProcessingState = wrapProcessingState( processingState );
		}
		if ( discriminatorAssembler != null ) {
			final EmbeddableDiscriminatorMapping discriminatorMapping = embedded.getEmbeddableTypeDescriptor()
					.getDiscriminatorMapping();
			assert discriminatorMapping != null;
			discriminatorValue = discriminatorAssembler.extractRawValue( wrappedProcessingState );
		}
	}

	@Override
	public void resolveInstance(RowProcessingState processingState) {
		// nothing to do
	}

	@Override
	public void initializeInstance(RowProcessingState processingState) {
		EmbeddableLoadingLogger.EMBEDDED_LOAD_LOGGER.debugf( "Initializing composite instance [%s]", navigablePath );

		// IMPORTANT: This method might be called multiple times for the same role for a single row.
		// 		EmbeddableAssembler calls it as part of its `#assemble` and the RowReader calls it
		// 		as part of its normal Initializer handling
		//
		// 		Unfortunately, as currently structured, we need this double call mainly to handle
		// 		the case composite keys, especially those with key-m-1 refs.
		//
		//		When we are processing a non-key embeddable, all initialization happens in
		//		the first call, and we can safely ignore the second call.
		//
		//		When we are processing a composite key, we really need to react to both calls.
		//
		//		Unfortunately, atm, because we reuse `EmbeddedAttributeMapping` in a few of these
		//		foreign-key scenarios, we cannot easily tell when one models a key or not.
		//
		//		While this is "important" to be able to avoid extra handling, it is really only
		//		critical in the case we have custom constructor injection.  Luckily, custom instantiation
		//		is only allowed for non-key usage atm, so we leverage that distinction here

		switch ( state ) {
			case NULL:
				return;
			case INITIAL:
				state = determinInitialState( processingState );
				if ( state != State.INITIAL ) {
					return;
				}

				extractRowState( wrappedProcessingState );
				prepareCompositeInstance( wrappedProcessingState );
				if ( state == State.NULL ) {
					return;
				}
				notifyResolutionListeners( compositeInstance );
			case EXTRACTED:
				if ( embedded.getParentInjectionAttributePropertyAccess() != null || embedded instanceof VirtualModelPart ) {
					handleParentInjection();

					final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( compositeInstance );
					// If the composite instance has a lazy initializer attached, this means that the embeddable is actually virtual
					// and the compositeInstance == entity, so we have to inject the row state into the entity when it finishes resolution
					if ( lazyInitializer != null ) {
						if ( fetchParentAccess != null ) {
							fetchParentAccess.registerResolutionListener( entity -> {
								embedded.getEmbeddableTypeDescriptor().setValues( entity, rowState );
								state = State.INJECTED;
							} );
						}
						else {
							// At this point, createEmptyCompositesEnabled is always true, so we generate
							// the composite instance.
							//
							// NOTE: `valuesAccess` is set to null to indicate that all values are null,
							//		as opposed to returning the all-null value array.  the instantiator
							//		interprets that as the values are not known or were all null.
							final Object target = embedded.getEmbeddableTypeDescriptor().getRepresentationStrategy()
									.getInstantiator()
									.instantiate( this, sessionFactory );
							state = State.INJECTED;
							lazyInitializer.setImplementation( target );
						}
					}
					else {
						embedded.getEmbeddableTypeDescriptor().setValues( compositeInstance, rowState );
						state = State.INJECTED;
					}
				}
				else {
					state = State.INJECTED;
				}
			case INJECTED:
				// Nothing to do
		}
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance, RowProcessingState rowProcessingState) {
		final AttributeMapping attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance = attributeMapping != null
				? attributeMapping.getValue( parentInstance )
				: parentInstance;
		compositeInstance = instance;
		state = State.INJECTED;
		if ( instance != null ) {
			for ( DomainResultAssembler<?> assembler : assemblers ) {
				final Initializer initializer = assembler.getInitializer();
				if ( initializer != null ) {
					initializer.initializeInstanceFromParent( instance, rowProcessingState );
				}
			}
		}
	}

	private void prepareCompositeInstance(RowProcessingState processingState) {
		// Virtual model parts use the owning entity as container which the fetch parent access provides.
		// For an identifier or foreign key this is called during the resolveKey phase of the fetch parent,
		// so we can't use the fetch parent access in that case.
		if ( fetchParentAccess != null && embedded instanceof VirtualModelPart && !isPartOfKey ) {
			fetchParentAccess.resolveInstance( processingState );
			compositeInstance = fetchParentAccess.getInitializedInstance();
			EntityInitializer entityInitializer = fetchParentAccess.asEntityInitializer();
			if ( entityInitializer != null && entityInitializer.isEntityInitialized() ) {
				return;
			}
		}

		if ( compositeInstance == null ) {
			compositeInstance = createCompositeInstance(
					discriminatorValue,
					navigablePath,
					sessionFactory
			);
		}

		EmbeddableLoadingLogger.EMBEDDED_LOAD_LOGGER.debugf(
				"Created composite instance [%s]",
				navigablePath
		);
	}

	private State determinInitialState(RowProcessingState rowProcessingState) {
		if ( isPartOfKey || isResultInitializer() ) {
			assert !isParentShallowCached();
			return State.INITIAL;
		}
		if ( isParentShallowCached() || shouldSkipInitializer( rowProcessingState ) ) {
			return State.NULL;
		}
		return State.INITIAL;
	}

	private void extractRowState(RowProcessingState processingState) {
		boolean stateAllNull = true;
		for ( int i = 0; i < assemblers.length; i++ ) {
			final DomainResultAssembler<?> assembler = assemblers[i];
			final Object contributorValue = assembler.assemble(
					processingState,
					processingState.getJdbcValuesSourceProcessingState().getProcessingOptions()
			);

			if ( contributorValue == BATCH_PROPERTY ) {
				rowState[i] = null;
			}
			else {
				rowState[i] = contributorValue;
			}
			if ( contributorValue != null ) {
				stateAllNull = false;
			}
			else if ( isPartOfKey ) {
				// If this is a foreign key and there is a null part, the whole thing has to be turned into null
				stateAllNull = true;
				break;
			}
		}

		state = stateAllNull ? State.NULL : State.EXTRACTED;
	}

	@Override
	public void resolveState(RowProcessingState rowProcessingState) {
		if ( determinInitialState( rowProcessingState ) == State.INITIAL ) {
			for ( final DomainResultAssembler<?> assembler : assemblers ) {
				assembler.resolveState( rowProcessingState );
			}
		}
	}

	private Object createCompositeInstance(
			Object discriminatorValue,
			NavigablePath navigablePath,
			SessionFactoryImplementor sessionFactory) {
		if ( state == State.NULL ) {
			// todo (6.0) : should we initialize the composite instance if it has a parent attribute?
//			if ( !createEmptyCompositesEnabled && embedded.getParentInjectionAttributePropertyAccess() == null ) {
			if ( !createEmptyCompositesEnabled ) {
				return null;
			}
		}

		final Object instance = embedded.getEmbeddableTypeDescriptor()
				.getRepresentationStrategy()
				.getInstantiatorForDiscriminator( discriminatorValue )
				.instantiate( this, sessionFactory );
		state = State.EXTRACTED;

		EmbeddableLoadingLogger.EMBEDDED_LOAD_LOGGER.debugf( "Created composite instance [%s] : %s", navigablePath, instance );

		return instance;
	}

	@Override
	public Object[] getValues() {
		return state == State.NULL ? null : rowState;
	}

	@Override
	public <T> T getValue(int i, Class<T> clazz) {
		return state == State.NULL ? null : clazz.cast( rowState[i] );
	}

	@Override
	public Object getOwner() {
		return fetchParentAccess.getInitializedInstance();
	}

	@Override
	public Object getDiscriminatorValue() {
		return discriminatorValue;
	}

	private void handleParentInjection() {
		final PropertyAccess parentInjectionAccess = embedded.getParentInjectionAttributePropertyAccess();
		if ( parentInjectionAccess == null ) {
			// embeddable defined no parent injection
			return;
		}

		final Initializer parentInitializer = determineParentInjectionInitializer();
		final Object parent = determineParentInstance( parentInitializer );
		if ( parent == null ) {
			EmbeddableLoadingLogger.EMBEDDED_LOAD_LOGGER.debugf(
					"Unable to determine parent for injection into embeddable [%s]",
					navigablePath
			);
			return;
		}

		EmbeddableLoadingLogger.EMBEDDED_LOAD_LOGGER.debugf(
				"Injecting parent into embeddable [%s] : `%s` -> `%s`",
				navigablePath,
				parent,
				compositeInstance
		);


		final Setter setter = parentInjectionAccess.getSetter();
		assert setter != null;

		final EntityInitializer entityInitializer;
		if ( ( entityInitializer = findFirstEntityInitializer( parentInitializer ) ) != null ) {
			entityInitializer.registerResolutionListener(
					o -> setter.set( compositeInstance, o )
			);
		}
		else {
			setter.set( compositeInstance, parent );
		}
	}

	private @Nullable EntityInitializer findFirstEntityInitializer(Initializer initializer) {
		final EntityInitializer entityInitializer = initializer.asEntityInitializer();
		if ( entityInitializer != null ) {
			return entityInitializer;
		}
		assert initializer.isCollectionInitializer();
		return ( (CollectionInitializer) initializer ).findFirstEntityInitializer();
	}

	private Initializer determineParentInjectionInitializer() {
		// Try to find the first non-embeddable fetch parent access
		// todo (6.x) - allow injection of containing composite as parent if
		//  	it is the direct parent
		FetchParentAccess parentAccess = fetchParentAccess;
		while ( parentAccess != null ) {
			if ( !parentAccess.isEmbeddableInitializer() ) {
				return parentAccess;
			}
			parentAccess = parentAccess.getFetchParentAccess();
		}
		throw new UnsupportedOperationException( "Injection of parent instance into embeddable result is not possible" );
	}

	private Object determineParentInstance(Initializer parentInitializer) {
		if ( parentInitializer == null ) {
			throw new UnsupportedOperationException( "Cannot determine Embeddable: " + navigablePath + " parent instance, parent initializer is null" );
		}

		if ( parentInitializer.isCollectionInitializer() ) {
			return ( (CollectionInitializer) parentInitializer ).getCollectionInstance().getOwner();
		}

		final EntityInitializer parentEntityInitializer = parentInitializer.asEntityInitializer();
		if ( parentEntityInitializer != null ) {
			return parentEntityInitializer.getInitializedInstance();
		}

		throw new UnsupportedOperationException( "The Embeddable: " + navigablePath + " parent initializer is neither an instance of an EntityInitializer nor of a CollectionInitializer" );
	}

	@Override
	public void markShallowCached() {
		assert !isPartOfKey;
		super.markShallowCached();
		markSubInitializersAsShallowCached();
	}

	private void markSubInitializersAsShallowCached() {
		for ( DomainResultAssembler<?> assembler : assemblers ) {
			final Initializer initializer = assembler.getInitializer();
			if ( initializer != null ) {
				initializer.markShallowCached();
			}
		};
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		compositeInstance = null;
		discriminatorValue = null;
		state = State.INITIAL;
		wrappedProcessingState = null;

		clearResolutionListeners();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + navigablePath + ") : `" + getInitializedPart().getJavaType().getJavaTypeClass() + "`";
	}
	enum State {
		INITIAL,
		EXTRACTED,
		NULL,
		INJECTED
	}
}
