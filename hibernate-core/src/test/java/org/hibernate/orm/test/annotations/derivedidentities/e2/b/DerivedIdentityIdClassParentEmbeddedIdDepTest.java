/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e2.b;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.orm.test.util.SchemaUtil;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Employee.class,
				Dependent.class

		}
)
@SessionFactory
public class DerivedIdentityIdClassParentEmbeddedIdDepTest {

	@Test
	public void testManyToOne(SessionFactoryScope scope) {
		final MetadataImplementor metadata = scope.getMetadataImplementor();
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "emp_firstName", metadata ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "emp_lastName", metadata ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "name", metadata ) );
		assertTrue( !SchemaUtil.isColumnPresent( "Dependent", "firstName", metadata ) );
		assertTrue( !SchemaUtil.isColumnPresent( "Dependent", "lastName", metadata ) );

		Employee e = new Employee();
		e.firstName = "Emmanuel";
		e.lastName = "Bernard";

		scope.inTransaction(
				session -> {
					session.persist( e );
					Dependent d = new Dependent();
					d.emp = e;
					d.id = new DependentId();
					d.id.name = "Doggy";
					session.persist( d );
					session.flush();
					session.clear();
					d = session.get( Dependent.class, d.id );
					assertNotNull( d.emp );
					assertEquals( e.firstName, d.emp.firstName );
				}
		);
	}
}
