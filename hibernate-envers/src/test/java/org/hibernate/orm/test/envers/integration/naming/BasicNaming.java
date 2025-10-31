/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.naming;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@DomainModel(annotatedClasses = {NamingTestEntity1.class})
@SessionFactory
public class BasicNaming {
	private Integer id1;
	private Integer id2;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			NamingTestEntity1 nte1 = new NamingTestEntity1( "data1" );
			NamingTestEntity1 nte2 = new NamingTestEntity1( "data2" );
			em.persist( nte1 );
			em.persist( nte2 );
			id1 = nte1.getId();
			id2 = nte2.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			NamingTestEntity1 nte1 = em.find( NamingTestEntity1.class, id1 );
			nte1.setData( "data1'" );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			NamingTestEntity1 nte2 = em.find( NamingTestEntity1.class, id2 );
			nte2.setData( "data2'" );
		} );
	}

	@Test
	public void testRevisionsCounts(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( NamingTestEntity1.class, id1 ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( NamingTestEntity1.class, id2 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			NamingTestEntity1 ver1 = new NamingTestEntity1( id1, "data1" );
			NamingTestEntity1 ver2 = new NamingTestEntity1( id1, "data1'" );

			assertEquals( ver1, auditReader.find( NamingTestEntity1.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( NamingTestEntity1.class, id1, 2 ) );
			assertEquals( ver2, auditReader.find( NamingTestEntity1.class, id1, 3 ) );
		} );
	}

	@Test
	public void testHistoryOfId2(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			NamingTestEntity1 ver1 = new NamingTestEntity1( id2, "data2" );
			NamingTestEntity1 ver2 = new NamingTestEntity1( id2, "data2'" );

			assertEquals( ver1, auditReader.find( NamingTestEntity1.class, id2, 1 ) );
			assertEquals( ver1, auditReader.find( NamingTestEntity1.class, id2, 2 ) );
			assertEquals( ver2, auditReader.find( NamingTestEntity1.class, id2, 3 ) );
		} );
	}

	@Test
	public void testTableName(DomainModelScope scope) {
		assertEquals( "naming_test_entity_1_versions", scope.getDomainModel()
				.getEntityBinding( "org.hibernate.orm.test.envers.integration.naming.NamingTestEntity1_AUD" ).getTable()
				.getName() );
	}
}
