/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.metamodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
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
 */
@Jpa(annotatedClasses = {
		MixedIdAndIdClassHandling.FullTimeEmployee.class
})
public class MixedIdAndIdClassHandling {

	@Test
	@JiraKey( value = "HHH-8533" )
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

		assertEquals( String.class, entityType.getIdType().getJavaType() );
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
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final EmployeeId other = (EmployeeId) obj;
			if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			int hash = 5;
			hash = 29 * hash + (this.id != null ? this.id.hashCode() : 0);
			return hash;
		}
	}
}
