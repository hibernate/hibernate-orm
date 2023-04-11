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
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.Initializer;
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
		implements EmbeddableInitializer,
		ValueAccess {
	private static final Object NULL_MARKER = new Object() {
		@Override
		public String toString() {
			return "Composite NULL_MARKER";
		}
	};

	private final NavigablePath navigablePath;
	private final NonAggregatedIdentifierMapping embedded;
	private final EmbeddableMappingType representationEmbeddable;
	private final EmbeddableRepresentationStrategy representationStrategy;
	private final FetchParentAccess fetchParentAccess;
	private final SessionFactoryImplementor sessionFactory;

	private final List<DomainResultAssembler<?>> assemblers;
	private final boolean needsVirtualIdState;


	// per-row state
	private final Object[] virtualIdState;
	private final Object[] idClassState;
	private boolean stateAllNull = true;
	private boolean stateInjected;
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

		final int size = virtualIdEmbeddable.getNumberOfFetchables();
		this.virtualIdState = new Object[ size ];
		this.idClassState = new Object[ size ];
		this.assemblers = arrayList( size );

		this.sessionFactory = creationState.getSqlAstCreationContext().getSessionFactory();
		this.needsVirtualIdState = embedded.hasContainingClass() && virtualIdEmbeddable != representationEmbeddable
				&& fetchParentAccess != null;
		initializeAssemblers( resultDescriptor, creationState, virtualIdEmbeddable );
	}


	protected void initializeAssemblers(
			EmbeddableResultGraphNode resultDescriptor,
			AssemblerCreationState creationState,
			EmbeddableMappingType embeddableTypeDescriptor) {
		final int size = embeddableTypeDescriptor.getNumberOfFetchables();
		for ( int i = 0; i < size; i++ ) {
			final Fetchable stateArrayContributor = embeddableTypeDescriptor.getFetchable( i );
			final Fetch fetch = resultDescriptor.findFetch( stateArrayContributor );

			final DomainResultAssembler<?> stateAssembler = fetch == null
					? new NullValueAssembler<>( stateArrayContributor.getJavaType() )
					: fetch.createAssembler( this, creationState );

			assemblers.add( stateAssembler );
		}
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
		return compositeInstance == NULL_MARKER ? null : compositeInstance;
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

		if ( stateInjected ) {
			return;
		}

		if ( compositeInstance == NULL_MARKER ) {
			// we already know it is null
			return;
		}
		else if ( compositeInstance == null ) {
			// We need to possibly wrap the processing state if the embeddable is within an aggregate
			processingState = wrapProcessingState( processingState );
			extractRowState( processingState );
			if ( stateAllNull ) {
				compositeInstance = NULL_MARKER;
				return;
			}
			else {
				compositeInstance = representationStrategy.getInstantiator().instantiate( this, sessionFactory );
			}
		}

		final Object parentInstance;
		if ( needsVirtualIdState && ( parentInstance = fetchParentAccess.getInitializedInstance() ) != null ) {
			notifyResolutionListeners( compositeInstance );

			final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( parentInstance );
			// If the composite instance has a lazy initializer attached, this means that the embeddable is actually virtual
			// and the compositeInstance == entity, so we have to inject the row state into the entity when it finishes resolution
			if ( lazyInitializer != null ) {
				final Initializer parentInitializer = processingState.resolveInitializer( navigablePath.getParent() );
				if ( parentInitializer != this ) {
					( (FetchParentAccess) parentInitializer ).registerResolutionListener( (entity) -> {
						embedded.getVirtualIdEmbeddable().setValues( entity, virtualIdState );
						stateInjected = true;
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
					stateInjected = true;
					lazyInitializer.setImplementation( target );
				}
			}
			else {
				embedded.getVirtualIdEmbeddable().setValues( parentInstance, virtualIdState );
				stateInjected = true;
			}
		}
	}

	private void extractRowState(RowProcessingState processingState) {
		final EmbeddableMappingType virtualIdEmbeddable = embedded.getEmbeddableTypeDescriptor();
		for ( int i = 0; i < assemblers.size(); i++ ) {
			final DomainResultAssembler<?> assembler = assemblers.get( i );
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
				if ( needsVirtualIdState ) {
					final AttributeMapping virtualIdAttribute = virtualIdEmbeddable.getAttributeMapping( i );
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
		stateAllNull = false;
	}

	@Override
	public Object[] getValues() {
		return stateAllNull ? null : idClassState;
	}

	@Override
	public <T> T getValue(int i, Class<T> clazz) {
		return stateAllNull ? null : clazz.cast( idClassState[i] );
	}

	@Override
	public Object getOwner() {
		return fetchParentAccess.getInitializedInstance();
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		compositeInstance = null;
		stateAllNull = true;
		stateInjected = false;

		clearResolutionListeners();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + navigablePath + ") : `" + getInitializedPart().getJavaType().getJavaTypeClass() + "`";
	}
}
