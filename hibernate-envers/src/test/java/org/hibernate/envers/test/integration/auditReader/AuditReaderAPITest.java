/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.auditReader;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A test which checks the correct behavior of AuditReader.isEntityClassAudited(Class entityClass).
 *
 * @author Hernan Chanfreau
 */
public class AuditReaderAPITest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {AuditedTestEntity.class, NotAuditedTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
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
	}

	@Test
	public void testIsEntityClassAuditedForAuditedEntity() {
		assertTrue( getAuditReader().isEntityClassAudited( AuditedTestEntity.class ) );
		assertEquals( Arrays.asList( 1, 2, 3 ), getAuditReader().getRevisions( AuditedTestEntity.class, 1 ) );
	}

	@Test
	public void testIsEntityClassAuditedForNotAuditedEntity() {

		assert !getAuditReader().isEntityClassAudited( NotAuditedTestEntity.class );

		try {
			getAuditReader().getRevisions( NotAuditedTestEntity.class, 1 );
		}
		catch (NotAuditedException nae) {
			// it's ok because the entity is not audited
			assert true;
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-7555" )
	public void testFindRevisionEntitiesWithoutDeletions() {
		List<?> revisionInfos = getAuditReader().createQuery()
				.forRevisionsOfEntity( AuditedTestEntity.class, false )
				.getResultList();
		assertEquals( 2, revisionInfos.size() );
		revisionInfos.forEach( e -> assertTyping( SequenceIdRevisionEntity.class, e ) );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-7555" )
	public void testFindRevisionEntitiesWithDeletions() {
		List<?> revisionInfos = getAuditReader().createQuery()
				.forRevisionsOfEntity( AuditedTestEntity.class, true )
				.getResultList();
		assertEquals( 3, revisionInfos.size() );
		revisionInfos.forEach( e -> assertTyping( SequenceIdRevisionEntity.class, e ) );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-7555" )
	public void testFindRevisionEntitiesNonAuditedEntity() {
		try {
			List<?> revisionInfos = getAuditReader().createQuery()
					.forRevisionsOfEntity( NotAuditedTestEntity.class, false )
					.getResultList();
			fail( "Expected a NotAuditedException" );
		}
		catch ( NotAuditedException e ) {
			// expected
		}
	}
}
