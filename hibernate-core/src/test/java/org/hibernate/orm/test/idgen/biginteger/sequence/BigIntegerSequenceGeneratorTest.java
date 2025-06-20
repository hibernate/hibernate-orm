/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.biginteger.sequence;


import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/idgen/biginteger/sequence/Mapping.hbm.xml"
)
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsSequences.class )
public class BigIntegerSequenceGeneratorTest {

	@Test
	public void testBasics(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Entity entity = new Entity( "BigInteger + sequence #1" );
					session.persist( entity );
					Entity entity2 = new Entity( "BigInteger + sequence #2" );
					session.persist( entity2 );

					// previously these checks were commented out due to the comment...

// hsqldb defines different behavior for the initial select from a sequence
// then say oracle
//		assertEquals( BigInteger.valueOf( 1 ), entity.getId() );
//		assertEquals( BigInteger.valueOf( 2 ), entity2.getId() );
				}
		);


	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
