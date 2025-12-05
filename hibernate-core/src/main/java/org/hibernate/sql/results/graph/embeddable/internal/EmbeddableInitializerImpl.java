/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import java.util.Arrays;
import java.util.function.BiConsumer;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.VirtualModelPart;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResultAssembler;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.internal.AbstractInitializer;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptableType;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;
import static org.hibernate.sql.results.graph.entity.internal.BatchEntityInsideEmbeddableSelectFetchInitializer.BATCH_PROPERTY;

/**
 * @author Steve Ebersole
 */
public class EmbeddableInitializerImpl
		extends AbstractInitializer<EmbeddableInitializerImpl.EmbeddableInitializerData>
		implements EmbeddableInitializer<EmbeddableInitializerImpl.EmbeddableInitializerData> {

	private final NavigablePath navigablePath;
	private final EmbeddableValuedModelPart embedded;
	private final EmbeddableMappingType embeddableMappingType;
	private final @Nullable InitializerParent<InitializerData> parent;
	private final boolean isResultInitializer;
	private final boolean isPartOfKey;

	protected final DomainResultAssembler<?>[][] assemblers;
	protected final BasicResultAssembler<?> discriminatorAssembler;
	protected final @Nullable DomainResultAssembler<Boolean> nullIndicatorAssembler;
	protected final @Nullable Initializer<InitializerData>[][] subInitializers;
	protected final @Nullable Initializer<InitializerData>[][] subInitializersForResolveFromInitialized;
	protected final @Nullable Initializer<InitializerData>[][] collectionContainingSubInitializers;
	protected final boolean lazyCapable;
	protected final boolean hasLazySubInitializer;

	public static class EmbeddableInitializerData extends InitializerData implements ValueAccess {
		protected final InitializerData parentData;
		protected final Object[] rowState;
		protected EmbeddableMappingType.ConcreteEmbeddableType concreteEmbeddableType;

		public EmbeddableInitializerData(EmbeddableInitializerImpl initializer, RowProcessingState rowProcessingState) {
			super( rowProcessingState );
			parentData = initializer.parent == null ? null : initializer.parent.getData( rowProcessingState );
			rowState = new Object[ initializer.embeddableMappingType.getNumberOfFetchables() ];
		}

		@Override
		public Object[] getValues() {
			return getState() == State.MISSING ? null : rowState;
		}

		@Override
		public <T> T getValue(int i, Class<T> clazz) {
			return getState() == State.MISSING ? null : clazz.cast( rowState[i] );
		}

		@Override
		public Object getOwner() {
			return parentData == null ? null : parentData.getInstance();
		}

		public int getSubclassId() {
			return concreteEmbeddableType == null ? 0 : concreteEmbeddableType.getSubclassId();
		}
	}

	public EmbeddableInitializerImpl(
			EmbeddableResultGraphNode resultDescriptor,
			BasicFetch<?> discriminatorFetch,
			@Nullable DomainResult<Boolean> nullIndicatorResult,
			InitializerParent<?> parent,
			AssemblerCreationState creationState,
			boolean isResultInitializer) {
		super( creationState );
		this.parent = (InitializerParent<InitializerData>) parent;
		this.isResultInitializer = isResultInitializer;

		navigablePath = resultDescriptor.getNavigablePath();
		embedded = resultDescriptor.getReferencedMappingContainer();
		embeddableMappingType = embedded.getEmbeddableTypeDescriptor();
		isPartOfKey = embedded.isEntityIdentifierMapping() || Initializer.isPartOfKey( navigablePath, parent );

		// We never want to create empty composites for the FK target or PK, otherwise collections would break
		final var concreteEmbeddableTypes = embeddableMappingType.getConcreteEmbeddableTypes();
		final var assemblers = new DomainResultAssembler[concreteEmbeddableTypes.isEmpty() ? 1 : concreteEmbeddableTypes.size()][];
		final @Nullable Initializer<InitializerData>[][] subInitializers = new Initializer[assemblers.length][];
		final @Nullable Initializer<InitializerData>[][] eagerSubInitializers = new Initializer[subInitializers.length][];
		final @Nullable Initializer<InitializerData>[][] collectionContainingSubInitializers = new Initializer[subInitializers.length][];
		fill( subInitializers );
		fill( eagerSubInitializers );
		fill( collectionContainingSubInitializers );
		final int numberOfFetchables = embeddableMappingType.getNumberOfFetchables();
		for (int i = 0; i < assemblers.length; i++ ) {
			assemblers[i] = new DomainResultAssembler[numberOfFetchables];
		}

		boolean lazyCapable = false;
		boolean hasLazySubInitializers = false;
		for ( int stateArrayPosition = 0; stateArrayPosition < numberOfFetchables; stateArrayPosition++ ) {
			final var stateArrayContributor = embeddableMappingType.getFetchable( stateArrayPosition );
			final var fetch = resultDescriptor.findFetch( stateArrayContributor );
			final var stateAssembler =
					fetch == null
							? new NullValueAssembler<>( stateArrayContributor.getJavaType() )
							: fetch.createAssembler( this, creationState );

			if ( concreteEmbeddableTypes.isEmpty() ) {
				assemblers[0][stateArrayPosition] = stateAssembler;
			}
			else {
				for ( var concreteEmbeddableType : concreteEmbeddableTypes ) {
					if ( concreteEmbeddableType.declaresAttribute( stateArrayPosition ) ) {
						assemblers[concreteEmbeddableType.getSubclassId()][stateArrayPosition] = stateAssembler;
					}
				}
			}

			//noinspection unchecked
			final var subInitializer = (Initializer<InitializerData>) stateAssembler.getInitializer();
			if ( subInitializer != null ) {
				for (int subclassId = 0; subclassId < assemblers.length; subclassId++ ) {
					if ( subInitializers[subclassId] == Initializer.EMPTY_ARRAY ) {
						subInitializers[subclassId] = new Initializer[numberOfFetchables];
					}
					subInitializers[subclassId][stateArrayPosition] = subInitializer;

					if ( subInitializer.isEager() ) {
						if ( eagerSubInitializers[subclassId] == Initializer.EMPTY_ARRAY ) {
							eagerSubInitializers[subclassId] = new Initializer[numberOfFetchables];
						}
						eagerSubInitializers[subclassId][stateArrayPosition] = subInitializer;
						hasLazySubInitializers = hasLazySubInitializers || subInitializer.hasLazySubInitializers();

						assert fetch != null;
						final var fetchParent = fetch.asFetchParent();
						if ( fetchParent != null && fetchParent.containsCollectionFetches()
								|| subInitializer.isCollectionInitializer() ) {
							if ( collectionContainingSubInitializers[subclassId] == Initializer.EMPTY_ARRAY ) {
								collectionContainingSubInitializers[subclassId] = new Initializer[numberOfFetchables];
							}
							collectionContainingSubInitializers[subclassId][stateArrayPosition] = subInitializer;
						}
					}
					else {
						hasLazySubInitializers = true;
					}
					lazyCapable = lazyCapable || subInitializer.isLazyCapable();
				}
			}
		}
		this.assemblers = assemblers;
		this.discriminatorAssembler =
				discriminatorFetch == null
						? null
						: (BasicResultAssembler<?>)
								discriminatorFetch.createAssembler( this, creationState );
		this.nullIndicatorAssembler =
				nullIndicatorResult == null ? null : nullIndicatorResult.createResultAssembler( this, creationState );
		this.subInitializers = subInitializers;
		this.subInitializersForResolveFromInitialized =
				isEnhancedForLazyLoading( embeddableMappingType )
						? subInitializers
						: eagerSubInitializers;
		this.collectionContainingSubInitializers = collectionContainingSubInitializers;
		this.lazyCapable = lazyCapable;
		this.hasLazySubInitializer = hasLazySubInitializers;
	}

	private static void fill(@Nullable Initializer<InitializerData>[][] initializers) {
		Arrays.fill( initializers, Initializer.EMPTY_ARRAY );
	}

	private static boolean isEnhancedForLazyLoading(EmbeddableMappingType embeddableMappingType) {
		return isPersistentAttributeInterceptableType(
				embeddableMappingType.getRepresentationStrategy().getMappedJavaType().getJavaTypeClass()
		);
	}

	@Override
	public EmbeddableValuedModelPart getInitializedPart() {
		return embedded;
	}

	@Override
	public @Nullable InitializerParent<?> getParent() {
		return parent;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public boolean isResultInitializer() {
		return isResultInitializer;
	}

	@Override
	public boolean isPartOfKey() {
		return isPartOfKey;
	}

	@Override
	public boolean isEager() {
		// Embeddables are never lazy
		return true;
	}

	@Override
	public boolean isLazyCapable() {
		return lazyCapable;
	}

	@Override
	public boolean hasLazySubInitializers() {
		return hasLazySubInitializer;
	}

	@Override
	protected InitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new EmbeddableInitializerData( this, rowProcessingState );
	}

	@Override
	public void resolveKey(EmbeddableInitializerData data) {
		if ( data.getState() == State.UNINITIALIZED ) {
			data.setInstance( null );
			if ( discriminatorAssembler != null ) {
				assert embeddableMappingType.getDiscriminatorMapping() != null;
				// todo: add more info into EmbeddableDiscriminatorConverter to extract this details object directly
				final Object discriminatorValue =
						discriminatorAssembler.extractRawValue( data.getRowProcessingState() );
				data.concreteEmbeddableType =
						discriminatorValue == null
								? null
								: embeddableMappingType.findSubtypeByDiscriminator( discriminatorValue );
			}
			if ( isPartOfKey ) {
				data.setState( State.KEY_RESOLVED );
				if ( subInitializers[data.getSubclassId()].length == 0 ) {
					// Resolve the component early to know if the key is missing or not
					resolveInstance( data );
				}
				else {
					resolveKeySubInitializers( data );
				}
			}
			else {
				super.resolveKey( data );
			}
		}
	}

	@Override
	public void resetResolvedEntityRegistrations(RowProcessingState rowProcessingState) {
		final var data = getData( rowProcessingState );
		for ( var initializer : subInitializers[data.getSubclassId()] ) {
			if ( initializer != null ) {
				final var entityInitializer = initializer.asEntityInitializer();
				if ( entityInitializer != null ) {
					entityInitializer.resetResolvedEntityRegistrations( rowProcessingState );
				}
				else {
					final var embeddableInitializer = initializer.asEmbeddableInitializer();
					if ( embeddableInitializer != null ) {
						embeddableInitializer.resetResolvedEntityRegistrations( rowProcessingState );
					}
				}
			}
		}
	}

	private void resolveKeySubInitializers(EmbeddableInitializerData data) {
		final var rowProcessingState = data.getRowProcessingState();
		for ( var initializer : subInitializers[data.getSubclassId()] ) {
			if ( initializer != null ) {
				final var subData = initializer.getData( rowProcessingState );
				initializer.resolveKey( subData );
				if ( subData.getState() == State.MISSING ) {
					data.setState( State.MISSING );
					return;
				}
			}
		}
	}

	@Override
	public void resolveFromPreviousRow(EmbeddableInitializerData data) {
		if ( data.getState() == State.UNINITIALIZED ) {
			if ( data.getInstance() == null ) {
				data.setState( State.MISSING );
			}
			else {
				final var rowProcessingState = data.getRowProcessingState();
				// When a previous row initialized this entity already, we only need to process collections
				for ( var initializer : collectionContainingSubInitializers[data.getSubclassId()] ) {
					if ( initializer != null ) {
						initializer.resolveFromPreviousRow( rowProcessingState );
					}
				}
				data.setState( State.INITIALIZED );
			}
		}
	}

	@Override
	public void resolveInstance(EmbeddableInitializerData data) {
		if ( data.getState() == State.KEY_RESOLVED ) {
			data.setState( State.RESOLVED );
			extractRowState( data );
			prepareCompositeInstance( data );
		}
	}

	@Override
	public void resolveInstance(@Nullable Object instance, EmbeddableInitializerData data) {
		if ( instance == null ) {
			data.setState( State.MISSING );
			data.setInstance( null );
		}
		else {
			data.setState( State.INITIALIZED );
			data.setInstance( instance );
			final int subclassId = data.getSubclassId();
			final var rowProcessingState = data.getRowProcessingState();
			resolveInstanceSubInitializers( subclassId, instance, rowProcessingState );
			if ( rowProcessingState.needsResolveState() ) {
				for ( var assembler : assemblers[subclassId] ) {
					assembler.resolveState( rowProcessingState );
				}
			}
		}
	}

	private void resolveInstanceSubInitializers(int subclassId, Object instance, RowProcessingState rowProcessingState) {
		final var initializers = subInitializersForResolveFromInitialized[subclassId];
		for ( int i = 0; i < initializers.length; i++ ) {
			final var initializer = initializers[i];
			if ( initializer != null ) {
				final Object subInstance = embeddableMappingType.getValue( instance, i );
				if ( subInstance == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
					// Go through the normal initializer process
					initializer.resolveKey( rowProcessingState );
				}
				else {
					initializer.resolveInstance( subInstance, rowProcessingState );
				}
			}
		}
	}

	@Override
	public void initializeInstance(EmbeddableInitializerData data) {
		if ( data.getState() == State.RESOLVED ) {
			data.setState( State.INITIALIZED );

			if ( embedded.getParentInjectionAttributePropertyAccess() != null
					|| embedded instanceof VirtualModelPart ) {
				handleParentInjection( data );

				final var lazyInitializer = extractLazyInitializer( data.getInstance() );
				// If the composite instance has a lazy initializer attached, this means that the embeddable is actually virtual
				// and the compositeInstance == entity, so we have to inject the row state into the entity when it finishes resolution
				if ( lazyInitializer != null ) {
					if ( parent != null ) {
						embeddableMappingType.setValues( lazyInitializer.getImplementation(), data.rowState );
					}
					else {
						// At this point, createEmptyCompositesEnabled is always true, so we generate
						// the composite instance.
						//
						// NOTE: `valuesAccess` is set to null to indicate that all values are null,
						//		as opposed to returning the all-null value array.  the instantiator
						//		interprets that as the values are not known or were all null.
						final Object target =
								embeddableMappingType.getRepresentationStrategy().getInstantiator()
										.instantiate( data );
						lazyInitializer.setImplementation( target );
					}
				}
				else {
					embeddableMappingType.setValues( data.getInstance(), data.rowState );
				}
			}
		}
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		final var embeddableInitializerData = (EmbeddableInitializerData) data;
		final var rowProcessingState = embeddableInitializerData.getRowProcessingState();
		if ( embeddableInitializerData.concreteEmbeddableType == null ) {
			for ( var initializers : subInitializers ) {
				for ( var initializer : initializers ) {
					if ( initializer != null ) {
						consumer.accept( initializer, rowProcessingState );
					}
				}
			}
		}
		else {
			for ( var initializer : subInitializers[embeddableInitializerData.getSubclassId()] ) {
				if ( initializer != null ) {
					consumer.accept( initializer, rowProcessingState );
				}
			}
		}
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance, EmbeddableInitializerData data) {
		final AttributeMapping attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance = attributeMapping != null
				? attributeMapping.getValue( parentInstance )
				: parentInstance;
		data.setInstance( instance );
		if ( instance == null ) {
			data.setState( State.MISSING );
		}
		else {
			data.setState( State.INITIALIZED );
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			for ( Initializer<?> initializer : subInitializers[data.getSubclassId()] ) {
				if ( initializer != null ) {
					initializer.initializeInstanceFromParent( instance, rowProcessingState );
				}
			}
		}
	}

	private void prepareCompositeInstance(EmbeddableInitializerData data) {
		// Virtual model parts use the owning entity as container which the fetch parent access provides.
		// For an identifier or foreign key this is called during the resolveKey phase of the fetch parent,
		// so we can't use the fetch parent access in that case.
		if ( parent != null && embedded instanceof VirtualModelPart && !isPartOfKey ) {
			final State state = data.getState();
			if ( state != State.MISSING ) {
				final InitializerData subData = parent.getData( data.getRowProcessingState() );
				parent.resolveInstance( subData );
				final Object targetInstance =
						((EntityInitializer<InitializerData>) parent)
								.getTargetInstance( subData );
				data.setInstance( targetInstance );
				if ( state == State.INITIALIZED ) {
					return;
				}
			}
		}

		if ( data.getInstance() == null ) {
			data.setInstance( createCompositeInstance( data ) );
		}

//		EMBEDDED_LOAD_LOGGER.tracef( "Created composite instance [%s]", navigablePath );
	}

	protected void extractRowState(EmbeddableInitializerData data) {
		boolean stateAllNull = true;
		final var subAssemblers = assemblers[data.getSubclassId()];
		final var rowProcessingState = data.getRowProcessingState();
		final Object[] rowState = data.rowState;
		for ( int i = 0; i < subAssemblers.length; i++ ) {
			final var assembler = subAssemblers[i];
			final Object contributorValue = assembler == null ? null : assembler.assemble( rowProcessingState );
			rowState[i] = contributorValue == BATCH_PROPERTY ? null : contributorValue;
			if ( contributorValue != null ) {
				stateAllNull = false;
			}
			else if ( isPartOfKey ) {
				// If this is a foreign key and there is a null part, the whole thing has to be turned into null
				stateAllNull = true;
				break;
			}
		}
		if ( stateAllNull ) {
			data.setState( isNull( data ) ? State.MISSING : State.RESOLVED );
		}
	}

	protected boolean isNull(EmbeddableInitializerData data) {
		return nullIndicatorAssembler == null
			|| Boolean.TRUE == nullIndicatorAssembler.assemble( data.getRowProcessingState() );
	}

	@Override
	public void resolveState(EmbeddableInitializerData data) {
		final var rowProcessingState = data.getRowProcessingState();
		for ( var assembler : assemblers[data.getSubclassId()] ) {
			assembler.resolveState( rowProcessingState );
		}
	}

	private Object createCompositeInstance(EmbeddableInitializerData data) {
		if ( data.getState() == State.MISSING ) {
			return null;
		}
		else {
			final var instantiator =
					data.concreteEmbeddableType == null
							? embeddableMappingType.getRepresentationStrategy().getInstantiator()
							: data.concreteEmbeddableType.getInstantiator();
			final Object instance = instantiator.instantiate( data );
			data.setState( State.RESOLVED );
//			EMBEDDED_LOAD_LOGGER.tracef( "Created composite instance [%s]: %s", navigablePath, instance );
			return instance;
		}
	}

	private void handleParentInjection(EmbeddableInitializerData data) {
		final PropertyAccess parentInjectionAccess = embedded.getParentInjectionAttributePropertyAccess();
		if ( parentInjectionAccess != null ) {
			final Object parent =
					determineParentInstance( determineOwningInitializer(), data.getRowProcessingState() );
			if ( parent != null ) {
				final Setter setter = parentInjectionAccess.getSetter();
				assert setter != null;
				setter.set( data.getInstance(), parent );
			}
		}
		// else embeddable defined no parent injection
	}

	private Initializer<?> determineOwningInitializer() {
		// Try to find the first non-embeddable fetch parent access
		// todo (6.x) - allow injection of containing composite as parent if
		//  	it is the direct parent
		InitializerParent<?> parent = this.parent;
		while ( parent != null ) {
			if ( !parent.isEmbeddableInitializer() ) {
				return parent;
			}
			parent = parent.getParent();
		}
		throw new UnsupportedOperationException( "Injection of parent instance into embeddable result is not possible" );
	}

	private Object determineParentInstance(Initializer<?> parentInitializer, RowProcessingState rowProcessingState) {
		if ( parentInitializer == null ) {
			throw new UnsupportedOperationException( "Cannot determine Embeddable: " + navigablePath + " parent instance, parent initializer is null" );
		}

		final var collectionInitializer = parentInitializer.asCollectionInitializer();
		if ( collectionInitializer != null ) {
			return collectionInitializer.getCollectionInstance( rowProcessingState ).getOwner();
		}

		final var parentEntityInitializer = parentInitializer.asEntityInitializer();
		if ( parentEntityInitializer != null ) {
			return parentEntityInitializer.getTargetInstance( rowProcessingState );
		}

		throw new UnsupportedOperationException( "The Embeddable: " + navigablePath + " parent initializer is neither an instance of an EntityInitializer nor of a CollectionInitializer" );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + navigablePath + ") : "
			+ getInitializedPart().getJavaType().getJavaTypeClass().getSimpleName();
	}
}
