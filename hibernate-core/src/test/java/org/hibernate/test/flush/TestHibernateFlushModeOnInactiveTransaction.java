/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.flush;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Test for issue https://hibernate.atlassian.net/browse/HHH-13663
 * 
 * @author Luca Domenichini
 */
@TestForIssue(jiraKey = "HHH-13663")
public class TestHibernateFlushModeOnInactiveTransaction extends BaseCoreFunctionalTestCase {

	@Test
	public void testHibernateFlushModeOnInactiveTransaction() {
		Session s = openSession();
		//s.setFlushMode(FlushMode.AUTO); // this does not throw (API is deprecated)
		s.setHibernateFlushMode(FlushMode.AUTO); // this should not throw even within an inactive transaction
	}

}
