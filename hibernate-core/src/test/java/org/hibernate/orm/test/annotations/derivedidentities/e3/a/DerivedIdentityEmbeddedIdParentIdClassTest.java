/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e3.a;

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
				Dependent.class,
				Employee.class
		}
)
@SessionFactory
public class DerivedIdentityEmbeddedIdParentIdClassTest {

	@Test
	public void testManyToOne(SessionFactoryScope scope) {
		final MetadataImplementor metadata = scope.getMetadataImplementor();
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "FK1", metadata ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "FK2", metadata ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "dep_name", metadata ) );
		assertTrue( !SchemaUtil.isColumnPresent( "Dependent", "firstName", metadata ) );
		assertTrue( !SchemaUtil.isColumnPresent( "Dependent", "lastName", metadata ) );

		Employee e = new Employee();
		e.empId = new EmployeeId();
		e.empId.firstName = "Emmanuel";
		e.empId.lastName = "Bernard";

		scope.inTransaction(
				session -> {
					session.persist( e );
					Dependent d = new Dependent();
					d.emp = e;
					d.name = "Doggy";
					DependentId dId = new DependentId();
					dId.emp = new EmployeeId();
					dId.emp.firstName = e.empId.firstName;
					dId.emp.lastName = e.empId.lastName;
					dId.name = d.name;
					session.persist( d );

					session.flush();
					session.clear();

					d = session.get( Dependent.class, dId );
					assertNotNull( d.emp );
					assertEquals( e.empId.firstName, d.emp.empId.firstName );
					session.remove( d );
					session.remove( d.emp );
				}
		);
	}
}
