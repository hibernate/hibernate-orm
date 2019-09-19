/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.composite;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.internal.NullValueAssembler;
import org.hibernate.sql.results.internal.domain.AbstractFetchParentAccess;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.CompositeInitializer;
import org.hibernate.sql.results.spi.CompositeResultMappingNode;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCompositeInitializer extends AbstractFetchParentAccess implements CompositeInitializer {
	private final NavigablePath navigablePath;
	private final EmbeddableValuedModelPart embeddedModelPartDescriptor;
	private final FetchParentAccess fetchParentAccess;

	private final Map<StateArrayContributorMapping, DomainResultAssembler> assemblerMap = new HashMap<>();

	// per-row state
	private Object compositeInstance;
	private Object[] resolvedValues;


	public AbstractCompositeInitializer(
			CompositeResultMappingNode resultDescriptor,
			FetchParentAccess fetchParentAccess,
			Consumer<Initializer> initializerConsumer,
			AssemblerCreationState creationState) {
		this.navigablePath = resultDescriptor.getNavigablePath();
		this.embeddedModelPartDescriptor = resultDescriptor.getReferencedMappingContainer();
		this.fetchParentAccess = fetchParentAccess;

		embeddedModelPartDescriptor.getEmbeddableTypeDescriptor().visitStateArrayContributors(
				stateArrayContributor -> {
					final Fetch fetch = resultDescriptor.findFetch( stateArrayContributor.getFetchableName() );

					final DomainResultAssembler stateAssembler = fetch == null
							? new NullValueAssembler( stateArrayContributor.getJavaTypeDescriptor() )
							: fetch.createAssembler( this, initializerConsumer, creationState );

					assemblerMap.put( stateArrayContributor, stateAssembler );
				}
		);
	}

	@Override
	public EmbeddableValuedModelPart getInitializingModelPart() {
		return embeddedModelPartDescriptor;
	}

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
				.instantiate( rowProcessingState.getSession() );
		CompositeLoadingLogger.INSTANCE.debugf(
				"Created composite instance [%s] : %s",
				LoggingHelper.toLoggableString( navigablePath ),
				compositeInstance
		);
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {

		CompositeLoadingLogger.INSTANCE.debugf(
				"Initializing composite instance [%s] : %s",
				LoggingHelper.toLoggableString( navigablePath ),
				compositeInstance
		);

		resolvedValues = new Object[ assemblerMap.size() ];

		for ( Map.Entry<StateArrayContributorMapping, DomainResultAssembler> entry : assemblerMap.entrySet() ) {
			final Object contributorValue = entry.getValue().assemble(
					rowProcessingState,
					rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions()
			);

			resolvedValues[ entry.getKey().getStateArrayPosition() ] = contributorValue;
		}

		embeddedModelPartDescriptor.getEmbeddableTypeDescriptor().setPropertyValues(
				compositeInstance,
				resolvedValues
		);

	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		compositeInstance = null;
		resolvedValues = null;

		clearParentResolutionListeners();
	}

	@Override
	public FetchParentAccess findFirstEntityDescriptorAccess() {
		return getFetchParentAccess().findFirstEntityDescriptorAccess();
	}
}
