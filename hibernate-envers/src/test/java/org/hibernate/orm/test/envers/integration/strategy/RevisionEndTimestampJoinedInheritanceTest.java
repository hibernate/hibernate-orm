/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.strategy;

import org.hibernate.envers.Audited;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.strategy.internal.ValidityAuditStrategy;

import org.hibernate.testing.envers.RequiresAuditStrategy;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.hibernate.orm.test.envers.integration.strategy.AbstractRevisionEndTimestampTest.TIMESTAMP_FIELD;

/**
 * @author Chris Cranford
 */
@JiraKey( value = "HHH-9092" )
@RequiresAuditStrategy( ValidityAuditStrategy.class )
@EnversTest
@Jpa(annotatedClasses = {
		RevisionEndTimestampJoinedInheritanceTest.Employee.class,
		RevisionEndTimestampJoinedInheritanceTest.FullTimeEmployee.class,
		RevisionEndTimestampJoinedInheritanceTest.Contractor.class,
		RevisionEndTimestampJoinedInheritanceTest.Executive.class
}, integrationSettings = {
		@Setting( name = EnversSettings.AUDIT_TABLE_SUFFIX, value = "_AUD" ),
		@Setting( name = EnversSettings.AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_FIELD_NAME, value = TIMESTAMP_FIELD ),
		@Setting( name = EnversSettings.AUDIT_STRATEGY_VALIDITY_STORE_REVEND_TIMESTAMP, value = "true" ),
		@Setting( name = EnversSettings.AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_LEGACY_PLACEMENT, value = "false" )
})
public class RevisionEndTimestampJoinedInheritanceTest extends AbstractRevisionEndTimestampTest {

	private Integer fullTimeEmployeeId;
	private Integer contractorId;
	private Integer executiveId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			FullTimeEmployee fullTimeEmployee = new FullTimeEmployee( "Employee", 50000 );
			Contractor contractor = new Contractor( "Contractor", 45 );
			Executive executive = new Executive( "Executive", 100000, "CEO" );

			em.persist( fullTimeEmployee );
			em.persist( contractor );
			em.persist( executive );

			fullTimeEmployeeId = fullTimeEmployee.getId();
			contractorId = contractor.getId();
			executiveId = executive.getId();
		} );

		// Revision 2 - raises for everyone!
		scope.inTransaction( em -> {
			FullTimeEmployee fullTimeEmployee = em.find( FullTimeEmployee.class, fullTimeEmployeeId );
			Contractor contractor = em.find( Contractor.class, contractorId );
			Executive executive = em.find( Executive.class, executiveId );

			fullTimeEmployee.setSalary( 60000 );
			contractor.setHourlyRate( 47 );
			executive.setSalary( 125000 );
		} );
	}

	@Test
	public void testRevisionEndTimestamps(EntityManagerFactoryScope scope) {
		verifyRevisionEndTimestampsInSubclass(scope, FullTimeEmployee.class, fullTimeEmployeeId );
		verifyRevisionEndTimestampsInSubclass(scope, Contractor.class, contractorId );
		verifyRevisionEndTimestampsInSubclass(scope, Executive.class, executiveId );
	}

	@Audited
	@Entity(name = "Employee")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn(length = 255)
	@DiscriminatorValue("EMP")
	public static class Employee {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;

		Employee() {

		}

		Employee(String name) {
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public int hashCode() {
			int result = ( id != null ? id.hashCode() : 0 );
			result = result * 31 + ( name != null ? name.hashCode() : 0 );
			return result;
		}

		@Override
		public boolean equals(Object object) {
			if ( this == object ) {
				return true;
			}
			if ( object == null || !( object instanceof Employee ) ) {
				return false;
			}
			Employee that = (Employee) object;
			if ( id != null ? !id.equals( that.id ) : that.id != null ) {
				return false;
			}
			return !( name != null ? !name.equals( that.name ) : that.name != null );
		}
	}

	@Audited
	@Entity(name = "FullTimeEmployee")
	@DiscriminatorValue("FT")
	public static class FullTimeEmployee extends Employee {
		private Integer salary;

		FullTimeEmployee() {

		}

		FullTimeEmployee(String name, Integer salary) {
			super( name );
			this.salary = salary;
		}

		public Integer getSalary() {
			return salary;
		}

		public void setSalary(Integer salary) {
			this.salary = salary;
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = result * 31 + ( salary != null ? salary.hashCode() : 0 );
			return result;
		}

		@Override
		public boolean equals(Object object) {
			if ( this == object ) {
				return true;
			}
			if ( object == null || !( object instanceof FullTimeEmployee ) ) {
				return false;
			}
			if ( !super.equals( object ) ) {
				return false;
			}
			FullTimeEmployee that = (FullTimeEmployee) object;
			return !( salary != null ? !salary.equals( that.salary ) : that.salary != null );
		}
	}

	@Audited
	@Entity(name = "Contractor")
	@DiscriminatorValue("CONTRACT")
	public static class Contractor extends Employee {
		private Integer hourlyRate;

		Contractor() {

		}

		Contractor(String name, Integer hourlyRate) {
			super( name );
			this.hourlyRate = hourlyRate;
		}

		public Integer getHourlyRate() {
			return hourlyRate;
		}

		public void setHourlyRate(Integer hourlyRate) {
			this.hourlyRate = hourlyRate;
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = result * 31 + ( hourlyRate != null ? hourlyRate.hashCode() : 0 );
			return result;
		}

		@Override
		public boolean equals(Object object) {
			if ( this == object ) {
				return true;
			}
			if ( object == null || !( object instanceof Contractor ) ) {
				return false;
			}
			if ( !super.equals( object ) ) {
				return false;
			}
			Contractor that = (Contractor) object;
			return !( hourlyRate != null ? !hourlyRate.equals( that.hourlyRate ) : that.hourlyRate != null );
		}
	}

	@Audited
	@Entity(name = "Executive")
	@DiscriminatorValue("EXEC")
	public static class Executive extends FullTimeEmployee {
		private String title;

		Executive() {

		}

		Executive(String name, Integer salary, String title) {
			super( name, salary );
			this.title = title;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = result * 31 + ( title != null ? title.hashCode() : 0 );
			return result;
		}

		@Override
		public boolean equals(Object object) {
			if ( this == object ) {
				return true;
			}
			if ( object == null || !( object instanceof Executive ) ) {
				return false;
			}
			if ( !super.equals( object ) ) {
				return false;
			}
			Executive that = (Executive) object;
			return !( title != null ? !title.equals( that.title ) : that.title != null );
		}
	}
}
