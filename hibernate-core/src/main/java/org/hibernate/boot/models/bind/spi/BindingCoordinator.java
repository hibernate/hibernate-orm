/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.bind.spi;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.bind.internal.RootEntityBinding;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.GlobalRegistrations;
import org.hibernate.models.spi.AnnotationUsage;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import static org.hibernate.boot.models.bind.ModelBindingLogging.MODEL_BINDING_LOGGER;

/**
 * Responsible for processing {@linkplain org.hibernate.boot.model.process.spi.ManagedResources managed-resources}
 * and table them into Hibernate's {@linkplain org.hibernate.mapping boot-time model}.
 *
 * @author Steve Ebersole
 */
public class BindingCoordinator {
	private final CategorizedDomainModel categorizedDomainModel;
	private final BindingState bindingState;
	private final BindingOptions bindingOptions;
	private final BindingContext bindingContext;

	public BindingCoordinator(
			CategorizedDomainModel categorizedDomainModel,
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext) {
		this.categorizedDomainModel = categorizedDomainModel;
		this.bindingOptions = bindingOptions;
		this.bindingState = bindingState;
		this.bindingContext = bindingContext;
	}

	/**
	 * Main entry point into this table coordination
	 *
	 * @param categorizedDomainModel The model to be processed
	 * @param options Options for the table
	 * @param bindingContext Access to needed information and delegates
	 */
	public static void coordinateBinding(
			CategorizedDomainModel categorizedDomainModel,
			BindingState state,
			BindingOptions options,
			BindingContext bindingContext) {
		final BindingCoordinator coordinator = new BindingCoordinator(
				categorizedDomainModel,
				state,
				options,
				bindingContext
		);

		coordinator.coordinateBinding();
	}

	private void coordinateBinding() {
		// todo : to really work on these, need to changes to MetadataBuildingContext/InFlightMetadataCollector

		coordinateGlobalBindings();
		coordinateModelBindings();
	}

	private void coordinateModelBindings() {
		// process hierarchy
		categorizedDomainModel.forEachEntityHierarchy( this::processHierarchy );

		// complete tables
//		modelBinders.getTableBinder().processSecondPasses();

//		bindingState.forEachType( this::processModelSecondPasses );
	}

//	private void processModelSecondPasses(String typeName, ManagedTypeBinding table) {
//		table.processSecondPasses();
//	}

	private void coordinateGlobalBindings() {
		processGenerators( categorizedDomainModel.getGlobalRegistrations() );
		processConverters( categorizedDomainModel.getGlobalRegistrations() );
		processJavaTypeRegistrations( categorizedDomainModel.getGlobalRegistrations() );
		processJdbcTypeRegistrations( categorizedDomainModel.getGlobalRegistrations() );
		processCustomTypes( categorizedDomainModel.getGlobalRegistrations() );
		processInstantiators( categorizedDomainModel.getGlobalRegistrations() );
		processEventListeners( categorizedDomainModel.getGlobalRegistrations() );
		processFilterDefinitions( categorizedDomainModel.getGlobalRegistrations() );
	}

	private RootEntityBinding processHierarchy(int index, EntityHierarchy hierarchy) {
		final EntityTypeMetadata rootTypeMetadata = hierarchy.getRoot();

		MODEL_BINDING_LOGGER.tracef( "Creating root entity table - %s", rootTypeMetadata.getEntityName() );

		return new RootEntityBinding( rootTypeMetadata, bindingOptions, bindingState, bindingContext );
	}

	private void processGenerators(GlobalRegistrations globalRegistrations) {
		// todo : process these
		globalRegistrations.getSequenceGeneratorRegistrations();
		globalRegistrations.getTableGeneratorRegistrations();
		globalRegistrations.getGenericGeneratorRegistrations();
	}

	private void processConverters(GlobalRegistrations globalRegistrations) {

		// todo : process these
		globalRegistrations.getConverterRegistrations();
	}

	private void processJavaTypeRegistrations(GlobalRegistrations globalRegistrations) {
		// todo : process these
		globalRegistrations.getJavaTypeRegistrations();
	}

	private void processJdbcTypeRegistrations(GlobalRegistrations globalRegistrations) {
		// todo : process these
		globalRegistrations.getJdbcTypeRegistrations();
	}

	private void processCustomTypes(GlobalRegistrations globalRegistrations) {
		// todo : process these
		globalRegistrations.getUserTypeRegistrations();
		globalRegistrations.getCompositeUserTypeRegistrations();
		globalRegistrations.getCollectionTypeRegistrations();
	}

	private void processInstantiators(GlobalRegistrations globalRegistrations) {
		// todo : process these
		globalRegistrations.getEmbeddableInstantiatorRegistrations();
	}

	private void processEventListeners(GlobalRegistrations globalRegistrations) {
		// todo : process these
		globalRegistrations.getEntityListenerRegistrations();
	}

	private void processFilterDefinitions(GlobalRegistrations globalRegistrations) {
		globalRegistrations.getFilterDefRegistrations().forEach( (s, filterDefRegistration) -> {
			bindingState.apply( filterDefRegistration );
		} );

	}

	private void processTables(AttributeMetadata attribute) {
		final AnnotationUsage<JoinTable> joinTableAnn = attribute.getMember().getAnnotationUsage( JoinTable.class );
		final AnnotationUsage<CollectionTable> collectionTableAnn = attribute.getMember().getAnnotationUsage( CollectionTable.class );

		final AnnotationUsage<OneToOne> oneToOneAnn = attribute.getMember().getAnnotationUsage( OneToOne.class );
		final AnnotationUsage<ManyToOne> manyToOneAnn = attribute.getMember().getAnnotationUsage( ManyToOne.class );
		final AnnotationUsage<ElementCollection> elementCollectionAnn = attribute.getMember().getAnnotationUsage( ElementCollection.class );
		final AnnotationUsage<OneToMany> oneToManyAnn = attribute.getMember().getAnnotationUsage( OneToMany.class );
		final AnnotationUsage<Any> anyAnn = attribute.getMember().getAnnotationUsage( Any.class );
		final AnnotationUsage<ManyToAny> manyToAnyAnn = attribute.getMember().getAnnotationUsage( ManyToAny.class );

		final boolean hasAnyTableAnnotations = joinTableAnn != null
				|| collectionTableAnn != null;

		final boolean hasAnyAssociationAnnotations = oneToOneAnn != null
				|| manyToOneAnn != null
				|| elementCollectionAnn != null
				|| oneToManyAnn != null
				|| anyAnn != null
				|| manyToAnyAnn != null;

		if ( !hasAnyAssociationAnnotations ) {
			if ( hasAnyTableAnnotations ) {
				throw new AnnotationPlacementException(
						"@JoinTable or @CollectionTable used on non-association attribute - " + attribute.getMember()
				);
			}
		}

		if ( elementCollectionAnn != null ) {
			if ( joinTableAnn != null ) {
				throw new AnnotationPlacementException(
						"@JoinTable should not be used with @ElementCollection; use @CollectionTable instead - " + attribute.getMember()
				);
			}

			// an element-collection "owns" the collection table, so create it right away

		}

		// ^^ accounting for owning v. "inverse" side
		//
		// on the owning side we get/create the reference and configure it
		//
		// on the inverse side we just get the reference.
		//
		// a cool idea here for "smarter second-pass"... on the inverse side -
		// 		TableReference mappedTable = bindingState.
		//

	}

}
