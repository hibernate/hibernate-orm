package org.hibernate.ejb.test.metadata;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.SingularAttribute;

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

	public void testBindable() throws Exception {
		EntityManagerFactory emf = factory;
		final EntityType<Fridge> entityType = emf.getMetamodel().entity( Fridge.class );
		assertEquals( Fridge.class, entityType.getBindableJavaType() );
		assertEquals( Bindable.BindableType.ENTITY_TYPE, entityType.getBindableType() );
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
				FoodItem.class
		};
	}

}
