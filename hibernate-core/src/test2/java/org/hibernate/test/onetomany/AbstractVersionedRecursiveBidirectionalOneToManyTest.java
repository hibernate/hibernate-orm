/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetomany;

import org.hibernate.Session;
import org.hibernate.Transaction;

import static org.junit.Assert.assertEquals;

/**
 * @author Burkhard Graves
 * @author Gail Badner
 */
public abstract class AbstractVersionedRecursiveBidirectionalOneToManyTest extends AbstractRecursiveBidirectionalOneToManyTest {
	@Override
	public String[] getMappings() {
		return new String[] { "onetomany/VersionedNode.hbm.xml" };
	}

	@Override
	void check(boolean simplePropertyUpdated) {
		super.check( simplePropertyUpdated );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Node node1 = ( Node ) s.get( Node.class, Integer.valueOf( 1 ) );
		Node node2 = ( Node ) s.get( Node.class, Integer.valueOf( 2 ) );
		Node node3 = ( Node ) s.get( Node.class, Integer.valueOf( 3 ) );
		assertEquals( 1, node1.getVersion() );
		assertEquals( 1, node2.getVersion() );
		assertEquals( 1, node3.getVersion() );
		tx.commit();
		s.close();
	}
}
