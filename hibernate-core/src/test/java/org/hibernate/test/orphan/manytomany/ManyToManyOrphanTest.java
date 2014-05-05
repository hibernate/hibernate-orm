/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.orphan.manytomany;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.FailureExpectedWithNewUnifiedXsd;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

@FailureExpectedWithNewUnifiedXsd(message = "many-to-manys w/ orphan removal not yet supported")
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
