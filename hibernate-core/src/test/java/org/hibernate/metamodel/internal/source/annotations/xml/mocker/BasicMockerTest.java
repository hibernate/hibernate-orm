/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.internal.source.annotations.xml.mocker;

import static org.junit.Assert.assertEquals;

import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.jandex.Default;
import org.hibernate.metamodel.source.internal.jandex.EntityMocker;
import org.hibernate.metamodel.source.internal.jandex.IndexBuilder;
import org.hibernate.metamodel.source.internal.jaxb.JaxbAttributes;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntity;
import org.hibernate.metamodel.source.internal.jaxb.JaxbGeneratedValue;
import org.hibernate.metamodel.source.internal.jaxb.JaxbId;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.Test;

/**
 * @author Strong Liu
 */
public class BasicMockerTest extends AbstractMockerTest {
	@Test
	public void testEntity() {
		JaxbEntity entity = createEntity();
		IndexBuilder indexBuilder = getIndexBuilder();
		EntityMocker entityMocker = new EntityMocker( indexBuilder, entity, new Default() );
		entityMocker.preProcess();
		entityMocker.process();

		Index index = indexBuilder.build( new Default() );
		assertEquals( 1, index.getKnownClasses().size() );
		DotName itemName = DotName.createSimple( Item.class.getName() );
		assertHasAnnotation( index, itemName, JPADotNames.ENTITY );
		assertHasAnnotation( index, itemName, JPADotNames.ID );
		assertHasAnnotation( index, itemName, JPADotNames.GENERATED_VALUE );
	}

	@Test
	public void testEntityWithEntityMappingsConfiguration() {
		JaxbEntity entity = new JaxbEntity();
		entity.setName( "Item" );
		entity.setClazz( "Item" );
		IndexBuilder indexBuilder = getIndexBuilder();
		Default defaults = new Default();
		defaults.setPackageName( getClass().getPackage().getName() );
		defaults.setSchema( "HIBERNATE_SCHEMA" );
		defaults.setCatalog( "HIBERNATE_CATALOG" );
		EntityMocker entityMocker = new EntityMocker( indexBuilder, entity, defaults );
		entityMocker.preProcess();
		entityMocker.process();

		Index index = indexBuilder.build( new Default() );
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


	private JaxbEntity createEntity() {
		JaxbEntity entity = new JaxbEntity();
		entity.setName( "Item" );
		entity.setClazz( Item.class.getName() );
		JaxbAttributes attributes = new JaxbAttributes();
		JaxbId id = new JaxbId();
		id.setName( "id" );
		id.setGeneratedValue( new JaxbGeneratedValue() );
		attributes.getId().add( id );
		entity.setAttributes( attributes );
		return entity;
	}


}
