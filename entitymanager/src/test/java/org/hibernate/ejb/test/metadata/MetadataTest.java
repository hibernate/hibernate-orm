package org.hibernate.ejb.test.metadata;

import java.util.Set;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.ListAttribute;

import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class MetadataTest extends TestCase {

	public void testBaseOfService() throws Exception {
		EntityManagerFactory emf = factory;
		assertNotNull( emf.getMetamodel() );
		final EntityType<Fridge> entityType = emf.getMetamodel().entity( Fridge.class );
		assertNotNull( entityType );
	}

	public void testEntity() throws Exception {
		final EntityType<Fridge> fridgeType = factory.getMetamodel().entity( Fridge.class );
		assertEquals( Fridge.class, fridgeType.getBindableJavaType() );
		assertEquals( Bindable.BindableType.ENTITY_TYPE, fridgeType.getBindableType() );
		assertNotNull( fridgeType.getDeclaredSingularAttribute( "temperature", Integer.class ) );
		assertNotNull( fridgeType.getDeclaredSingularAttribute( "temperature" ) );
		assertNotNull( fridgeType.getDeclaredAttribute( "temperature" ) );
		final SingularAttribute<Fridge, Long> id = fridgeType.getDeclaredId( Long.class );
		assertNotNull( id );
		assertTrue( id.isId() );
		assertEquals( Fridge.class.getName(), fridgeType.getName() );
		assertEquals( Long.class, fridgeType.getIdType().getJavaType() );
		assertTrue( fridgeType.hasSingleIdAttribute() );
		assertFalse( fridgeType.hasVersionAttribute() );
		assertEquals( Type.PersistenceType.ENTITY, fridgeType.getPersistenceType() );

		final EntityType<House> houseType = factory.getMetamodel().entity( House.class );
		assertTrue( houseType.hasSingleIdAttribute() );
		final SingularAttribute<House, House.Key> houseId = houseType.getDeclaredId( House.Key.class );
		assertNotNull( houseId );
		assertTrue( houseId.isId() );
		assertEquals( Attribute.PersistentAttributeType.EMBEDDED, houseId.getPersistentAttributeType() );
		
		final EntityType<Person> personType = factory.getMetamodel().entity( Person.class );
		assertFalse( personType.hasSingleIdAttribute() );
		final Set<SingularAttribute<? super Person,?>> ids = personType.getIdClassAttributes();
		assertNotNull( ids );
		assertEquals( 2, ids.size() );
		for (SingularAttribute<? super Person,?> localId : ids) {
			assertTrue( localId.isId() );
		}

		final EntityType<FoodItem> foodType = factory.getMetamodel().entity( FoodItem.class );
		assertTrue( foodType.hasVersionAttribute() );
		final SingularAttribute<? super FoodItem, Long> version = foodType.getVersion( Long.class );
		assertNotNull( version );
		assertTrue( version.isVersion() );

	}

	public void testBasic() throws Exception {
		final EntityType<Fridge> entityType = factory.getMetamodel().entity( Fridge.class );
		final SingularAttribute<? super Fridge,Integer> singularAttribute = entityType.getDeclaredSingularAttribute(
				"temperature",
				Integer.class
		);
		assertEquals( Integer.class, singularAttribute.getBindableJavaType() );
		assertEquals( Bindable.BindableType.SINGULAR_ATTRIBUTE, singularAttribute.getBindableType() );
		assertFalse( singularAttribute.isId() );
		assertFalse( singularAttribute.isOptional() );
		assertFalse( entityType.getDeclaredSingularAttribute( "brand", String.class ).isOptional() );
		assertEquals( Integer.class, singularAttribute.getType().getJavaType() );
		assertEquals( Type.PersistenceType.BASIC, singularAttribute.getType().getPersistenceType() );
		final Attribute<? super Fridge, ?> attribute = entityType.getDeclaredAttribute( "temperature" );
		assertNotNull( attribute );
		assertEquals( "temperature", attribute.getName() );
		assertEquals( Fridge.class, attribute.getDeclaringType().getJavaType() );
		assertEquals( Attribute.PersistentAttributeType.BASIC, attribute.getPersistentAttributeType() );
		assertEquals( Integer.class, attribute.getJavaType() );
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

	public void testEmbeddable() throws Exception {
		final EntityType<House> entityType = factory.getMetamodel().entity( House.class );
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

		final EmbeddableType<Address> directType = factory.getMetamodel().embeddable( Address.class );
		assertNotNull( directType );
		assertEquals( Type.PersistenceType.EMBEDDABLE, directType.getPersistenceType() );
	}

	public void testElementCollection() throws Exception {
		final EntityType<House> entityType = factory.getMetamodel().entity( House.class );
		final SetAttribute<House,Room> rooms = entityType.getDeclaredSet( "rooms", Room.class );
		assertNotNull( rooms );
		assertTrue( rooms.isAssociation() );
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

		//todo test plural

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Fridge.class,
				FoodItem.class,
				Person.class,
				House.class
		};
	}

}
