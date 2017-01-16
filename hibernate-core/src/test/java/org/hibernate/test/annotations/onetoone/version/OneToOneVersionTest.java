/**
 * 
 */
package org.hibernate.test.annotations.onetoone.version;

import static org.junit.Assert.assertEquals;

import org.hibernate.Session;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-9005")
public class OneToOneVersionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityA.class, EntityB.class };
	}

	/**
	 * Tests that the version of two entities that references each other with a bidirectional one-to-one association is
	 * updated, when the association is changed.
	 */
	@Test
	@FailureExpected(jiraKey = "HHH-9005")
	public void testVersion() {
		Session session = openSession();

		session.getTransaction().begin();
		final EntityA a = new EntityA();
		a.setId( 1 );
		session.persist( a );
		final EntityB b = new EntityB();
		b.setId( 2 );
		session.persist( b );
		session.getTransaction().commit();
		assertEquals( "Unexpected version for a", Integer.valueOf( 0 ), a.getVersion() );
		assertEquals( "Unexpected version for b", Integer.valueOf( 0 ), b.getVersion() );

		session.getTransaction().begin();
		a.setEntityB( b );
		b.setEntityA( a );
		session.getTransaction().commit();
		assertEquals( "Unexpected version for b", Integer.valueOf( 1 ), b.getVersion() );
		assertEquals( "Unexpected version for a", Integer.valueOf( 1 ), a.getVersion() );
	}

}
