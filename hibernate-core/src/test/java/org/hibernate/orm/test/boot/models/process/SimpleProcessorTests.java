/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.process;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.orm.test.boot.models.BootstrapContextTesting;
import org.hibernate.orm.test.boot.models.ManagedResourcesImpl;
import org.hibernate.orm.test.boot.models.MyStringConverter;
import org.hibernate.orm.test.boot.models.MyUuidConverter;
import org.hibernate.orm.test.boot.models.SourceModelTestHelper;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.FilterDefRegistration;
import org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.type.CharBooleanConverter;
import org.hibernate.type.YesNoConverter;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.StringJavaType;

import org.junit.jupiter.api.Test;

import org.jboss.jandex.Index;

import jakarta.persistence.InheritanceType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;

/**
 * Tests the general process / flow of using hibernate-models
 *
 * @author Steve Ebersole
 */
public class SimpleProcessorTests {
	@Test
	void testSimpleUsage() {

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// ManagedResources is built by scanning and from explicit resources
		// during ORM bootstrap
		final ManagedResourcesImpl.Builder managedResourcesBuilder = new ManagedResourcesImpl.Builder();
		managedResourcesBuilder
				.addLoadedClasses( Person.class, Root.class, Sub.class, MyStringConverter.class, MyUuidConverter.class )
				.addPackages( "org.hibernate.models.orm.process" );
		final ManagedResources managedResources = managedResourcesBuilder.build();
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// The Jandex index would generally (1) be built by WF and passed
		// to ORM or (2) be built by ORM
		final Index jandexIndex = SourceModelTestHelper.buildJandexIndex(
				SIMPLE_CLASS_LOADING,
				Person.class,
				Root.class,
				Sub.class,
				MyStringConverter.class,
				MyUuidConverter.class,
				YesNoConverter.class,
				CharBooleanConverter.class,
				BasicValueConverter.class,
				StringJavaType.class,
				AbstractClassJavaType.class
		);
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuilderImpl.MetadataBuildingOptionsImpl metadataBuildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
			final BootstrapContextTesting bootstrapContext = new BootstrapContextTesting( jandexIndex, serviceRegistry, metadataBuildingOptions );
			final CategorizedDomainModel categorizedDomainModel = ManagedResourcesProcessor.processManagedResources( managedResources, bootstrapContext );

			assertThat( categorizedDomainModel.getEntityHierarchies() ).hasSize( 2 );
			final Iterator<EntityHierarchy> hierarchies = categorizedDomainModel.getEntityHierarchies().iterator();
			final EntityHierarchy one = hierarchies.next();
			final EntityHierarchy two = hierarchies.next();

			assertThat( one.getRoot() ).isNotNull();
			assertThat( one.getRoot().getClassDetails() ).isNotNull();
			assertThat( one.getRoot().getClassDetails().getClassName() ).isNotNull();
			if ( one.getRoot().getClassDetails().getClassName().endsWith( "Person" ) ) {
				validatePersonHierarchy( one );
				validateJoinedHierarchy( two );
			}
			else {
				validatePersonHierarchy( two );
				validateJoinedHierarchy( one );
			}

			validateFilterDefs( categorizedDomainModel.getGlobalRegistrations().getFilterDefRegistrations() );
		}
	}

	private void validatePersonHierarchy(EntityHierarchy hierarchy) {
		assertThat( hierarchy.getInheritanceType() ).isEqualTo( InheritanceType.SINGLE_TABLE );
		final EntityTypeMetadata personMetadata = hierarchy.getRoot();
		assertThat( personMetadata.getClassDetails().getClassName() ).isEqualTo( Person.class.getName() );
		assertThat( personMetadata.getJpaEntityName() ).isEqualTo( "Person" );
		assertThat( personMetadata.getEntityName() ).isEqualTo( Person.class.getName() );

		assertThat( personMetadata.getSuperType() ).isNull();
		assertThat( personMetadata.hasSubTypes() ).isFalse();
		assertThat( personMetadata.getNumberOfSubTypes() ).isEqualTo( 0 );
	}

	private void validateJoinedHierarchy(EntityHierarchy hierarchy) {
		assertThat( hierarchy.getInheritanceType() ).isEqualTo( InheritanceType.JOINED );
		final EntityTypeMetadata rootMetadata = hierarchy.getRoot();
		assertThat( rootMetadata.getClassDetails().getClassName() ).isEqualTo( Root.class.getName() );
		assertThat( rootMetadata.getJpaEntityName() ).isEqualTo( "Root" );
		assertThat( rootMetadata.getEntityName() ).isEqualTo( Root.class.getName() );

		assertThat( rootMetadata.getSuperType() ).isNull();
		assertThat( rootMetadata.hasSubTypes() ).isTrue();
		assertThat( rootMetadata.getNumberOfSubTypes() ).isEqualTo( 1 );

		final EntityTypeMetadata subMetadata = (EntityTypeMetadata) rootMetadata.getSubTypes().iterator().next();
		assertThat( subMetadata ).isNotNull();
		assertThat( subMetadata.getClassDetails().getClassName() ).isEqualTo( Sub.class.getName() );
		assertThat( subMetadata.getJpaEntityName() ).isEqualTo( "Sub" );
		assertThat( subMetadata.getEntityName() ).isEqualTo( Sub.class.getName() );
		assertThat( subMetadata.getSuperType() ).isEqualTo( rootMetadata );
		assertThat( subMetadata.hasSubTypes() ).isFalse();
		assertThat( subMetadata.getNumberOfSubTypes() ).isEqualTo( 0 );
	}

	private void validateFilterDefs(Map<String, FilterDefRegistration> filterDefRegistrations) {
		assertThat( filterDefRegistrations ).hasSize( 1 );
		assertThat( filterDefRegistrations ).containsKey( "name_filter" );
		final FilterDefRegistration nameFilter = filterDefRegistrations.get( "name_filter" );
		assertThat( nameFilter.getDefaultCondition() ).isEqualTo( "name = :name" );
		final Map<String, ClassDetails> parameters = nameFilter.getParameters();
		assertThat( parameters ).hasSize( 1 );
		assertThat( parameters ).containsKey( "name" );
		assertThat( parameters.get( "name" ).getName() ).isEqualTo( String.class.getName() );
	}
}
