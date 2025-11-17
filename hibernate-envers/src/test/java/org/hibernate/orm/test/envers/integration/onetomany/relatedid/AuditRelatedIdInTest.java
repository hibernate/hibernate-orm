/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.relatedid;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-8070")
@EnversTest
@Jpa(annotatedClasses = {Company.class, Employee.class})
public class AuditRelatedIdInTest {
	private Integer company1Id;
	private Integer company2Id;
	private Integer company3Id;
	private Integer employee1Id;
	private Integer employee2Id;
	private Integer employee3Id;
	private Integer employee4Id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// Revision 1
			Company company1 = new Company( "COMPANY1" );
			Company company2 = new Company( "COMPANY2" );
			Employee employee1 = new Employee( "Employee1", company1 );
			Employee employee2 = new Employee( "Employee2", company2 );
			Employee employee3 = new Employee( "Employee3", company2 );
			em.persist( company1 );
			em.persist( company2 );
			em.persist( employee1 );
			em.persist( employee2 );
			em.persist( employee3 );

			// cache ids
			company1Id = company1.getId();
			company2Id = company2.getId();
			employee1Id = employee1.getId();
			employee2Id = employee2.getId();
			employee3Id = employee3.getId();
		} );

		scope.inTransaction( em -> {
			// Revision 2
			Employee employee2 = em.find( Employee.class, employee2Id );
			employee2.setCompany( em.find( Company.class, company1Id ) );
			Company company2 = em.find( Company.class, company2Id );
			company2.setName( "COMPANY2-CHANGED" );
			em.merge( employee2 );
			em.merge( company2 );
		} );

		scope.inTransaction( em -> {
			// Revision 3
			Company company3 = new Company( "COMPANY3" );
			Employee employee4 = new Employee( "Employee4", company3 );
			em.persist( company3 );
			em.persist( employee4 );

			// cache ids
			employee4Id = employee4.getId();
			company3Id = company3.getId();
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			// companies
			assertEquals( 1, auditReader.getRevisions( Company.class, company1Id ).size() );
			assertEquals( 2, auditReader.getRevisions( Company.class, company2Id ).size() );
			assertEquals( 1, auditReader.getRevisions( Company.class, company3Id ).size() );
			// employees
			assertEquals( 1, auditReader.getRevisions( Employee.class, employee1Id ).size() );
			assertEquals( 2, auditReader.getRevisions( Employee.class, employee2Id ).size() );
			assertEquals( 1, auditReader.getRevisions( Employee.class, employee3Id ).size() );
			assertEquals( 1, auditReader.getRevisions( Employee.class, employee4Id ).size() );
		} );
	}

	@Test
	public void testCompany1EmployeeIn(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			AuditQuery auditQuery = auditReader.createQuery().forRevisionsOfEntity( Employee.class, true, true );
			auditQuery.add( AuditEntity.relatedId( "company" ).in( new Integer[]{ company1Id } ) );
			final List<Employee> results = auditQuery.getResultList();
			assertEquals( 2, results.size() );
			final Employee employee1 = makeEmployee( employee1Id, "Employee1", company1Id, "COMPANY1" );
			final Employee employee2 = makeEmployee( employee2Id, "Employee2", company1Id, "COMPANY1" );
			assertTrue( results.contains( employee1 ) );
			assertTrue( results.contains( employee2 ) );
		} );
	}

	@Test
	public void testCompany2EmployeeIn(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			AuditQuery auditQuery = auditReader.createQuery().forRevisionsOfEntity( Employee.class, true, true );
			auditQuery.add( AuditEntity.relatedId( "company" ).in( new Integer[]{ company2Id } ) );
			final List<Employee> results = auditQuery.getResultList();
			assertEquals( 2, results.size() );

			final Employee employee1 = makeEmployee( employee2Id, "Employee2", company2Id, "COMPANY2" );
			final Employee employee2 = makeEmployee( employee3Id, "Employee3", company2Id, "COMPANY2" );
			assertTrue( results.contains( employee1 ) );
			assertTrue( results.contains( employee2 ) );
		} );
	}

	@Test
	public void testCompany3EmployeeIn(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			AuditQuery auditQuery = auditReader.createQuery().forRevisionsOfEntity( Employee.class, true, true );
			auditQuery.add( AuditEntity.relatedId( "company" ).in( new Integer[]{ company3Id } ) );
			final List<Employee> results = auditQuery.getResultList();
			assertEquals( 1, results.size() );
			final Employee employee = makeEmployee( employee4Id, "Employee4", company3Id, "COMPANY3" );
			assertEquals( TestTools.makeList( employee ), results );
		} );
	}

	private Employee makeEmployee(Integer employeeId, String employeeName, Integer companyId, String companyName) {
		return new Employee( employeeId, employeeName, new Company( companyId, companyName ) );
	}
}
