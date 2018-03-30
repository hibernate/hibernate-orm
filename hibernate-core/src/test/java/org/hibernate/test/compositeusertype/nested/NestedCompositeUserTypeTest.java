/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.compositeusertype.nested;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

public class NestedCompositeUserTypeTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				LineEntity.class
		};
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12166")
	public void testIt() {
		Line line = new Line( new Point( 0, 0 ), new Point( 42, 84 ) );

		doInHibernate(
				this::sessionFactory, session -> {
					LineEntity le = new LineEntity();
					le.setLine( line );
					session.save( le );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					LineEntity lineEntry = session.createQuery( "from " + LineEntity.class.getName(), LineEntity.class )
							.uniqueResult();
					Assert.assertEquals( line, lineEntry.getLine() );
				}
		);
	}
}
