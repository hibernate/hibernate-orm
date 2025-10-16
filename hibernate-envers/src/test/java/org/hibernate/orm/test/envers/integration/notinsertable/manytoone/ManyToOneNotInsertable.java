/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.notinsertable.manytoone;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnversTest
@Jpa(annotatedClasses = {ManyToOneNotInsertableEntity.class, NotInsertableEntityType.class})
public class ManyToOneNotInsertable {
	private Integer mto_id1;
	private Integer type_id1;
	private Integer type_id2;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		mto_id1 = 1;
		type_id1 = 2;
		type_id2 = 3;

		scope.inEntityManager( em -> {
			// Rev 1
			// Preparing the types
			em.getTransaction().begin();

			NotInsertableEntityType type1 = new NotInsertableEntityType( type_id1, "type1" );
			NotInsertableEntityType type2 = new NotInsertableEntityType( type_id2, "type2" );

			em.persist( type1 );
			em.persist( type2 );

			em.getTransaction().commit();

			// Rev 2
			em.getTransaction().begin();

			ManyToOneNotInsertableEntity entity = new ManyToOneNotInsertableEntity( mto_id1, type_id1, type1 );
			em.persist( entity );

			em.getTransaction().commit();

			// Rev 2
			em.getTransaction().begin();

			entity = em.find( ManyToOneNotInsertableEntity.class, mto_id1 );
			entity.setNumber( type_id2 );

			em.getTransaction().commit();
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( NotInsertableEntityType.class, type_id1 ) );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( NotInsertableEntityType.class, type_id2 ) );
			assertEquals( Arrays.asList( 2, 3 ),
					auditReader.getRevisions( ManyToOneNotInsertableEntity.class, mto_id1 ) );
		} );
	}

	@Test
	public void testNotInsertableEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			ManyToOneNotInsertableEntity ver1 = auditReader.find( ManyToOneNotInsertableEntity.class, mto_id1, 1 );
			ManyToOneNotInsertableEntity ver2 = auditReader.find( ManyToOneNotInsertableEntity.class, mto_id1, 2 );
			ManyToOneNotInsertableEntity ver3 = auditReader.find( ManyToOneNotInsertableEntity.class, mto_id1, 3 );

			NotInsertableEntityType type1 = em.find( NotInsertableEntityType.class, type_id1 );
			NotInsertableEntityType type2 = em.find( NotInsertableEntityType.class, type_id2 );

			assertEquals( null, ver1 );
			assertEquals( type1, ver2.getType() );
			assertEquals( type2, ver3.getType() );
		} );
	}
}
