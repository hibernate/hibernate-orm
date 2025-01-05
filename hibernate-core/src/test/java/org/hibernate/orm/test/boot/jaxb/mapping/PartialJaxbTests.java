/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping;

import org.hibernate.boot.archive.internal.RepeatableInputStreamAccess;
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
			final String resourceLocation = "xml/jaxb/mapping/partial/caching.xml";
			//noinspection unchecked
			final Binding<JaxbEntityMappingsImpl> binding = mappingBinder.bind(
					new RepeatableInputStreamAccess( resourceLocation, cls.locateResourceStream( resourceLocation )),
					new Origin( SourceType.RESOURCE, resourceLocation )
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
