/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.temporal;

import java.sql.Timestamp;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.junit.Test;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.MySQL57Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner.
 */
@TestForIssue( jiraKey = "HHH-8401")
@RequiresDialect( MySQL57Dialect.class )
public class MySQL57TimestampFspFunctionTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testTimeStampFunctions() {
		// add an entity just so it can be queried.

		Session s=openSession();
		Transaction tx = s.beginTransaction();
		s.persist( new Entity() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();

		// current_timestamp(), localtime(), and localtimestamp() are synonyms for now(),
		// which returns the time at which the statement began to execute.
		// the returned values for now(), current_timestamp(), localtime(), and
		// localtimestamp() should be the same.
		// sysdate() is the time at which the function itself is executed, so the
		// value returned for sysdate() should be different.
		Query q=s.createQuery(
				"select now(), current_timestamp(), localtime(), localtimestamp(), sysdate() from MySQL57TimestampFspFunctionTest$Entity"
		);
		Object[] oArray = (Object[]) q.uniqueResult();
		final Timestamp now = (Timestamp) oArray[0];
		assertEquals( now, oArray[1] );
		assertEquals( now, oArray[2] );
		assertEquals( now, oArray[3] );
		final Timestamp sysdate = (Timestamp) oArray[4];
		assertTrue( now.compareTo( sysdate ) < 0 );
		// all should have nanos > 0
		assertTrue( now.getNanos() > 0 );
		assertTrue( sysdate.getNanos() > 0 );

		tx.commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Entity.class };
	}

	// If MySQL supported something like Oracle's "dual", then this entity wouldn't be needed.
	@javax.persistence.Entity
	@Table(name = "DummyEntity")
	public static class Entity {
		@GeneratedValue
		@Id
		private long id;
	}
}
