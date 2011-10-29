package org.hibernate.test.tm;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.test.jdbc.Person;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6780")
public class TransactionTimeoutTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "jdbc/Mappings.hbm.xml" };
	}

	@Test(expected = TransactionException.class)
	public void testTransactionTimeoutFailure() throws InterruptedException {
		Session session = openSession();
		Transaction transaction = session.getTransaction();
		transaction.setTimeout( 1 );
		transaction.begin();
		Thread.sleep( 1000 );
		session.persist( new Person( "Lukasz", "Antoniak" ) );
		transaction.commit();
		session.close();
	}

	@Test
	public void testTransactionTimeoutSuccess() {
		Session session = openSession();
		Transaction transaction = session.getTransaction();
		transaction.setTimeout( 2 );
		transaction.begin();
		session.persist( new Person( "Lukasz", "Antoniak" ) );
		transaction.commit();
		session.close();
	}
}
