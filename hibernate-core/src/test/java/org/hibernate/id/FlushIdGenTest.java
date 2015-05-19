/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.id;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-8611")
@RequiresDialectFeature( DialectChecks.SupportsIdentityColumns.class )
public class FlushIdGenTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testPersistBeforeTransaction() {
		Session session = openSession();
		RootEntity ent1_0 = new RootEntity();
		RootEntity ent1_1 = new RootEntity();

		session.persist( ent1_0 );
		session.persist( ent1_1 );

		Transaction tx = session.beginTransaction();
		tx.commit(); //flush
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
				RootEntity.class,
				RelatedEntity.class,
		};
	}

}
