/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import java.util.BitSet;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.internal.AbstractInitializer;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.sql.results.graph.entity.internal.BatchEntityInsideEmbeddableSelectFetchInitializer.BATCH_PROPERTY;

/**
 * @author Steve Ebersole
 */
public class NonAggregatedIdentifierMappingInitializer extends AbstractInitializer<NonAggregatedIdentifierMappingInitializer.NonAggregatedIdentifierMappingInitializerData>
		implements EmbeddableInitializer<NonAggregatedIdentifierMappingInitializer.NonAggregatedIdentifierMappingInitializerData> {

	private final NavigablePath navigablePath;
	private final NonAggregatedIdentifierMapping embedded;
	private final EmbeddableMappingType virtualIdEmbeddable;
	private final EmbeddableMappingType representationEmbeddable;
	private final EmbeddableInstantiator embeddableInstantiator;
	private final @Nullable InitializerParent<?> parent;
	private final SessionFactoryImplementor sessionFactory;
	private final boolean isResultInitializer;

	private final DomainResultAssembler<?>[] assemblers;
	private final @Nullable Initializer<InitializerData>[] initializers;
	private final BitSet subInitializersNeedingResolveIfParentInitialized;
	private final boolean hasIdClass;

	public static class NonAggregatedIdentifierMappingInitializerData extends InitializerData implements ValueAccess {
		protected final boolean isFindByIdLookup;
		protected final InitializerData parentData;
		protected final Object[] virtualIdState;
		protected final Object[] idClassState;

		public NonAggregatedIdentifierMappingInitializerData(NonAggregatedIdentifierMappingInitializer initializer, RowProcessingState rowProcessingState) {
			super( rowProcessingState );
			this.isFindByIdLookup = !initializer.hasIdClass && rowProcessingState.getEntityId() != null
					&& initializer.navigablePath.getParent().getParent() == null
					&& initializer.navigablePath instanceof EntityIdentifierNavigablePath;
			this.parentData = initializer.parent == null ? null : initializer.parent.getData( rowProcessingState );
			final EmbeddableMappingType virtualIdEmbeddable = initializer.embedded.getEmbeddableTypeDescriptor();
			final int size = virtualIdEmbeddable.getNumberOfFetchables();
			this.virtualIdState = new Object[ size ];
			this.idClassState = new Object[ size ];
		}

		@Override
		public Object[] getValues() {
			assert getState() == State.RESOLVED;
			return idClassState;
		}

		@Override
		public <T> T getValue(int i, Class<T> clazz) {
			assert getState() == State.RESOLVED;
			return clazz.cast( idClassState[i] );
		}

		@Override
		public Object getOwner() {
			return parentData == null ? null : parentData.getInstance();
		}
	}

	public NonAggregatedIdentifierMappingInitializer(
			EmbeddableResultGraphNode resultDescriptor,
			InitializerParent<?> parent,
			AssemblerCreationState creationState,
			boolean isResultInitializer) {
		this( resultDescriptor, parent, creationState, isResultInitializer, Function.identity() );
	}

	protected NonAggregatedIdentifierMappingInitializer(
			EmbeddableResultGraphNode resultDescriptor,
			InitializerParent<?> parent,
			AssemblerCreationState creationState,
			boolean isResultInitializer,
			Function<Fetch, Fetch> fetchConverter) {
		super( creationState );
		this.navigablePath = resultDescriptor.getNavigablePath();
		this.embedded = (NonAggregatedIdentifierMapping) resultDescriptor.getReferencedMappingContainer();
		this.parent = parent;
		this.isResultInitializer = isResultInitializer;

		this.virtualIdEmbeddable = embedded.getEmbeddableTypeDescriptor();
		this.representationEmbeddable = embedded.getMappedIdEmbeddableTypeDescriptor();
		this.embeddableInstantiator = representationEmbeddable.getRepresentationStrategy().getInstantiator();
		this.hasIdClass = embedded.hasContainingClass() && virtualIdEmbeddable != representationEmbeddable;

		this.sessionFactory = creationState.getSqlAstCreationContext().getSessionFactory();
		this.assemblers = createAssemblers( this, resultDescriptor, creationState, virtualIdEmbeddable, fetchConverter );
		final Initializer<?>[] initializers = new Initializer[assemblers.length];
		final BitSet subInitializersNeedingResolveIfParentInitialized = new BitSet(assemblers.length);
		boolean empty = true;
		for ( int i = 0; i < assemblers.length; i++ ) {
			final Initializer<?> initializer = assemblers[i].getInitializer();
			if ( initializer != null ) {
				if ( initializer.isEager() ) {
					subInitializersNeedingResolveIfParentInitialized.set( i );
				}
				initializers[i] = initializer;
				empty = false;
			}
		}
		//noinspection unchecked
		this.initializers = (Initializer<InitializerData>[]) (
				empty ? Initializer.EMPTY_ARRAY : initializers
		);
		this.subInitializersNeedingResolveIfParentInitialized = subInitializersNeedingResolveIfParentInitialized;
	}

	protected static DomainResultAssembler<?>[] createAssemblers(
			InitializerParent<?> parent,
			EmbeddableResultGraphNode resultDescriptor,
			AssemblerCreationState creationState,
			EmbeddableMappingType embeddableTypeDescriptor,
			Function<Fetch, Fetch> fetchConverter) {
		final int size = embeddableTypeDescriptor.getNumberOfFetchables();
		final DomainResultAssembler<?>[] assemblers = new DomainResultAssembler[size];
		for ( int i = 0; i < size; i++ ) {
			final Fetchable stateArrayContributor = embeddableTypeDescriptor.getFetchable( i );
			final Fetch fetch = fetchConverter.apply( resultDescriptor.findFetch( stateArrayContributor ) );

			final DomainResultAssembler<?> stateAssembler = fetch == null
					? new NullValueAssembler<>( stateArrayContributor.getJavaType() )
					: fetch.createAssembler( parent, creationState );

			assemblers[i] = stateAssembler;
		}
		return assemblers;
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
	protected InitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new NonAggregatedIdentifierMappingInitializerData( this, rowProcessingState );
	}

	@Override
	public void resolveKey(NonAggregatedIdentifierMappingInitializerData data) {
		if ( data.getState() != State.UNINITIALIZED ) {
			return;
		}
		// We need to possibly wrap the processing state if the embeddable is within an aggregate
		data.setInstance( null );
		data.setState( State.KEY_RESOLVED );
		if ( initializers.length == 0 ) {
			// Resolve the component early to know if the key is missing or not
			resolveInstance( data );
		}
		else {
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			for ( Initializer<InitializerData> initializer : initializers ) {
				if ( initializer != null ) {
					final InitializerData subData = initializer.getData( rowProcessingState );
					initializer.resolveKey( subData );
					if ( subData.getState() == State.MISSING ) {
						data.setState( State.MISSING );
						return;
					}
				}
			}
		}
	}

	@Override
	public void resolveInstance(NonAggregatedIdentifierMappingInitializerData data) {
		if ( data.getState() != State.KEY_RESOLVED ) {
			return;
		}

		// If we don't have an id class and this is a find by id lookup, we just use that instance
		if ( data.isFindByIdLookup ) {
			data.setInstance( data.getRowProcessingState().getEntityId() );
			data.setState( State.INITIALIZED );
			return;
		}
		data.setState( State.RESOLVED );
		// We need to possibly wrap the processing state if the embeddable is within an aggregate
		extractRowState( data );
		if ( data.getState() == State.MISSING ) {
			data.setInstance( null );
		}
		else {
			data.setInstance( embeddableInstantiator.instantiate( data, sessionFactory ) );
		}
	}

	@Override
	public void resolveInstance(@Nullable Object instance, NonAggregatedIdentifierMappingInitializerData data) {
		if ( instance == null ) {
			data.setState( State.MISSING );
			data.setInstance( null );
		}
		else {
			data.setState( State.INITIALIZED );
			data.setInstance( instance );
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			resolveInstanceSubInitializers( instance, rowProcessingState );
			if ( rowProcessingState.needsResolveState() ) {
				for ( DomainResultAssembler<?> assembler : assemblers ) {
					assembler.resolveState( rowProcessingState );
				}
			}
		}
	}

	private void resolveInstanceSubInitializers(Object instance, RowProcessingState rowProcessingState) {
		if ( subInitializersNeedingResolveIfParentInitialized.nextSetBit( 0 ) < 0) {
			return;
		}
		for ( int i = 0; i < initializers.length; i++ ) {
			if ( subInitializersNeedingResolveIfParentInitialized.get( i ) ) {
				final Initializer<InitializerData> initializer = initializers[i];
				assert initializer != null;
				final Object subInstance = virtualIdEmbeddable.getValue( instance, i );
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
	public void initializeInstance(NonAggregatedIdentifierMappingInitializerData data) {
		if ( data.getState() != State.RESOLVED ) {
			return;
		}
		data.setState( State.INITIALIZED );

		if ( parent != null ) {
			assert parent.isEntityInitializer();
			final Object parentInstance = parent.getResolvedInstance( data.getRowProcessingState() );
			assert parentInstance != null;
			final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( parentInstance );
			// If the composite instance has a lazy initializer attached, this means that the embeddable is actually virtual
			// and the compositeInstance == entity, so we have to inject the row state into the entity when it finishes resolution
			if ( lazyInitializer != null ) {
				virtualIdEmbeddable.setValues( lazyInitializer.getImplementation(), data.virtualIdState );
			}
			else {
				virtualIdEmbeddable.setValues( parentInstance, data.virtualIdState );
			}
		}
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		for ( Initializer<?> initializer : initializers ) {
			if ( initializer != null ) {
				consumer.accept( initializer, rowProcessingState );
			}
		}
	}

	private void extractRowState(NonAggregatedIdentifierMappingInitializerData data) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		for ( int i = 0; i < assemblers.length; i++ ) {
			final Object contributorValue = assemblers[i].assemble( rowProcessingState );

			if ( contributorValue == null ) {
				// This is a key and there is a null part, the whole thing has to be turned into null
				data.setState( State.MISSING );
				return;
			}
			if ( contributorValue == BATCH_PROPERTY ) {
				data.virtualIdState[i] = null;
				data.idClassState[i] = null;
			}
			else {
				data.virtualIdState[i] = contributorValue;
				data.idClassState[i] = contributorValue;
				if ( hasIdClass ) {
					final AttributeMapping virtualIdAttribute = virtualIdEmbeddable.getAttributeMapping( i );
					final AttributeMapping mappedIdAttribute = representationEmbeddable.getAttributeMapping( i );
					if ( virtualIdAttribute instanceof ToOneAttributeMapping
							&& !( mappedIdAttribute instanceof ToOneAttributeMapping ) ) {
						final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) virtualIdAttribute;
						final ForeignKeyDescriptor fkDescriptor = toOneAttributeMapping.getForeignKeyDescriptor();
						final Object associationKey = fkDescriptor.getAssociationKeyFromSide(
								data.virtualIdState[i],
								toOneAttributeMapping.getSideNature().inverse(),
								rowProcessingState.getSession()
						);
						data.idClassState[i] = associationKey;
					}
				}
			}
		}
	}

	@Override
	public void resolveState(NonAggregatedIdentifierMappingInitializerData data) {
		if ( !data.isFindByIdLookup ) {
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			for ( final DomainResultAssembler<?> assembler : assemblers ) {
				assembler.resolveState( rowProcessingState );
			}
		}
	}

	@Override
	public boolean isPartOfKey() {
		return true;
	}

	@Override
	public boolean isEager() {
		// Embeddables are never lazy
		return true;
	}

	@Override
	public boolean hasEagerSubInitializers() {
		// Since embeddables are never lazy, we only need to check the components
		return !subInitializersNeedingResolveIfParentInitialized.isEmpty();
	}

	/*
	 * Used by Hibernate Reactive
	 */
	protected @Nullable Initializer<InitializerData>[] getInitializers() {
		return initializers;
	}

	@Override
	public String toString() {
		return "NonAggregatedIdentifierMappingInitializer(" + navigablePath + ") : `" + getInitializedPart().getJavaType().getJavaTypeClass() + "`";
	}
}
