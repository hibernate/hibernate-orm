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
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmJoinedSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmUnionSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.transform.HbmXmlTransformerComponentHandler;
import org.hibernate.boot.jaxb.hbm.transform.TransformationState;
import org.hibernate.boot.jaxb.hbm.transform.XmlPreprocessor;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that {@link HbmXmlTransformerComponentHandler} can be used standalone
 * (without a boot model) to create embeddable entries from HBM XML.
 */
public class HbmXmlComponentVisitorTest {

	@Test
	void componentInRootEntity() {
		JaxbHbmHibernateMapping hbmMapping = new JaxbHbmHibernateMapping();

		JaxbHbmRootEntityType entity = new JaxbHbmRootEntityType();
		entity.setName( "MyEntity" );

		JaxbHbmCompositeAttributeType component = new JaxbHbmCompositeAttributeType();
		component.setName( "address" );
		component.setClazz( "com.example.Address" );
		entity.getAttributes().add( component );

		hbmMapping.getClazz().add( entity );

		JaxbEntityMappingsImpl result = processWithHandler( hbmMapping );

		assertThat( result.getEmbeddables() )
				.hasSize( 1 )
				.extracting( e -> e.getClazz() )
				.containsExactly( "com.example.Address" );
	}

	@Test
	void nestedComponents() {
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

		JaxbEntityMappingsImpl result = processWithHandler( hbmMapping );

		assertThat( result.getEmbeddables() )
				.hasSize( 2 )
				.extracting( e -> e.getClazz() )
				.containsExactlyInAnyOrder( "com.example.Address", "com.example.City" );
	}

	@Test
	void componentInDiscriminatorSubclass() {
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

		JaxbEntityMappingsImpl result = processWithHandler( hbmMapping );

		assertThat( result.getEmbeddables() )
				.hasSize( 1 )
				.extracting( e -> e.getClazz() )
				.containsExactly( "com.example.Address" );
	}

	@Test
	void componentInJoinedSubclass() {
		JaxbHbmHibernateMapping hbmMapping = new JaxbHbmHibernateMapping();

		JaxbHbmJoinedSubclassEntityType subclass = new JaxbHbmJoinedSubclassEntityType();
		subclass.setName( "JoinedChild" );

		JaxbHbmCompositeAttributeType component = new JaxbHbmCompositeAttributeType();
		component.setName( "details" );
		component.setClazz( "com.example.Details" );
		subclass.getAttributes().add( component );

		hbmMapping.getJoinedSubclass().add( subclass );

		JaxbEntityMappingsImpl result = processWithHandler( hbmMapping );

		assertThat( result.getEmbeddables() )
				.hasSize( 1 )
				.extracting( e -> e.getClazz() )
				.containsExactly( "com.example.Details" );
	}

	@Test
	void componentInUnionSubclass() {
		JaxbHbmHibernateMapping hbmMapping = new JaxbHbmHibernateMapping();

		JaxbHbmUnionSubclassEntityType subclass = new JaxbHbmUnionSubclassEntityType();
		subclass.setName( "UnionChild" );

		JaxbHbmCompositeAttributeType component = new JaxbHbmCompositeAttributeType();
		component.setName( "info" );
		component.setClazz( "com.example.Info" );
		subclass.getAttributes().add( component );

		hbmMapping.getUnionSubclass().add( subclass );

		JaxbEntityMappingsImpl result = processWithHandler( hbmMapping );

		assertThat( result.getEmbeddables() )
				.hasSize( 1 )
				.extracting( e -> e.getClazz() )
				.containsExactly( "com.example.Info" );
	}

	@Test
	void duplicateComponentClassIsNotDuplicated() {
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

		JaxbEntityMappingsImpl result = processWithHandler( hbmMapping );

		assertThat( result.getEmbeddables() )
				.extracting( e -> e.getClazz() )
				.containsExactly( "com.example.Address" );
	}

	@Test
	void entityWithoutComponentYieldsNoEmbeddables() {
		JaxbHbmHibernateMapping hbmMapping = new JaxbHbmHibernateMapping();

		JaxbHbmRootEntityType entity = new JaxbHbmRootEntityType();
		entity.setName( "SimpleEntity" );
		hbmMapping.getClazz().add( entity );

		JaxbEntityMappingsImpl result = processWithHandler( hbmMapping );

		assertThat( result.getEmbeddables() ).isEmpty();
	}

	/**
	 * Uses the lightweight constructor (no boot model) to collect embeddables.
	 */
	private static JaxbEntityMappingsImpl processWithHandler(JaxbHbmHibernateMapping hbmMapping) {
		final var mappingRoot = new JaxbEntityMappingsImpl();
		final var handler = new HbmXmlTransformerComponentHandler( mappingRoot );

		final var transformationState = new TransformationState();
		XmlPreprocessor.preprocessHbmXml(
				List.of( new Binding<>( hbmMapping, new Origin( SourceType.OTHER, "test" ) ) ),
				transformationState
		);

		for ( var entry : transformationState.getHbmEntityByName().entrySet() ) {
			for ( Object attr : entry.getValue().getAttributes() ) {
				if ( attr instanceof JaxbHbmCompositeAttributeType composite ) {
					handler.applyEmbeddable( entry.getKey(), composite, null );
				}
			}
		}

		return mappingRoot;
	}
}
