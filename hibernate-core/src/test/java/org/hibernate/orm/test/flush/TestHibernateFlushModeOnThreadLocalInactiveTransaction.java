/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.flush;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Test for issue https://hibernate.atlassian.net/browse/HHH-13663
 * 
 * @author Luca Domenichini
 */
@JiraKey(value = "HHH-13663")
public class TestHibernateFlushModeOnThreadLocalInactiveTransaction extends BaseCoreFunctionalTestCase {

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "thread" );
	}

	@Test
	public void testHibernateFlushModeOnInactiveTransaction() {
		try ( Session s = sessionFactory().getCurrentSession() ) {
			//s.setFlushMode( FlushMode.AUTO ); // this does not throw (API is deprecated)
			s.setHibernateFlushMode( FlushMode.AUTO ); // this should not throw even within an inactive transaction
		}
	}

}
