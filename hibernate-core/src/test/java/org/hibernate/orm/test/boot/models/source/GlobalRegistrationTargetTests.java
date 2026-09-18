/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.source;

import java.nio.file.Path;

import org.hibernate.boot.mapping.internal.categorize.DomainModelCategorizationCollector;
import org.hibernate.orm.test.boot.models.source.registrationtargets.Target;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.orm.module.TestModule;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Common global registrations must have the same semantics at every supported target.
///
/// @author Steve Ebersole
class GlobalRegistrationTargetTests {
	@Test
	void collectsRepeatableRegistrationsAndRetainsConflictHandling(@TempDir Path directory) throws Exception {
		final var module = TestModule.load( ShrinkWrap.create( JavaArchive.class, "registrations.jar" ).addClass( Target.class ), """
				/// @author Steve Ebersole
				@org.hibernate.annotations.FilterDef(name = "first", defaultCondition = "1=1")
				@org.hibernate.annotations.FilterDef(name = "second", defaultCondition = "2=2")
				@org.hibernate.annotations.NamedQuery(name = "queryOne", query = "from Target")
				@org.hibernate.annotations.NamedQuery(name = "queryTwo", query = "from Target where id = 1")
				module test.registrations {}
				""", directory, org.hibernate.annotations.FilterDef.class );
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder().build()) {
			final var buildingContext = new MetadataBuildingContextTestingImpl( registry );
			final var models = buildingContext.getModelsContext();
			for (int target = 0; target < 3; target++) {
				final var collector = new DomainModelCategorizationCollector( models, null, name -> null );
				final Runnable collect = switch ( target ) {
					case 0 -> () -> collector.apply( models.getClassDetailsRegistry().resolveClassDetails( Target.class.getName() ) );
					case 1 -> () -> collector.applyPackageDescriptor( models.getClassDetailsRegistry().resolveExplicitPackageDetails( Target.class.getPackageName() ) );
					default -> () -> collector.apply( models.getModuleDetailsRegistry().resolveModuleDetails( module.module() ) );
				};
				collect.run();
				assertThat( collector.getGlobalRegistrations().getFilterDefRegistrations() ).containsOnlyKeys( "first", "second" );
				assertThat( collector.getGlobalRegistrations().getNamedQueryRegistrations() ).containsOnlyKeys( "queryOne", "queryTwo" );
				if ( target == 0 ) {
					assertThat( collector.getSourceClasses() ).containsOnlyKeys( Target.class.getName() );
				}
				else {
					assertThat( collector.getSourceClasses() ).isEmpty();
				}
				assertThatThrownBy( collect::run ).isInstanceOf( org.hibernate.AnnotationException.class )
						.hasMessageContaining( "first" );
			}
		}
	}
}
