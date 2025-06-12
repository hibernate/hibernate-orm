/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.basic;

import org.hibernate.Hibernate;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Gavin King
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry
@DomainModel(xmlMappings = "org/hibernate/orm/test/component/basic/Employee.xml")
@SessionFactory(generateStatistics = true)
public class Component2Test {

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testComponentQueries(SessionFactoryScope factoryScope) {
		final StatisticsImplementor statistics = factoryScope.getSessionFactory().getStatistics();
		statistics.clear();

		factoryScope.inTransaction(
				s -> {
					Employee emp = new Employee();
					emp.setHireDate( new Date() );
					emp.setPerson( new Person() );
					emp.getPerson().setName( "steve" );
					emp.getPerson().setDob( new Date() );
					s.persist( emp );

					s.createQuery( "from Employee e where e.person = :p and 1 = 1 and 2=2" ).setParameter( "p", emp.getPerson() ).list();
					s.createQuery( "from Employee e where :p = e.person" ).setParameter( "p", emp.getPerson() ).list();
					s.createQuery( "from Employee e where e.person = ('', '', current_timestamp, 0.0, 'steve', '', 0)" ).list();
				}
		);
	}

	@Test
	@RequiresDialect( value = SybaseASEDialect.class )
	@FailureExpected( jiraKey = "HHH-3150" )
	public void testComponentQueryMethodNoParensFailureExpected(SessionFactoryScope factoryScope) {
		final StatisticsImplementor statistics = factoryScope.getSessionFactory().getStatistics();
		statistics.clear();

		// Sybase should translate "current_timestamp" in HQL to "getdate()";
		// This fails currently due to HHH-3510. The following test should be
		// deleted and testComponentQueries() should be updated (as noted
		// in that test case) when HHH-3510 is fixed.
		factoryScope.inTransaction(
				s -> {
					Employee emp = new Employee();
					emp.setHireDate( new Date() );
					emp.setPerson( new Person() );
					emp.getPerson().setName( "steve" );
					emp.getPerson().setDob( new Date() );
					s.persist( emp );
					s.createQuery( "from Employee e where e.person = (current_timestamp, 'steve')" ).list();
				}
		);
	}

	@Test
	public void testMergeComponent(SessionFactoryScope factoryScope) {
		Long empId = factoryScope.fromTransaction(
				s -> {
					Employee e = new Employee();
					e.setHireDate( new Date() );
					e.setPerson( new Person() );
					e.getPerson().setName( "steve" );
					e.getPerson().setDob( new Date() );
					s.persist( e );
					return e.getId();
				}
		);

		Employee emp = factoryScope.fromTransaction(
				s -> s.find( Employee.class, empId )
		);

		assertNull(emp.getOptionalComponent());
		emp.setOptionalComponent( new OptionalComponent() );
		emp.getOptionalComponent().setValue1( "emp-value1" );
		emp.getOptionalComponent().setValue2( "emp-value2" );

		factoryScope.inTransaction(
				s -> s.merge( emp )
		);

		Employee emp2 = factoryScope.fromTransaction(
				s -> s.find( Employee.class, empId )
		);

		assertEquals("emp-value1", emp2.getOptionalComponent().getValue1());
		assertEquals("emp-value2", emp2.getOptionalComponent().getValue2());
		emp2.getOptionalComponent().setValue1( null );
		emp2.getOptionalComponent().setValue2( null );

		factoryScope.inTransaction(
				s -> s.merge( emp2 )
		);

		Employee emp3 = factoryScope.fromTransaction(
				s -> {
					Employee _emp = s.find( Employee.class, empId );
					Hibernate.initialize(_emp.getDirectReports());
					return _emp;
				}
		);

		assertNull(emp3.getOptionalComponent());

		Employee dr = new Employee();
		dr.setHireDate( new Date() );
		dr.setPerson( new Person() );
		dr.getPerson().setName( "bozo" );
		dr.getPerson().setDob( new Date() );
		emp3.getDirectReports().add( dr );

		factoryScope.inTransaction(
				s -> s.merge( emp3 )
		);

		Employee emp4 = factoryScope.fromTransaction(
				s -> {
					Employee _emp = s.find( Employee.class, empId );
					Hibernate.initialize(_emp.getDirectReports());
					return _emp;
				}
		);

		assertEquals(1, emp4.getDirectReports().size());
		dr = (Employee)emp4.getDirectReports().iterator().next();
		assertNull( dr.getOptionalComponent() );
		dr.setOptionalComponent( new OptionalComponent() );
		dr.getOptionalComponent().setValue1( "dr-value1" );
		dr.getOptionalComponent().setValue2( "dr-value2" );

		factoryScope.inTransaction(
				s -> s.merge( emp4 )
		);

		Employee emp5 = factoryScope.fromTransaction(
				s -> {
					Employee _emp = s.find( Employee.class, empId );
					Hibernate.initialize(_emp.getDirectReports());
					return _emp;
				}
		);

		assertEquals(1, emp5.getDirectReports().size());
		dr = (Employee)emp5.getDirectReports().iterator().next();
		assertEquals( "dr-value1", dr.getOptionalComponent().getValue1());
		assertEquals( "dr-value2", dr.getOptionalComponent().getValue2());
		dr.getOptionalComponent().setValue1( null );
		dr.getOptionalComponent().setValue2( null );

		factoryScope.inTransaction(
				s -> s.merge( emp5 )
		);

		Employee emp6 = factoryScope.fromTransaction(
				s -> {
					Employee _emp = s.find( Employee.class, empId );
					Hibernate.initialize(_emp.getDirectReports());
					return _emp;
				}
		);

		assertEquals(1, emp6.getDirectReports().size());
		dr = (Employee)emp6.getDirectReports().iterator().next();
		assertNull(dr.getOptionalComponent());
	}
}
