/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.categorize;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.source.internal.annotations.AnnotationMetadataSourceProcessorImpl;
import org.hibernate.boot.models.categorize.internal.DomainModelCategorizationCollector;
import org.hibernate.boot.models.categorize.internal.EntityHierarchyBuilder;
import org.hibernate.boot.models.categorize.internal.ModelCategorizationContextImpl;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.orm.test.boot.models.BootstrapContextTesting;

import org.hibernate.testing.orm.domain.retail.LineItem;
import org.hibernate.testing.orm.domain.retail.Order;
import org.hibernate.testing.orm.domain.retail.Payment;
import org.hibernate.testing.orm.domain.retail.Product;
import org.hibernate.testing.orm.domain.retail.RetailDomainModel;
import org.hibernate.testing.orm.domain.retail.SalesAssociate;
import org.hibernate.testing.orm.domain.retail.Vendor;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class SimpleHierarchyBuildingTests {
	@Test
	@ServiceRegistry
	void testRetailModelHierarchyBuilding(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final MetadataBuilderImpl.MetadataBuildingOptionsImpl metadataBuildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		final BootstrapContextTesting bootstrapContext = new BootstrapContextTesting( null, serviceRegistry, metadataBuildingOptions );
		metadataBuildingOptions.setBootstrapContext( bootstrapContext );
		final InFlightMetadataCollectorImpl metadataCollector = new InFlightMetadataCollectorImpl( bootstrapContext, metadataBuildingOptions );
		final SourceModelBuildingContext sourceModelBuildingContext = metadataCollector.getSourceModelBuildingContext();
		final ClassDetailsRegistry classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();


		final DomainModelCategorizationCollector modelCategorizationCollector = new DomainModelCategorizationCollector(
				false,
				metadataCollector.getGlobalRegistrations(),
				sourceModelBuildingContext
		);


		final HashSet<String> processedClassNames = new HashSet<>();
		final HashSet<ClassDetails> knownClasses = new HashSet<>();

		final Class<?>[] annotatedClasses = RetailDomainModel.INSTANCE.getAnnotatedClasses();
		for ( int i = annotatedClasses.length - 1; i >= 0; i-- ) {
			final ClassDetails classDetails = classDetailsRegistry.resolveClassDetails( annotatedClasses[i].getName() );
			AnnotationMetadataSourceProcessorImpl.applyKnownClass(
					classDetails,
					processedClassNames,
					knownClasses,
					modelCategorizationCollector
			);
		}

		final Set<ClassDetails> rootEntities = modelCategorizationCollector.getRootEntities();
		assertThat( rootEntities ).hasSize( 6 );
		assertThat( rootEntities.stream().map( ClassDetails::getClassName ) ).containsOnly(
				SalesAssociate.class.getName(),
				Vendor.class.getName(),
				Product.class.getName(),
				Order.class.getName(),
				LineItem.class.getName(),
				Payment.class.getName()
		);


		final ModelCategorizationContextImpl modelCategorizationContext = new ModelCategorizationContextImpl(
				sourceModelBuildingContext.getClassDetailsRegistry().makeImmutableCopy(),
				sourceModelBuildingContext.getAnnotationDescriptorRegistry().makeImmutableCopy(),
				modelCategorizationCollector.getGlobalRegistrations()
		);

		final Set<EntityHierarchy> entityHierarchies = EntityHierarchyBuilder.createEntityHierarchies(
				rootEntities,
				null,
				modelCategorizationContext
		);
		assertThat( entityHierarchies ).hasSize( rootEntities.size() );
	}
}
