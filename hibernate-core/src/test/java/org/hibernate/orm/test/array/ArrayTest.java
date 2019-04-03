/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.array;


import org.hibernate.dialect.AbstractHANADialect;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
@FailureExpected("Post insert identifier generator not yet implemented")
public class ArrayTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected String[] getHbmMappingFiles() {
		return new String[] { "array/A.hbm.xml" };
	}

	@Test
	@SkipForDialect(value = AbstractHANADialect.class, comment = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testArrayJoinFetch() {
		A a = new A();
		inTransaction(
				session -> {
					B b = new B();
					a.setBs( new B[] { b } );
					session.persist( a );
				}
		);

		inTransaction(
				session -> {
					A a1 = session.get( A.class, a.getId() );
					assertNotNull( a1 );
					assertNotNull( a1.getBs() );
					assertEquals( 1, a1.getBs().length );
					assertNotNull( a1.getBs()[0] );

					session.delete( a1 );
					session.delete( a1.getBs()[0] );
				}
		);
	}
}
