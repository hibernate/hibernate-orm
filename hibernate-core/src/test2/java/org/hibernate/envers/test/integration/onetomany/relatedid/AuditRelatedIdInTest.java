/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.onetomany.relatedid;

import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.tools.TestTools;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-8070")
public class AuditRelatedIdInTest extends BaseEnversJPAFunctionalTestCase {
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

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getOrCreateEntityManager();
		try {
			// Revision 1
			Company company1 = new Company( "COMPANY1" );
			Company company2 = new Company( "COMPANY2" );
			Employee employee1 = new Employee( "Employee1", company1 );
			Employee employee2 = new Employee( "Employee2", company2 );
			Employee employee3 = new Employee( "Employee3", company2 );
			em.getTransaction().begin();
			em.persist( company1 );
			em.persist( company2 );
			em.persist( employee1 );
			em.persist( employee2 );
			em.persist( employee3 );
			em.getTransaction().commit();

			// cache ids
			company1Id = company1.getId();
			company2Id = company2.getId();
			employee1Id = employee1.getId();
			employee2Id = employee2.getId();
			employee3Id = employee3.getId();

			// Revision 2
			em.getTransaction().begin();
			employee2 = em.find( Employee.class, employee2.getId() );
			employee2.setCompany( company1 );
			company2 = em.find( Company.class, company2.getId() );
			company2.setName( "COMPANY2-CHANGED" );
			em.merge( employee2 );
			em.merge( company2 );
			em.getTransaction().commit();

			// Revision 3
			Company company3 = new Company( "COMPANY3" );
			Employee employee4 = new Employee( "Employee4", company3 );
			em.getTransaction().begin();
			em.persist( company3 );
			em.persist( employee4 );
			em.getTransaction().commit();

			// cache ids
			employee4Id = employee4.getId();
			company3Id = company3.getId();
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testRevisionCounts() {
		// companies
		assertEquals( 1, getAuditReader().getRevisions( Company.class, company1Id ).size() );
		assertEquals( 2, getAuditReader().getRevisions( Company.class, company2Id ).size() );
		assertEquals( 1, getAuditReader().getRevisions( Company.class, company3Id ).size() );
		// employees
		assertEquals( 1, getAuditReader().getRevisions( Employee.class, employee1Id ).size() );
		assertEquals( 2, getAuditReader().getRevisions( Employee.class, employee2Id ).size() );
		assertEquals( 1, getAuditReader().getRevisions( Employee.class, employee3Id ).size() );
		assertEquals( 1, getAuditReader().getRevisions( Employee.class, employee4Id ).size() );
	}

	@Test
	public void testCompany1EmployeeIn() {
		AuditQuery auditQuery = getAuditReader().createQuery().forRevisionsOfEntity( Employee.class, true, true );
		auditQuery.add( AuditEntity.relatedId( "company" ).in( new Integer[]{ company1Id } ) );
		final List<Employee> results = auditQuery.getResultList();
		assertEquals( 2, results.size() );
		final Employee employee1 = makeEmployee( employee1Id, "Employee1", company1Id, "COMPANY1" );
		final Employee employee2 = makeEmployee( employee2Id, "Employee2", company1Id, "COMPANY1" );
		assertThat( results.contains( employee1 ), is(true) );
		assertThat( results.contains( employee2 ), is(true) );
	}

	@Test
	public void testCompany2EmployeeIn() {
		AuditQuery auditQuery = getAuditReader().createQuery().forRevisionsOfEntity( Employee.class, true, true );
		auditQuery.add( AuditEntity.relatedId( "company" ).in( new Integer[]{ company2Id } ) );
		final List<Employee> results = auditQuery.getResultList();
		assertEquals( 2, results.size() );

		final Employee employee1 = makeEmployee( employee2Id, "Employee2", company2Id, "COMPANY2" );
		final Employee employee2 = makeEmployee( employee3Id, "Employee3", company2Id, "COMPANY2" );
		assertThat( results.contains( employee1 ), is(true) );
		assertThat( results.contains( employee2 ), is(true) );
	}

	@Test
	public void testCompany3EmployeeIn() {
		AuditQuery auditQuery = getAuditReader().createQuery().forRevisionsOfEntity( Employee.class, true, true );
		auditQuery.add( AuditEntity.relatedId( "company" ).in( new Integer[]{ company3Id } ) );
		final List<Employee> results = auditQuery.getResultList();
		assertEquals( 1, results.size() );
		final Employee employee = makeEmployee( employee4Id, "Employee4", company3Id, "COMPANY3" );
		assertEquals( results, TestTools.makeList( employee ) );
	}

	private Employee makeEmployee(Integer employeeId, String employeeName, Integer companyId, String companyName) {
		return new Employee( employeeId, employeeName, new Company( companyId, companyName ) );
	}
}
