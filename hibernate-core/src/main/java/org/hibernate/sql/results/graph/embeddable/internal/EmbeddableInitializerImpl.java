/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.VirtualModelPart;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResultAssembler;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableLoadingLogger;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.internal.AbstractInitializer;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.sql.results.graph.entity.internal.BatchEntityInsideEmbeddableSelectFetchInitializer.BATCH_PROPERTY;

/**
 * @author Steve Ebersole
 */
public class EmbeddableInitializerImpl extends AbstractInitializer
		implements EmbeddableInitializer, ValueAccess {

	private final NavigablePath navigablePath;
	private final EmbeddableValuedModelPart embedded;
	private final InitializerParent parent;
	private final boolean isResultInitializer;
	private final boolean isPartOfKey;
	private final boolean createEmptyCompositesEnabled;
	private final SessionFactoryImplementor sessionFactory;

	protected final DomainResultAssembler<?>[][] assemblers;
	private final BasicResultAssembler<?> discriminatorAssembler;
	protected final Initializer[][] subInitializers;

	// per-row state
	private final Object[] rowState;
	protected Object compositeInstance;
	protected EmbeddableMappingType.ConcreteEmbeddableType concreteEmbeddableType;

	public EmbeddableInitializerImpl(
			EmbeddableResultGraphNode resultDescriptor,
			BasicFetch<?> discriminatorFetch,
			InitializerParent parent,
			AssemblerCreationState creationState,
			boolean isResultInitializer) {
		this.navigablePath = resultDescriptor.getNavigablePath();
		this.embedded = resultDescriptor.getReferencedMappingContainer();
		this.parent = parent;
		this.isResultInitializer = isResultInitializer;

		final EmbeddableMappingType embeddableTypeDescriptor = embedded.getEmbeddableTypeDescriptor();
		final int size = embeddableTypeDescriptor.getNumberOfFetchables();
		this.rowState = new Object[ size ];

		this.isPartOfKey = embedded.isEntityIdentifierMapping() || Initializer.isPartOfKey( navigablePath, parent );
		// We never want to create empty composites for the FK target or PK, otherwise collections would break
		this.createEmptyCompositesEnabled = !isPartOfKey && embeddableTypeDescriptor.isCreateEmptyCompositesEnabled();
		this.sessionFactory = creationState.getSqlAstCreationContext().getSessionFactory();
		this.assemblers = createAssemblers(
				resultDescriptor,
				creationState,
				embeddableTypeDescriptor
		);
		this.discriminatorAssembler = discriminatorFetch != null ?
				(BasicResultAssembler<?>) discriminatorFetch.createAssembler( this, creationState ) :
				null;
		this.subInitializers = createInitializers( assemblers );
	}

	protected DomainResultAssembler<?>[][] createAssemblers(
			EmbeddableResultGraphNode resultDescriptor,
			AssemblerCreationState creationState,
			EmbeddableMappingType embeddableTypeDescriptor) {
		final int size = embeddableTypeDescriptor.getNumberOfFetchables();
		final DomainResultAssembler<?>[] overallAssemblers = new DomainResultAssembler[size];
		for ( int i = 0; i < size; i++ ) {
			final Fetchable stateArrayContributor = embeddableTypeDescriptor.getFetchable( i );
			final Fetch fetch = resultDescriptor.findFetch( stateArrayContributor );

			final DomainResultAssembler<?> stateAssembler = fetch == null
					? new NullValueAssembler<>( stateArrayContributor.getJavaType() )
					: fetch.createAssembler( (InitializerParent) this, creationState );

			overallAssemblers[i] = stateAssembler;
		}
		if ( embeddableTypeDescriptor.isPolymorphic() ) {
			final Collection<EmbeddableMappingType.ConcreteEmbeddableType> concreteEmbeddableTypes = embeddableTypeDescriptor.getConcreteEmbeddableTypes();
			final DomainResultAssembler<?>[][] assemblers = new DomainResultAssembler[concreteEmbeddableTypes.size()][];
			for ( EmbeddableMappingType.ConcreteEmbeddableType concreteEmbeddableType : concreteEmbeddableTypes ) {
				final DomainResultAssembler<?>[] subAssemblers = new DomainResultAssembler[overallAssemblers.length];
				for ( int i = 0; i < overallAssemblers.length; i++ ) {
					if ( concreteEmbeddableType.declaresAttribute( i ) ) {
						subAssemblers[i] = overallAssemblers[i];
					}
				}
				assemblers[concreteEmbeddableType.getSubclassId()] = subAssemblers;
			}
			return assemblers;
		}
		return new DomainResultAssembler[][] { overallAssemblers };
	}

	protected static Initializer[][] createInitializers(DomainResultAssembler<?>[][] assemblers) {
		Initializer[][] subInitializers = new Initializer[assemblers.length][];
		for ( int i = 0; i < assemblers.length; i++ ) {
			final DomainResultAssembler<?>[] subAssemblers = assemblers[i];
			if ( subAssemblers != null ) {
				final ArrayList<Initializer> initializers = new ArrayList<>( subAssemblers.length );
				for ( DomainResultAssembler<?> assembler : subAssemblers ) {
					if ( assembler != null ) {
						final Initializer initializer = assembler.getInitializer();
						if ( initializer != null ) {
							initializers.add( initializer );
						}
					}
				}
				subInitializers[i] = initializers.isEmpty()
						? Initializer.EMPTY_ARRAY
						: initializers.toArray( EMPTY_ARRAY );
			}
			else {
				subInitializers[i] = Initializer.EMPTY_ARRAY;
			}
		}
		return subInitializers;
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
	public boolean isPartOfKey() {
		return isPartOfKey;
	}

	@Override
	public void resolveKey() {
		if ( state != State.UNINITIALIZED ) {
			return;
		}
		// We need to possibly wrap the processing state if the embeddable is within an aggregate
		compositeInstance = null;
		if ( discriminatorAssembler != null ) {
			final EmbeddableDiscriminatorMapping discriminatorMapping = embedded.getEmbeddableTypeDescriptor()
					.getDiscriminatorMapping();
			assert discriminatorMapping != null;
			// todo: add more info into EmbeddableDiscriminatorConverter to extract this details object directly
			final Object discriminatorValue = discriminatorAssembler.extractRawValue( rowProcessingState );
			concreteEmbeddableType = discriminatorValue == null
					? null
					: embedded.getEmbeddableTypeDescriptor().findSubtypeByDiscriminator( discriminatorValue );
		}
		if ( isPartOfKey ) {
			state = State.KEY_RESOLVED;
			if ( subInitializers.length == 0 ) {
				// Resolve the component early to know if the key is missing or not
				resolveInstance();
			}
			else {
				resolveKeySubInitializers( rowProcessingState );
			}
		}
		else {
			super.resolveKey();
		}
	}

	private void resolveKeySubInitializers(RowProcessingState rowProcessingState) {
		for ( Initializer initializer : subInitializers[getSubclassId()] ) {
			initializer.resolveKey();
			if ( initializer.getState() == State.MISSING ) {
				state = State.MISSING;
				return;
			}
		}
	}

	@Override
	public void resolveInstance() {
		if ( state != State.KEY_RESOLVED ) {
			return;
		}

		state = State.RESOLVED;
		extractRowState();
		prepareCompositeInstance();
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
			for ( Initializer initializer : subInitializers[getSubclassId()] ) {
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
				for ( DomainResultAssembler<?> assembler : assemblers[getSubclassId()] ) {
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

		if ( embedded.getParentInjectionAttributePropertyAccess() != null || embedded instanceof VirtualModelPart ) {
			handleParentInjection();

			final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( compositeInstance );
			// If the composite instance has a lazy initializer attached, this means that the embeddable is actually virtual
			// and the compositeInstance == entity, so we have to inject the row state into the entity when it finishes resolution
			if ( lazyInitializer != null ) {
				if ( parent != null ) {
					embedded.getEmbeddableTypeDescriptor().setValues(
							lazyInitializer.getImplementation(),
							rowState
					);
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
					lazyInitializer.setImplementation( target );
				}
			}
			else {
				embedded.getEmbeddableTypeDescriptor().setValues( compositeInstance, rowState );
			}
		}
	}

	@Override
	protected <X> void forEachSubInitializer(BiConsumer<Initializer, X> consumer, X arg) {
		if ( concreteEmbeddableType == null ) {
			for ( Initializer[] initializers : subInitializers ) {
				for ( Initializer initializer : initializers ) {
					consumer.accept( initializer, arg );
				}
			}
		}
		else {
			for ( Initializer initializer : subInitializers[getSubclassId()] ) {
				consumer.accept( initializer, arg );
			}
		}
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance) {
		final AttributeMapping attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance = attributeMapping != null
				? attributeMapping.getValue( parentInstance )
				: parentInstance;
		compositeInstance = instance;
		if ( instance == null ) {
			state = State.MISSING;
		}
		else {
			state = State.INITIALIZED;
			for ( Initializer initializer : subInitializers[getSubclassId()] ) {
				initializer.initializeInstanceFromParent( instance );
			}
		}
	}

	private void prepareCompositeInstance() {
		// Virtual model parts use the owning entity as container which the fetch parent access provides.
		// For an identifier or foreign key this is called during the resolveKey phase of the fetch parent,
		// so we can't use the fetch parent access in that case.
		if ( parent != null && embedded instanceof VirtualModelPart && !isPartOfKey ) {
			parent.resolveInstance();
			compositeInstance = parent.getInitializedInstance();
			EntityInitializer entityInitializer = parent.asEntityInitializer();
			if ( entityInitializer != null && entityInitializer.isEntityInitialized() ) {
				return;
			}
		}

		if ( compositeInstance == null ) {
			compositeInstance = createCompositeInstance();
		}

		EmbeddableLoadingLogger.EMBEDDED_LOAD_LOGGER.debugf(
				"Created composite instance [%s]",
				navigablePath
		);
	}

	private void extractRowState() {
		boolean stateAllNull = true;
		final DomainResultAssembler<?>[] subAssemblers = assemblers[getSubclassId()];
		for ( int i = 0; i < subAssemblers.length; i++ ) {
			final DomainResultAssembler<?> assembler = subAssemblers[i];
			final Object contributorValue = assembler == null ? null : assembler.assemble(
					rowProcessingState,
					rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions()
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
		if ( stateAllNull ) {
			state = State.MISSING;
		}
	}

	@Override
	public void resolveState(RowProcessingState rowProcessingState) {
		for ( final DomainResultAssembler<?> assembler : assemblers[getSubclassId()] ) {
			assembler.resolveState( rowProcessingState );
		}
	}

	private Object createCompositeInstance() {
		if ( state == State.MISSING ) {
			// todo (6.0) : should we initialize the composite instance if it has a parent attribute?
//			if ( !createEmptyCompositesEnabled && embedded.getParentInjectionAttributePropertyAccess() == null ) {
			if ( !createEmptyCompositesEnabled ) {
				return null;
			}
		}

		final EmbeddableInstantiator instantiator = concreteEmbeddableType == null
				? embedded.getEmbeddableTypeDescriptor().getRepresentationStrategy().getInstantiator()
				: concreteEmbeddableType.getInstantiator();
		final Object instance = instantiator.instantiate( this, sessionFactory );
		state = State.RESOLVED;

		EmbeddableLoadingLogger.EMBEDDED_LOAD_LOGGER.debugf( "Created composite instance [%s] : %s", navigablePath, instance );

		return instance;
	}

	@Override
	public Object[] getValues() {
		return state == State.MISSING ? null : rowState;
	}

	@Override
	public <T> T getValue(int i, Class<T> clazz) {
		return state == State.MISSING ? null : clazz.cast( rowState[i] );
	}

	public int getSubclassId() {
		return concreteEmbeddableType == null ? 0 : concreteEmbeddableType.getSubclassId();
	}

	@Override
	public Object getOwner() {
		return parent.getInitializedInstance();
	}

	private void handleParentInjection() {
		final PropertyAccess parentInjectionAccess = embedded.getParentInjectionAttributePropertyAccess();
		if ( parentInjectionAccess == null ) {
			// embeddable defined no parent injection
			return;
		}

		final Initializer owningInitializer = determineOwningInitializer();
		final Object parent = determineParentInstance( owningInitializer );
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

		setter.set( compositeInstance, parent );
	}

	private Initializer determineOwningInitializer() {
		// Try to find the first non-embeddable fetch parent access
		// todo (6.x) - allow injection of containing composite as parent if
		//  	it is the direct parent
		InitializerParent parent = this.parent;
		while ( parent != null ) {
			if ( !parent.isEmbeddableInitializer() ) {
				return parent;
			}
			parent = parent.getParent();
		}
		throw new UnsupportedOperationException( "Injection of parent instance into embeddable result is not possible" );
	}

	private Object determineParentInstance(Initializer parentInitializer) {
		if ( parentInitializer == null ) {
			throw new UnsupportedOperationException( "Cannot determine Embeddable: " + navigablePath + " parent instance, parent initializer is null" );
		}

		final CollectionInitializer collectionInitializer = parentInitializer.asCollectionInitializer();
		if ( collectionInitializer != null ) {
			return collectionInitializer.getCollectionInstance().getOwner();
		}

		final EntityInitializer parentEntityInitializer = parentInitializer.asEntityInitializer();
		if ( parentEntityInitializer != null ) {
			return parentEntityInitializer.getTargetInstance();
		}

		throw new UnsupportedOperationException( "The Embeddable: " + navigablePath + " parent initializer is neither an instance of an EntityInitializer nor of a CollectionInitializer" );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + navigablePath + ") : `" + getInitializedPart().getJavaType().getJavaTypeClass() + "`";
	}
}
