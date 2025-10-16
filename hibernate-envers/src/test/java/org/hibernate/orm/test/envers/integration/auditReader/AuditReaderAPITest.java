/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.auditReader;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A test which checks the correct behavior of AuditReader.isEntityClassAudited(Class entityClass).
 *
 * @author Hernan Chanfreau
 */
@EnversTest
@Jpa(annotatedClasses = {AuditedTestEntity.class, NotAuditedTestEntity.class})
public class AuditReaderAPITest {

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			AuditedTestEntity ent1 = new AuditedTestEntity( 1, "str1" );
			NotAuditedTestEntity ent2 = new NotAuditedTestEntity( 1, "str1" );

			em.persist( ent1 );
			em.persist( ent2 );
			em.getTransaction().commit();

			em.getTransaction().begin();

			ent1 = em.find( AuditedTestEntity.class, 1 );
			ent2 = em.find( NotAuditedTestEntity.class, 1 );
			ent1.setStr1( "str2" );
			ent2.setStr1( "str2" );
			em.getTransaction().commit();

			em.getTransaction().begin();
			ent1 = em.find( AuditedTestEntity.class, 1 );
			em.remove( ent1 );
			em.getTransaction().commit();
		} );
	}

	@Test
	public void testIsEntityClassAuditedForAuditedEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertTrue( auditReader.isEntityClassAudited( AuditedTestEntity.class ) );
			assertEquals( Arrays.asList( 1, 2, 3 ),
					auditReader.getRevisions( AuditedTestEntity.class, 1 ) );
		} );
	}

	@Test
	public void testIsEntityClassAuditedForNotAuditedEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertFalse( auditReader.isEntityClassAudited( NotAuditedTestEntity.class ) );

			try {
				auditReader.getRevisions( NotAuditedTestEntity.class, 1 );
				fail( "Expected a NotAuditedException" );
			}
			catch (NotAuditedException nae) {
				// expected
			}
		} );
	}

	@Test
	public void testFindRevisionEntitiesWithoutDeletions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<?> revisionInfos = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( AuditedTestEntity.class, false )
					.getResultList();
			assertEquals( 2, revisionInfos.size() );
			revisionInfos.forEach( e -> assertTyping( SequenceIdRevisionEntity.class, e ) );
		} );
	}

	@Test
	public void testFindRevisionEntitiesWithDeletions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<?> revisionInfos = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( AuditedTestEntity.class, true )
					.getResultList();
			assertEquals( 3, revisionInfos.size() );
			revisionInfos.forEach( e -> assertTyping( SequenceIdRevisionEntity.class, e ) );
		} );
	}

	@Test
	public void testFindRevisionEntitiesNonAuditedEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			try {
				AuditReaderFactory.get( em ).createQuery()
						.forRevisionsOfEntity( NotAuditedTestEntity.class, false )
						.getResultList();
				fail( "Expected a NotAuditedException" );
			}
			catch (NotAuditedException e) {
				// expected
			}
		} );
	}
}
