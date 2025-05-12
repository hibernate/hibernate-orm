/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metadata;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.MappedSuperclassType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */
@Jpa(
		annotatedClasses = {
				Fridge.class,
				FoodItem.class,
				Person.class,
				Giant.class,
				House.class,
				Dog.class,
				Cat.class,
				Cattish.class,
				Feline.class,
				Garden.class,
				Flower.class,
				JoinedManyToOneOwner.class,
				Parent.class,
				Child.class
		}
)
public class MetadataTest {

	@Test
	public void testBaseOfService(EntityManagerFactoryScope scope) {
		EntityManagerFactory emf = scope.getEntityManagerFactory();
		assertNotNull(emf.getMetamodel());
		final EntityType<Fridge> entityType = emf.getMetamodel().entity(Fridge.class);
		assertNotNull(entityType);

	}

	@Test
	public void testInvalidAttributeCausesIllegalArgumentException(EntityManagerFactoryScope scope) {
		// should not matter the exact subclass of ManagedType since this is implemented on the base class but
		// check each anyway..

		// entity
		checkNonExistentAttributeAccess(scope.getEntityManagerFactory().getMetamodel().entity(Fridge.class));

		// embeddable
		checkNonExistentAttributeAccess(scope.getEntityManagerFactory().getMetamodel().embeddable(Address.class));
	}

	private void checkNonExistentAttributeAccess(ManagedType managedType) {
		final String NAME = "NO_SUCH_ATTRIBUTE";
		try {
			managedType.getAttribute(NAME);
			fail("Lookup of non-existent attribute (getAttribute) should have caused IAE : " + managedType);
		} catch (IllegalArgumentException expected) {
		}
		try {
			managedType.getSingularAttribute(NAME);
			fail("Lookup of non-existent attribute (getSingularAttribute) should have caused IAE : " + managedType);
		} catch (IllegalArgumentException expected) {
		}
		try {
			managedType.getCollection(NAME);
			fail("Lookup of non-existent attribute (getCollection) should have caused IAE : " + managedType);
		} catch (IllegalArgumentException expected) {
		}
	}

	@Test
	public void testBuildingMetamodelWithParameterizedCollection() {
		Metadata metadata = new MetadataSources( ServiceRegistryUtil.serviceRegistry() )
				.addAnnotatedClass(WithGenericCollection.class)
				.buildMetadata();
		SessionFactoryImplementor sfi = (SessionFactoryImplementor) metadata.buildSessionFactory();
		sfi.close();
	}

	@Test
	public void testLogicalManyToOne(EntityManagerFactoryScope scope) {
		final EntityType<JoinedManyToOneOwner> entityType = scope.getEntityManagerFactory().getMetamodel().entity(
				JoinedManyToOneOwner.class);
		final SingularAttribute attr = entityType.getDeclaredSingularAttribute("house");
		assertEquals(Attribute.PersistentAttributeType.MANY_TO_ONE, attr.getPersistentAttributeType());
		assertEquals(House.class, attr.getJavaType());
		assertEquals(House.class, attr.getBindableJavaType());
		final EntityType<House> houseType = scope.getEntityManagerFactory().getMetamodel().entity(House.class);
		assertEquals(houseType.getJavaType(), attr.getJavaType());
		assertFalse(entityType.getDeclaredSingularAttribute("house2").isOptional());
	}

