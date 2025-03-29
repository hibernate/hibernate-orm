/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.hbm;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;

import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry
public class BasicTransformationTests {

	@Test
	public void testBasicTransformation(ServiceRegistryScope scope) {
		final JaxbEntityMappingsImpl transformed = TransformationHelper.transform( "xml/jaxb/mapping/basic/hbm.xml", scope.getRegistry() );

		assertThat( transformed ).isNotNull();

		assertThat( transformed.getPackage() ).isEqualTo( "org.hibernate.orm.test.boot.jaxb.mapping" );
		assertThat( transformed.getCatalog() ).isNull();
		assertThat( transformed.getSchema() ).isNull();
		assertThat( transformed.getAccess() ).isNull();
		assertThat( transformed.getAttributeAccessor() ).isEqualTo( "property" );
		assertThat( transformed.getDefaultCascade() ).isEqualTo( "none" );

		assertThat( transformed.getEntities() ).hasSize( 1 );
		assertThat( transformed.getEmbeddables() ).hasSize( 0 );

		final JaxbEntityImpl ormEntity = transformed.getEntities().get( 0 );
		assertThat( ormEntity.getName() ).isNull();
		assertThat( ormEntity.getClazz() ).isEqualTo( "SimpleEntity" );

		assertThat( ormEntity.getAttributes().getIdAttributes() ).hasSize( 1 );
		assertThat( ormEntity.getAttributes().getBasicAttributes() ).hasSize( 1 );
		assertThat( ormEntity.getAttributes().getEmbeddedAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getOneToOneAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getManyToOneAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getAnyMappingAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getOneToManyAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getManyToManyAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getPluralAnyMappingAttributes() ).isEmpty();

		TransformationHelper.verifyTransformation( transformed );
	}

	@Test
	public void testBasicTransformation2(ServiceRegistryScope scope) {
		final JaxbEntityMappingsImpl transformed = TransformationHelper.transform( "mappings/hbm/basic.xml", scope.getRegistry() );

		assertThat( transformed ).isNotNull();

		assertThat( transformed.getPackage() ).isEqualTo( "org.hibernate.orm.test.boot.jaxb.hbm" );
		assertThat( transformed.getCatalog() ).isEqualTo( "the_catalog" );
		assertThat( transformed.getSchema() ).isEqualTo( "the_schema" );
		assertThat( transformed.getAccess() ).isNull();
		assertThat( transformed.getAttributeAccessor() ).isEqualTo( "field" );
		assertThat( transformed.getDefaultCascade() ).isEqualTo( "all" );

		assertThat( transformed.getEntities() ).hasSize( 1 );
		assertThat( transformed.getEmbeddables() ).hasSize( 1 );

		final JaxbEntityImpl ormEntity = transformed.getEntities().get( 0 );
		assertThat( ormEntity.getName() ).isNull();
		assertThat( ormEntity.getClazz() ).isEqualTo( "BasicEntity" );

		assertThat( ormEntity.getAttributes().getIdAttributes() ).hasSize( 1 );
		assertThat( ormEntity.getAttributes().getBasicAttributes() ).hasSize( 1 );
		assertThat( ormEntity.getAttributes().getEmbeddedAttributes() ).hasSize( 1 );
		assertThat( ormEntity.getAttributes().getOneToOneAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getManyToOneAttributes() ).hasSize(1 );
		assertThat( ormEntity.getAttributes().getAnyMappingAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getOneToManyAttributes() ).hasSize( 1 );
		assertThat( ormEntity.getAttributes().getManyToManyAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getPluralAnyMappingAttributes() ).isEmpty();

		final JaxbEmbeddableImpl jaxbEmbeddable = transformed.getEmbeddables().get( 0 );
		assertThat( jaxbEmbeddable.isMetadataComplete() ).isTrue();
		assertThat( jaxbEmbeddable.getName() ).isEqualTo( "org.hibernate.orm.test.boot.jaxb.hbm.BasicComposition" );
		assertThat( jaxbEmbeddable.getClazz() ).isEqualTo( "org.hibernate.orm.test.boot.jaxb.hbm.BasicComposition" );

		TransformationHelper.verifyTransformation( transformed );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16822" )
	public void testSimpleTransformation(ServiceRegistryScope scope) {
		final JaxbEntityMappingsImpl transformed = TransformationHelper.transform( "mappings/hbm/simple.xml", scope.getRegistry() );

		assertThat( transformed ).isNotNull();
		assertThat( transformed.getEntities() ).hasSize( 1 );
		assertThat( transformed.getEmbeddables() ).hasSize( 0 );

		final JaxbEntityImpl ormEntity = transformed.getEntities().get( 0 );
		assertThat( ormEntity.getName() ).isEqualTo( "SimpleEntity" );
		assertThat( ormEntity.getClazz() ).isNull();

		assertThat( ormEntity.getAttributes().getIdAttributes() ).hasSize( 1 );
		assertThat( ormEntity.getAttributes().getBasicAttributes() ).hasSize( 1 );
		assertThat( ormEntity.getAttributes().getEmbeddedAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getOneToOneAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getManyToOneAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getAnyMappingAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getOneToManyAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getManyToManyAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getPluralAnyMappingAttributes() ).isEmpty();

		TransformationHelper.verifyTransformation( transformed );
	}

}
