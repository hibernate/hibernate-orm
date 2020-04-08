/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.subselect;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:stliu@hibernate.org">Strong Liu</a>
 */
@SkipForDialect(value = H2Dialect.class, comment = "H2 doesn't support this sql syntax")
@SkipForDialect(value = SQLServerDialect.class, comment = "mssql doesn't support multiple columns in the 'where' clause of a 'where in' query")
@SkipForDialect(value = SybaseDialect.class, comment = "sybase doesn't support multiple columns in the 'where' clause of a 'where in' query")
@SkipForDialect(value = DerbyDialect.class, comment = "derby doesn't support multiple columns in the 'where' clause of a 'where in' query")
@TestForIssue( jiraKey = "HHH-8312")
public class CompositeIdTypeBindingTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Employee.class, EmployeeGroup.class };
	}

	@Test
	public void testCompositeTypeBinding() {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// prepare test data
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

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Perform the test
		session = openSession();
		session.beginTransaction();

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
		session.getTransaction().commit();
		session.close();


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// clean up test data
		session = openSession();
		session.beginTransaction();
		List<EmployeeGroup> egs = session.createQuery( "from EmployeeGroup" ).list();
		for ( EmployeeGroup eg : egs ) {
			eg.getEmployees().clear();
		}
		session.flush();
		session.createQuery( "delete from EmployeeGroup" ).executeUpdate();
		session.createQuery( "delete from Employee" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}
}
