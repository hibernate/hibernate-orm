/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.orphan.manytomany;

import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ManyToManyOrphanTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] { "orphan/manytomany/UserGroup.hbm.xml" };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8749")
	public void testManyToManyWithCascadeDeleteOrphan() {
		inTransaction(
				s -> {
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
				}
		);

		inTransaction(
				s -> {
					User b = s.get( User.class, "bob" );
					assertEquals( 2, b.getGroups().size() );
					Group sG = s.get( Group.class, "seam" );
					assertEquals( (Integer) 1, sG.getGroupType() );
					Group hbG = s.get( Group.class, "hibernate" );
					assertEquals( (Integer) 2, hbG.getGroupType() );
				}
		);

		inTransaction(
				s -> {
					User b = s.get( User.class, "bob" );
					assertEquals( 2, b.getGroups().size() );
					Group hG = s.get( Group.class, "hibernate" );
					b.getGroups().remove( hG.getGroupType() );
					assertEquals( 1, b.getGroups().size() );
				}
		);

		inTransaction(
				s -> {
					User b = s.get( User.class, "bob" );
					assertEquals( 1, b.getGroups().size() );
				}
		);

		// Verify orphan group was deleted
		inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Group> criteria = criteriaBuilder.createQuery( Group.class );
					criteria.from( Group.class );
					List<Group> groups = s.createQuery( criteria ).list();

//					List<Group> groups = s.createCriteria( Group.class ).list();
					assertEquals( 1, groups.size() );
					assertEquals( "seam", groups.get( 0 ).getName() );
				}
		);

	}
}
