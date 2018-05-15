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
package org.hibernate.test.annotations.derivedidentities.e3.b3;

import org.hibernate.Session;
import org.hibernate.test.util.SchemaUtil;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Emmanuel Bernard
 * @author Matt Drees
 */
public class DerivedIdentityEmbeddedIdParentEmbeddedIdGrandparentEmbeddedIdColumnOverridesDepTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testManyToOne() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "FIRSTNAME", metadata() ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "LASTNAME", metadata() ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "name", metadata() ) );

		assertTrue( SchemaUtil.isColumnPresent( "Policy", "FIRSTNAME", metadata() ) );
		assertTrue( SchemaUtil.isColumnPresent( "Policy", "LASTNAME", metadata() ) );
		assertTrue( SchemaUtil.isColumnPresent( "Policy", "NAME", metadata() ) );
		assertTrue( SchemaUtil.isColumnPresent( "Policy", "type", metadata() ) );


		final Employee e = new Employee();
		e.empId = new EmployeeId();
		e.empId.firstName = "Emmanuel";
		e.empId.lastName = "Bernard";
		final Session s = openSession();
		s.getTransaction().begin();
		s.persist( e );
		final Dependent d = new Dependent();
		d.emp = e;
		d.id = new DependentId();
		d.id.name = "Doggy";
		s.persist( d );
		Policy p = new Policy();
		p.dep = d;
		p.id = new PolicyId();
		p.id.type = "Vet Insurance";
		s.persist( p );

		s.flush();
		s.clear();
		p = (Policy) s.get( Policy.class, p.id );
		assertNotNull( p.dep );
		assertEquals( e.empId.firstName, p.dep.emp.empId.firstName );
		s.getTransaction().rollback();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{Policy.class, Dependent.class, Employee.class};
	}
}
