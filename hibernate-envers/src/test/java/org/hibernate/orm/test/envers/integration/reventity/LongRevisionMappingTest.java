/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.reventity.LongRevisionMappingRevEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that a revision entity extending {@link org.hibernate.envers.LongRevisionMapping}
 * provides working {@code long} revision numbers.
 */
@EnversTest
@Jpa(annotatedClasses = {StrTestEntity.class, LongRevisionMappingRevEntity.class})
public class LongRevisionMappingTest {
	private Integer id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			StrTestEntity te = new StrTestEntity( "x" );
			em.persist( te );
			id = te.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			StrTestEntity te = em.find( StrTestEntity.class, id );
			te.setStr( "y" );
		} );
	}

	@Test
	public void testFindRevision(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			AuditReader vr = AuditReaderFactory.get( em );

			assertEquals( 1L, vr.findRevision( LongRevisionMappingRevEntity.class, 1L ).getId() );
			assertEquals( 2L, vr.findRevision( LongRevisionMappingRevEntity.class, 2L ).getId() );
		} );
	}

	@Test
	public void testFindRevisions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			AuditReader vr = AuditReaderFactory.get( em );

			Set<Number> revNumbers = new HashSet<>();
			revNumbers.add( 1L );
			revNumbers.add( 2L );

			Map<Number, LongRevisionMappingRevEntity> revisionMap = vr.findRevisions( LongRevisionMappingRevEntity.class, revNumbers );
			assertEquals( 2, revisionMap.size() );
			assertEquals( vr.findRevision( LongRevisionMappingRevEntity.class, 1L ), revisionMap.get( 1L ) );
			assertEquals( vr.findRevision( LongRevisionMappingRevEntity.class, 2L ), revisionMap.get( 2L ) );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1L, 2L ), AuditReaderFactory.get( em ).getRevisions( StrTestEntity.class, id ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			StrTestEntity ver1 = new StrTestEntity( "x", id );
			StrTestEntity ver2 = new StrTestEntity( "y", id );

			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( StrTestEntity.class, id, 1L ) );
			assertEquals( ver2, auditReader.find( StrTestEntity.class, id, 2L ) );
		} );
	}
}
