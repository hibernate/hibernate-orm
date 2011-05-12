package org.hibernate.metamodel.source.annotations.xml.mocker;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.Test;

import org.hibernate.metamodel.source.annotation.xml.XMLAttributes;
import org.hibernate.metamodel.source.annotation.xml.XMLEntity;
import org.hibernate.metamodel.source.annotation.xml.XMLGeneratedValue;
import org.hibernate.metamodel.source.annotation.xml.XMLId;
import org.hibernate.metamodel.source.annotations.JPADotNames;

import static org.junit.Assert.assertEquals;

/**
 * @author Strong Liu
 */
public class BasicMockerTest extends AbstractMockerTest {
	@Test
	public void testEntity() {
		XMLEntity entity = createEntity();
		IndexBuilder indexBuilder = getIndexBuilder();
		EntityMocker entityMocker = new EntityMocker( indexBuilder, entity, new EntityMappingsMocker.Default() );
		entityMocker.preProcess();
		entityMocker.process();

		Index index = indexBuilder.build( new EntityMappingsMocker.Default() );
		assertEquals( 1, index.getKnownClasses().size() );
		DotName itemName = DotName.createSimple( Item.class.getName() );
		assertHasAnnotation( index, itemName, JPADotNames.ENTITY );
		assertHasAnnotation( index, itemName, JPADotNames.ID );
		assertHasAnnotation( index, itemName, JPADotNames.GENERATED_VALUE );
	}

	@Test
	public void testEntityWithEntityMappingsConfiguration() {
		XMLEntity entity = new XMLEntity();
		entity.setName( "Item" );
		entity.setClazz( "Item" );
		IndexBuilder indexBuilder = getIndexBuilder();
		EntityMappingsMocker.Default defaults = new EntityMappingsMocker.Default();
		defaults.setPackageName( "org.hibernate.metamodel.source.annotations.xml.mocker" );
		defaults.setSchema( "HIBERNATE_SCHEMA" );
		defaults.setCatalog( "HIBERNATE_CATALOG" );
		EntityMocker entityMocker = new EntityMocker( indexBuilder, entity, defaults );
		entityMocker.preProcess();
		entityMocker.process();

		Index index = indexBuilder.build( new EntityMappingsMocker.Default() );
		assertEquals( 1, index.getKnownClasses().size() );
		DotName itemName = DotName.createSimple( Item.class.getName() );
		assertHasAnnotation( index, itemName, JPADotNames.ENTITY );
		assertHasAnnotation( index, itemName, JPADotNames.TABLE );
		assertAnnotationValue(
				index, itemName, JPADotNames.TABLE, new AnnotationValueChecker() {
					@Override
					public void check(AnnotationInstance annotationInstance) {
						AnnotationValue schemaValue = annotationInstance.value( "schema" );
						AnnotationValue catalogValue = annotationInstance.value( "catalog" );
						assertStringAnnotationValue( "HIBERNATE_SCHEMA", schemaValue );
						assertStringAnnotationValue( "HIBERNATE_CATALOG", catalogValue );
					}
				}
		);

	}


	private XMLEntity createEntity() {
		XMLEntity entity = new XMLEntity();
		entity.setName( "Item" );
		entity.setClazz( Item.class.getName() );
		XMLAttributes attributes = new XMLAttributes();
		XMLId id = new XMLId();
		id.setName( "id" );
		id.setGeneratedValue( new XMLGeneratedValue() );
		attributes.getId().add( id );
		entity.setAttributes( attributes );
		return entity;
	}


}
