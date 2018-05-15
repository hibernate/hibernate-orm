/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql.joinedSubclass;

import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.junit.Test;

import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Stephen Fikes
 * @author Gail Badner
 */
public class JoinedSubclassSubQueryTest extends BaseCoreFunctionalTestCase {

	@Test
	@TestForIssue(jiraKey = "HHH-11182")
	public void testSubQueryConstraintPropertyInSuperclassTable() {

		Session s = openSession();
		try {
			s.getTransaction().begin();
			// employee.firstName is in Person table (not Employee)
			String queryHQL = "from InvestmentCompany investmentCompany "
					+ "where exists "
					+ "(select employee "
					+ "from investmentCompany.employees as employee "
					+ "  where employee.firstName = 'Joe')";
			s.createQuery( queryHQL ).uniqueResult();
			s.getTransaction().commit();
		}
		catch (Exception e) {
			if ( s.getTransaction() != null && s.getTransaction().isActive() ) {
				s.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			s.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11182")
	public void testSubQueryConstraintPropertyInEntityTable() {

		Session s = openSession();
		try {
			s.getTransaction().begin();
			// employee.employeeNumber is in Employee table
			String queryHQL = "from InvestmentCompany investmentCompany "
					+ "where exists "
					+ "(select employee "
					+ "from investmentCompany.employees as employee "
					+ "  where employee.employeeNumber = 666 )";
			s.createQuery( queryHQL ).uniqueResult();
		}
		catch (Exception e) {
			if ( s.getTransaction() != null && s.getTransaction().isActive() ) {
				s.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			s.close();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				InvestmentCompany.class,
				Person.class,
				Employee.class
		};
	}

	@Entity(name = "InvestmentCompany")
	@Table
	public static class InvestmentCompany {
		@Id
		@GeneratedValue
		@Column
		private Long id;

		@Basic(optional = false)
		@Column(nullable = false, length = 255)
		private String fullName;

		@Basic(optional = false)
		@Column(nullable = false, length = 16)
		private String shortName;

		@OneToMany(mappedBy = "company")
		List<Employee> employees;
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class Person {
		@Id
		@GeneratedValue
		@Column
		private Long id;

		@Basic(optional = false)
		@Column(nullable = false)
		private String firstName;
	}

	@Entity(name = "Employee")
	public static class Employee extends Person {
		@Id
		@GeneratedValue
		@Column
		private Long id;

		private int employeeNumber;

		@ManyToOne(optional = false)
		@JoinColumn(nullable = false)
		private InvestmentCompany company;
	}
}
