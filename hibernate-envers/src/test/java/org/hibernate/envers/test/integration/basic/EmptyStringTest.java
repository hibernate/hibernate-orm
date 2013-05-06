package org.hibernate.envers.test.integration.basic;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;

import org.junit.Test;
import junit.framework.Assert;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7246")
@RequiresDialect(Oracle8iDialect.class)
public class EmptyStringTest extends BaseEnversJPAFunctionalTestCase {
	private Integer emptyId = null;
	private Integer nullId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {StrTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		StrTestEntity emptyEntity = new StrTestEntity( "" );
		em.persist( emptyEntity );
		StrTestEntity nullEntity = new StrTestEntity( null );
		em.persist( nullEntity );
		em.getTransaction().commit();

		emptyId = emptyEntity.getId();
		nullId = nullEntity.getId();

		em.close();
		em = getEntityManager();

		// Should not generate revision after NULL to "" modification and vice versa on Oracle.
		em.getTransaction().begin();
		emptyEntity = em.find( StrTestEntity.class, emptyId );
		emptyEntity.setStr( null );
		em.merge( emptyEntity );
		nullEntity = em.find( StrTestEntity.class, nullId );
		nullEntity.setStr( "" );
		em.merge( nullEntity );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testRevisionsCounts() {
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( StrTestEntity.class, emptyId ) );
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( StrTestEntity.class, nullId ) );
	}
}
