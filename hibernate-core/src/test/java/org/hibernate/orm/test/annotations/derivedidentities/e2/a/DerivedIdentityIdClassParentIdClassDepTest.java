/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e2.a;

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
public class DerivedIdentityIdClassParentIdClassDepTest {

	@Test
	public void testManyToOne(SessionFactoryScope scope) {
		final MetadataImplementor metadata = scope.getMetadataImplementor();
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "FK1", metadata ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "FK2", metadata ) );
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
					d.name = "Doggy";
					session.persist( d );
					session.flush();
					session.clear();

					DependentId dId = new DependentId();
					EmployeeId eId = new EmployeeId();
					dId.name = d.name;
					dId.emp = eId;
					eId.firstName = e.firstName;
					eId.lastName = e.lastName;
					d = session.get( Dependent.class, dId );
					assertNotNull( d.emp );
					assertEquals( e.firstName, d.emp.firstName );

					session.remove( d );
					session.remove( d.emp );
				}
		);
	}
}
