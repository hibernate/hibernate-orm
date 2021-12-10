/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.internal.StandardEmbeddableInstantiator;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.EmbeddableRepresentationStrategy;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.ResultsLogger;
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.EntityJavaTypeDescriptor;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEmbeddableInitializer extends AbstractFetchParentAccess implements EmbeddableInitializer {
	private static final Object NULL_MARKER = new Object() {
		@Override
		public String toString() {
			return "Composite NULL_MARKER";
		}
	};

	private final NavigablePath navigablePath;
	private final EmbeddableValuedModelPart embedded;
	private final EmbeddableMappingType representationEmbeddable;
	private final EmbeddableRepresentationStrategy representationStrategy;
	private final FetchParentAccess fetchParentAccess;
	private final boolean createEmptyCompositesEnabled;
	private final SessionFactoryImplementor sessionFactory;

	private final List<DomainResultAssembler<?>> assemblers;

	private final boolean usesStandardInstatiation;

	// per-row state
	private final Object[] rowState;
	private Boolean stateAllNull;
	private Boolean stateInjected;
	private Object compositeInstance;


	@SuppressWarnings("WeakerAccess")
	public AbstractEmbeddableInitializer(
			EmbeddableResultGraphNode resultDescriptor,
			FetchParentAccess fetchParentAccess,
			AssemblerCreationState creationState) {
		this.navigablePath = resultDescriptor.getNavigablePath();
		this.embedded = resultDescriptor.getReferencedMappingContainer();
		this.fetchParentAccess = fetchParentAccess;

		final EmbeddableMappingType embeddableTypeDescriptor = embedded.getEmbeddableTypeDescriptor();
		if ( embedded instanceof CompositeIdentifierMapping ) {
			representationEmbeddable = ( (CompositeIdentifierMapping) embedded ).getMappedIdEmbeddableTypeDescriptor();
		}
		else {
			representationEmbeddable = embeddableTypeDescriptor;
		}

		this.representationStrategy = representationEmbeddable.getRepresentationStrategy();
		this.usesStandardInstatiation = representationStrategy.getInstantiator() instanceof StandardEmbeddableInstantiator;

		final int numOfAttrs = embeddableTypeDescriptor.getNumberOfAttributeMappings();
		this.rowState = new Object[ numOfAttrs ];
		this.assemblers = arrayList( numOfAttrs );

		embeddableTypeDescriptor.visitFetchables(
				stateArrayContributor -> {
					final Fetch fetch = resultDescriptor.findFetch( stateArrayContributor );

					final DomainResultAssembler<?> stateAssembler = fetch == null
							? new NullValueAssembler<>( stateArrayContributor.getJavaTypeDescriptor() )
							: fetch.createAssembler( this, creationState );

					assemblers.add( stateAssembler );
				},
				null
		);

		// We never want to create empty composites for the FK target or PK, otherwise collections would break
		createEmptyCompositesEnabled = !ForeignKeyDescriptor.PART_NAME.equals( navigablePath.getLocalName() )
				&& !EntityIdentifierMapping.ROLE_LOCAL_NAME.equals( navigablePath.getLocalName() )
				&& embeddableTypeDescriptor.isCreateEmptyCompositesEnabled();

		sessionFactory = creationState.getSqlAstCreationContext().getSessionFactory();
	}

	@Override
	public EmbeddableValuedModelPart getInitializedPart() {
		return embedded;
	}

	@SuppressWarnings("WeakerAccess")
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
		return getFetchParentAccess().findFirstEntityDescriptorAccess();
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
		EmbeddableLoadingLogger.INSTANCE.debugf( "Initializing composite instance [%s]", navigablePath );

		if ( compositeInstance == NULL_MARKER ) {
			// we already know it is null
			return;
		}

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

		if ( ! usesStandardInstatiation ) {
			// we have a custom instantiator
			if ( compositeInstance != null ) {
				return;
			}
		}

		stateInjected = false;

		extractRowState( processingState );
		prepareCompositeInstance( processingState );
		handleParentInjection( processingState );

		if ( compositeInstance != NULL_MARKER ) {
			notifyResolutionListeners( compositeInstance );

			if ( compositeInstance instanceof HibernateProxy ) {
				final Initializer parentInitializer = processingState.resolveInitializer( navigablePath.getParent() );
				if ( parentInitializer != this ) {
					( (FetchParentAccess) parentInitializer ).registerResolutionListener( (entity) -> {
						representationEmbeddable.setValues( entity, rowState );
						stateInjected = true;
					} );
				}
				else {
					// At this point, createEmptyCompositesEnabled is always true, so we generate
					// the composite instance.
					//
					// NOTE: `valuesAccess` is set to null to indicate that all values are null,
					//		as opposed to returning the all-null value array.  the instantiator
					//		interprets that as the values are not known or were all null.
					final Supplier<Object[]> valuesAccess = stateAllNull
							? null
							: () -> rowState;
					final Object target = representationStrategy
							.getInstantiator()
							.instantiate( valuesAccess, sessionFactory);
					stateInjected = true;
					( (HibernateProxy) compositeInstance ).getHibernateLazyInitializer().setImplementation( target );
				}
			}
			else if ( stateAllNull == FALSE && stateInjected != TRUE ) {
				representationEmbeddable.setValues( compositeInstance, rowState );
				stateInjected = true;
			}
		}
	}

	private void prepareCompositeInstance(RowProcessingState processingState) {
		if ( compositeInstance != null ) {
			return;
		}

		// Special handling for non-aggregated attributes which use the actual entity instance as container,
		// which we access through the fetch parent access.
		// If this model part is an identifier, we must construct the instance as this is called during resolveKey
		final EmbeddableMappingType embeddableTypeDescriptor = embedded.getEmbeddableTypeDescriptor();
		final JavaType<?> embeddableJtd = embeddableTypeDescriptor.getMappedJavaTypeDescriptor();

		if ( fetchParentAccess != null &&
				embeddableJtd.getJavaTypeClass().isAssignableFrom( fetchParentAccess.getInitializedPart().getJavaTypeDescriptor().getJavaTypeClass() )
				&& embeddableJtd instanceof EntityJavaTypeDescriptor<?>
				&& !( embedded instanceof CompositeIdentifierMapping )
				&& !EntityIdentifierMapping.ROLE_LOCAL_NAME.equals( embedded.getFetchableName() ) ) {
			fetchParentAccess.resolveInstance( processingState );
			compositeInstance = fetchParentAccess.getInitializedInstance();
		}

		if ( compositeInstance == null ) {
			compositeInstance = createCompositeInstance(
					navigablePath,
					representationStrategy,
					processingState,
					sessionFactory
			);
		}

		EmbeddableLoadingLogger.INSTANCE.debugf(
				"Created composite instance [%s]",
				navigablePath
		);
	}

	private void extractRowState(RowProcessingState processingState) {
		stateAllNull = true;
		for ( int i = 0; i < assemblers.size(); i++ ) {
			final DomainResultAssembler<?> assembler = assemblers.get( i );
			final Object contributorValue = assembler.assemble(
					processingState,
					processingState.getJdbcValuesSourceProcessingState().getProcessingOptions()
			);

			rowState[i] = contributorValue;
			if ( contributorValue != null ) {
				stateAllNull = false;
			}
		}

		applyMapsId( processingState );
	}

	private void applyMapsId(RowProcessingState processingState) {
		final SharedSessionContractImplementor session = processingState.getSession();
		if ( embedded instanceof CompositeIdentifierMapping ) {
			final CompositeIdentifierMapping cid = (CompositeIdentifierMapping) embedded;
			final EmbeddableMappingType mappedIdEmbeddable = cid.getMappedIdEmbeddableTypeDescriptor();
			if ( cid.hasContainingClass() ) {
				final EmbeddableMappingType virtualIdEmbeddable = embedded.getEmbeddableTypeDescriptor();
				if ( virtualIdEmbeddable == mappedIdEmbeddable ) {
					return;
				}

				virtualIdEmbeddable.forEachAttributeMapping(
						(position, virtualIdAttribute) -> {
							final AttributeMapping mappedIdAttribute = mappedIdEmbeddable.getAttributeMapping( position );

							if ( virtualIdAttribute instanceof ToOneAttributeMapping
									&& !( mappedIdAttribute instanceof ToOneAttributeMapping ) ) {
								final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) virtualIdAttribute;
								final ForeignKeyDescriptor fkDescriptor = toOneAttributeMapping.getForeignKeyDescriptor();
								final Object associationKey = fkDescriptor.getAssociationKeyFromSide(
										rowState[position],
										toOneAttributeMapping.getSideNature().inverse(),
										session
								);
								rowState[position] = associationKey;
							}
						}
				);
			}
		}
	}

	private Object createCompositeInstance(
			NavigablePath navigablePath,
			EmbeddableRepresentationStrategy representationStrategy,
			RowProcessingState processingState,
			SessionFactoryImplementor sessionFactory) {
		if ( !createEmptyCompositesEnabled && stateAllNull == TRUE ) {
			return NULL_MARKER;
		}

		final Supplier<Object[]> valuesAccess = stateAllNull == TRUE
				? null
				: () -> rowState;
		final Object instance = representationStrategy.getInstantiator().instantiate( valuesAccess, sessionFactory );
		stateInjected = true;

		EmbeddableLoadingLogger.INSTANCE.debugf( "Created composite instance [%s] : %s", navigablePath, instance );

		return instance;
	}

	private void handleParentInjection(RowProcessingState processingState) {
		// todo (6.0) : should we initialize the composite instance if we get here and it is null (not NULL_MARKER)?

		// we want to avoid injection for `NULL_MARKER`
		if ( compositeInstance == null || compositeInstance == NULL_MARKER ) {
			EmbeddableLoadingLogger.INSTANCE.debugf(
					"Skipping parent injection for null embeddable [%s]",
					navigablePath
			);
			return;
		}

		final PropertyAccess parentInjectionAccess = embedded.getParentInjectionAttributePropertyAccess();
		if ( parentInjectionAccess == null ) {
			// embeddable defined no parent injection
			return;
		}

		final Object parent = determineParentInstance( processingState );
		if ( parent == null ) {
			EmbeddableLoadingLogger.INSTANCE.debugf(
					"Unable to determine parent for injection into embeddable [%s]",
					navigablePath
			);
			return;
		}

		EmbeddableLoadingLogger.INSTANCE.debugf(
				"Injecting parent into embeddable [%s] : `%s` -> `%s`",
				navigablePath,
				parent,
				compositeInstance
		);

		parentInjectionAccess.getSetter().set( compositeInstance, parent );
	}

	private Object determineParentInstance(RowProcessingState processingState) {
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

			final FetchParentAccess firstEntityDescriptorAccess = fetchParentAccess.findFirstEntityDescriptorAccess();
			return firstEntityDescriptorAccess.getInitializedInstance();
		}

		// Otherwise, fallback to determining the parent-initializer by path
		//		todo (6.0) - this is the part that should be "subsumed" based on the
		//			comment above

		final NavigablePath parentPath = navigablePath.getParent();
		if ( parentPath == null ) {
			return null;
		}

		final Initializer parentInitializer = processingState.resolveInitializer( parentPath );

		if ( parentInitializer instanceof CollectionInitializer ) {
			return ( (CollectionInitializer) parentInitializer ).getCollectionInstance().getOwner();
		}

		if ( parentInitializer instanceof EntityInitializer ) {
			return ( (EntityInitializer) parentInitializer ).getEntityInstance();
		}

		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		compositeInstance = null;
		stateAllNull = null;
		stateInjected = null;

		clearResolutionListeners();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + navigablePath + ") : `" + getInitializedPart().getJavaTypeDescriptor().getJavaTypeClass() + "`";
	}
}
