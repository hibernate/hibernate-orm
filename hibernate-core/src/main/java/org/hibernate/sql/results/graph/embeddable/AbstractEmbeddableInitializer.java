/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEmbeddableInitializer extends AbstractFetchParentAccess implements EmbeddableInitializer {
	private final NavigablePath navigablePath;
	private final EmbeddableValuedModelPart embeddedModelPartDescriptor;
	private final FetchParentAccess fetchParentAccess;

	private final Map<StateArrayContributorMapping, DomainResultAssembler> assemblerMap;

	// per-row state
	private final Object[] resolvedValues;
	private final boolean createEmptyCompositesEnabled;
	private Object compositeInstance;


	@SuppressWarnings("WeakerAccess")
	public AbstractEmbeddableInitializer(
			EmbeddableResultGraphNode resultDescriptor,
			FetchParentAccess fetchParentAccess,
			Consumer<Initializer> initializerConsumer,
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
					final Fetch fetch = resultDescriptor.findFetch( stateArrayContributor.getFetchableName() );

					final DomainResultAssembler stateAssembler = fetch == null
							? new NullValueAssembler( stateArrayContributor.getJavaTypeDescriptor() )
							: fetch.createAssembler( this, initializerConsumer, creationState );

					assemblerMap.put( stateArrayContributor, stateAssembler );
				}
		);

		createEmptyCompositesEnabled = embeddableTypeDescriptor.isCreateEmptyCompositesEnabled();

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
		// todo (6.0) : register "parent resolution listener" if the composite is defined for `@Parent`
		//		something like:

		final SingularAttributeMapping parentInjectionTarget = embeddedModelPartDescriptor.getParentInjectionAttributeMapping();

		if ( parentInjectionTarget != null ) {
			getFetchParentAccess().findFirstEntityDescriptorAccess().registerResolutionListener(
					// todo (6.0) : this is the legacy behavior
					// 		- the first entity is injected as the parent, even if the composite
					//		is defined on another composite
					owner -> {
						if ( compositeInstance == null ) {
							return;
						}
						parentInjectionTarget.getPropertyAccess().getSetter().set(
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
		compositeInstance = embeddedModelPartDescriptor.getEmbeddableTypeDescriptor()
				.getRepresentationStrategy()
				.getInstantiator()
				.instantiate( rowProcessingState.getSession().getFactory() );
		EmbeddableLoadingLogger.INSTANCE.debugf(
				"Created composite instance [%s] : %s",
				navigablePath,
				compositeInstance
		);
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {

		EmbeddableLoadingLogger.INSTANCE.debugf(
				"Initializing composite instance [%s] : %s",
				navigablePath,
				compositeInstance
		);

		boolean areAllValuesNull = true;
		for ( Map.Entry<StateArrayContributorMapping, DomainResultAssembler> entry : assemblerMap.entrySet() ) {
			final Object contributorValue = entry.getValue().assemble(
					rowProcessingState,
					rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions()
			);

			resolvedValues[ entry.getKey().getStateArrayPosition() ] = contributorValue;
			if ( contributorValue != null ) {
				areAllValuesNull = false;
			}
		}


		if ( !createEmptyCompositesEnabled && areAllValuesNull ) {
			compositeInstance = null;
		}
		else {
			embeddedModelPartDescriptor.getEmbeddableTypeDescriptor().setPropertyValues(
					compositeInstance,
					resolvedValues
			);
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