	@Test
	public void testEntity(EntityManagerFactoryScope scope) {
		final EntityType<Fridge> fridgeType = scope.getEntityManagerFactory().getMetamodel().entity(Fridge.class);
		assertEquals(Fridge.class, fridgeType.getJavaType());
		assertEquals(Bindable.BindableType.ENTITY_TYPE, fridgeType.getBindableType());
		SingularAttribute<Fridge, Integer> wrapped = fridgeType.getDeclaredSingularAttribute(
				"temperature",
				Integer.class
		);
		assertNotNull(wrapped);
		SingularAttribute<Fridge, Integer> primitive = fridgeType.getDeclaredSingularAttribute(
				"temperature",
				int.class
		);
		assertNotNull(primitive);
		assertNotNull(fridgeType.getDeclaredSingularAttribute("temperature"));
		assertNotNull(fridgeType.getDeclaredAttribute("temperature"));
		final SingularAttribute<Fridge, Long> id = fridgeType.getDeclaredId(Long.class);
		assertNotNull(id);
		assertTrue(id.isId());
		try {
			fridgeType.getDeclaredId(java.util.Date.class);
			fail("expecting failure");
		} catch (IllegalArgumentException ignore) {
			// expected result
		}
		final SingularAttribute<? super Fridge, Long> id2 = fridgeType.getId(Long.class);
		assertNotNull(id2);

		assertEquals("Fridge", fridgeType.getName());
		assertEquals(Long.class, fridgeType.getIdType().getJavaType());
		assertTrue(fridgeType.hasSingleIdAttribute());
		assertFalse(fridgeType.hasVersionAttribute());
		assertEquals(Type.PersistenceType.ENTITY, fridgeType.getPersistenceType());

		assertEquals(4, fridgeType.getDeclaredAttributes().size());

		final EntityType<House> houseType = scope.getEntityManagerFactory().getMetamodel().entity(House.class);
		assertEquals("House", houseType.getName());
		assertTrue(houseType.hasSingleIdAttribute());
		final SingularAttribute<House, House.Key> houseId = houseType.getDeclaredId(House.Key.class);
		assertNotNull(houseId);
		assertTrue(houseId.isId());
		assertEquals(Attribute.PersistentAttributeType.EMBEDDED, houseId.getPersistentAttributeType());

		final EntityType<Person> personType = scope.getEntityManagerFactory().getMetamodel().entity(Person.class);
		assertEquals("Homo", personType.getName());
		assertFalse(personType.hasSingleIdAttribute());
		final Set<SingularAttribute<? super Person, ?>> ids = personType.getIdClassAttributes();
		assertNotNull(ids);
		assertEquals(2, ids.size());
		for (SingularAttribute<? super Person, ?> localId : ids) {
			assertTrue(localId.isId());
			assertSame(personType, localId.getDeclaringType());
			assertSame(localId, personType.getDeclaredAttribute(localId.getName()));
			assertSame(localId, personType.getDeclaredSingularAttribute(localId.getName()));
			assertSame(localId, personType.getAttribute(localId.getName()));
			assertSame(localId, personType.getSingularAttribute(localId.getName()));
			assertTrue(personType.getAttributes().contains(localId));
		}

		final EntityType<Giant> giantType = scope.getEntityManagerFactory().getMetamodel().entity(Giant.class);
		assertEquals("HomoGigantus", giantType.getName());
		assertFalse(giantType.hasSingleIdAttribute());
		final Set<SingularAttribute<? super Giant, ?>> giantIds = giantType.getIdClassAttributes();
		assertNotNull(giantIds);
		assertEquals(2, giantIds.size());
		assertEquals(personType.getIdClassAttributes(), giantIds);
		for (SingularAttribute<? super Giant, ?> localGiantId : giantIds) {
			assertTrue(localGiantId.isId());
			try {
				giantType.getDeclaredAttribute(localGiantId.getName());
				fail(localGiantId.getName() + " is a declared attribute, but shouldn't be");
			} catch (IllegalArgumentException ex) {
				// expected
			}
			try {
				giantType.getDeclaredSingularAttribute(localGiantId.getName());
				fail(localGiantId.getName() + " is a declared singular attribute, but shouldn't be");
			} catch (IllegalArgumentException ex) {
				// expected
			}
			assertSame(localGiantId, giantType.getAttribute(localGiantId.getName()));
			assertTrue(giantType.getAttributes().contains(localGiantId));
		}

		final EntityType<FoodItem> foodType = scope.getEntityManagerFactory().getMetamodel().entity(FoodItem.class);
		assertTrue(foodType.hasVersionAttribute());
		final SingularAttribute<? super FoodItem, Long> version = foodType.getVersion(Long.class);
		assertNotNull(version);
		assertTrue(version.isVersion());
		assertEquals(3, foodType.getDeclaredAttributes().size());

	}

