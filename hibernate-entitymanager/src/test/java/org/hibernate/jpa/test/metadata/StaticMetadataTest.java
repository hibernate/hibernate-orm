/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.jpa.test.metadata;

import java.util.Set;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.Type;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@FailureExpectedWithNewMetamodel( jiraKey="HHH-8935" )
public class StaticMetadataTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testInjections() throws Exception {
		// Address (embeddable)
		assertNotNull( Address_.address1 );
		assertNotNull( Address_.address2 );
		assertNotNull( Address_.city );
		final EmbeddableType<Address> addressType = ( EmbeddableType<Address> ) House_.address.getType();
		assertEquals( addressType.getDeclaredSingularAttribute( "address1" ), Address_.address1 );
		assertEquals( addressType.getDeclaredSingularAttribute( "address2" ), Address_.address2 );
		assertTrue( Address_.address1.isOptional() );
		assertFalse( Address_.address2.isOptional() );

		// Animal (mapped superclass)
		assertNotNull( Animal_.id );
		assertTrue( Animal_.id.isId() );
		assertEquals( Long.class, Animal_.id.getJavaType() );
		assertNotNull( Animal_.legNbr );
//		assertEquals( Integer.class, Animal_.legNbr.getJavaType() );
		assertEquals( int.class, Animal_.legNbr.getJavaType() );

		// Cat (hierarchy)
		assertNotNull( Cat_.id );
		assertNotNull( Cat_.id.isId() );
		assertEquals( Animal.class, Cat_.id.getJavaMember().getDeclaringClass() );
		assertNotNull( Cat_.nickname );

		// FoodItem
		assertNotNull( FoodItem_.version );
		assertTrue( FoodItem_.version.isVersion() );

		// Fridge
		assertNotNull( Fridge_.id );
		assertTrue( Fridge_.id.isId() );
		assertEquals( Long.class, Fridge_.id.getJavaType() );
		assertNotNull( Fridge_.temperature );
		assertEquals( "temperature", Fridge_.temperature.getName() );
		assertEquals( Fridge.class, Fridge_.temperature.getDeclaringType().getJavaType() );
//		assertEquals( Integer.class, Fridge_.temperature.getJavaType() );
//		assertEquals( Integer.class, Fridge_.temperature.getBindableJavaType() );
//		assertEquals( Integer.class, Fridge_.temperature.getType().getJavaType() );
		assertEquals( int.class, Fridge_.temperature.getJavaType() );
		assertEquals( int.class, Fridge_.temperature.getBindableJavaType() );
		assertEquals( int.class, Fridge_.temperature.getType().getJavaType() );
		assertEquals( Bindable.BindableType.SINGULAR_ATTRIBUTE, Fridge_.temperature.getBindableType() );
		assertEquals( Type.PersistenceType.BASIC, Fridge_.temperature.getType().getPersistenceType() );
		assertEquals( Attribute.PersistentAttributeType.BASIC, Fridge_.temperature.getPersistentAttributeType() );
		assertFalse( Fridge_.temperature.isId() );
		assertFalse( Fridge_.temperature.isOptional() );
		assertFalse( Fridge_.temperature.isAssociation() );
		assertFalse( Fridge_.temperature.isCollection() );
		assertFalse( Fridge_.brand.isOptional() );

		// House (embedded id)
		assertNotNull( House_.key );
		assertTrue( House_.key.isId() );
		assertEquals( Attribute.PersistentAttributeType.EMBEDDED, House_.key.getPersistentAttributeType() );
		assertNotNull( House_.address );
		assertEquals( Attribute.PersistentAttributeType.EMBEDDED, House_.address.getPersistentAttributeType() );
		assertFalse( House_.address.isCollection() );
		assertFalse( House_.address.isAssociation() );
		assertNotNull( House_.rooms );
		assertTrue( House_.rooms.isAssociation() );
		assertTrue( House_.rooms.isCollection() );
		assertEquals( Attribute.PersistentAttributeType.ELEMENT_COLLECTION, House_.rooms.getPersistentAttributeType() );
		assertEquals( Room.class, House_.rooms.getBindableJavaType() );
		assertEquals( Bindable.BindableType.PLURAL_ATTRIBUTE, House_.rooms.getBindableType() );
		assertEquals( Set.class, House_.rooms.getJavaType() );
		assertEquals( PluralAttribute.CollectionType.SET, House_.rooms.getCollectionType() );
		assertEquals( Type.PersistenceType.EMBEDDABLE, House_.rooms.getElementType().getPersistenceType() );
		assertNotNull( House_.roomsByName );
		assertEquals( String.class, House_.roomsByName.getKeyJavaType() );
		assertEquals( Type.PersistenceType.BASIC, House_.roomsByName.getKeyType().getPersistenceType() );
		assertEquals( PluralAttribute.CollectionType.MAP, House_.roomsByName.getCollectionType() );
		assertNotNull( House_.roomsBySize );
		assertEquals( Type.PersistenceType.EMBEDDABLE, House_.roomsBySize.getElementType().getPersistenceType() );
		assertEquals( PluralAttribute.CollectionType.LIST, House_.roomsBySize.getCollectionType() );

		// Person (mapped id)
		assertNotNull( Person_.firstName );
		assertNotNull( Person_.lastName );
		assertTrue( Person_.firstName.isId() );
		assertTrue( Person_.lastName.isId() );
		assertTrue( Person_.lastName.isId() );

		//Garden List as bag
		assertNotNull( Garden_.flowers );
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Fridge.class,
				FoodItem.class,
				Person.class,
				House.class,
				Dog.class,
				Cat.class,
				Cattish.class,
				Feline.class,
				Garden.class,
				Flower.class
		};
	}
}