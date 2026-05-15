/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.hbm.transform;

import java.util.List;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDiscriminatorSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.transform.TransformationState;
import org.hibernate.boot.jaxb.hbm.transform.XmlPreprocessor;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class XmlPreprocessorTest {

	@Test
	void componentIsPreprocessedAsEmbeddable() {
		JaxbHbmHibernateMapping hbmMapping = new JaxbHbmHibernateMapping();

		JaxbHbmRootEntityType entity = new JaxbHbmRootEntityType();
		entity.setName( "MyEntity" );

		JaxbHbmCompositeAttributeType component = new JaxbHbmCompositeAttributeType();
		component.setName( "address" );
		component.setClazz( "com.example.Address" );
		entity.getAttributes().add( component );

		hbmMapping.getClazz().add( entity );

		List<Binding<JaxbEntityMappingsImpl>> result = XmlPreprocessor.preprocessHbmXml(
				List.of( new Binding<>( hbmMapping, new Origin( SourceType.OTHER, "test" ) ) ),
				new TransformationState()
		);

		JaxbEntityMappingsImpl mappingRoot = result.get( 0 ).getRoot();
		assertThat( mappingRoot.getEntities() ).hasSize( 1 );
		assertThat( mappingRoot.getEmbeddables() ).hasSize( 1 );
		assertThat( mappingRoot.getEmbeddables().get( 0 ).getClazz() ).isEqualTo( "com.example.Address" );
	}

	@Test
	void nestedComponentIsPreprocessedAsEmbeddable() {
		JaxbHbmHibernateMapping hbmMapping = new JaxbHbmHibernateMapping();

		JaxbHbmRootEntityType entity = new JaxbHbmRootEntityType();
		entity.setName( "MyEntity" );

		JaxbHbmCompositeAttributeType innerComponent = new JaxbHbmCompositeAttributeType();
		innerComponent.setName( "city" );
		innerComponent.setClazz( "com.example.City" );

		JaxbHbmCompositeAttributeType outerComponent = new JaxbHbmCompositeAttributeType();
		outerComponent.setName( "address" );
		outerComponent.setClazz( "com.example.Address" );
		outerComponent.getAttributes().add( innerComponent );

		entity.getAttributes().add( outerComponent );
		hbmMapping.getClazz().add( entity );

		List<Binding<JaxbEntityMappingsImpl>> result = XmlPreprocessor.preprocessHbmXml(
				List.of( new Binding<>( hbmMapping, new Origin( SourceType.OTHER, "test" ) ) ),
				new TransformationState()
		);

		JaxbEntityMappingsImpl mappingRoot = result.get( 0 ).getRoot();
		assertThat( mappingRoot.getEmbeddables() )
				.hasSize( 2 )
				.extracting( e -> e.getClazz() )
				.containsExactlyInAnyOrder( "com.example.Address", "com.example.City" );
	}

	@Test
	void subclassComponentIsPreprocessedAsEmbeddable() {
		JaxbHbmHibernateMapping hbmMapping = new JaxbHbmHibernateMapping();

		JaxbHbmRootEntityType rootEntity = new JaxbHbmRootEntityType();
		rootEntity.setName( "ParentEntity" );

		JaxbHbmDiscriminatorSubclassEntityType subclass = new JaxbHbmDiscriminatorSubclassEntityType();
		subclass.setName( "ChildEntity" );

		JaxbHbmCompositeAttributeType component = new JaxbHbmCompositeAttributeType();
		component.setName( "address" );
		component.setClazz( "com.example.Address" );
		subclass.getAttributes().add( component );

		rootEntity.getSubclass().add( subclass );
		hbmMapping.getClazz().add( rootEntity );

		List<Binding<JaxbEntityMappingsImpl>> result = XmlPreprocessor.preprocessHbmXml(
				List.of( new Binding<>( hbmMapping, new Origin( SourceType.OTHER, "test" ) ) ),
				new TransformationState()
		);

		JaxbEntityMappingsImpl mappingRoot = result.get( 0 ).getRoot();
		assertThat( mappingRoot.getEmbeddables() )
				.hasSize( 1 )
				.extracting( e -> e.getClazz() )
				.containsExactly( "com.example.Address" );
	}

	@Test
	void sameComponentInRootAndSubclassIsNotDuplicated() {
		JaxbHbmHibernateMapping hbmMapping = new JaxbHbmHibernateMapping();

		JaxbHbmRootEntityType rootEntity = new JaxbHbmRootEntityType();
		rootEntity.setName( "ParentEntity" );

		JaxbHbmCompositeAttributeType rootComponent = new JaxbHbmCompositeAttributeType();
		rootComponent.setName( "address" );
		rootComponent.setClazz( "com.example.Address" );
		rootEntity.getAttributes().add( rootComponent );

		JaxbHbmDiscriminatorSubclassEntityType subclass = new JaxbHbmDiscriminatorSubclassEntityType();
		subclass.setName( "ChildEntity" );

		JaxbHbmCompositeAttributeType subclassComponent = new JaxbHbmCompositeAttributeType();
		subclassComponent.setName( "shippingAddress" );
		subclassComponent.setClazz( "com.example.Address" );
		subclass.getAttributes().add( subclassComponent );

		rootEntity.getSubclass().add( subclass );
		hbmMapping.getClazz().add( rootEntity );

		List<Binding<JaxbEntityMappingsImpl>> result = XmlPreprocessor.preprocessHbmXml(
				List.of( new Binding<>( hbmMapping, new Origin( SourceType.OTHER, "test" ) ) ),
				new TransformationState()
		);

		JaxbEntityMappingsImpl mappingRoot = result.get( 0 ).getRoot();
		assertThat( mappingRoot.getEmbeddables() )
				.extracting( e -> e.getClazz() )
				.containsExactly( "com.example.Address" );
	}

	@Test
	void entityWithoutComponentHasNoEmbeddables() {
		JaxbHbmHibernateMapping hbmMapping = new JaxbHbmHibernateMapping();

		JaxbHbmRootEntityType entity = new JaxbHbmRootEntityType();
		entity.setName( "SimpleEntity" );
		hbmMapping.getClazz().add( entity );

		List<Binding<JaxbEntityMappingsImpl>> result = XmlPreprocessor.preprocessHbmXml(
				List.of( new Binding<>( hbmMapping, new Origin( SourceType.OTHER, "test" ) ) ),
				new TransformationState()
		);

		JaxbEntityMappingsImpl mappingRoot = result.get( 0 ).getRoot();
		assertThat( mappingRoot.getEntities() ).hasSize( 1 );
		assertThat( mappingRoot.getEmbeddables() ).isEmpty();
	}
}
