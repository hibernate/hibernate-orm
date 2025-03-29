/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e1.c;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.orm.test.util.SchemaUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class DerivedIdentitySimpleParentEmbeddedDepTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testManyToOne() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "emp_empId", metadata() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "Dependent", "empPK", metadata() ) );
		Employee e = new Employee();
		e.empId = 1;
		e.empName = "Emmanuel";
		Session s = openSession(  );
		s.getTransaction().begin();
		s.persist( e );
		Dependent d = new Dependent();
		d.emp = e;
		d.name = "Doggy";
		s.persist( d );
		s.flush();
		s.clear();
		d = getDerivedClassById( e, d.name, s );
		assertEquals( e.empId, d.emp.empId );
		s.getTransaction().rollback();
		s.close();
	}

	private Dependent getDerivedClassById(Employee e, String name, Session s) {
		final String qry = "from Dependent d where d.name = :name and d.emp.empId = :empId";

		return s.createQuery( qry, Dependent.class )
				.setParameter( "empId", e.empId )
				.setParameter( "name", name )
				.uniqueResult();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Dependent.class,
				Employee.class
		};
	}
}
