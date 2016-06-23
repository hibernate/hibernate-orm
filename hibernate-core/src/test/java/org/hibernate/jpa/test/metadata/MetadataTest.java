/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metadata;

import java.util.Locale;
import java.util.Set;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.MappedSuperclassType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.metamodel.internal.JpaMetaModelPopulationSetting;
import org.hibernate.metamodel.internal.MetamodelImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class MetadataTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testBaseOfService() throws Exception {
		EntityManagerFactory emf = entityManagerFactory();
		assertNotNull( emf.getMetamodel() );
		final EntityType<Fridge> entityType = emf.getMetamodel().entity( Fridge.class );
		assertNotNull( entityType );
	}

	@Test
	public void testInvalidAttributeCausesIllegalArgumentException() {
		// should not matter the exact subclass of ManagedType since this is implemented on the base class but
		// check each anyway..

		// entity
		checkNonExistentAttributeAccess( entityManagerFactory().getMetamodel().entity( Fridge.class ) );

		// embeddable
		checkNonExistentAttributeAccess( entityManagerFactory().getMetamodel().embeddable( Address.class ) );
	}

	private void checkNonExistentAttributeAccess(ManagedType managedType) {
		final String NAME = "NO_SUCH_ATTRIBUTE";
		try {
			managedType.getAttribute( NAME );
			fail( "Lookup of non-existent attribute (getAttribute) should have caused IAE : " + managedType );
		}
		catch (IllegalArgumentException expected) {
		}
		try {
			managedType.getSingularAttribute( NAME );
			fail( "Lookup of non-existent attribute (getSingularAttribute) should have caused IAE : " + managedType );
		}
		catch (IllegalArgumentException expected) {
		}
		try {
			managedType.getCollection( NAME );
			fail( "Lookup of non-existent attribute (getCollection) should have caused IAE : " + managedType );
		}
		catch (IllegalArgumentException expected) {
		}
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void testBuildingMetamodelWithParameterizedCollection() {
		Metadata metadata = new MetadataSources()
				.addAnnotatedClass( WithGenericCollection.class )
				.buildMetadata();
		SessionFactoryImplementor sfi = (SessionFactoryImplementor) metadata.buildSessionFactory();
		MetamodelImpl metamodel = new MetamodelImpl( sfi );
		metamodel.initialize( (MetadataImplementor) metadata, JpaMetaModelPopulationSetting.IGNORE_UNSUPPORTED );
		sfi.close();
	}

	@Test
	public void testLogicalManyToOne() throws Exception {
		final EntityType<JoinedManyToOneOwner> entityType = entityManagerFactory().getMetamodel().entity( JoinedManyToOneOwner.class );
		final SingularAttribute attr = entityType.getDeclaredSingularAttribute( "house" );
		assertEquals( Attribute.PersistentAttributeType.MANY_TO_ONE, attr.getPersistentAttributeType() );
		assertEquals( House.class, attr.getBindableJavaType() );
		final EntityType<House> houseType = entityManagerFactory().getMetamodel().entity( House.class );
		assertEquals( houseType.getBindableJavaType(), attr.getBindableJavaType() );
	}

	@Test
	public void testEntity() throws Exception {
		final EntityType<Fridge> fridgeType = entityManagerFactory().getMetamodel().entity( Fridge.class );
		assertEquals( Fridge.class, fridgeType.getBindableJavaType() );
		assertEquals( Bindable.BindableType.ENTITY_TYPE, fridgeType.getBindableType() );
		SingularAttribute<Fridge,Integer> wrapped = fridgeType.getDeclaredSingularAttribute( "temperature", Integer.class );
		assertNotNull( wrapped );
		SingularAttribute<Fridge,Integer> primitive = fridgeType.getDeclaredSingularAttribute( "temperature", int.class );
		assertNotNull( primitive );
		assertNotNull( fridgeType.getDeclaredSingularAttribute( "temperature" ) );
		assertNotNull( fridgeType.getDeclaredAttribute( "temperature" ) );
		final SingularAttribute<Fridge, Long> id = fridgeType.getDeclaredId( Long.class );
		assertNotNull( id );
		assertTrue( id.isId() );
		try {
			fridgeType.getDeclaredId( java.util.Date.class );
			fail( "expecting failure" );
		}
		catch ( IllegalArgumentException ignore ) {
			// expected result
		}
		final SingularAttribute<? super Fridge, Long> id2 = fridgeType.getId( Long.class );
		assertNotNull( id2 );

		assertEquals( "Fridge", fridgeType.getName() );
		assertEquals( Long.class, fridgeType.getIdType().getJavaType() );
		assertTrue( fridgeType.hasSingleIdAttribute() );
		assertFalse( fridgeType.hasVersionAttribute() );
		assertEquals( Type.PersistenceType.ENTITY, fridgeType.getPersistenceType() );

		assertEquals( 3, fridgeType.getDeclaredAttributes().size() );

		final EntityType<House> houseType = entityManagerFactory().getMetamodel().entity( House.class );
		assertEquals( "House", houseType.getName() );
		assertTrue( houseType.hasSingleIdAttribute() );
		final SingularAttribute<House, House.Key> houseId = houseType.getDeclaredId( House.Key.class );
		assertNotNull( houseId );
		assertTrue( houseId.isId() );
		assertEquals( Attribute.PersistentAttributeType.EMBEDDED, houseId.getPersistentAttributeType() );
		
		final EntityType<Person> personType = entityManagerFactory().getMetamodel().entity( Person.class );
		assertEquals( "Homo", personType.getName() );
		assertFalse( personType.hasSingleIdAttribute() );
		final Set<SingularAttribute<? super Person,?>> ids = personType.getIdClassAttributes();
		assertNotNull( ids );
		assertEquals( 2, ids.size() );
		for (SingularAttribute<? super Person,?> localId : ids) {
			assertTrue( localId.isId() );
			assertSame( personType, localId.getDeclaringType() );
			assertSame( localId, personType.getDeclaredAttribute( localId.getName() ) );
			assertSame( localId, personType.getDeclaredSingularAttribute( localId.getName() ) );
			assertSame( localId, personType.getAttribute( localId.getName() ) );
			assertSame( localId, personType.getSingularAttribute( localId.getName() ) );
			assertTrue( personType.getAttributes().contains( localId ) );
		}

		final EntityType<Giant> giantType = entityManagerFactory().getMetamodel().entity( Giant.class );
		assertEquals( "HomoGigantus", giantType.getName() );
		assertFalse( giantType.hasSingleIdAttribute() );
		final Set<SingularAttribute<? super Giant,?>> giantIds = giantType.getIdClassAttributes();
		assertNotNull( giantIds );
		assertEquals( 2, giantIds.size() );
		assertEquals( personType.getIdClassAttributes(), giantIds );
		for (SingularAttribute<? super Giant,?> localGiantId : giantIds) {
			assertTrue( localGiantId.isId() );
			try {
				giantType.getDeclaredAttribute( localGiantId.getName() );
				fail( localGiantId.getName() + " is a declared attribute, but shouldn't be");
			}
			catch ( IllegalArgumentException ex) {
				// expected
			}
			try {
				giantType.getDeclaredSingularAttribute( localGiantId.getName() );
				fail( localGiantId.getName() + " is a declared singular attribute, but shouldn't be");
			}
			catch ( IllegalArgumentException ex) {
				// expected
			}
			assertSame( localGiantId, giantType.getAttribute( localGiantId.getName() ) );
			assertTrue( giantType.getAttributes().contains( localGiantId ) );
		}

		final EntityType<FoodItem> foodType = entityManagerFactory().getMetamodel().entity( FoodItem.class );
		assertTrue( foodType.hasVersionAttribute() );
		final SingularAttribute<? super FoodItem, Long> version = foodType.getVersion( Long.class );
		assertNotNull( version );
		assertTrue( version.isVersion() );
		assertEquals( 3, foodType.getDeclaredAttributes().size() );

	}

	@Test
	public void testBasic() throws Exception {
		final EntityType<Fridge> entityType = entityManagerFactory().getMetamodel().entity( Fridge.class );
		final SingularAttribute<? super Fridge,Integer> singularAttribute = entityType.getDeclaredSingularAttribute(
				"temperature",
				Integer.class
		);
//		assertEquals( Integer.class, singularAttribute.getBindableJavaType() );
//		assertEquals( Integer.class, singularAttribute.getType().getJavaType() );
		assertEquals( int.class, singularAttribute.getBindableJavaType() );
		assertEquals( int.class, singularAttribute.getType().getJavaType() );
		assertEquals( Bindable.BindableType.SINGULAR_ATTRIBUTE, singularAttribute.getBindableType() );
		assertFalse( singularAttribute.isId() );
		assertFalse( singularAttribute.isOptional() );
		assertFalse( entityType.getDeclaredSingularAttribute( "brand", String.class ).isOptional() );
		assertEquals( Type.PersistenceType.BASIC, singularAttribute.getType().getPersistenceType() );
		final Attribute<? super Fridge, ?> attribute = entityType.getDeclaredAttribute( "temperature" );
		assertNotNull( attribute );
		assertEquals( "temperature", attribute.getName() );
		assertEquals( Fridge.class, attribute.getDeclaringType().getJavaType() );
		assertEquals( Attribute.PersistentAttributeType.BASIC, attribute.getPersistentAttributeType() );
//		assertEquals( Integer.class, attribute.getJavaType() );
		assertEquals( int.class, attribute.getJavaType() );
		assertFalse( attribute.isAssociation() );
		assertFalse( attribute.isCollection() );

		boolean found = false;
		for (Attribute<Fridge, ?> attr : entityType.getDeclaredAttributes() ) {
			if ("temperature".equals( attr.getName() ) ) {
				found = true;
				break;
			}
		}
		assertTrue( found );
	}

	@Test
	public void testEmbeddable() throws Exception {
		final EntityType<House> entityType = entityManagerFactory().getMetamodel().entity( House.class );
		final SingularAttribute<? super House,Address> address = entityType.getDeclaredSingularAttribute(
				"address",
				Address.class
		);
		assertNotNull( address );
		assertEquals( Attribute.PersistentAttributeType.EMBEDDED, address.getPersistentAttributeType() );
		assertFalse( address.isCollection() );
		assertFalse( address.isAssociation() );
		final EmbeddableType<Address> addressType = (EmbeddableType<Address>) address.getType();
		assertEquals( Type.PersistenceType.EMBEDDABLE, addressType.getPersistenceType() );
		assertEquals( 3, addressType.getDeclaredAttributes().size() );
		assertTrue( addressType.getDeclaredSingularAttribute( "address1" ).isOptional() );
		assertFalse( addressType.getDeclaredSingularAttribute( "address2" ).isOptional() );

		final EmbeddableType<Address> directType = entityManagerFactory().getMetamodel().embeddable( Address.class );
		assertNotNull( directType );
		assertEquals( Type.PersistenceType.EMBEDDABLE, directType.getPersistenceType() );
	}

	@Test
	public void testCollection() throws Exception {
		final EntityType<Garden> entiytype = entityManagerFactory().getMetamodel().entity( Garden.class );
		final Set<PluralAttribute<? super Garden, ?, ?>> attributes = entiytype.getPluralAttributes();
		assertEquals( 1, attributes.size() );
		PluralAttribute<? super Garden, ?, ?> flowers = attributes.iterator().next();
		assertTrue( flowers instanceof ListAttribute );
	}

	@Test
	public void testElementCollection() throws Exception {
		final EntityType<House> entityType = entityManagerFactory().getMetamodel().entity( House.class );
		final SetAttribute<House,Room> rooms = entityType.getDeclaredSet( "rooms", Room.class );
		assertNotNull( rooms );
		assertFalse( rooms.isAssociation() );
		assertTrue( rooms.isCollection() );
		assertEquals( Attribute.PersistentAttributeType.ELEMENT_COLLECTION, rooms.getPersistentAttributeType() );
		assertEquals( Room.class, rooms.getBindableJavaType() );
		assertEquals( Bindable.BindableType.PLURAL_ATTRIBUTE, rooms.getBindableType() );
		assertEquals( Set.class, rooms.getJavaType() );
		assertEquals( PluralAttribute.CollectionType.SET, rooms.getCollectionType() );
		assertEquals( 3, entityType.getDeclaredPluralAttributes().size() );
		assertEquals( Type.PersistenceType.EMBEDDABLE, rooms.getElementType().getPersistenceType() );

		final MapAttribute<House,String,Room> roomsByName = entityType.getDeclaredMap(
				"roomsByName", String.class, Room.class
		);
		assertNotNull( roomsByName );
		assertEquals( String.class, roomsByName.getKeyJavaType() );
		assertEquals( Type.PersistenceType.BASIC, roomsByName.getKeyType().getPersistenceType() );
		assertEquals( PluralAttribute.CollectionType.MAP, roomsByName.getCollectionType() );

		final ListAttribute<House,Room> roomsBySize = entityType.getDeclaredList( "roomsBySize", Room.class );
		assertNotNull( roomsBySize );
		assertEquals( Type.PersistenceType.EMBEDDABLE, roomsBySize.getElementType().getPersistenceType() );
		assertEquals( PluralAttribute.CollectionType.LIST, roomsBySize.getCollectionType() );
	}

	@Test
	public void testHierarchy() {
		final EntityType<Cat> cat = entityManagerFactory().getMetamodel().entity( Cat.class );
		assertNotNull( cat );
		assertEquals( 7, cat.getAttributes().size() );
		assertEquals( 1, cat.getDeclaredAttributes().size() );
		ensureProperMember(cat.getDeclaredAttributes());

		assertTrue( cat.hasVersionAttribute() );
		assertEquals( "version", cat.getVersion(Long.class).getName() );
		verifyDeclaredVersionNotPresent( cat );
		verifyDeclaredIdNotPresentAndIdPresent(cat);

		assertEquals( Type.PersistenceType.MAPPED_SUPERCLASS, cat.getSupertype().getPersistenceType() );
		MappedSuperclassType<Cattish> cattish = (MappedSuperclassType<Cattish>) cat.getSupertype();
		assertEquals( 6, cattish.getAttributes().size() );
		assertEquals( 1, cattish.getDeclaredAttributes().size() );
		ensureProperMember(cattish.getDeclaredAttributes());

		assertTrue( cattish.hasVersionAttribute() );
		assertEquals( "version", cattish.getVersion(Long.class).getName() );
		verifyDeclaredVersionNotPresent( cattish );
		verifyDeclaredIdNotPresentAndIdPresent(cattish);

		assertEquals( Type.PersistenceType.ENTITY, cattish.getSupertype().getPersistenceType() );
		EntityType<Feline> feline = (EntityType<Feline>) cattish.getSupertype();
		assertEquals( 5, feline.getAttributes().size() );
		assertEquals( 1, feline.getDeclaredAttributes().size() );
		ensureProperMember(feline.getDeclaredAttributes());

		assertTrue( feline.hasVersionAttribute() );
		assertEquals( "version", feline.getVersion(Long.class).getName() );
		verifyDeclaredVersionNotPresent( feline );
		verifyDeclaredIdNotPresentAndIdPresent(feline);

		assertEquals( Type.PersistenceType.MAPPED_SUPERCLASS, feline.getSupertype().getPersistenceType() );
		MappedSuperclassType<Animal> animal = (MappedSuperclassType<Animal>) feline.getSupertype();
		assertEquals( 4, animal.getAttributes().size() );
		assertEquals( 2, animal.getDeclaredAttributes().size() );
		ensureProperMember(animal.getDeclaredAttributes());

		assertTrue( animal.hasVersionAttribute() );
		assertEquals( "version", animal.getVersion(Long.class).getName() );
		verifyDeclaredVersionNotPresent( animal );
		assertEquals( "id", animal.getId(Long.class).getName() );
		final SingularAttribute<Animal, Long> id = animal.getDeclaredId( Long.class );
		assertEquals( "id", id.getName() );
		assertNotNull( id.getJavaMember() );

		assertEquals( Type.PersistenceType.MAPPED_SUPERCLASS, animal.getSupertype().getPersistenceType() );
		MappedSuperclassType<Thing> thing = (MappedSuperclassType<Thing>) animal.getSupertype();
		assertEquals( 2, thing.getAttributes().size() );
		assertEquals( 2, thing.getDeclaredAttributes().size() );
		ensureProperMember(thing.getDeclaredAttributes());
		final SingularAttribute<Thing, Double> weight = thing.getDeclaredSingularAttribute( "weight", Double.class );
		assertEquals( Double.class, weight.getJavaType() );

		assertEquals( "version", thing.getVersion(Long.class).getName() );
		final SingularAttribute<Thing, Long> version = thing.getDeclaredVersion( Long.class );
		assertEquals( "version", version.getName() );
		assertNotNull( version.getJavaMember() );
		assertNull( thing.getId( Long.class ) );

		assertNull( thing.getSupertype() );
	}

	@Test
	public void testBackrefAndGenerics() throws Exception {
		final EntityType<Parent> parent = entityManagerFactory().getMetamodel().entity( Parent.class );
		assertNotNull( parent );
		final SetAttribute<? super Parent, ?> children = parent.getSet( "children" );
		assertNotNull( children );
		assertEquals( 1, parent.getPluralAttributes().size() );
		assertEquals( 4, parent.getAttributes().size() );
		final EntityType<Child> child = entityManagerFactory().getMetamodel().entity( Child.class );
		assertNotNull( child );
		assertEquals( 2, child.getAttributes().size() );
		final SingularAttribute<? super Parent, Parent.Relatives> attribute = parent.getSingularAttribute(
				"siblings", Parent.Relatives.class
		);
		final EmbeddableType<Parent.Relatives> siblings = (EmbeddableType<Parent.Relatives>) attribute.getType();
		assertNotNull(siblings);
		final SetAttribute<? super Parent.Relatives, ?> siblingsCollection = siblings.getSet( "siblings" );
		assertNotNull( siblingsCollection );
		final Type<?> collectionElement = siblingsCollection.getElementType();
		assertNotNull( collectionElement );
		assertEquals( collectionElement, child );
	}

	private void ensureProperMember(Set<?> attributes) {
		//we do not update the set so we are safe
		@SuppressWarnings( "unchecked" )
		final Set<Attribute<?, ?>> safeAttributes = ( Set<Attribute<?, ?>> ) attributes;
		for (Attribute<?,?> attribute : safeAttributes ) {
			final String name = attribute.getJavaMember().getName();
			assertNotNull( attribute.getJavaMember() );
			assertTrue( name.toLowerCase(Locale.ROOT).endsWith( attribute.getName().toLowerCase(Locale.ROOT) ) );
		}
	}

	private void verifyDeclaredIdNotPresentAndIdPresent(IdentifiableType<?> type) {
		assertEquals( "id", type.getId(Long.class).getName() );
		try {
			type.getDeclaredId(Long.class);
			fail("Should not have a declared id");
		}
		catch (IllegalArgumentException e) {
			//success
		}
	}

	private void verifyDeclaredVersionNotPresent(IdentifiableType<?> type) {
		try {
			type.getDeclaredVersion(Long.class);
			fail("Should not have a declared version");
		}
		catch (IllegalArgumentException e) {
			//success
		}
	}

	//todo test plural

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
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
		};
	}

}
