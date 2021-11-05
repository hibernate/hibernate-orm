/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.orm.test.annotations.derivedidentities.e3.b2;

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
public class DerivedIdentityEmbeddedIdParentEmbeddedIdGrandparentEmbeddedIdDepTest {
	@Test
	public void testManyToOne(SessionFactoryScope scope) {
		final MetadataImplementor metadata = scope.getMetadataImplementor();
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "emp_firstName", metadata ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "emp_lastName", metadata ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "name", metadata ) );
		assertTrue( !SchemaUtil.isColumnPresent( "Dependent", "firstName", metadata ) );
		assertTrue( !SchemaUtil.isColumnPresent( "Dependent", "lastName", metadata ) );

		assertTrue( SchemaUtil.isColumnPresent( "Policy", "dep_emp_firstName", metadata ) );
		assertTrue( SchemaUtil.isColumnPresent( "Policy", "dep_emp_lastName", metadata ) );
		assertTrue( SchemaUtil.isColumnPresent( "Policy", "type", metadata ) );
		assertTrue( !SchemaUtil.isColumnPresent( "Policy", "firstName", metadata ) );
		assertTrue( !SchemaUtil.isColumnPresent( "Policy", "lastName", metadata ) );
		assertTrue( !SchemaUtil.isColumnPresent( "Policy", "name", metadata ) );


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
