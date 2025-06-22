/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.propertyref.basic;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Brett Meyer
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/propertyref/basic/EntityClass.hbm.xml"
)
@SessionFactory
public class BasicPropertiesTest {

	/**
	 * Really simple regression test for HHH-8689.
	 */
	@Test
	@JiraKey(value = "HHH-8689")
	public void testProperties(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityClass ec = new EntityClass();
					ec.setKey( 1l );
					ec.setField1( "foo1" );
					ec.setField2( "foo2" );
					session.persist( ec );
				}
		);

		EntityClass ec = scope.fromTransaction(
				session ->
						session.get( EntityClass.class, 1l )
		);

		assertNotNull( ec );
		assertEquals( ec.getField1(), "foo1" );
		assertEquals( ec.getField2(), "foo2" );
	}
}