	@Test
	public void testBasic(EntityManagerFactoryScope scope) {
		final EntityType<Fridge> entityType = scope.getEntityManagerFactory().getMetamodel().entity(Fridge.class);
		final SingularAttribute<? super Fridge, Integer> singularAttribute = entityType.getDeclaredSingularAttribute(
				"temperature",
				Integer.class
		);
		assertEquals( int.class, singularAttribute.getJavaType() );
		assertEquals( int.class, singularAttribute.getType().getJavaType() );
		assertEquals(Bindable.BindableType.SINGULAR_ATTRIBUTE, singularAttribute.getBindableType());
		assertFalse(singularAttribute.isId());
		assertFalse(singularAttribute.isOptional());
		assertFalse(entityType.getDeclaredSingularAttribute("brand", String.class).isOptional());
		assertFalse(entityType.getDeclaredSingularAttribute("height", Integer.class).isOptional());
		assertEquals(Type.PersistenceType.BASIC, singularAttribute.getType().getPersistenceType());
		final Attribute<? super Fridge, ?> attribute = entityType.getDeclaredAttribute("temperature");
		assertNotNull(attribute);
		assertEquals("temperature", attribute.getName());
		assertEquals(Fridge.class, attribute.getDeclaringType().getJavaType());
		assertEquals(Attribute.PersistentAttributeType.BASIC, attribute.getPersistentAttributeType());
		assertEquals( int.class, attribute.getJavaType() );
		assertFalse(attribute.isAssociation());
		assertFalse(attribute.isCollection());

		boolean found = false;
		for (Attribute<Fridge, ?> attr : entityType.getDeclaredAttributes()) {
			if ("temperature".equals(attr.getName())) {
				found = true;
				break;
			}
		}
		assertTrue(found);
	}

	@Test
	public void testEmbeddable(EntityManagerFactoryScope scope) {
		final EntityType<House> entityType = scope.getEntityManagerFactory().getMetamodel().entity(House.class);
		final SingularAttribute<? super House, Address> address = entityType.getDeclaredSingularAttribute(
				"address",
				Address.class
		);
		assertNotNull(address);
		assertEquals(Attribute.PersistentAttributeType.EMBEDDED, address.getPersistentAttributeType());
		assertFalse(address.isCollection());
		assertFalse(address.isAssociation());
		final EmbeddableType<Address> addressType = (EmbeddableType<Address>) address.getType();
		assertEquals(Type.PersistenceType.EMBEDDABLE, addressType.getPersistenceType());
		assertEquals(3, addressType.getDeclaredAttributes().size());
		assertTrue(addressType.getDeclaredSingularAttribute("address1").isOptional());
		assertFalse(addressType.getDeclaredSingularAttribute("address2").isOptional());

		final EmbeddableType<Address> directType = scope.getEntityManagerFactory()
				.getMetamodel()
				.embeddable(Address.class);
		assertNotNull(directType);
		assertEquals(Type.PersistenceType.EMBEDDABLE, directType.getPersistenceType());
	}

	@Test
	public void testCollection(EntityManagerFactoryScope scope) {
		final EntityType<Garden> entiytype = scope.getEntityManagerFactory().getMetamodel().entity(Garden.class);
		final Set<PluralAttribute<? super Garden, ?, ?>> attributes = entiytype.getPluralAttributes();
		assertEquals(1, attributes.size());
		PluralAttribute<? super Garden, ?, ?> flowers = attributes.iterator().next();
		assertTrue(flowers instanceof ListAttribute);
	}

	@Test
	@JiraKey(value = "HHH-14346")
	public void testEmptyPluralAttributeSet(EntityManagerFactoryScope scope) {
		final EntityType<Feline> entityType = scope.getEntityManagerFactory().getMetamodel().entity(Feline.class);
		final Set<PluralAttribute<? super Feline, ?, ?>> attributes = entityType.getPluralAttributes();
		assertEquals(0, attributes.size());
	}

