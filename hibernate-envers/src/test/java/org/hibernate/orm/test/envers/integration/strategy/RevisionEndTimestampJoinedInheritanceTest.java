/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.strategy;

import org.hibernate.envers.Audited;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.orm.test.envers.Priority;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.envers.RequiresAuditStrategy;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * @author Chris Cranford
 */
@JiraKey( value = "HHH-9092" )
@RequiresAuditStrategy( ValidityAuditStrategy.class )
public class RevisionEndTimestampJoinedInheritanceTest extends AbstractRevisionEndTimestampTest {

	private Integer fullTimeEmployeeId;
	private Integer contractorId;
	private Integer executiveId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Employee.class, FullTimeEmployee.class, Contractor.class, Executive.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager entityManager = getEntityManager();
		try {
			FullTimeEmployee fullTimeEmployee = new FullTimeEmployee( "Employee", 50000 );
			Contractor contractor = new Contractor( "Contractor", 45 );
			Executive executive = new Executive( "Executive", 100000, "CEO" );

			// Revision 1
			entityManager.getTransaction().begin();
			entityManager.persist( fullTimeEmployee );
			entityManager.persist( contractor );
			entityManager.persist( executive );
			entityManager.getTransaction().commit();

			// Revision 2 - raises for everyone!
			entityManager.getTransaction().begin();
			fullTimeEmployee.setSalary( 60000 );
			contractor.setHourlyRate( 47 );
			executive.setSalary( 125000 );
			entityManager.getTransaction().commit();

			fullTimeEmployeeId = fullTimeEmployee.getId();
			contractorId = contractor.getId();
			executiveId = executive.getId();
		}
		catch ( Exception e ) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testRevisionEndTimestamps() {
		verifyRevisionEndTimestampsInSubclass(FullTimeEmployee.class, fullTimeEmployeeId );
		verifyRevisionEndTimestampsInSubclass(Contractor.class, contractorId );
		verifyRevisionEndTimestampsInSubclass(Executive.class, executiveId );
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
	public class Executive extends FullTimeEmployee {
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
