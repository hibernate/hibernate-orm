/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.propertyref.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
public class BasicPropertiesTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "propertyref/basic/EntityClass.hbm.xml" };
	}

	/**
	 * Really simple regression test for HHH-8689.
	 */
	@Test
	@TestForIssue(jiraKey = "HHH-8689")
	public void testProperties() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		EntityClass ec = new EntityClass();
		ec.setKey( 1l );
		ec.setField1( "foo1" );
		ec.setField2( "foo2" );
		s.persist( ec );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		ec = (EntityClass) s.get( EntityClass.class, 1l );
		t.commit();
		s.close();
		
		assertNotNull( ec );
		assertEquals( ec.getField1(), "foo1" );
		assertEquals( ec.getField2(), "foo2" );
	}
}

