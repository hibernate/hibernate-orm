/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.hbm._extends;

import jakarta.persistence.InheritanceType;
import org.hibernate.boot.archive.internal.RepeatableInputStreamAccess;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class ExtendsTests {

	@ServiceRegistry()
	@Test
	void testDiscriminatedSeparated(ServiceRegistryScope registryScope) {
		final StandardServiceRegistry serviceRegistry = registryScope.getRegistry();
		final ClassLoaderService classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		final MappingBinder mappingBinder = new MappingBinder( serviceRegistry );
		final String mappingName = "mappings/models/hbm/extends/discriminated-separate.xml";

		try (InputStream stream = classLoaderService.locateResourceStream( mappingName )) {
			final Binding<JaxbBindableMappingDescriptor> binding = mappingBinder.bind(
					new RepeatableInputStreamAccess( SourceType.INPUT_STREAM.toString(), stream),
					new Origin( SourceType.RESOURCE, mappingName )
			);
			verifyHierarchy( (JaxbEntityMappingsImpl) binding.getRoot(), InheritanceType.SINGLE_TABLE );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	private void verifyHierarchy(JaxbEntityMappingsImpl transformed, InheritanceType inheritanceType) {
		assertThat( transformed ).isNotNull();
		assertThat( transformed.getEntities() ).hasSize( 3 );

		for ( JaxbEntityImpl jaxbEntity : transformed.getEntities() ) {
			if ( "Root".equals( jaxbEntity.getName() ) ) {
				assertThat( jaxbEntity.getInheritance() ).isNotNull();
				assertThat( jaxbEntity.getInheritance().getStrategy() ).isEqualTo( inheritanceType );
				assertThat( jaxbEntity.getExtends() ).isNull();
				assertThat( jaxbEntity.getDiscriminatorColumn().getName() ).isEqualTo( "the_type" );
				assertThat( jaxbEntity.getDiscriminatorValue() ).isEqualTo( "R" );
			}
			else if ( "Branch".equals( jaxbEntity.getName() ) ) {
				assertThat( jaxbEntity.getInheritance() ).isNull();
				assertThat( jaxbEntity.getDiscriminatorValue() ).isEqualTo( "B" );
				assertThat( jaxbEntity.getExtends() ).isEqualTo( "Root" );
			}
			else if ( "Leaf".equals( jaxbEntity.getName() ) ) {
				assertThat( jaxbEntity.getInheritance() ).isNull();
				assertThat( jaxbEntity.getDiscriminatorValue() ).isEqualTo( "L" );
				assertThat( jaxbEntity.getExtends() ).isEqualTo( "Branch" );

			}
		}
	}
}
