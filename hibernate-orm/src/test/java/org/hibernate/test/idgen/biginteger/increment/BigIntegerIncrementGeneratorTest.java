/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idgen.biginteger.increment;

import java.math.BigInteger;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class BigIntegerIncrementGeneratorTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "idgen/biginteger/increment/Mapping.hbm.xml" };
	}

	@Test
	public void testBasics() {
		Session s = openSession();
		s.beginTransaction();
		Entity entity = new Entity( "BigInteger + increment #1" );
		s.save( entity );
		Entity entity2 = new Entity( "BigInteger + increment #2" );
		s.save( entity2 );
		s.getTransaction().commit();
		s.close();

		assertEquals( BigInteger.valueOf( 1 ), entity.getId() );
		assertEquals( BigInteger.valueOf( 2 ), entity2.getId() );

		s = openSession();
		s.beginTransaction();
		s.delete( entity );
		s.delete( entity2 );
		s.getTransaction().commit();
		s.close();

	}
}
