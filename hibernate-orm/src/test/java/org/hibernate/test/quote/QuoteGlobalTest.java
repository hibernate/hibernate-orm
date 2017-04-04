/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.quote;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 * @author Brett Meyer
 */
public class QuoteGlobalTest extends BaseNonConfigCoreFunctionalTestCase {
	
	@Test
	@TestForIssue(jiraKey = "HHH-7890")
	public void testQuotedUniqueConstraint() {
		Iterator<UniqueKey> itr = metadata().getEntityBinding( Person.class.getName() )
				.getTable().getUniqueKeyIterator();
		while ( itr.hasNext() ) {
			UniqueKey uk = itr.next();
			assertEquals( uk.getColumns().size(), 1 );
			assertEquals( uk.getColumn( 0 ).getName(),  "name");
			return;
		}
		fail( "GLOBALLY_QUOTED_IDENTIFIERS caused the unique key creation to fail." );
	}
	
	@Test
	public void testQuoteManytoMany() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		User u = new User();
		s.persist( u );
		Role r = new Role();
		s.persist( r );
		u.getRoles().add( r );
		s.flush();
		s.clear();
		u = (User) s.get( User.class, u.getId() );
		assertEquals( 1, u.getRoles().size() );
		tx.rollback();
		String role = User.class.getName() + ".roles";
		assertEquals( "User_Role", metadata().getCollectionBinding( role ).getCollectionTable().getName() );
		s.close();
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-8520")
	public void testHbmQuoting() {
		doTestHbmQuoting( DataPoint.class );
		doTestHbmQuoting( AssociatedDataPoint.class );
	}
	
	private void doTestHbmQuoting(Class clazz) {
		Table table = metadata().getEntityBinding( clazz.getName() ).getTable();
		assertTrue( table.isQuoted() );
		Iterator itr = table.getColumnIterator();
		while(itr.hasNext()) {
			Column column = (Column) itr.next();
			assertTrue( column.isQuoted() );
		}
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, "true" );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				User.class,
				Role.class,
				Phone.class,
				Person.class,
				House.class
		};
	}

	@Override
	protected String[] getMappings() {
		return new String[] { "quote/DataPoint.hbm.xml" };
	}

}
