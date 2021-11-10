/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.StateArrayContributorMapping;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.entity.AbstractEntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.internal.domain.CircularBiDirectionalFetchImpl;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.spi.EntityJavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEmbeddableInitializer extends AbstractFetchParentAccess implements EmbeddableInitializer {
	private final NavigablePath navigablePath;
	private final EmbeddableValuedModelPart embeddedModelPartDescriptor;
	private FetchParentAccess fetchParentAccess;

	private final Map<StateArrayContributorMapping, DomainResultAssembler> assemblerMap;

	// per-row state
	private final Object[] resolvedValues;
	private final boolean createEmptyCompositesEnabled;
	private Object compositeInstance;


	@SuppressWarnings("WeakerAccess")
	public AbstractEmbeddableInitializer(
			EmbeddableResultGraphNode resultDescriptor,
			FetchParentAccess fetchParentAccess,
			AssemblerCreationState creationState) {
		this.navigablePath = resultDescriptor.getNavigablePath();
		this.embeddedModelPartDescriptor = resultDescriptor.getReferencedMappingContainer();
		this.fetchParentAccess = fetchParentAccess;

		final EmbeddableMappingType embeddableTypeDescriptor = embeddedModelPartDescriptor.getEmbeddableTypeDescriptor();
		final int numOfAttrs = embeddableTypeDescriptor.getNumberOfAttributeMappings();
		this.resolvedValues = new Object[ numOfAttrs ];
		this.assemblerMap = new IdentityHashMap<>( numOfAttrs );

		embeddableTypeDescriptor.visitStateArrayContributors(
				stateArrayContributor -> {
					final Fetch fetch = resultDescriptor.findFetch( stateArrayContributor );

					final DomainResultAssembler<?> stateAssembler = fetch == null
							? new NullValueAssembler<>( stateArrayContributor.getJavaTypeDescriptor() )
							: fetch.createAssembler( this, creationState );

					assemblerMap.put( stateArrayContributor, stateAssembler );
				}
		);

		// We never want to create empty composites for the FK target or PK, otherwise collections would break
		createEmptyCompositesEnabled = !ForeignKeyDescriptor.PART_NAME.equals( navigablePath.getLocalName() )
				&& !EntityIdentifierMapping.ROLE_LOCAL_NAME.equals( navigablePath.getLocalName() )
				&& embeddableTypeDescriptor.isCreateEmptyCompositesEnabled();

	}

	@Override
	public EmbeddableValuedModelPart getInitializedPart() {
		return embeddedModelPartDescriptor;
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
		return compositeInstance;
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		if ( compositeInstance != null ) {
			return;
		}
		final PropertyAccess parentInjectionPropertyAccess = embeddedModelPartDescriptor.getParentInjectionAttributePropertyAccess();

		final FetchParentAccess fetchParentAccess = getFetchParentAccess();
		if ( parentInjectionPropertyAccess != null && fetchParentAccess != null ) {
			fetchParentAccess.findFirstEntityDescriptorAccess().registerResolutionListener(
					// todo (6.0) : this is the legacy behavior
					// 		- the first entity is injected as the parent, even if the composite
					//		is defined on another composite
					owner -> {
						if ( compositeInstance == null ) {
							return;
						}
						parentInjectionPropertyAccess.getSetter().set(
								compositeInstance,
								owner,
								rowProcessingState.getSession().getFactory()
						);
					}
			);
		}
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		if ( compositeInstance != null ) {
			return;
		}

		// Special handling for non-aggregated attributes which use the actual entity instance as container,
		// which we access through the fetch parent access.
		// If this model part is an identifier, we must construct the instance as this is called during resolveKey
		final EmbeddableMappingType embeddableTypeDescriptor = embeddedModelPartDescriptor.getEmbeddableTypeDescriptor();
		if ( fetchParentAccess != null && embeddableTypeDescriptor.getMappedJavaTypeDescriptor().getJavaTypeClass()
				.isAssignableFrom( fetchParentAccess.getInitializedPart().getJavaTypeDescriptor().getJavaTypeClass() )
				&& embeddableTypeDescriptor.getMappedJavaTypeDescriptor() instanceof EntityJavaTypeDescriptor<?>
				&& !( embeddedModelPartDescriptor instanceof CompositeIdentifierMapping )
				&& !EntityIdentifierMapping.ROLE_LOCAL_NAME.equals( embeddedModelPartDescriptor.getFetchableName() ) ) {
			fetchParentAccess.resolveInstance( rowProcessingState );
			compositeInstance = fetchParentAccess.getInitializedInstance();
		}

		if ( compositeInstance == null ) {
			compositeInstance = embeddableTypeDescriptor
					.getRepresentationStrategy()
					.getInstantiator()
					.instantiate( VALUE_ACCESS, rowProcessingState.getSession().getFactory() );
		}

		EmbeddableLoadingLogger.INSTANCE.debugf(
				"Created composite instance [%s]",
				navigablePath
		);
	}

	private static Supplier<Object[]> VALUE_ACCESS = () -> {
		throw new NotYetImplementedFor6Exception( "Constructor value injection for embeddables not yet implemented" );
	};

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {

		final PropertyAccess parentInjectionPropertyAccess = embeddedModelPartDescriptor.getParentInjectionAttributePropertyAccess();

		Initializer initializer = rowProcessingState.resolveInitializer( navigablePath.getParent() );
		if ( parentInjectionPropertyAccess != null ) {
			final Object owner;
			if ( initializer instanceof CollectionInitializer ) {
				owner = ( (CollectionInitializer) initializer ).getCollectionInstance().getOwner();
			}
			else if ( initializer instanceof EntityInitializer ) {
				owner = ( (EntityInitializer) initializer ).getEntityInstance();

				parentInjectionPropertyAccess.getSetter().set(
						compositeInstance,
						owner,
						rowProcessingState.getSession().getFactory()
				);
			}
			else {
				throw new NotYetImplementedFor6Exception( getClass() );
			}
			parentInjectionPropertyAccess.getSetter().set(
					compositeInstance,
					owner,
					rowProcessingState.getSession().getFactory()
			);
		}

		EmbeddableLoadingLogger.INSTANCE.debugf(
				"Initializing composite instance [%s]",
				navigablePath
		);

		boolean areAllValuesNull = true;
		final Set<Map.Entry<StateArrayContributorMapping, DomainResultAssembler>> entries = assemblerMap.entrySet();
		final int size = entries.size();
		for ( Map.Entry<StateArrayContributorMapping, DomainResultAssembler> entry : entries ) {
			final DomainResultAssembler value = entry.getValue();
			final Object contributorValue = value.assemble(
					rowProcessingState,
					rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions()
			);

			resolvedValues[entry.getKey().getStateArrayPosition()] = contributorValue;
			if ( contributorValue != null && ( !( value instanceof CircularBiDirectionalFetchImpl.CircularFetchAssembler ) || size == 1 ) ) {
				areAllValuesNull = false;
			}
		}

		if ( !createEmptyCompositesEnabled && areAllValuesNull ) {
			compositeInstance = null;
		}
		else {
			notifyParentResolutionListeners( compositeInstance );
			if ( compositeInstance instanceof HibernateProxy ) {
				if ( initializer != this ) {
					( (AbstractEntityInitializer) initializer ).registerResolutionListener(
							entityInstance -> {
								embeddedModelPartDescriptor.getEmbeddableTypeDescriptor().setPropertyValues(
										entityInstance,
										resolvedValues
								);
							}
					);
				}
				else {
					Object target = embeddedModelPartDescriptor.getEmbeddableTypeDescriptor()
							.getRepresentationStrategy()
							.getInstantiator()
							.instantiate( VALUE_ACCESS, rowProcessingState.getSession().getFactory() );
					embeddedModelPartDescriptor.getEmbeddableTypeDescriptor().setPropertyValues(
							target,
							resolvedValues
					);
					( (HibernateProxy) compositeInstance ).getHibernateLazyInitializer().setImplementation( target );
				}
			}
			// At this point, createEmptyCompositesEnabled is always true.
			// We can only set the property values on the compositeInstance though if there is at least one non null value.
			// If the values are all null, we would normally not create a composite instance at all because no values exist.
			// Setting all properties to null could cause IllegalArgumentExceptions though when the component has primitive properties.
			// To avoid this exception and align with what Hibernate 5 did, we skip setting properties if all values are null.
			// A possible alternative could be to initialize the resolved values for primitive fields to their default value,
			// but that might cause unexpected outcomes for Hibernate 5 users that use createEmptyCompositesEnabled when updating.
			// You can see the need for this by running EmptyCompositeEquivalentToNullTest
			else if ( !areAllValuesNull ) {
				embeddedModelPartDescriptor.getEmbeddableTypeDescriptor().setPropertyValues(
						compositeInstance,
						resolvedValues
				);
			}
		}
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		compositeInstance = null;
		clearParentResolutionListeners();
	}

	@Override
	public FetchParentAccess findFirstEntityDescriptorAccess() {
		return getFetchParentAccess().findFirstEntityDescriptorAccess();
	}
}
