package org.hibernate.test.metamodel;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

public class IdClassAssociationMetamodelTest extends BaseEntityManagerFunctionalTestCase {

	@Entity @IdClass(EntityType.EntityId.class) public static class EntityType {
		@Id private Long key2;
		@Id @ManyToOne
		private SomeAssociationType key1;

		public static class EntityId implements Serializable {
			private Long key1, key2;
		}
	}

	@Entity public static class SomeAssociationType {
		@Id
		private Long id;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityType.class, SomeAssociationType.class };
	}

	@Test
	public void idClassAttributeIsProperlyRepresentedInMetamodelTest() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			javax.persistence.metamodel.EntityType<EntityType> entity = entityManager.getMetamodel()
					.entity( EntityType.class );

			SingularAttribute<? super EntityType, ?> key1 = entity.getSingularAttribute( "key1" );
			assertEquals( SomeAssociationType.class, key1.getJavaType() );
			assertEquals( SomeAssociationType.class, key1.getType().getJavaType()  );
		} );
	}
}
