/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import org.hibernate.Session;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.test.annotations.query.Attrset;
import org.hibernate.test.annotations.query.Attrvalue;
import org.hibernate.test.annotations.query.Employee;
import org.hibernate.test.annotations.query.Employeegroup;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class DeleteWithSubqueryTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Attrset.class,
				Attrvalue.class,
				Employee.class,
				Employeegroup.class,
				Panel.class,
				TrtPanel.class
		};
	}

	@Test
	@TestForIssue( jiraKey = "HHH-8318" )
	@SkipForDialect( value = MySQLDialect.class, comment = "Cannot use Attrvalue in the delete and from clauses simultaneously." )
	public void testDeleteMemberOf() {
		final String qry = "delete Attrvalue aval where aval.id in ( "
				+ "select val2.id from Employee e, Employeegroup eg, Attrset aset, Attrvalue val2 "
				+ "where eg.id = e.employeegroup.id " + "and aset.id = e.attrset.id "
				+ "and val2 member of aset.attrvalues)";
		Session s = openSession();
		s.getTransaction().begin();
		s.createQuery( qry ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}
	
	@Test
	@TestForIssue( jiraKey = "HHH-8447" )
	public void testDeleteMultipleWhereIns() {
		Session s = openSession();
		s.getTransaction().begin();
        s.createQuery("DELETE FROM Panel panelEntity WHERE " +
		        " panelEntity.clientId IN ( SELECT trtPanel.clientId FROM TrtPanel trtPanel ) " +
		        " AND panelEntity.deltaStamp NOT IN ( SELECT trtPanel.deltaStamp FROM TrtPanel trtPanel )").executeUpdate();
        s.getTransaction().commit();
        s.close();
	}
}
