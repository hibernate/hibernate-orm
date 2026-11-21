package org.hibernate.orm.test.hbm.cascade;

import org.hibernate.boot.pipeline.internal.MetadataBuildingHelper;
import org.hibernate.boot.pipeline.internal.source.MappingSources;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;


@BaseUnitTest
class DefaultCascadeBindingTest {

	@Test
	void legacyDefaultCascadeMappingIsRejected() {
		try ( var registry = ServiceRegistryBuilder.buildServiceRegistry() ) {
			final var sources = new MappingSources().addMappingResource(
					"org/hibernate/orm/test/hbm/cascade/default-cascade.hbm.xml" );
			assertThatThrownBy( () -> MetadataBuildingHelper.buildMetadata( registry, sources ) )
					.hasStackTraceContaining( "Support for binding hbm.xml documents has been removed" );
		}
	}
}
