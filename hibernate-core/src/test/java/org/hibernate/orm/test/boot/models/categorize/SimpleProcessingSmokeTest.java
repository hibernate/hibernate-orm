/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.categorize;

import java.util.List;

import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.RootMappingDefaults;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.models.categorize.internal.DomainModelCategorizationCollector;
import org.hibernate.boot.models.categorize.internal.LifecycleCallbackCollector;
import org.hibernate.boot.models.categorize.internal.StandardPersistentAttributeMemberResolver;
import org.hibernate.boot.models.categorize.spi.ClassAttributeAccessType;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.orm.test.boot.models.BootstrapContextTesting;
import org.hibernate.orm.test.boot.models.MyStringConverter;
import org.hibernate.orm.test.boot.models.MyUuidConverter;
import org.hibernate.orm.test.boot.models.SourceModelTestHelper;
import org.hibernate.orm.test.boot.models.process.Person;
import org.hibernate.orm.test.boot.models.process.Root;
import org.hibernate.orm.test.boot.models.process.Sub;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import org.jboss.jandex.Index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;

/**
 * Simple smoke-test showing how to possibly move forward with the "categorization" consumption of
 * hibernate-models (replacing all the binder stuff)
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class SimpleProcessingSmokeTest {
	@Test
	@ServiceRegistry
	void testSimpleUsage(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// ManagedResources is built by scanning and from explicit resources
		// during ORM bootstrap.
		// Here we build one manually for testing
		final AdditionalManagedResourcesImpl.Builder managedResourcesBuilder = new AdditionalManagedResourcesImpl.Builder();
		managedResourcesBuilder
				.addLoadedClasses( Person.class, Root.class, Sub.class, MyStringConverter.class, MyUuidConverter.class )
				.addPackages( "org.hibernate.models.orm.process" );
		final ManagedResources managedResources = managedResourcesBuilder.build();
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// The Jandex index would generally (1) be built by WF and passed
		// to ORM or (2) be built by ORM.
		// Again, here we build one manually for testing
		final Index jandexIndex = SourceModelTestHelper.buildJandexIndex(
				SIMPLE_CLASS_LOADING,
				SimpleEntity.class
		);
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// This is also all internal set up stuff done as part of bootstrapping
		// Again, here we do this all manually for testing
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		final MetadataBuilderImpl.MetadataBuildingOptionsImpl metadataBuildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		final BootstrapContextTesting bootstrapContext = new BootstrapContextTesting( jandexIndex, serviceRegistry, metadataBuildingOptions );
		metadataBuildingOptions.setBootstrapContext( bootstrapContext );
		final InFlightMetadataCollectorImpl metadataCollector = new InFlightMetadataCollectorImpl( bootstrapContext, metadataBuildingOptions );
		final SourceModelBuildingContext sourceModelBuildingContext = metadataCollector.getSourceModelBuildingContext();
		final ClassDetailsRegistry classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final ClassDetails simpleEntityClassDetails = classDetailsRegistry.resolveClassDetails( SimpleEntity.class.getName() );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Now we get to the meat and potatoes...
		//
		// At a high level we want to iterate over class members just once.  During
		// this process we -
		//		1. collect "attribute members"
		//		2. collect "call back" methods
		//
		// This would be triggered for each "managed class" (entity, mapped-super
		// and embeddable)
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		final LifecycleCallbackCollector lifecycleCallbackCollector = new LifecycleCallbackCollector( simpleEntityClassDetails, null );
		final List<MemberDetails> attributeMembers = StandardPersistentAttributeMemberResolver.INSTANCE.resolveAttributesMembers(
				simpleEntityClassDetails,
				ClassAttributeAccessType.IMPLICIT_FIELD,
				lifecycleCallbackCollector
		);

		assertThat( attributeMembers ).hasSize( 2 );
		assertThat( attributeMembers ).containsExactly(
				simpleEntityClassDetails.findFieldByName( "id" ),
				simpleEntityClassDetails.findFieldByName( "name" )
		);

		assertThat( lifecycleCallbackCollector.getPostLoad() ).isNotNull();

		assertThat( lifecycleCallbackCollector.getPrePersist() ).isNotNull();
		assertThat( lifecycleCallbackCollector.getPostPersist() ).isNotNull();

		assertThat( lifecycleCallbackCollector.getPreUpdate() ).isNull();
		assertThat( lifecycleCallbackCollector.getPostUpdate() ).isNull();

		assertThat( lifecycleCallbackCollector.getPreRemove() ).isNull();
		assertThat( lifecycleCallbackCollector.getPostRemove() ).isNull();
	}
}
