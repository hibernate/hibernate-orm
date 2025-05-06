/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.array;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "org/hibernate/orm/test/array/A.xml")
@SessionFactory
public class ArrayTest {

	@Test
	public void testArrayJoinFetch(SessionFactoryScope scope) {
		A a = new A();
		scope.inTransaction(
				session -> {
					B b = new B();
					a.setBs( new B[] { b } );
					session.persist( a );
				}
		);

		scope.inTransaction(
				session -> {
					A retrieved = session.find( A.class, a.getId() );
					assertNotNull( retrieved );
					assertNotNull( retrieved.getBs() );
					assertEquals( 1, retrieved.getBs().length );
					assertNotNull( retrieved.getBs()[0] );

					session.remove( retrieved );
					session.remove( retrieved.getBs()[0] );
				}
		);
	}
}
