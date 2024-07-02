/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.hbm._extends;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.orm.test.boot.jaxb.hbm.TransformationHelper;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.InheritanceType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class ExtendsTests {

	@ServiceRegistry()
	@Test
	void testDiscriminatedStructured(ServiceRegistryScope registryScope) {
		final JaxbEntityMappingsImpl transformed = TransformationHelper.transform(
				"mappings/models/hbm/extends/discriminated-structured.xml",
				registryScope.getRegistry()
		);
		verifyHierarchy( transformed, InheritanceType.SINGLE_TABLE );
	}

	@ServiceRegistry()
	@Test
	void testDiscriminatedSeparated(ServiceRegistryScope registryScope) {
		final JaxbEntityMappingsImpl transformed = TransformationHelper.transform(
				"mappings/models/hbm/extends/discriminated-separate.xml",
				registryScope.getRegistry()
		);
		verifyHierarchy( transformed, InheritanceType.SINGLE_TABLE );
	}

	private void verifyHierarchy(JaxbEntityMappingsImpl transformed, InheritanceType inheritanceType) {
		assertThat( transformed ).isNotNull();
		assertThat( transformed.getEntities() ).hasSize( 3 );

		for ( JaxbEntityImpl jaxbEntity : transformed.getEntities() ) {
			if ( "org.hibernate.test.hbm._extends.Root".equals( jaxbEntity.getClazz() ) ) {
				assertThat( jaxbEntity.getInheritance() ).isNotNull();
				assertThat( jaxbEntity.getInheritance().getStrategy() ).isEqualTo( inheritanceType );
				assertThat( jaxbEntity.getExtends() ).isNull();
				assertThat( jaxbEntity.getDiscriminatorColumn().getName() ).isEqualTo( "the_type" );
				assertThat( jaxbEntity.getDiscriminatorValue() ).isEqualTo( "R" );
			}
			else if ( "org.hibernate.test.hbm._extends.Branch".equals( jaxbEntity.getName() ) ) {
				assertThat( jaxbEntity.getInheritance() ).isNull();
				assertThat( jaxbEntity.getDiscriminatorValue() ).isEqualTo( "B" );
				assertThat( jaxbEntity.getExtends() ).isEqualTo( "org.hibernate.test.hbm._extends.Root" );
			}
			else if ( "org.hibernate.test.hbm._extends.Leaf".equals( jaxbEntity.getName() ) ) {
				assertThat( jaxbEntity.getInheritance() ).isNull();
				assertThat( jaxbEntity.getDiscriminatorValue() ).isEqualTo( "L" );
				assertThat( jaxbEntity.getExtends() ).isEqualTo( "org.hibernate.test.hbm._extends.Branch" );

			}
		}
	}
}
