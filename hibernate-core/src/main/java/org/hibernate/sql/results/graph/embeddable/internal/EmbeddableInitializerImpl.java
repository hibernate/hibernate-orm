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
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResultAssembler;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
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
public class EmbeddableInitializerImpl extends AbstractInitializer<EmbeddableInitializerImpl.EmbeddableInitializerData>
		implements EmbeddableInitializer<EmbeddableInitializerImpl.EmbeddableInitializerData> {

	private final NavigablePath navigablePath;
	private final EmbeddableValuedModelPart embedded;
	private final InitializerParent<InitializerData> parent;
	private final boolean isResultInitializer;
	private final boolean isPartOfKey;
	private final boolean createEmptyCompositesEnabled;
	private final SessionFactoryImplementor sessionFactory;

	protected final DomainResultAssembler<?>[][] assemblers;
	private final BasicResultAssembler<?> discriminatorAssembler;
	protected final Initializer<InitializerData>[][] subInitializers;

	public static class EmbeddableInitializerData extends InitializerData implements ValueAccess {
		protected final InitializerData parentData;
		protected final Object[] rowState;
		protected EmbeddableMappingType.ConcreteEmbeddableType concreteEmbeddableType;

		public EmbeddableInitializerData(EmbeddableInitializerImpl initializer, RowProcessingState rowProcessingState) {
			super( rowProcessingState );
			this.parentData = initializer.parent == null ? null : initializer.parent.getData( rowProcessingState );
			final EmbeddableMappingType embeddableTypeDescriptor = initializer.embedded.getEmbeddableTypeDescriptor();
			final int size = embeddableTypeDescriptor.getNumberOfFetchables();
			this.rowState = new Object[ size ];
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
			InitializerParent<?> parent,
			AssemblerCreationState creationState,
			boolean isResultInitializer) {
		super( creationState );
		this.navigablePath = resultDescriptor.getNavigablePath();
		this.embedded = resultDescriptor.getReferencedMappingContainer();
		this.parent = (InitializerParent<InitializerData>) parent;
		this.isResultInitializer = isResultInitializer;

		final EmbeddableMappingType embeddableTypeDescriptor = embedded.getEmbeddableTypeDescriptor();

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
					: fetch.createAssembler( this, creationState );

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

	protected static Initializer<InitializerData>[][] createInitializers(DomainResultAssembler<?>[][] assemblers) {
		Initializer<?>[][] subInitializers = new Initializer<?>[assemblers.length][];
		for ( int i = 0; i < assemblers.length; i++ ) {
			final DomainResultAssembler<?>[] subAssemblers = assemblers[i];
			if ( subAssemblers != null ) {
				final ArrayList<Initializer<?>> initializers = new ArrayList<>( subAssemblers.length );
				for ( DomainResultAssembler<?> assembler : subAssemblers ) {
					if ( assembler != null ) {
						final Initializer<?> initializer = assembler.getInitializer();
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
		//noinspection unchecked
		return (Initializer<InitializerData>[][]) subInitializers;
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
	public Object getCompositeInstance(EmbeddableInitializerData data) {
		final State state = data.getState();
		return state == State.RESOLVED || state == State.INITIALIZED ? data.getInstance() : null;
	}

	@Override
	public boolean isPartOfKey() {
		return isPartOfKey;
	}

	@Override
	protected InitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new EmbeddableInitializerData( this, rowProcessingState );
	}

	@Override
	public void resolveKey(EmbeddableInitializerData data) {
		if ( data.getState() != State.UNINITIALIZED ) {
			return;
		}
		// We need to possibly wrap the processing state if the embeddable is within an aggregate
		data.setInstance( null );
		if ( discriminatorAssembler != null ) {
			assert embedded.getEmbeddableTypeDescriptor().getDiscriminatorMapping() != null;
			// todo: add more info into EmbeddableDiscriminatorConverter to extract this details object directly
			final Object discriminatorValue = discriminatorAssembler.extractRawValue( data.getRowProcessingState() );
			data.concreteEmbeddableType = discriminatorValue == null
					? null
					: embedded.getEmbeddableTypeDescriptor().findSubtypeByDiscriminator( discriminatorValue );
		}
		if ( isPartOfKey ) {
			data.setState( State.KEY_RESOLVED );
			if ( subInitializers.length == 0 ) {
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

	private void resolveKeySubInitializers(EmbeddableInitializerData data) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		for ( Initializer<InitializerData> initializer : subInitializers[data.getSubclassId()] ) {
			final InitializerData subData = initializer.getData( rowProcessingState );
			initializer.resolveKey( subData );
			if ( subData.getState() == State.MISSING ) {
				data.setState( State.MISSING );
				return;
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
				final RowProcessingState rowProcessingState = data.getRowProcessingState();
				for ( Initializer<InitializerData> initializer : subInitializers[data.getSubclassId()] ) {
					initializer.resolveFromPreviousRow( rowProcessingState );
				}
				data.setState( State.INITIALIZED );
			}
		}
	}

	@Override
	public void resolveInstance(EmbeddableInitializerData data) {
		if ( data.getState() != State.KEY_RESOLVED ) {
			return;
		}

		data.setState( State.RESOLVED );
		extractRowState( data );
		prepareCompositeInstance( data );
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
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			for ( Initializer<?> initializer : subInitializers[data.getSubclassId()] ) {
				final Object subInstance = initializer.getInitializedPart()
						.asAttributeMapping()
						.getValue( instance );
				if ( subInstance == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
					// Go through the normal initializer process
					initializer.resolveKey( rowProcessingState );
				}
				else {
					initializer.resolveInstance( subInstance, rowProcessingState );
				}
			}
			if ( rowProcessingState.needsResolveState() ) {
				for ( DomainResultAssembler<?> assembler : assemblers[data.getSubclassId()] ) {
					assembler.resolveState( rowProcessingState );
				}
			}
		}
	}

	@Override
	public void initializeInstance(EmbeddableInitializerData data) {
		if ( data.getState() != State.RESOLVED ) {
			return;
		}
		data.setState( State.INITIALIZED );

		if ( embedded.getParentInjectionAttributePropertyAccess() != null || embedded instanceof VirtualModelPart ) {
			handleParentInjection( data );

			final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( data.getInstance() );
			// If the composite instance has a lazy initializer attached, this means that the embeddable is actually virtual
			// and the compositeInstance == entity, so we have to inject the row state into the entity when it finishes resolution
			if ( lazyInitializer != null ) {
				if ( parent != null ) {
					embedded.getEmbeddableTypeDescriptor().setValues(
							lazyInitializer.getImplementation(),
							data.rowState
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
							.instantiate( data, sessionFactory );
					lazyInitializer.setImplementation( target );
				}
			}
			else {
				embedded.getEmbeddableTypeDescriptor().setValues( data.getInstance(), data.rowState );
			}
		}
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		final EmbeddableInitializerData embeddableInitializerData = (EmbeddableInitializerData) data;
		final RowProcessingState rowProcessingState = embeddableInitializerData.getRowProcessingState();
		if ( embeddableInitializerData.concreteEmbeddableType == null ) {
			for ( Initializer<?>[] initializers : subInitializers ) {
				for ( Initializer<?> initializer : initializers ) {
					consumer.accept( initializer, rowProcessingState );
				}
			}
		}
		else {
			for ( Initializer<?> initializer : subInitializers[embeddableInitializerData.getSubclassId()] ) {
				consumer.accept( initializer, rowProcessingState );
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
			for ( Initializer<?> initializer : subInitializers[data.getSubclassId()] ) {
				initializer.initializeInstanceFromParent( instance, data.getRowProcessingState() );
			}
		}
	}

	private void prepareCompositeInstance(EmbeddableInitializerData data) {
		// Virtual model parts use the owning entity as container which the fetch parent access provides.
		// For an identifier or foreign key this is called during the resolveKey phase of the fetch parent,
		// so we can't use the fetch parent access in that case.
		if ( parent != null && embedded instanceof VirtualModelPart && !isPartOfKey ) {
			final InitializerData subData = parent.getData( data.getRowProcessingState() );
			parent.resolveInstance( subData );
			data.setInstance( parent.getResolvedInstance( subData ) );
			if ( data.getState() == State.INITIALIZED ) {
				return;
			}
		}

		if ( data.getInstance() == null ) {
			data.setInstance( createCompositeInstance( data ) );
		}
	}

	private void extractRowState(EmbeddableInitializerData data) {
		boolean stateAllNull = true;
		final DomainResultAssembler<?>[] subAssemblers = assemblers[data.getSubclassId()];
		for ( int i = 0; i < subAssemblers.length; i++ ) {
			final DomainResultAssembler<?> assembler = subAssemblers[i];
			final Object contributorValue = assembler == null ? null : assembler.assemble(
					data.getRowProcessingState()
			);

			if ( contributorValue == BATCH_PROPERTY ) {
				data.rowState[i] = null;
			}
			else {
				data.rowState[i] = contributorValue;
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
			data.setState( State.MISSING );
		}
	}

	@Override
	public void resolveState(RowProcessingState rowProcessingState) {
		final EmbeddableInitializerData data = getData( rowProcessingState );
		for ( final DomainResultAssembler<?> assembler : assemblers[data.getSubclassId()] ) {
			assembler.resolveState( rowProcessingState );
		}
	}

	private Object createCompositeInstance(EmbeddableInitializerData data) {
		if ( data.getState() == State.MISSING ) {
			// todo (6.0) : should we initialize the composite instance if it has a parent attribute?
//			if ( !createEmptyCompositesEnabled && embedded.getParentInjectionAttributePropertyAccess() == null ) {
			if ( !createEmptyCompositesEnabled ) {
				return null;
			}
		}

		final EmbeddableInstantiator instantiator = data.concreteEmbeddableType == null
				? embedded.getEmbeddableTypeDescriptor().getRepresentationStrategy().getInstantiator()
				: data.concreteEmbeddableType.getInstantiator();
		final Object instance = instantiator.instantiate( data, sessionFactory );
		data.setState( State.RESOLVED );
		return instance;
	}

	private void handleParentInjection(EmbeddableInitializerData data) {
		final PropertyAccess parentInjectionAccess = embedded.getParentInjectionAttributePropertyAccess();
		if ( parentInjectionAccess == null ) {
			// embeddable defined no parent injection
			return;
		}

		final Initializer<?> owningInitializer = determineOwningInitializer();
		final Object parent = determineParentInstance( owningInitializer, data.getRowProcessingState() );
		if ( parent == null ) {
			return;
		}

		final Setter setter = parentInjectionAccess.getSetter();
		assert setter != null;

		setter.set( data.getInstance(), parent );
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

		final CollectionInitializer<?> collectionInitializer = parentInitializer.asCollectionInitializer();
		if ( collectionInitializer != null ) {
			return collectionInitializer.getCollectionInstance( rowProcessingState ).getOwner();
		}

		final EntityInitializer<?> parentEntityInitializer = parentInitializer.asEntityInitializer();
		if ( parentEntityInitializer != null ) {
			return parentEntityInitializer.getTargetInstance( rowProcessingState );
		}

		throw new UnsupportedOperationException( "The Embeddable: " + navigablePath + " parent initializer is neither an instance of an EntityInitializer nor of a CollectionInitializer" );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + navigablePath + ") : `" + getInitializedPart().getJavaType().getJavaTypeClass() + "`";
	}
}
