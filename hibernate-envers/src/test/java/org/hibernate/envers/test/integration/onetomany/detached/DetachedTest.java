package org.hibernate.envers.test.integration.onetomany.detached;

import java.util.Arrays;

import org.hibernate.Session;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.onetomany.detached.ListRefCollEntity;

import org.junit.Test;
import junit.framework.Assert;

import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class DetachedTest extends BaseEnversFunctionalTestCase {
	private Integer parentId = null;
	private Integer childId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ListRefCollEntity.class, StrTestEntity.class};
	}

	@Test
	@Priority(10)
	@TestForIssue(jiraKey = "HHH-7543")
	public void testUpdatingDetachedEntityWithRelation() {
		Session session = getSession();

		// Revision 1
		session.getTransaction().begin();
		ListRefCollEntity parent = new ListRefCollEntity( 1, "initial data" );
		StrTestEntity child = new StrTestEntity( "data" );
		session.save( child );
		parent.setCollection( Arrays.asList( child ) );
		session.save( parent );
		session.getTransaction().commit();

		session.close();
		session = getSession();

		// Revision 2 - updating detached entity
		session.getTransaction().begin();
		parent.setData( "modified data" );
		session.update( parent );
		session.getTransaction().commit();

		session.close();

		parentId = parent.getId();
		childId = child.getId();
	}

	@Test
	public void testRevisionsCounts() {
		Assert.assertEquals(
				Arrays.asList( 1, 2 ), getAuditReader().getRevisions(
				ListRefCollEntity.class,
				parentId
		)
		);
		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( StrTestEntity.class, childId ) );
	}

	@Test
	public void testHistoryOfParent() {
		ListRefCollEntity parent = new ListRefCollEntity( parentId, "initial data" );
		parent.setCollection( Arrays.asList( new StrTestEntity( "data", childId ) ) );

		ListRefCollEntity ver1 = getAuditReader().find( ListRefCollEntity.class, parentId, 1 );

		Assert.assertEquals( parent, ver1 );
		Assert.assertEquals( parent.getCollection(), ver1.getCollection() );

		parent.setData( "modified data" );

		ListRefCollEntity ver2 = getAuditReader().find( ListRefCollEntity.class, parentId, 2 );

		Assert.assertEquals( parent, ver2 );
		Assert.assertEquals( parent.getCollection(), ver2.getCollection() );
	}
}