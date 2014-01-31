/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.derivedidentities.bidirectional;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.test.util.SchemaUtil;
import org.hibernate.testing.Skip;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
@Skip( condition = Skip.AlwaysSkip.class,message = "sdf")
public class DerivedIdentityWithBidirectionalAssociationTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testBidirectionalAssociation() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "emp_empId", metadata() ) );
		assertTrue( !SchemaUtil.isColumnPresent( "Dependent", "empPK", metadata() ) );

		Employee e = new Employee();
		e.empId = 1;
		e.empName = "Emmanuel";

		Session s = openSession();
		s.getTransaction().begin();
		s.persist( e );
		Dependent d = new Dependent();
		d.emp = e;
		s.persist( d );
		s.flush();
		s.clear();
		d = getDerivedClassById( e, s, Dependent.class );
		assertEquals( e.empId, d.emp.empId );
		s.getTransaction().rollback();
		s.close();
	}

	private <T> T getDerivedClassById(Employee e, Session s, Class<T> clazz) {
		return clazz.cast( s.createQuery( "from " + clazz.getName() + " d where d.emp.empId = :empId" )
						.setParameter( "empId", e.empId ).uniqueResult() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Dependent.class,
				Employee.class
		};
	}
}