	@Test
	public void testElementCollection(EntityManagerFactoryScope scope) {
		final EntityType<House> entityType = scope.getEntityManagerFactory().getMetamodel().entity(House.class);
		final SetAttribute<House, Room> rooms = entityType.getDeclaredSet("rooms", Room.class);
		assertNotNull(rooms);
		assertFalse(rooms.isAssociation());
		assertTrue(rooms.isCollection());
		assertEquals(Attribute.PersistentAttributeType.ELEMENT_COLLECTION, rooms.getPersistentAttributeType());
		assertEquals(Room.class, rooms.getBindableJavaType());
		assertEquals(Bindable.BindableType.PLURAL_ATTRIBUTE, rooms.getBindableType());
		assertEquals(Set.class, rooms.getJavaType());
		assertEquals(PluralAttribute.CollectionType.SET, rooms.getCollectionType());
		assertEquals(3, entityType.getDeclaredPluralAttributes().size());
		assertEquals(Type.PersistenceType.EMBEDDABLE, rooms.getElementType().getPersistenceType());

		final MapAttribute<House, String, Room> roomsByName = entityType.getDeclaredMap(
				"roomsByName", String.class, Room.class
		);
		assertNotNull(roomsByName);
		assertEquals(String.class, roomsByName.getKeyJavaType());
		assertEquals(Type.PersistenceType.BASIC, roomsByName.getKeyType().getPersistenceType());
		assertEquals(PluralAttribute.CollectionType.MAP, roomsByName.getCollectionType());
		assertEquals(Map.class, roomsByName.getJavaType());
		assertEquals(Room.class, roomsByName.getBindableJavaType());

		final ListAttribute<House, Room> roomsBySize = entityType.getDeclaredList("roomsBySize", Room.class);
		assertNotNull(roomsBySize);
		assertEquals(Type.PersistenceType.EMBEDDABLE, roomsBySize.getElementType().getPersistenceType());
		assertEquals(PluralAttribute.CollectionType.LIST, roomsBySize.getCollectionType());
		assertEquals( Room.class, roomsBySize.getBindableJavaType() );
		assertEquals( List.class, roomsBySize.getJavaType() );
	}

	@Test
	public void testHierarchy(EntityManagerFactoryScope scope) {
		final EntityType<Cat> cat = scope.getEntityManagerFactory().getMetamodel().entity(Cat.class);
		assertNotNull(cat);
		assertEquals(7, cat.getAttributes().size());
		assertEquals(1, cat.getDeclaredAttributes().size());
		ensureProperMember(cat.getDeclaredAttributes());

		assertTrue(cat.hasVersionAttribute());
		assertEquals("version", cat.getVersion(Long.class).getName());
		verifyDeclaredVersionNotPresent(cat);
		verifyDeclaredIdNotPresentAndIdPresent(cat);

		assertEquals(Type.PersistenceType.MAPPED_SUPERCLASS, cat.getSupertype().getPersistenceType());
		MappedSuperclassType cattish = (MappedSuperclassType) cat.getSupertype();
		assertEquals(6, cattish.getAttributes().size());
		assertEquals(1, cattish.getDeclaredAttributes().size());
		ensureProperMember(cattish.getDeclaredAttributes());

		assertTrue(cattish.hasVersionAttribute());
		assertEquals("version", cattish.getVersion(Long.class).getName());
		verifyDeclaredVersionNotPresent(cattish);
		verifyDeclaredIdNotPresentAndIdPresent(cattish);

		assertEquals(Type.PersistenceType.ENTITY, cattish.getSupertype().getPersistenceType());
		EntityType<Feline> feline = (EntityType<Feline>) cattish.getSupertype();
		assertEquals(5, feline.getAttributes().size());
		assertEquals(1, feline.getDeclaredAttributes().size());
		ensureProperMember(feline.getDeclaredAttributes());

		assertTrue(feline.hasVersionAttribute());
		assertEquals("version", feline.getVersion(Long.class).getName());
		verifyDeclaredVersionNotPresent(feline);
		verifyDeclaredIdNotPresentAndIdPresent(feline);

		assertEquals(Type.PersistenceType.MAPPED_SUPERCLASS, feline.getSupertype().getPersistenceType());
		MappedSuperclassType animal = (MappedSuperclassType) feline.getSupertype();
		assertEquals(4, animal.getAttributes().size());
		assertEquals(2, animal.getDeclaredAttributes().size());
		ensureProperMember(animal.getDeclaredAttributes());

		assertTrue(animal.hasVersionAttribute());
		assertEquals("version", animal.getVersion(Long.class).getName());
		verifyDeclaredVersionNotPresent(animal);
		assertEquals("id", animal.getId(Long.class).getName());
		final SingularAttribute<Animal, Long> id = animal.getDeclaredId(Long.class);
		assertEquals("id", id.getName());
		assertNotNull(id.getJavaMember());

		assertEquals(Type.PersistenceType.MAPPED_SUPERCLASS, animal.getSupertype().getPersistenceType());
		MappedSuperclassType<Thing> thing = (MappedSuperclassType<Thing>) animal.getSupertype();
		assertEquals(2, thing.getAttributes().size());
		assertEquals(2, thing.getDeclaredAttributes().size());
		ensureProperMember(thing.getDeclaredAttributes());
		final SingularAttribute<Thing, Double> weight = thing.getDeclaredSingularAttribute("weight", Double.class);
		assertEquals(Double.class, weight.getJavaType());

		assertEquals("version", thing.getVersion(Long.class).getName());
		final SingularAttribute<Thing, Long> version = thing.getDeclaredVersion(Long.class);
		assertEquals("version", version.getName());
		assertNotNull(version.getJavaMember());
		assertNull(thing.getId(Long.class));

		assertNull(thing.getSupertype());
	}

