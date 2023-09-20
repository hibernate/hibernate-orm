/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import java.util.List;

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
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.embeddable.AbstractEmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableLoadingLogger;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.sql.results.graph.entity.internal.BatchEntityInsideEmbeddableSelectFetchInitializer.BATCH_PROPERTY;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNonAggregatedIdentifierMappingInitializer extends AbstractFetchParentAccess
		implements EmbeddableInitializer, ValueAccess {

	private final NavigablePath navigablePath;
	private final NonAggregatedIdentifierMapping embedded;
	private final EmbeddableMappingType representationEmbeddable;
	private final EmbeddableRepresentationStrategy representationStrategy;
	private final FetchParentAccess fetchParentAccess;
	private final SessionFactoryImplementor sessionFactory;

	private final DomainResultAssembler<?>[] assemblers;
	private final boolean hasIdClass;


	// per-row state
	private final Object[] virtualIdState;
	private final Object[] idClassState;
	private State state = State.INITIAL;
	protected Object compositeInstance;

	public AbstractNonAggregatedIdentifierMappingInitializer(
			EmbeddableResultGraphNode resultDescriptor,
			FetchParentAccess fetchParentAccess,
			AssemblerCreationState creationState) {
		this.navigablePath = resultDescriptor.getNavigablePath();
		this.embedded = (NonAggregatedIdentifierMapping) resultDescriptor.getReferencedMappingContainer();
		this.fetchParentAccess = fetchParentAccess;

		final EmbeddableMappingType virtualIdEmbeddable = embedded.getEmbeddableTypeDescriptor();
		this.representationEmbeddable = embedded.getMappedIdEmbeddableTypeDescriptor();
		this.representationStrategy = representationEmbeddable.getRepresentationStrategy();
		this.hasIdClass = embedded.hasContainingClass() && virtualIdEmbeddable != representationEmbeddable;

		final int size = virtualIdEmbeddable.getNumberOfFetchables();
		this.virtualIdState = new Object[ size ];
		this.idClassState = new Object[ size ];

		this.sessionFactory = creationState.getSqlAstCreationContext().getSessionFactory();
		this.assemblers = createAssemblers( this, resultDescriptor, creationState, virtualIdEmbeddable );
	}


	protected static DomainResultAssembler<?>[] createAssemblers(
			FetchParentAccess parentAccess,
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
					: fetch.createAssembler( parentAccess, creationState );

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

	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public Object getCompositeInstance() {
		return compositeInstance;
	}

	@Override
	public FetchParentAccess findFirstEntityDescriptorAccess() {
		if ( fetchParentAccess == null ) {
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
		// nothing to do
	}

	@Override
	public void resolveInstance(RowProcessingState processingState) {
		// nothing to do
	}

	@Override
	public void initializeInstance(RowProcessingState processingState) {
		EmbeddableLoadingLogger.EMBEDDED_LOAD_LOGGER.debugf( "Initializing composite instance [%s]", navigablePath );

		switch ( state ) {
			case NULL:
				return;
			case INITIAL:
				// If we don't have an id class and this is a find by id lookup, we just use that instance
				if ( isFindByIdLookup( processingState ) ) {
					compositeInstance = processingState.getEntityId();
					state = State.INJECTED;
					return;
				}
				// We need to possibly wrap the processing state if the embeddable is within an aggregate
				processingState = wrapProcessingState( processingState );
				extractRowState( processingState );
				if ( state == State.NULL ) {
					return;
				}
				else {
					compositeInstance = representationStrategy.getInstantiator().instantiate( this, sessionFactory );
				}
			case EXTRACTED:
				final Object parentInstance;
				if ( fetchParentAccess != null && ( parentInstance = fetchParentAccess.getInitializedInstance() ) != null ) {
					notifyResolutionListeners( compositeInstance );

					final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( parentInstance );
					// If the composite instance has a lazy initializer attached, this means that the embeddable is actually virtual
					// and the compositeInstance == entity, so we have to inject the row state into the entity when it finishes resolution
					if ( lazyInitializer != null ) {
						final Initializer parentInitializer = processingState.resolveInitializer( navigablePath.getParent() );
						if ( parentInitializer != this ) {
							( (FetchParentAccess) parentInitializer ).registerResolutionListener( (entity) -> {
								embedded.getVirtualIdEmbeddable().setValues( entity, virtualIdState );
								state = State.INJECTED;
							} );
						}
						else {
							assert false;
							// At this point, createEmptyCompositesEnabled is always true, so we generate
							// the composite instance.
							//
							// NOTE: `valuesAccess` is set to null to indicate that all values are null,
							//		as opposed to returning the all-null value array.  the instantiator
							//		interprets that as the values are not known or were all null.
							final Object target = representationStrategy
									.getInstantiator()
									.instantiate( this, sessionFactory);

							state = State.INJECTED;
							lazyInitializer.setImplementation( target );
						}
					}
					else {
						embedded.getVirtualIdEmbeddable().setValues( parentInstance, virtualIdState );
						state = State.INJECTED;
					}
				}
			case INJECTED:
				// Nothing to do
		}
	}

	private boolean isFindByIdLookup(RowProcessingState processingState) {
		return !hasIdClass && processingState.getEntityId() != null
				&& navigablePath.getParent().getParent() == null
				&& navigablePath instanceof EntityIdentifierNavigablePath;
	}

	private void extractRowState(RowProcessingState processingState) {
		state = State.NULL;
		for ( int i = 0; i < assemblers.length; i++ ) {
			final DomainResultAssembler<?> assembler = assemblers[i];
			final Object contributorValue = assembler.assemble(
					processingState,
					processingState.getJdbcValuesSourceProcessingState().getProcessingOptions()
			);

			if ( contributorValue == null ) {
				// This is a key and there is a null part, the whole thing has to be turned into null
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
								processingState.getSession()
						);
						idClassState[i] = associationKey;
					}
				}
			}
		}
		state = State.EXTRACTED;
	}

	@Override
	public void resolveState(RowProcessingState rowProcessingState) {
		if ( !isFindByIdLookup( rowProcessingState ) ) {
			for ( final DomainResultAssembler<?> assembler : assemblers ) {
				assembler.resolveState( rowProcessingState );
			}
		}
	}

	@Override
	public Object[] getValues() {
		return state == State.NULL ? null : idClassState;
	}

	@Override
	public <T> T getValue(int i, Class<T> clazz) {
		return state == State.NULL ? null : clazz.cast( idClassState[i] );
	}

	@Override
	public Object getOwner() {
		return fetchParentAccess.getInitializedInstance();
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		compositeInstance = null;
		state = State.INITIAL;

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
