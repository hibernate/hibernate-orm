/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.interfaces.components;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
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
@Jpa(annotatedClasses = {ComponentTestEntity.class})
public class InterfacesComponents {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			ComponentTestEntity cte1 = new ComponentTestEntity( new Component1( "a" ) );
			em.persist( cte1 );
			id1 = cte1.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			ComponentTestEntity cte1 = em.find( ComponentTestEntity.class, id1 );
			cte1.setComp1( new Component1( "b" ) );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			ComponentTestEntity cte1 = em.find( ComponentTestEntity.class, id1 );
			cte1.getComp1().setData( "c" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( ComponentTestEntity.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			ComponentTestEntity ver1 = new ComponentTestEntity( id1, new Component1( "a" ) );
			ComponentTestEntity ver2 = new ComponentTestEntity( id1, new Component1( "b" ) );
			ComponentTestEntity ver3 = new ComponentTestEntity( id1, new Component1( "c" ) );

			assertEquals( ver1, auditReader.find( ComponentTestEntity.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( ComponentTestEntity.class, id1, 2 ) );
			assertEquals( ver3, auditReader.find( ComponentTestEntity.class, id1, 3 ) );
		} );
	}
}
