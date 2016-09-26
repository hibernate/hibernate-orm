/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.discriminator.joined;

import org.hibernate.Session;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11133")
public class JoinedSubclassInheritanceTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] { "discriminator/joined/JoinedSubclassInheritance.hbm.xml" };
	}

	@Test
	public void testConfiguredDiscriminatorValue() {
		Session session = openSession();
		try {
			ChildEntity ce = new ChildEntity( 1, "Child" );
			session.getTransaction().begin();
			session.save( ce );
			session.getTransaction().commit();
			session.clear();

			ce = (ChildEntity) session.find( ChildEntity.class, 1 );
			assertEquals( "ce", ce.getType() );
		}
		catch ( Exception e ) {
			if ( session.getTransaction().isActive() ) {
				session.getTransaction().rollback();
			}
		}
		finally {
			session.close();
		}
	}

}
