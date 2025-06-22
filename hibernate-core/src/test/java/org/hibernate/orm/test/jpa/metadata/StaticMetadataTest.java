/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metadata;

import java.util.Set;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.Type;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Steve Ebersole
 */
@Jpa(
		annotatedClasses = {
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
		}
)
public class StaticMetadataTest {

	@Test
	public void testInjections(EntityManagerFactoryScope scope) {
		// Make sure the entity manager factory is properly initialized
		scope.getEntityManagerFactory();

		// Address (embeddable)
		assertNotNull( Address_.address1 );
		assertNotNull( Address_.address2 );
		assertNotNull( Address_.city );
		final EmbeddableType addressType = (EmbeddableType) House_.address.getType();
		assertEquals( addressType.getDeclaredSingularAttribute( "address1" ), Address_.address1 );
		assertEquals( addressType.getDeclaredSingularAttribute( "address2" ), Address_.address2 );
		assertTrue( Address_.address1.isOptional() );
		assertFalse( Address_.address2.isOptional() );

		// Animal (mapped superclass)
		assertNotNull( Animal_.id );
		assertTrue( Animal_.id.isId() );
		assertEquals( Long.class, Animal_.id.getJavaType() );
		assertNotNull( Animal_.legNbr );
		assertEquals( int.class, Animal_.legNbr.getJavaType() );

		// Cat (hierarchy)
		assertNotNull( Cat_.id );
		assertTrue( Cat_.id.isId() );
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
		assertEquals( int.class, Fridge_.temperature.getJavaType() );
		assertEquals( int.class, Fridge_.temperature.getJavaType() );
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
		assertFalse( House_.rooms.isAssociation() );
		assertTrue( House_.rooms.isCollection() );
		assertEquals( Attribute.PersistentAttributeType.ELEMENT_COLLECTION, House_.rooms.getPersistentAttributeType() );
		assertEquals( Room.class, House_.rooms.getBindableJavaType() );
		assertEquals( Set.class, House_.rooms.getJavaType() );
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
}
