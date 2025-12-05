/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.mapkey;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {IdMapKeyEntity.class, StrTestEntity.class})
public class IdMapKey {
	private Integer imke_id;

	private Integer ste1_id;
	private Integer ste2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			IdMapKeyEntity imke = new IdMapKeyEntity();

			// Revision 1 (intialy 1 mapping)
			em.getTransaction().begin();

			StrTestEntity ste1 = new StrTestEntity( "x" );
			StrTestEntity ste2 = new StrTestEntity( "y" );

			em.persist( ste1 );
			em.persist( ste2 );

			imke.getIdmap().put( ste1.getId(), ste1 );

			em.persist( imke );

			em.getTransaction().commit();

			// Revision 2 (sse1: adding 1 mapping)
			em.getTransaction().begin();

			ste2 = em.find( StrTestEntity.class, ste2.getId() );
			imke = em.find( IdMapKeyEntity.class, imke.getId() );

			imke.getIdmap().put( ste2.getId(), ste2 );

			em.getTransaction().commit();

			//

			imke_id = imke.getId();

			ste1_id = ste1.getId();
			ste2_id = ste2.getId();
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2 ), AuditReaderFactory.get( em ).getRevisions( IdMapKeyEntity.class, imke_id ) );
		} );
	}

	@Test
	public void testHistoryOfImke(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			StrTestEntity ste1 = em.find( StrTestEntity.class, ste1_id );
			StrTestEntity ste2 = em.find( StrTestEntity.class, ste2_id );

			var auditReader = AuditReaderFactory.get( em );
			IdMapKeyEntity rev1 = auditReader.find( IdMapKeyEntity.class, imke_id, 1 );
			IdMapKeyEntity rev2 = auditReader.find( IdMapKeyEntity.class, imke_id, 2 );

			assertEquals( TestTools.makeMap( ste1.getId(), ste1 ), rev1.getIdmap() );
			assertEquals( TestTools.makeMap( ste1.getId(), ste1, ste2.getId(), ste2 ), rev2.getIdmap() );
		} );
	}
}
