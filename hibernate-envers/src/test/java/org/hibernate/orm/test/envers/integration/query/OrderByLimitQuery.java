/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.entities.IntTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {IntTestEntity.class})
public class OrderByLimitQuery {
	private Integer id1;
	private Integer id2;
	private Integer id3;
	private Integer id4;
	private Integer id5;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// Revision 1
			IntTestEntity ite1 = new IntTestEntity( 12 );
			IntTestEntity ite2 = new IntTestEntity( 5 );
			IntTestEntity ite3 = new IntTestEntity( 8 );
			IntTestEntity ite4 = new IntTestEntity( 1 );

			em.persist( ite1 );
			em.persist( ite2 );
			em.persist( ite3 );
			em.persist( ite4 );

			id1 = ite1.getId();
			id2 = ite2.getId();
			id3 = ite3.getId();
			id4 = ite4.getId();
		} );

		scope.inTransaction( em -> {
			// Revision 2
			IntTestEntity ite5 = new IntTestEntity( 3 );
			em.persist( ite5 );
			id5 = ite5.getId();

			final var ite1 = em.find( IntTestEntity.class, id1 );
			ite1.setNumber( 0 );

			final var ite4 = em.find( IntTestEntity.class, id4 );
			ite4.setNumber( 15 );
		} );
	}

	@Test
	public void testEntitiesOrderLimitByQueryRev1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final var res_0_to_1 = auditReader.createQuery()
					.forEntitiesAtRevision( IntTestEntity.class, 1 )
					.addOrder( AuditEntity.property( "number" ).desc() )
					.setFirstResult( 0 )
					.setMaxResults( 2 )
					.getResultList();

			final var res_2_to_3 = auditReader.createQuery()
					.forEntitiesAtRevision( IntTestEntity.class, 1 )
					.addOrder( AuditEntity.property( "number" ).desc() )
					.setFirstResult( 2 )
					.setMaxResults( 2 )
					.getResultList();

			final var res_empty = auditReader.createQuery()
					.forEntitiesAtRevision( IntTestEntity.class, 1 )
					.addOrder( AuditEntity.property( "number" ).desc() )
					.setFirstResult( 4 )
					.setMaxResults( 2 )
					.getResultList();

			assertEquals( List.of( new IntTestEntity( 12, id1 ), new IntTestEntity( 8, id3 ) ), res_0_to_1 );
			assertEquals( List.of( new IntTestEntity( 5, id2 ), new IntTestEntity( 1, id4 ) ), res_2_to_3 );
			assertEquals( 0, res_empty.size() );
		} );
	}

	@Test
	public void testEntitiesOrderLimitByQueryRev2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final var res_0_to_1 = auditReader.createQuery()
					.forEntitiesAtRevision( IntTestEntity.class, 2 )
					.addOrder( AuditEntity.property( "number" ).desc() )
					.setFirstResult( 0 )
					.setMaxResults( 2 )
					.getResultList();

			final var res_2_to_3 = auditReader.createQuery()
					.forEntitiesAtRevision( IntTestEntity.class, 2 )
					.addOrder( AuditEntity.property( "number" ).desc() )
					.setFirstResult( 2 )
					.setMaxResults( 2 )
					.getResultList();

			final var res_4 = auditReader.createQuery()
					.forEntitiesAtRevision( IntTestEntity.class, 2 )
					.addOrder( AuditEntity.property( "number" ).desc() )
					.setFirstResult( 4 )
					.setMaxResults( 2 )
					.getResultList();

			assertEquals( List.of( new IntTestEntity( 15, id4 ), new IntTestEntity( 8, id3 ) ), res_0_to_1 );
			assertEquals( List.of( new IntTestEntity( 5, id2 ), new IntTestEntity( 3, id5 ) ), res_2_to_3 );
			assertEquals( List.of( new IntTestEntity( 0, id1 ) ), res_4 );
		} );

	}
}
