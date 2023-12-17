/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable;

import org.hibernate.engine.internal.ManagedTypeHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.VirtualModelPart;
import org.hibernate.metamodel.mapping.internal.EmbeddedCollectionPart;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.property.access.spi.PropertyAccess;
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
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import static org.hibernate.sql.results.graph.entity.internal.BatchEntityInsideEmbeddableSelectFetchInitializer.BATCH_PROPERTY;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEmbeddableInitializer extends AbstractFetchParentAccess
		implements EmbeddableInitializer, ValueAccess {

	private final NavigablePath navigablePath;
	private final EmbeddableValuedModelPart embedded;
	private final FetchParentAccess fetchParentAccess;
	private final boolean isPartOfKey;
	private final boolean createEmptyCompositesEnabled;
	private final SessionFactoryImplementor sessionFactory;

	protected final DomainResultAssembler<?>[] assemblers;

	// per-row state
	private final Object[] rowState;
	private State state = State.INITIAL;
	protected Object compositeInstance;
	private RowProcessingState wrappedProcessingState;

	public AbstractEmbeddableInitializer(
			EmbeddableResultGraphNode resultDescriptor,
			FetchParentAccess fetchParentAccess,
			AssemblerCreationState creationState) {
		this.navigablePath = resultDescriptor.getNavigablePath();
		this.embedded = resultDescriptor.getReferencedMappingContainer();
		this.fetchParentAccess = fetchParentAccess;

		final EmbeddableMappingType embeddableTypeDescriptor = embedded.getEmbeddableTypeDescriptor();
		final int size = embeddableTypeDescriptor.getNumberOfFetchables();
		this.rowState = new Object[ size ];

		this.isPartOfKey = isPartOfKey( embedded, navigablePath, fetchParentAccess );
		// We never want to create empty composites for the FK target or PK, otherwise collections would break
		this.createEmptyCompositesEnabled = !isPartOfKey && embeddableTypeDescriptor.isCreateEmptyCompositesEnabled();
		this.sessionFactory = creationState.getSqlAstCreationContext().getSessionFactory();
		this.assemblers = createAssemblers( resultDescriptor, creationState, embeddableTypeDescriptor );
	}

	private static boolean isPartOfKey(EmbeddableValuedModelPart modelPart, NavigablePath navigablePath, FetchParentAccess fetchParentAccess) {
		return modelPart.isEntityIdentifierMapping()
				|| ForeignKeyDescriptor.PART_NAME.equals( navigablePath.getLocalName() )
				|| ForeignKeyDescriptor.TARGET_PART_NAME.equals( navigablePath.getLocalName() )
				|| navigablePath instanceof EntityIdentifierNavigablePath
				|| fetchParentAccess != null && fetchParentAccess.isEmbeddableInitializer()
				&& ( (AbstractEmbeddableInitializer) fetchParentAccess ).isPartOfKey;
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

	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public Object getCompositeInstance() {
		return state == State.NULL ? null : compositeInstance;
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
		// nothing to do
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
				state = determinInitialState();
				if ( state != State.INITIAL ) {
					return;
				}

				// We need to possibly wrap the processing state if the embeddable is within an aggregate
				if ( wrappedProcessingState == null ) {
					wrappedProcessingState = wrapProcessingState( processingState );
				}
				extractRowState( wrappedProcessingState );
				prepareCompositeInstance( wrappedProcessingState );
				if ( state == State.NULL ) {
					return;
				}
				notifyResolutionListeners( compositeInstance );
			case EXTRACTED:
				if ( embedded.getParentInjectionAttributePropertyAccess() != null || embedded instanceof VirtualModelPart ) {
					handleParentInjection( wrappedProcessingState );

					final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( compositeInstance );
					// If the composite instance has a lazy initializer attached, this means that the embeddable is actually virtual
					// and the compositeInstance == entity, so we have to inject the row state into the entity when it finishes resolution
					if ( lazyInitializer != null ) {
						final Initializer parentInitializer = wrappedProcessingState.resolveInitializer( navigablePath.getParent() );
						if ( parentInitializer != this ) {
							( (FetchParentAccess) parentInitializer ).registerResolutionListener( (entity) -> {
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
									.instantiate( this, sessionFactory);
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
					navigablePath,
					sessionFactory
			);
		}

		EmbeddableLoadingLogger.EMBEDDED_LOAD_LOGGER.debugf(
				"Created composite instance [%s]",
				navigablePath
		);
	}

	private State determinInitialState(){
		final EntityInitializer entityInitializer = getOwningEntityInitializer();
		if ( entityInitializer != null ) {
			if ( entityInitializer.getParentKey() == null ) {
				// parent instance is null;
				return State.NULL;
			}
			else if ( !entityInitializer.getConcreteDescriptor().isTypeOrSuperType( embedded.findContainingEntityMapping() ) ) {
				// parent instance is of a supertype which doesn't contain this embeddable
				return State.NULL;
			}
			else if ( entityInitializer.isEntityInitialized() && !(embedded instanceof EmbeddedCollectionPart )) {
				// parent instance has been initialized, we do not need to inject the state
				return State.INJECTED;
			}
		}
		return State.INITIAL;
	}

	private EntityInitializer getOwningEntityInitializer() {
		if ( isPartOfKey ) {
			return null;
		}
		FetchParentAccess parentAccess = fetchParentAccess;

		while ( parentAccess != null && parentAccess.isEmbeddableInitializer() ) {
			assert !( parentAccess.getInitializedPart() instanceof CompositeIdentifierMapping )
					: "isPartOfKey should have been true in this case";
			parentAccess = parentAccess.getFetchParentAccess();
		}
		if ( parentAccess == null ) {
			return null;
		}
		final EntityInitializer entityInitializer = parentAccess.asEntityInitializer();
		return entityInitializer;
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
		if ( determinInitialState() == State.INITIAL ) {
			for ( final DomainResultAssembler<?> assembler : assemblers ) {
				assembler.resolveState( rowProcessingState );
			}
		}
	}

	private Object createCompositeInstance(NavigablePath navigablePath, SessionFactoryImplementor sessionFactory) {
		if ( state == State.NULL ) {
			// todo (6.0) : should we initialize the composite instance if it has a parent attribute?
//			if ( !createEmptyCompositesEnabled && embedded.getParentInjectionAttributePropertyAccess() == null ) {
			if ( !createEmptyCompositesEnabled ) {
				return null;
			}
		}

		final Object instance = embedded.getEmbeddableTypeDescriptor()
				.getRepresentationStrategy()
				.getInstantiator()
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

	private void handleParentInjection(RowProcessingState processingState) {
		final PropertyAccess parentInjectionAccess = embedded.getParentInjectionAttributePropertyAccess();
		if ( parentInjectionAccess == null ) {
			// embeddable defined no parent injection
			return;
		}

		Initializer parentInitializer = determineParentInitializer( processingState );
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


		final HibernateProxy proxy;
		if ( fetchParentAccess != null
				&& ( proxy = ManagedTypeHelper.asHibernateProxyOrNull( parent ) ) != null ) {
			assert parentInitializer != null;
			assert parentInitializer instanceof EntityInitializer;
			parentInitializer.asEntityInitializer().registerResolutionListener(
					o ->
							parentInjectionAccess.getSetter()
									.set(
											compositeInstance,
											proxy.getHibernateLazyInitializer().getImplementation()
									)
			);
		}
		else {
			parentInjectionAccess.getSetter().set( compositeInstance, parent );
		}
	}

	private Initializer determineParentInitializer(RowProcessingState processingState){
		// use `fetchParentAccess` if it is available - it is more efficient
		// and the correct way to do it.

		// NOTE: indicates that we are initializing a DomainResult as opposed to a Fetch
		// todo (6.0) - this^^ does not work atm when the embeddable is the key or
		//  element of a collection because it passes in null as the fetch-parent-access.
		//  it should really pass the collection-initializer as the fetch-parent,
		//  or at least the fetch-parent of the collection could get passed.
		if ( fetchParentAccess != null ) {
			// the embeddable being initialized is a fetch, so use the fetchParentAccess
			// to get the parent reference
			//
			// at the moment, this uses the legacy behavior of injecting the "first
			// containing entity" as the parent.  however,
			// todo (6.x) - allow injection of containing composite as parent if
			//  	it is the direct parent

			return fetchParentAccess.findFirstEntityDescriptorAccess();
		}

		// Otherwise, fallback to determining the parent-initializer by path
		//		todo (6.0) - this is the part that should be "subsumed" based on the
		//			comment above

		final NavigablePath parentPath = navigablePath.getParent();
		assert parentPath != null;

		return processingState.resolveInitializer( parentPath );
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
	public void finishUpRow(RowProcessingState rowProcessingState) {
		compositeInstance = null;
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
