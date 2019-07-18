/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.orphan;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;


/**
 * @author Steve Ebersole
 */
@TestForIssue(jiraKey = "HHH-565")
public class PropertyRefTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "orphan/User.hbm.xml", "orphan/Mail.hbm.xml" };
	}

	@Test
	public void testDeleteParentWithBidirOrphanDeleteCollectionBasedOnPropertyRef() {
		User user = new User( "test" );
		inTransaction(
				session -> {
					user.addMail( "test" );
					user.addMail( "test" );
					session.save( user );
				}
		);

		inTransaction(
				session -> {
					User u = session.load( User.class, user.getId() );
					session.delete( u );
				}
		);

		inTransaction(
				s -> {
					session.createQuery( "delete from Mail where alias = :alias" )
							.setParameter( "alias", "test" )
							.executeUpdate();
					session.createQuery( "delete from User where userid = :userid" )
							.setParameter( "userid", "test" )
							.executeUpdate();

				}
		);
	}

}
