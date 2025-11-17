/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.joined.relation;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
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
@Jpa(annotatedClasses = {ChildIngEntity.class, ParentIngEntity.class, ReferencedEntity.class})
public class PolymorphicCollection {
	private Integer ed_id1;
	private Integer c_id;
	private Integer p_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		ed_id1 = 1;
		p_id = 10;
		c_id = 100;

		// Rev 1
		scope.inTransaction( em -> {
			ReferencedEntity re = new ReferencedEntity( ed_id1 );
			em.persist( re );
		} );

		// Rev 2
		scope.inTransaction( em -> {
			ReferencedEntity re = em.find( ReferencedEntity.class, ed_id1 );

			ParentIngEntity pie = new ParentIngEntity( p_id, "x" );
			pie.setReferenced( re );
			em.persist( pie );
			p_id = pie.getId();
		} );

		// Rev 3
		scope.inTransaction( em -> {
			ReferencedEntity re = em.find( ReferencedEntity.class, ed_id1 );

			ChildIngEntity cie = new ChildIngEntity( c_id, "y", 1l );
			cie.setReferenced( re );
			em.persist( cie );
			c_id = cie.getId();
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( ReferencedEntity.class, ed_id1 ) );
			assertEquals( Arrays.asList( 2 ), auditReader.getRevisions( ParentIngEntity.class, p_id ) );
			assertEquals( Arrays.asList( 3 ), auditReader.getRevisions( ChildIngEntity.class, c_id ) );
		} );
	}

	@Test
	public void testHistoryOfReferencedCollection(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( 0, auditReader.find( ReferencedEntity.class, ed_id1, 1 ).getReferencing().size() );
			assertEquals(
					TestTools.makeSet( new ParentIngEntity( p_id, "x" ) ),
					auditReader.find( ReferencedEntity.class, ed_id1, 2 ).getReferencing()
			);
			assertEquals(
					TestTools.makeSet( new ParentIngEntity( p_id, "x" ), new ChildIngEntity( c_id, "y", 1l ) ),
					auditReader.find( ReferencedEntity.class, ed_id1, 3 ).getReferencing()
			);
		} );
	}
}
