package org.hibernate.envers.test.integration.proxy;

import java.util.Arrays;
import java.util.List;

import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class QueryingWithProxyObjectTest extends BaseEnversFunctionalTestCase {
	private Integer id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		getSession().getTransaction().begin();
		StrTestEntity ste = new StrTestEntity( "data" );
		getSession().persist( ste );
		getSession().getTransaction().commit();
		id = ste.getId();
		getSession().close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-4760")
	@SuppressWarnings("unchecked")
	public void testQueryingWithProxyObject() {
		StrTestEntity originalSte = new StrTestEntity( "data", id );
		// Load the proxy instance
		StrTestEntity proxySte = (StrTestEntity) getSession().load( StrTestEntity.class, id );

		Assert.assertTrue( getAuditReader().isEntityClassAudited( proxySte.getClass() ) );

		StrTestEntity ste = getAuditReader().find( proxySte.getClass(), proxySte.getId(), 1 );
		Assert.assertEquals( originalSte, ste );

		List<Number> revisions = getAuditReader().getRevisions( proxySte.getClass(), proxySte.getId() );
		Assert.assertEquals( Arrays.asList( 1 ), revisions );

		List<StrTestEntity> entities = getAuditReader().createQuery()
				.forEntitiesAtRevision( proxySte.getClass(), 1 )
				.getResultList();
		Assert.assertEquals( Arrays.asList( originalSte ), entities );

		ste = (StrTestEntity) getAuditReader().createQuery()
				.forRevisionsOfEntity( proxySte.getClass(), true, false )
				.getSingleResult();
		Assert.assertEquals( originalSte, ste );

		ste = (StrTestEntity) getAuditReader().createQuery()
				.forEntitiesModifiedAtRevision( proxySte.getClass(), 1 )
				.getSingleResult();
		Assert.assertEquals( originalSte, ste );

		getSession().close();
	}
}
