package org.hibernate.ejb.test.metadata;

import java.util.Set;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

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

		//TODO IdClass
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

		//TODO test embedded
		//todo test plural
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Fridge.class,
				FoodItem.class,
				Person.class
		};
	}

}
