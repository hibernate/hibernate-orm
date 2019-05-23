/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany.relatedid;

import java.util.List;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetomany.relatedid.Company;
import org.hibernate.envers.test.support.domains.onetomany.relatedid.Employee;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-8070")
public class AuditRelatedIdInTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer company1Id;
	private Integer company2Id;
	private Integer company3Id;
	private Integer employee1Id;
	private Integer employee2Id;
	private Integer employee3Id;
	private Integer employee4Id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{
				Company.class,
				Employee.class
		};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		// Initial objects
		Company company1 = new Company( "COMPANY1" );
		Company company2 = new Company( "COMPANY2" );
		Employee employee1 = new Employee( "Employee1", company1 );
		Employee employee2 = new Employee( "Employee2", company2 );
		Employee employee3 = new Employee( "Employee3", company2 );

		inTransactions(
				// Revision 1
				entityManager -> {
					entityManager.persist( company1 );
					entityManager.persist( company2 );
					entityManager.persist( employee1 );
					entityManager.persist( employee2 );
					entityManager.persist( employee3 );

					// cache ids
					company1Id = company1.getId();
					company2Id = company2.getId();
					employee1Id = employee1.getId();
					employee2Id = employee2.getId();
					employee3Id = employee3.getId();
				},

				// Revision 2
				entityManager -> {
					Employee employee = entityManager.find( Employee.class, employee2.getId() );
					employee.setCompany( company1 );

					Company company = entityManager.find( Company.class, company2.getId() );
					company.setName( "COMPANY2-CHANGED" );

					entityManager.merge( employee );
					entityManager.merge( company );
				},

				// Revision 3
				entityManager -> {
					Company company3 = new Company( "COMPANY3" );
					Employee employee4 = new Employee( "Employee4", company3 );

					entityManager.persist( company3 );
					entityManager.persist( employee4 );

					employee4Id = employee4.getId();
					company3Id = company3.getId();
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		// companies
		assertThat( getAuditReader().getRevisions( Company.class, company1Id ), CollectionMatchers.hasSize( 1 ) );
		assertThat( getAuditReader().getRevisions( Company.class, company2Id ), CollectionMatchers.hasSize( 2 ) );
		assertThat( getAuditReader().getRevisions( Company.class, company3Id ), CollectionMatchers.hasSize( 1 ) );

		// employees
		assertThat( getAuditReader().getRevisions( Employee.class, employee1Id ), CollectionMatchers.hasSize( 1 ) );
		assertThat( getAuditReader().getRevisions( Employee.class, employee2Id ), CollectionMatchers.hasSize( 2 ) );
		assertThat( getAuditReader().getRevisions( Employee.class, employee3Id ), CollectionMatchers.hasSize( 1 ) );
		assertThat( getAuditReader().getRevisions( Employee.class, employee4Id ), CollectionMatchers.hasSize( 1 ) );
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testCompany1EmployeeIn() {
		final Employee employee1 = makeEmployee( employee1Id, "Employee1", company1Id, "COMPANY1" );
		final Employee employee2 = makeEmployee( employee2Id, "Employee2", company1Id, "COMPANY1" );

		final List<Employee> results = (List<Employee>) getAuditReader().createQuery()
				.forRevisionsOfEntity( Employee.class, true, true )
				.add( AuditEntity.relatedId( "company" ).in( new Integer[]{ company1Id } ) )
				.getResultList();

		assertThat( results, containsInAnyOrder( employee1, employee2 ) );
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testCompany2EmployeeIn() {
		final Employee employee1 = makeEmployee( employee2Id, "Employee2", company2Id, "COMPANY2" );
		final Employee employee2 = makeEmployee( employee3Id, "Employee3", company2Id, "COMPANY2" );

		final List<Employee> results = (List<Employee>) getAuditReader().createQuery()
				.forRevisionsOfEntity( Employee.class, true, true )
				.add( AuditEntity.relatedId( "company" ).in( new Integer[]{ company2Id } ) )
				.getResultList();

		assertThat( results, containsInAnyOrder( employee1, employee2 ) );
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testCompany3EmployeeIn() {
		final Employee employee = makeEmployee( employee4Id, "Employee4", company3Id, "COMPANY3" );

		final List<Employee> results = (List<Employee>) getAuditReader().createQuery()
				.forRevisionsOfEntity( Employee.class, true, true )
				.add( AuditEntity.relatedId( "company" ).in( new Integer[] { company3Id } ) )
				.getResultList();

		assertThat( results, contains( employee ) );
	}

	private Employee makeEmployee(Integer employeeId, String employeeName, Integer companyId, String companyName) {
		return new Employee( employeeId, employeeName, new Company( companyId, companyName ) );
	}
}
