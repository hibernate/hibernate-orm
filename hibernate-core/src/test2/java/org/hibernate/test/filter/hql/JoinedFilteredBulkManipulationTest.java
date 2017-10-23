/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.filter.hql;
import java.util.Date;

import org.hibernate.dialect.CUBRIDDialect;
import org.hibernate.testing.SkipForDialect;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@SkipForDialect(
        value = CUBRIDDialect.class,
        comment = "As of verion 8.4.1 CUBRID doesn't support temporary tables. This test fails with" +
                "HibernateException: cannot doAfterTransactionCompletion multi-table deletes using dialect not supporting temp tables"
)
public class JoinedFilteredBulkManipulationTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] {
			"filter/hql/filter-defs.hbm.xml",
			"filter/hql/Joined.hbm.xml"
		};
	}

	@Test
	public void testFilteredJoinedSubclassHqlDeleteRoot() {
		Session s = openSession();
		s.beginTransaction();
		s.save( new Employee( "John", 'M', "john", new Date() ) );
		s.save( new Employee( "Jane", 'F', "jane", new Date() ) );
		s.save( new Customer( "Charlie", 'M', "charlie", "Acme" ) );
		s.save( new Customer( "Wanda", 'F', "wanda", "ABC" ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.enableFilter( "sex" ).setParameter( "sexCode", Character.valueOf( 'M' ) );
		int count = s.createQuery( "delete Person" ).executeUpdate();
		assertEquals( 2, count );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete Person" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testFilteredJoinedSubclassHqlDeleteNonLeaf() {
		Session s = openSession();
		s.beginTransaction();
		s.save( new Employee( "John", 'M', "john", new Date() ) );
		s.save( new Employee( "Jane", 'F', "jane", new Date() ) );
		s.save( new Customer( "Charlie", 'M', "charlie", "Acme" ) );
		s.save( new Customer( "Wanda", 'F', "wanda", "ABC" ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.enableFilter( "sex" ).setParameter( "sexCode", Character.valueOf( 'M' ) );
		int count = s.createQuery( "delete User" ).executeUpdate();
		assertEquals( 2, count );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete Person" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testFilteredJoinedSubclassHqlDeleteLeaf() {
		Session s = openSession();
		s.beginTransaction();
		s.save( new Employee( "John", 'M', "john", new Date() ) );
		s.save( new Employee( "Jane", 'F', "jane", new Date() ) );
		s.save( new Customer( "Charlie", 'M', "charlie", "Acme" ) );
		s.save( new Customer( "Wanda", 'F', "wanda", "ABC" ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.enableFilter( "sex" ).setParameter( "sexCode", Character.valueOf( 'M' ) );
		int count = s.createQuery( "delete Employee" ).executeUpdate();
		assertEquals( 1, count );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete Person" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testFilteredJoinedSubclassHqlUpdateRoot() {
		Session s = openSession();
		s.beginTransaction();
		s.save( new Employee( "John", 'M', "john", new Date() ) );
		s.save( new Employee( "Jane", 'F', "jane", new Date() ) );
		s.save( new Customer( "Charlie", 'M', "charlie", "Acme" ) );
		s.save( new Customer( "Wanda", 'F', "wanda", "ABC" ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.enableFilter( "sex" ).setParameter( "sexCode", Character.valueOf( 'M' ) );
		int count = s.createQuery( "update Person p set p.name = '<male>'" ).executeUpdate();
		assertEquals( 2, count );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete Person" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testFilteredJoinedSubclassHqlUpdateNonLeaf() {
		Session s = openSession();
		s.beginTransaction();
		s.save( new Employee( "John", 'M', "john", new Date() ) );
		s.save( new Employee( "Jane", 'F', "jane", new Date() ) );
		s.save( new Customer( "Charlie", 'M', "charlie", "Acme" ) );
		s.save( new Customer( "Wanda", 'F', "wanda", "ABC" ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.enableFilter( "sex" ).setParameter( "sexCode", Character.valueOf( 'M' ) );
		int count = s.createQuery( "update User u set u.username = :un where u.name = :n" )
				.setString( "un", "charlie" )
				.setString( "n", "Wanda" )
				.executeUpdate();
		assertEquals( 0, count );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete Person" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testFilteredJoinedSubclassHqlUpdateLeaf() {
		Session s = openSession();
		s.beginTransaction();
		s.save( new Employee( "John", 'M', "john", new Date() ) );
		s.save( new Employee( "Jane", 'F', "jane", new Date() ) );
		s.save( new Customer( "Charlie", 'M', "charlie", "Acme" ) );
		s.save( new Customer( "Wanda", 'F', "wanda", "ABC" ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.enableFilter( "sex" ).setParameter( "sexCode", Character.valueOf( 'M' ) );
		int count = s.createQuery( "update Customer c set c.company = 'XYZ'" ).executeUpdate();
		assertEquals( 1, count );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete Person" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}
}
