/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import java.util.ArrayList;
import java.util.function.BiConsumer;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableLoadingLogger;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.internal.AbstractInitializer;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.sql.results.graph.entity.internal.BatchEntityInsideEmbeddableSelectFetchInitializer.BATCH_PROPERTY;

/**
 * @author Steve Ebersole
 */
public class NonAggregatedIdentifierMappingInitializer extends AbstractInitializer
		implements EmbeddableInitializer, ValueAccess {

	private final NavigablePath navigablePath;
	private final NonAggregatedIdentifierMapping embedded;
	private final EmbeddableMappingType representationEmbeddable;
	private final EmbeddableRepresentationStrategy representationStrategy;
	private final @Nullable InitializerParent parent;
	private final SessionFactoryImplementor sessionFactory;
	private final boolean isResultInitializer;

	private final DomainResultAssembler<?>[] assemblers;
	private final Initializer[] initializers;
	private final boolean hasIdClass;


	// per-row state
	private final Object[] virtualIdState;
	private final Object[] idClassState;
	protected Object compositeInstance;

	public NonAggregatedIdentifierMappingInitializer(
			EmbeddableResultGraphNode resultDescriptor,
			InitializerParent parent,
			AssemblerCreationState creationState,
			boolean isResultInitializer) {
		this.navigablePath = resultDescriptor.getNavigablePath();
		this.embedded = (NonAggregatedIdentifierMapping) resultDescriptor.getReferencedMappingContainer();
		this.parent = parent;
		this.isResultInitializer = isResultInitializer;

		final EmbeddableMappingType virtualIdEmbeddable = embedded.getEmbeddableTypeDescriptor();
		this.representationEmbeddable = embedded.getMappedIdEmbeddableTypeDescriptor();
		this.representationStrategy = representationEmbeddable.getRepresentationStrategy();
		this.hasIdClass = embedded.hasContainingClass() && virtualIdEmbeddable != representationEmbeddable;

		final int size = virtualIdEmbeddable.getNumberOfFetchables();
		this.virtualIdState = new Object[ size ];
		this.idClassState = new Object[ size ];

		this.sessionFactory = creationState.getSqlAstCreationContext().getSessionFactory();
		this.assemblers = createAssemblers( this, resultDescriptor, creationState, virtualIdEmbeddable );
		final ArrayList<Initializer> initializers = new ArrayList<>( assemblers.length );
		for ( DomainResultAssembler<?> assembler : assemblers ) {
			final Initializer initializer = assembler.getInitializer();
			if ( initializer != null ) {
				initializers.add( initializer );
			}
		}
		this.initializers = initializers.isEmpty() ? Initializer.EMPTY_ARRAY : initializers.toArray( EMPTY_ARRAY );
	}


	protected static DomainResultAssembler<?>[] createAssemblers(
			InitializerParent parent,
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
	public @Nullable FetchParentAccess getFetchParentAccess() {
		return (FetchParentAccess) parent;
	}

	@Override
	public @Nullable InitializerParent getParent() {
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
	public Object getCompositeInstance() {
		return state == State.RESOLVED || state == State.INITIALIZED ? compositeInstance : null;
	}

	@Override
	public void resolveKey() {
		if ( state != State.UNINITIALIZED ) {
			return;
		}
		// We need to possibly wrap the processing state if the embeddable is within an aggregate
		compositeInstance = null;
		state = State.KEY_RESOLVED;
		if ( initializers.length == 0 ) {
			// Resolve the component early to know if the key is missing or not
			resolveInstance();
		}
		else {
			for ( Initializer initializer : initializers ) {
				initializer.resolveKey();
				if ( initializer.getState() == State.MISSING ) {
					state = State.MISSING;
					return;
				}
			}
		}
	}

	@Override
	public void resolveInstance() {
		if ( state != State.KEY_RESOLVED ) {
			return;
		}

		// If we don't have an id class and this is a find by id lookup, we just use that instance
		if ( isFindByIdLookup() ) {
			compositeInstance = rowProcessingState.getEntityId();
			state = State.INITIALIZED;
			return;
		}
		state = State.RESOLVED;
		// We need to possibly wrap the processing state if the embeddable is within an aggregate
		extractRowState();
		if ( state == State.MISSING ) {
			compositeInstance = null;
		}
		else {
			compositeInstance = representationStrategy.getInstantiator().instantiate( this, sessionFactory );
		}
	}

	@Override
	public void resolveInstance(@Nullable Object instance) {
		if ( instance == null ) {
			state = State.MISSING;
			compositeInstance = null;
		}
		else {
			state = State.INITIALIZED;
			compositeInstance = instance;
			for ( Initializer initializer : initializers ) {
				final Object subInstance = initializer.getInitializedPart()
						.asAttributeMapping()
						.getValue( instance );
				if ( subInstance == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
					// Go through the normal initializer process
					initializer.resolveKey();
				}
				else {
					initializer.resolveInstance( subInstance );
				}
			}
			if ( !rowProcessingState.isQueryCacheHit() && rowProcessingState.getQueryOptions().isResultCachingEnabled() == Boolean.TRUE ) {
				// Resolve the state of the assemblers if result caching is enabled and this is not a query cache hit
				for ( DomainResultAssembler<?> assembler : assemblers ) {
					assembler.resolveState( rowProcessingState );
				}
			}
		}
	}

	@Override
	public void initializeInstance() {
		if ( state != State.RESOLVED ) {
			return;
		}
		state = State.INITIALIZED;
		EmbeddableLoadingLogger.EMBEDDED_LOAD_LOGGER.debugf( "Initializing composite instance [%s]", navigablePath );

		if ( parent != null ) {
			assert parent.isEntityInitializer();
			final Object parentInstance = parent.getInitializedInstance();
			assert parentInstance != null;
			final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( parentInstance );
			// If the composite instance has a lazy initializer attached, this means that the embeddable is actually virtual
			// and the compositeInstance == entity, so we have to inject the row state into the entity when it finishes resolution
			if ( lazyInitializer != null ) {
				embedded.getVirtualIdEmbeddable().setValues(
						lazyInitializer.getImplementation(),
						virtualIdState
				);
			}
			else {
				embedded.getVirtualIdEmbeddable().setValues( parentInstance, virtualIdState );
			}
		}
	}

	@Override
	protected <X> void forEachSubInitializer(BiConsumer<Initializer, X> consumer, X arg) {
		for ( Initializer initializer : initializers ) {
			consumer.accept( initializer, arg );
		}
	}

	private boolean isFindByIdLookup() {
		return !hasIdClass && rowProcessingState.getEntityId() != null
				&& navigablePath.getParent().getParent() == null
				&& navigablePath instanceof EntityIdentifierNavigablePath;
	}

	private void extractRowState() {
		for ( int i = 0; i < assemblers.length; i++ ) {
			final DomainResultAssembler<?> assembler = assemblers[i];
			final Object contributorValue = assembler.assemble(
					rowProcessingState,
					rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions()
			);

			if ( contributorValue == null ) {
				// This is a key and there is a null part, the whole thing has to be turned into null
				state = State.MISSING;
				return;
			}
			if ( contributorValue == BATCH_PROPERTY ) {
				virtualIdState[i] = null;
				idClassState[i] = null;
			}
			else {
				virtualIdState[i] = contributorValue;
				idClassState[i] = contributorValue;
				if ( hasIdClass ) {
					final AttributeMapping virtualIdAttribute = embedded.getEmbeddableTypeDescriptor().getAttributeMapping( i );
					final AttributeMapping mappedIdAttribute = representationEmbeddable.getAttributeMapping( i );
					if ( virtualIdAttribute instanceof ToOneAttributeMapping
							&& !( mappedIdAttribute instanceof ToOneAttributeMapping ) ) {
						final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) virtualIdAttribute;
						final ForeignKeyDescriptor fkDescriptor = toOneAttributeMapping.getForeignKeyDescriptor();
						final Object associationKey = fkDescriptor.getAssociationKeyFromSide(
								virtualIdState[i],
								toOneAttributeMapping.getSideNature().inverse(),
								rowProcessingState.getSession()
						);
						idClassState[i] = associationKey;
					}
				}
			}
		}
	}

	@Override
	public void resolveState(RowProcessingState rowProcessingState) {
		if ( !isFindByIdLookup() ) {
			for ( final DomainResultAssembler<?> assembler : assemblers ) {
				assembler.resolveState( rowProcessingState );
			}
		}
	}

	@Override
	public Object[] getValues() {
		assert state == State.RESOLVED;
		return idClassState;
	}

	@Override
	public <T> T getValue(int i, Class<T> clazz) {
		assert state == State.RESOLVED;
		return clazz.cast( idClassState[i] );
	}

	@Override
	public Object getOwner() {
		return parent == null ? null : parent.getInitializedInstance();
	}

	@Override
	public boolean isPartOfKey() {
		return true;
	}

	@Override
	public String toString() {
		return "NonAggregatedIdentifierMappingInitializer(" + navigablePath + ") : `" + getInitializedPart().getJavaType().getJavaTypeClass() + "`";
	}
}
