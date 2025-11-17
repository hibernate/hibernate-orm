/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class PartialJaxbTests {
	@Test
	public void cachingTest(ServiceRegistryScope scope) {
		final MappingBinder mappingBinder = new MappingBinder( scope.getRegistry() );
		scope.withService( ClassLoaderService.class, (cls) -> {
			//noinspection unchecked
			final Binding<JaxbEntityMappingsImpl> binding = mappingBinder.bind(
					cls.locateResourceStream( "xml/jaxb/mapping/partial/caching.xml" ),
					new Origin( SourceType.RESOURCE, "xml/jaxb/mapping/partial/caching.xml" )
			);

			final JaxbEntityMappingsImpl entityMappings = binding.getRoot();
			assertThat( entityMappings ).isNotNull();
			assertThat( entityMappings.getEntities() ).hasSize( 1 );
			assertThat( entityMappings.getPackage() ).isEqualTo( "org.hibernate.orm.test.boot.jaxb.mapping" );

			final JaxbEntityImpl ormEntity = entityMappings.getEntities().get( 0 );
			assertThat( ormEntity.getName() ).isNull();
			assertThat( ormEntity.getClazz() ).isEqualTo( "SimpleEntity" );
			assertThat( ormEntity.isCacheable() ).isTrue();
			assertThat( ormEntity.getCaching() ).isNotNull();
			assertThat( ormEntity.getCaching().getRegion() ).isEqualTo( "netherworld" );

			assertThat( ormEntity.getAttributes() ).isNull();
		} );
	}
}
