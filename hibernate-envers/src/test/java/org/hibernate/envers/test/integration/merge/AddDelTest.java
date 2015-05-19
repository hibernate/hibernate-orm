/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.merge;

import java.util.Arrays;

import org.hibernate.Session;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6753")
public class AddDelTest extends BaseEnversFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class, GivenIdStrEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		Session session = openSession();
		session.getTransaction().begin();
		GivenIdStrEntity entity = new GivenIdStrEntity( 1, "data" );
		session.persist( entity );
		session.getTransaction().commit();

		// Revision 2
		session.getTransaction().begin();
		session.persist( new StrTestEntity( "another data" ) ); // Just to create second revision.
		entity = (GivenIdStrEntity) session.get( GivenIdStrEntity.class, 1 );
		session.delete( entity ); // First try to remove the entity.
		session.save( entity ); // Then save it.
		session.getTransaction().commit();

		// Revision 3
		session.getTransaction().begin();
		entity = (GivenIdStrEntity) session.get( GivenIdStrEntity.class, 1 );
		session.delete( entity ); // First try to remove the entity.
		entity.setData( "modified data" ); // Then change it's state.
		session.save( entity ); // Finally save it.
		session.getTransaction().commit();

		session.close();
	}

	@Test
	public void testRevisionsCountOfGivenIdStrEntity() {
		// Revision 2 has not changed entity's state.
		Assert.assertEquals( Arrays.asList( 1, 3 ), getAuditReader().getRevisions( GivenIdStrEntity.class, 1 ) );

		getSession().close();
	}

	@Test
	public void testHistoryOfGivenIdStrEntity() {
		Assert.assertEquals( new GivenIdStrEntity( 1, "data" ), getAuditReader().find( GivenIdStrEntity.class, 1, 1 ) );
		Assert.assertEquals(
				new GivenIdStrEntity( 1, "modified data" ), getAuditReader().find(
				GivenIdStrEntity.class,
				1,
				3
		)
		);

		getSession().close();
	}
}
