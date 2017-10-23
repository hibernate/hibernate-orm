/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idgen.biginteger.sequence;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Steve Ebersole
 */
@RequiresDialectFeature( value = DialectChecks.SupportsSequences.class )
public class BigIntegerSequenceGeneratorTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "idgen/biginteger/sequence/Mapping.hbm.xml" };
	}

	@Test
	public void testBasics() {
		Session s = openSession();
		s.beginTransaction();
		Entity entity = new Entity( "BigInteger + sequence #1" );
		s.save( entity );
		Entity entity2 = new Entity( "BigInteger + sequence #2" );
		s.save( entity2 );
		s.getTransaction().commit();
		s.close();

// hsqldb defines different behavior for the initial select from a sequence
// then say oracle
//		assertEquals( BigInteger.valueOf( 1 ), entity.getId() );
//		assertEquals( BigInteger.valueOf( 2 ), entity2.getId() );

		s = openSession();
		s.beginTransaction();
		s.delete( entity );
		s.delete( entity2 );
		s.getTransaction().commit();
		s.close();

	}
}
