/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.orphan.manytomany;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class ManyToManyOrphanTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] { "orphan/manytomany/UserGroup.hbm.xml" };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8749")
	public void testManyToManyWithCascadeDeleteOrphan() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User bob = new User( "bob", "jboss" );
		Group seam = new Group( "seam", "jboss" );
		seam.setGroupType( 1 );
		Group hb = new Group( "hibernate", "jboss" );
		hb.setGroupType( 2 );
		bob.getGroups().put( seam.getGroupType(), seam );
		bob.getGroups().put( hb.getGroupType(), hb );
		s.persist( bob );
		s.persist( seam );
		s.persist( hb );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		bob = (User) s.get( User.class, "bob" );
		assertEquals( 2, bob.getGroups().size() );
		seam = (Group) s.get( Group.class, "seam" );
		assertEquals( (Integer) 1, seam.getGroupType() );
		hb = (Group) s.get( Group.class, "hibernate" );
		assertEquals( (Integer) 2, hb.getGroupType() );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		bob = (User) s.get( User.class, "bob" );
		assertEquals( 2, bob.getGroups().size() );
		hb = (Group) s.get( Group.class, "hibernate" );
		bob.getGroups().remove( hb.getGroupType() );
		assertEquals( 1, bob.getGroups().size() );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		bob = (User) s.get( User.class, "bob" );
		assertEquals( 1, bob.getGroups().size() );
		t.commit();
		s.close();

		// Verify orphan group was deleted
		s = openSession();
		t = s.beginTransaction();
		List<Group> groups = s.createCriteria( Group.class ).list();
		assertEquals( 1, groups.size() );
		assertEquals( "seam", groups.get( 0 ).getName() );
		t.commit();
		s.close();
	}
}
