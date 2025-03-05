/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e3.b3;

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
 * @author Matt Drees
 */
@DomainModel(
		annotatedClasses = { Policy.class, Dependent.class, Employee.class }
)
@SessionFactory
public class DerivedIdentityEmbeddedIdParentEmbeddedIdGrandparentEmbeddedIdColumnOverridesDepTest {

	@Test
	public void testManyToOne(SessionFactoryScope scope) {
		final MetadataImplementor metadata = scope.getMetadataImplementor();
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "FIRSTNAME", metadata ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "LASTNAME", metadata ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "name", metadata ) );

		assertTrue( SchemaUtil.isColumnPresent( "Policy", "FIRSTNAME", metadata ) );
		assertTrue( SchemaUtil.isColumnPresent( "Policy", "LASTNAME", metadata ) );
		assertTrue( SchemaUtil.isColumnPresent( "Policy", "NAME", metadata ) );
		assertTrue( SchemaUtil.isColumnPresent( "Policy", "type", metadata ) );


		final Employee e = new Employee();
		e.empId = new EmployeeId();
		e.empId.firstName = "Emmanuel";
		e.empId.lastName = "Bernard";

		scope.inTransaction(
				session -> {
					session.persist( e );
					final Dependent d = new Dependent();
					d.emp = e;
					d.id = new DependentId();
					d.id.name = "Doggy";
					session.persist( d );
					Policy p = new Policy();
					p.dep = d;
					p.id = new PolicyId();
					p.id.type = "Vet Insurance";
					session.persist( p );

					session.flush();
					session.clear();
					p = session.get( Policy.class, p.id );

					assertNotNull( p.dep );
					assertEquals( e.empId.firstName, p.dep.emp.empId.firstName );
				}
		);
	}

}