	@Test
	public void testBackrefAndGenerics(EntityManagerFactoryScope scope) {
		final EntityType<Parent> parent = scope.getEntityManagerFactory().getMetamodel().entity(Parent.class);
		assertNotNull(parent);
		final SetAttribute<? super Parent, ?> children = parent.getSet("children");
		assertNotNull(children);
		assertEquals(1, parent.getPluralAttributes().size());
		assertEquals(4, parent.getAttributes().size());
		final EntityType<Child> child = scope.getEntityManagerFactory().getMetamodel().entity(Child.class);
		assertNotNull(child);
		assertEquals(2, child.getAttributes().size());
		final SingularAttribute<? super Parent, Parent.Relatives> attribute = parent.getSingularAttribute(
				"siblings", Parent.Relatives.class
		);
		final EmbeddableType<Parent.Relatives> siblings = (EmbeddableType<Parent.Relatives>) attribute.getType();
		assertNotNull(siblings);
		final SetAttribute<? super Parent.Relatives, ?> siblingsCollection = siblings.getSet("siblings");
		assertNotNull(siblingsCollection);
		final Type<?> collectionElement = siblingsCollection.getElementType();
		assertNotNull(collectionElement);
		assertEquals(collectionElement, child);
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-17465")
	public void testInheritedVersion(EntityManagerFactoryScope scope) {
		EntityManagerFactory emf = scope.getEntityManagerFactory();
		assertNotNull(emf.getMetamodel());
		final EntityType<Cat> entityType = emf.getMetamodel().entity(Cat.class);
		assertTrue(entityType.hasVersionAttribute());
		assertTrue(entityType.getSingularAttribute("version").isVersion());

	}

	private void ensureProperMember(Set<?> attributes) {
		//we do not update the set so we are safe
		@SuppressWarnings("unchecked") final Set<Attribute<?, ?>> safeAttributes = (Set<Attribute<?, ?>>) attributes;
		for (Attribute<?, ?> attribute : safeAttributes) {
			final String name = attribute.getJavaMember().getName();
			assertNotNull(attribute.getJavaMember());
			assertTrue(name.toLowerCase(Locale.ROOT).endsWith(attribute.getName().toLowerCase(Locale.ROOT)));
		}
	}

	private void verifyDeclaredIdNotPresentAndIdPresent(IdentifiableType<?> type) {
		assertEquals("id", type.getId(Long.class).getName());
		try {
			type.getDeclaredId(Long.class);
			fail("Should not have a declared id");
		} catch (IllegalArgumentException e) {
			//success
		}
	}

	private void verifyDeclaredVersionNotPresent(IdentifiableType<?> type) {
		try {
			type.getDeclaredVersion(Long.class);
			fail("Should not have a declared version");
		} catch (IllegalArgumentException e) {
			//success
		}
	}

}
