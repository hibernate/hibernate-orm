/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.jaxb.hbm;

import org.hibernate.boot.jaxb.mapping.JaxbEmbeddable;
import org.hibernate.boot.jaxb.mapping.JaxbEmbedded;
import org.hibernate.boot.jaxb.mapping.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.JaxbEntityMappings;

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
		final JaxbEntityMappings transformed = TransformationHelper.transform( "xml/jaxb/mapping/basic/hbm.xml", scope.getRegistry() );

		assertThat( transformed ).isNotNull();

		assertThat( transformed.getPackage() ).isEqualTo( "org.hibernate.orm.test.boot.jaxb.mapping" );
		assertThat( transformed.getCatalog() ).isNull();
		assertThat( transformed.getSchema() ).isNull();
		assertThat( transformed.getAccess() ).isNull();
		assertThat( transformed.getAttributeAccessor() ).isEqualTo( "property" );
		assertThat( transformed.getDefaultCascade() ).isEqualTo( "none" );

		assertThat( transformed.getEntities() ).hasSize( 1 );
		assertThat( transformed.getEmbeddables() ).hasSize( 0 );

		final JaxbEntity ormEntity = transformed.getEntities().get( 0 );
		assertThat( ormEntity.getName() ).isNull();
		assertThat( ormEntity.getClazz() ).isEqualTo( "SimpleEntity" );

		assertThat( ormEntity.getAttributes().getId() ).hasSize( 1 );
		assertThat( ormEntity.getAttributes().getBasicAttributes() ).hasSize( 1 );
		assertThat( ormEntity.getAttributes().getEmbeddedAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getOneToOneAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getManyToOneAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getDiscriminatedAssociations() ).isEmpty();
		assertThat( ormEntity.getAttributes().getOneToManyAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getManyToManyAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getPluralDiscriminatedAssociations() ).isEmpty();

		TransformationHelper.verifyTransformation( transformed );
	}

	@Test
	public void testBasicTransformation2(ServiceRegistryScope scope) {
		final JaxbEntityMappings transformed = TransformationHelper.transform( "mappings/hbm/basic.xml", scope.getRegistry() );

		assertThat( transformed ).isNotNull();

		assertThat( transformed.getPackage() ).isEqualTo( "org.hibernate.orm.test.boot.jaxb.hbm" );
		assertThat( transformed.getCatalog() ).isEqualTo( "the_catalog" );
		assertThat( transformed.getSchema() ).isEqualTo( "the_schema" );
		assertThat( transformed.getAccess() ).isNull();
		assertThat( transformed.getAttributeAccessor() ).isEqualTo( "field" );
		assertThat( transformed.getDefaultCascade() ).isEqualTo( "all" );

		assertThat( transformed.getEntities() ).hasSize( 1 );
		assertThat( transformed.getEmbeddables() ).hasSize( 1 );

		final JaxbEntity ormEntity = transformed.getEntities().get( 0 );
		assertThat( ormEntity.getName() ).isNull();
		assertThat( ormEntity.getClazz() ).isEqualTo( "BasicEntity" );

		assertThat( ormEntity.getAttributes().getId() ).hasSize( 1 );
		assertThat( ormEntity.getAttributes().getBasicAttributes() ).hasSize( 1 );
		assertThat( ormEntity.getAttributes().getEmbeddedAttributes() ).hasSize( 1 );
		assertThat( ormEntity.getAttributes().getOneToOneAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getManyToOneAttributes() ).hasSize(1 );
		assertThat( ormEntity.getAttributes().getDiscriminatedAssociations() ).isEmpty();
		assertThat( ormEntity.getAttributes().getOneToManyAttributes() ).hasSize( 1 );
		assertThat( ormEntity.getAttributes().getManyToManyAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getPluralDiscriminatedAssociations() ).isEmpty();

		final JaxbEmbeddable jaxbEmbeddable = transformed.getEmbeddables().get( 0 );
		assertThat( jaxbEmbeddable.isMetadataComplete() ).isTrue();
		assertThat( jaxbEmbeddable.getClazz() ).isEqualTo( "composition_1" );

		TransformationHelper.verifyTransformation( transformed );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16822" )
	public void testSimpleTransformation(ServiceRegistryScope scope) {
		final JaxbEntityMappings transformed = TransformationHelper.transform( "mappings/hbm/simple.xml", scope.getRegistry() );

		assertThat( transformed ).isNotNull();
		assertThat( transformed.getEntities() ).hasSize( 1 );
		assertThat( transformed.getEmbeddables() ).hasSize( 0 );

		final JaxbEntity ormEntity = transformed.getEntities().get( 0 );
		assertThat( ormEntity.getName() ).isNull();
		assertThat( ormEntity.getClazz() ).isEqualTo( "SimpleEntity" );

		assertThat( ormEntity.getAttributes().getId() ).hasSize( 1 );
		assertThat( ormEntity.getAttributes().getBasicAttributes() ).hasSize( 1 );
		assertThat( ormEntity.getAttributes().getEmbeddedAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getOneToOneAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getManyToOneAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getDiscriminatedAssociations() ).isEmpty();
		assertThat( ormEntity.getAttributes().getOneToManyAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getManyToManyAttributes() ).isEmpty();
		assertThat( ormEntity.getAttributes().getPluralDiscriminatedAssociations() ).isEmpty();

		TransformationHelper.verifyTransformation( transformed );
	}

}
