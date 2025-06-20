/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.biginteger.increment;

import java.math.BigInteger;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

@DomainModel(
		xmlMappings = "org/hibernate/orm/test/idgen/biginteger/increment/Mapping.hbm.xml"
)
@SessionFactory
public class BigIntegerIncrementGeneratorTest {

	@Test
	public void testBasics(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Entity entity = new Entity( "BigInteger + increment #1" );
					session.persist( entity );
					Entity entity2 = new Entity( "BigInteger + increment #2" );
					session.persist( entity2 );

					session.flush();

					assertEquals( BigInteger.valueOf( 1 ), entity.getId() );
					assertEquals( BigInteger.valueOf( 2 ), entity2.getId() );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
