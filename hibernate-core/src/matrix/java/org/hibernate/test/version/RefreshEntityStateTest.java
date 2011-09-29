package org.hibernate.test.version;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;
import org.hibernate.event.spi.EventSource;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-5944")
public class RefreshEntityStateTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "version/PersonThing.hbm.xml" };
	}

	@Test
	public void testClearActionQueueAfterEntityRefresh() {
		Session s1 = openSession();
		Transaction t1 = s1.beginTransaction();
		Thing computer = new Thing();
		computer.setDescription( "Computer" );
		computer.setDescription( "My Computer" );
		s1.persist( computer );
		t1.commit();
		s1.close();

		s1 = openSession();
		t1 = s1.beginTransaction();
		computer = (Thing) s1.get( Thing.class, computer.getDescription() );

		/* Change entity in another session. */
		Session s2 = openSession();
		Transaction t2 = s2.beginTransaction();
		Thing newComputer = (Thing) s2.get( Thing.class, computer.getDescription() );
		newComputer.setLongDescription( "My New Computer" );
		s2.update( newComputer );
		t2.commit();
		s2.close();

		computer.setLongDescription( "My New Laptop" );
		s1.update( computer );
		try {
			s1.flush();
			Assert.assertTrue( "StaleObjectStateException expected.", false );
		}
		catch ( StaleObjectStateException e ) {
			s1.refresh( computer );
			Assert.assertEquals( "My New Computer", computer.getLongDescription() );
			// All events associated with computer object shall be removed.
			Assert.assertEquals( 0, ( (EventSource) s1 ).getActionQueue().numberOfUpdates() );
		}
		computer.setLongDescription( "My New Laptop" );
		s1.update( computer );
		s1.flush();
		t1.commit();
		s1.close();

		Assert.assertEquals( "My New Laptop", computer.getLongDescription() );
	}
}
