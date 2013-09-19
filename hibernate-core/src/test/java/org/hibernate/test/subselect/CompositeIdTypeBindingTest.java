/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.subselect;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.hibernate.Session;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.SkipForDialects;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
@SkipForDialects({
		@SkipForDialect(value = H2Dialect.class, comment = "H2 doesn't support this sql syntax"),
		@SkipForDialect(value = SQLServerDialect.class, comment = "mssql doesn't support multiple columns in the 'where' clause of a 'where in' query"),
		@SkipForDialect(value = SybaseDialect.class, comment = "sybase doesn't support multiple columns in the 'where' clause of a 'where in' query")})
@TestForIssue( jiraKey = "HHH-8312")
public class CompositeIdTypeBindingTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Employee.class, EmployeeGroup.class };
	}

	@Test
	public void testCompositeTypeBinding() {
		Session session = openSession();
		session.beginTransaction();

		EmployeeGroup employeegroup = new EmployeeGroup( new EmployeeGroupId( "a", "b" ) );
		employeegroup.addEmployee( new Employee( "stliu" ) );
		employeegroup.addEmployee( new Employee( "david" ) );
		session.save( employeegroup );


		employeegroup = new EmployeeGroup( new EmployeeGroupId( "c", "d" ) );
		employeegroup.addEmployee( new Employee( "gail" ) );
		employeegroup.addEmployee( new Employee( "steve" ) );
		session.save( employeegroup );


		session.getTransaction().commit();

		session.close();

		session = openSession();

		List<EmployeeGroupId> parameters = new ArrayList<EmployeeGroupId>();
		parameters.add( new EmployeeGroupId( "a", "b" ) );
		parameters.add( new EmployeeGroupId( "c", "d" ) );
		parameters.add( new EmployeeGroupId( "e", "f" ) );

		List result = session.createQuery( "select eg from EmployeeGroup eg where eg.id in (:employeegroupIds)" )
				.setParameterList( "employeegroupIds", parameters ).list();

		Assert.assertEquals( 2, result.size() );

		employeegroup = (EmployeeGroup) result.get( 0 );

		Assert.assertEquals( "a", employeegroup.getId().getGroupName() );
		Assert.assertNotNull( employeegroup.getEmployees() );
		Assert.assertEquals( 2, employeegroup.getEmployees().size() );
		session.close();
	}
}
