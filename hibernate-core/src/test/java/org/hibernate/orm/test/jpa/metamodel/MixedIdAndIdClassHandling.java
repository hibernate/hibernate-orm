/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Ugh
 *
 * @author Steve Ebersole
 * @author Yanming Zhou
 */
@Jpa(annotatedClasses = {
		MixedIdAndIdClassHandling.FullTimeEmployee.class,
		MixedIdAndIdClassHandling.Person.class
})
public class MixedIdAndIdClassHandling {

	@Test
	@JiraKey( "HHH-8533" )
	public void testAccess(EntityManagerFactoryScope scope) {
		EntityType<FullTimeEmployee> entityType = scope.getEntityManagerFactory().getMetamodel().entity( FullTimeEmployee.class );
		try {
			entityType.getId( String.class );
			fail( "getId on entity defining @IdClass should cause IAE" );
		}
		catch (IllegalArgumentException expected) {
		}

		assertNotNull( entityType.getSupertype().getIdClassAttributes() );
		assertEquals( 1, entityType.getSupertype().getIdClassAttributes().size() );

		assertFalse( entityType.hasSingleIdAttribute() );
	}

	@Test
	@JiraKey( "HHH-6951" )
	public void testGetIdType(EntityManagerFactoryScope scope) {
		EntityType<FullTimeEmployee> fullTimeEmployeeEntityType = scope.getEntityManagerFactory().getMetamodel().entity( FullTimeEmployee.class );
		assertEquals( String.class, fullTimeEmployeeEntityType.getIdType().getJavaType() ); // return single @Id instead of @IdClass

		EntityType<Person> personEntityType = scope.getEntityManagerFactory().getMetamodel().entity( Person.class );
		assertEquals( PersonId.class, personEntityType.getIdType().getJavaType() ); // return @IdClass instead of null
	}

	@MappedSuperclass
	@IdClass( EmployeeId.class )
	public static abstract class Employee {
		@Id
		private String id;
		private String name;
	}

	@Entity( name = "FullTimeEmployee" )
	@Table( name="EMPLOYEE" )
	public static class FullTimeEmployee extends Employee {
		@Column(name="SALARY")
		private float salary;

		public FullTimeEmployee() {
		}
	}

	public static class EmployeeId implements java.io.Serializable {
		String id;

		public EmployeeId() {
		}

		public EmployeeId(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof EmployeeId ) ) {
				return false;
			}
			EmployeeId that = ( EmployeeId ) o;
			return Objects.equals( id, that.id );
		}

		@Override
		public int hashCode() {
			return Objects.hashCode( id );
		}
	}

	@IdClass( PersonId.class )
	@Entity( name = "Person" )
	@Table( name="PERSON" )
	public static class Person {
		@Id
		private String type;
		@Id
		private String no;
		private String name;
	}

	public static class PersonId implements java.io.Serializable {
		String type;
		String no;

		public PersonId() {
		}

		public PersonId(String type, String no) {
			this.type = type;
			this.no = no;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getNo() {
			return no;
		}

		public void setNo(String no) {
			this.no = no;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof PersonId ) ) {
				return false;
			}
			PersonId that = ( PersonId ) o;
			return Objects.equals( type, that.type ) && Objects.equals( no, that.no );
		}

		@Override
		public int hashCode() {
			return Objects.hash( type, no );
		}
	}
}
