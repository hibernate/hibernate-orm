/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.group;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testing behavior of {@linkplain org.hibernate.annotations.LazyGroup}
 * on the inverse side of a one-to-one association.
 *
 * @author Steve Ebersole
 * @author Jan-Oliver Lustig
 */
@DomainModel(annotatedClasses = {
		LazyGroupOneToOneMappedByTests.Employee.class,
		LazyGroupOneToOneMappedByTests.EmploymentDetails.class
})
@SessionFactory(useCollectingStatementInspector = true)
@BytecodeEnhanced
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-11986")
public class LazyGroupOneToOneMappedByTests {
	@Test
	void testLazyGroupBehavior(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
		sqlCollector.clear();

		factoryScope.inTransaction( (session) -> {
			final EmploymentDetails employment = session.find( EmploymentDetails.class, 1001 );
			checkInitializationState( employment, false, false );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " from employments " );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).doesNotContain( " employees " );

			sqlCollector.clear();

			// access `costCenter` which is in the default lazy-group
			//noinspection ResultOfMethodCallIgnored
			employment.getCostCenter();
			checkInitializationState( employment, true, false );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " from employments " );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).doesNotContain( " employees " );

			sqlCollector.clear();

			// access `contract` which is in the special lazy-group
			//noinspection ResultOfMethodCallIgnored
			employment.getContract();
			checkInitializationState( employment, true, true );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " from employments " );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " join employees " );
		} );
	}

	private void checkInitializationState(
			EmploymentDetails employment,
			boolean defaultLazyGroupInitialized,
			boolean specialLazyGroupInitialized) {
		assertThat( Hibernate.isPropertyInitialized( employment, "id" ) ).isTrue();
		assertThat( Hibernate.isPropertyInitialized( employment, "startDate" ) ).isTrue();
		assertThat( Hibernate.isPropertyInitialized( employment, "endDate" ) ).isTrue();

		assertThat( Hibernate.isPropertyInitialized( employment, "costCenter" ) ).isEqualTo( defaultLazyGroupInitialized );

		assertThat( Hibernate.isPropertyInitialized( employment, "contract" ) ).isEqualTo( specialLazyGroupInitialized );
		assertThat( Hibernate.isPropertyInitialized( employment, "employee" ) ).isEqualTo( specialLazyGroupInitialized );

	}

	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			final Employee steve = new Employee( 1, "Steve" );
			steve.createEmployment(
					1001,
					"I solemnly swear I am up to no good",
					"ENG"
			);
			session.persist( steve );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Entity(name="Employee")
	@Table(name="employees")
	public static class Employee {
		@Id
		private Integer id;
		private String name;
		@OneToOne(cascade = CascadeType.ALL)
		@JoinColumn(name = "employment_fk")
		private EmploymentDetails employmentDetails;

		public Employee() {
		}

		public Employee(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public EmploymentDetails getEmploymentDetails() {
			return employmentDetails;
		}

		public void createEmployment(Integer id, String contract, String costCenter) {
			if ( employmentDetails != null ) {
				employmentDetails.endDate = Instant.now();
			}
			employmentDetails = new EmploymentDetails( id, Instant.now(), contract, costCenter, this );
		}

		public void setEmploymentDetails(EmploymentDetails employmentDetails) {
			this.employmentDetails = employmentDetails;
		}
	}

	@Entity(name="EmploymentDetails")
	@Table(name="employments")
	public static class EmploymentDetails {
		// id is never lazy
		@Id
		private Integer id;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// not lazy
		private Instant startDate;
		private Instant endDate;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// lazy w/o LazyGroup - default fetch group
		@Basic(fetch = FetchType.LAZY)
		private String costCenter;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// special LazyGroup
		@Basic(fetch = FetchType.LAZY)
		@LazyGroup(value = "specialLazyGroup")
		@Lob
		private String contract;

		@OneToOne(mappedBy = "employmentDetails", fetch = FetchType.LAZY)
		@LazyGroup(value = "specialLazyGroup")
		private Employee employee;

		public EmploymentDetails() {
		}

		public EmploymentDetails(Integer id, Instant startDate, String contract, String costCenter, Employee employee) {
			this.id = id;
			this.startDate = startDate;
			this.contract = contract;
			this.costCenter = costCenter;
			this.employee = employee;
		}

		public Integer getId() {
			return id;
		}

		public Instant getStartDate() {
			return startDate;
		}

		public void setStartDate(Instant startDate) {
			this.startDate = startDate;
		}

		public Instant getEndDate() {
			return endDate;
		}

		public void setEndDate(Instant endDate) {
			this.endDate = endDate;
		}

		public String getCostCenter() {
			return costCenter;
		}

		public void setCostCenter(String costCenter) {
			this.costCenter = costCenter;
		}

		public String getContract() {
			return contract;
		}

		public void setContract(String contract) {
			this.contract = contract;
		}

		public Employee getEmployee() {
			return employee;
		}

		public void setEmployee(Employee employee) {
			this.employee = employee;
		}
	}
}
