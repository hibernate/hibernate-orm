/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.abstractembeddedcomponents.cid;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Steve Ebersole
 */
public class AbstractCompositeIdTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "abstractembeddedcomponents/cid/Mappings.hbm.xml" };
	}

	@Test
	public void testEmbeddedCompositeIdentifierOnAbstractClass() {
		MyInterfaceImpl myInterface = new MyInterfaceImpl();
		myInterface.setKey1( "key1" );
		myInterface.setKey2( "key2" );
		myInterface.setName( "test" );

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.save( myInterface );
		s.flush();

		s.createQuery( "from MyInterface" ).list();

		s.delete( myInterface );
		t.commit();
		s.close();

	}
}
