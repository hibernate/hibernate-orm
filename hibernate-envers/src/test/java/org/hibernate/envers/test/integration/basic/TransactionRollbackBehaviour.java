package org.hibernate.envers.test.integration.basic;

import javax.persistence.EntityManager;
import java.util.List;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.entities.IntTestEntity;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

/**
 * @author Tomasz Dziurko (tdziurko at gmail dot com)
 */
public class TransactionRollbackBehaviour extends BaseEnversJPAFunctionalTestCase {
	@Test
	public void testAuditRecordsRollback() {
		// Given
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		IntTestEntity iteToRollback = new IntTestEntity( 30 );
		em.persist( iteToRollback );
		Integer rollbackedIteId = iteToRollback.getId();
		em.getTransaction().rollback();

		// When
		em.getTransaction().begin();
		IntTestEntity ite2 = new IntTestEntity( 50 );
		em.persist( ite2 );
		Integer ite2Id = ite2.getId();
		em.getTransaction().commit();

		// Then
		List<Number> revisionsForSavedClass = getAuditReader().getRevisions( IntTestEntity.class, ite2Id );
		Assert.assertEquals( "There should be one revision for inserted entity.", 1, revisionsForSavedClass.size() );

		List<Number> revisionsForRolledbackClass = getAuditReader().getRevisions(
				IntTestEntity.class,
				rollbackedIteId
		);
		Assert.assertEquals(
				"There should be no revision for rolled back transaction.",
				0,
				revisionsForRolledbackClass.size()
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8189")
	public void testFlushedAuditRecordsRollback() {
		// Given
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		IntTestEntity iteToRollback = new IntTestEntity( 30 );
		em.persist( iteToRollback );
		em.flush();
		Integer rollbackedIteId = iteToRollback.getId();
		em.getTransaction().rollback();

		// When
		em.getTransaction().begin();
		IntTestEntity ite2 = new IntTestEntity( 50 );
		em.persist( ite2 );
		em.flush();
		Integer ite2Id = ite2.getId();
		em.getTransaction().commit();

		// Then
		List<Number> revisionsForSavedClass = getAuditReader().getRevisions( IntTestEntity.class, ite2Id );
		Assert.assertEquals( "There should be one revision for inserted entity.", 1, revisionsForSavedClass.size() );

		List<Number> revisionsForRolledbackClass = getAuditReader().getRevisions(
				IntTestEntity.class,
				rollbackedIteId
		);
		Assert.assertEquals(
				"There should be no revision for rolled back transaction.",
				0,
				revisionsForRolledbackClass.size()
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {IntTestEntity.class};
	}
}
