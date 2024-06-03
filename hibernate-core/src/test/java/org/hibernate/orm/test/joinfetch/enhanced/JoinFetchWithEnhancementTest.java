/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.joinfetch.enhanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@JiraKey("HHH-12298")
@DomainModel(
		annotatedClasses = {
			JoinFetchWithEnhancementTest.Employee.class, JoinFetchWithEnhancementTest.OtherEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class JoinFetchWithEnhancementTest {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			Employee e = new Employee( "John Smith" );
			OtherEntity other = new OtherEntity( "test" );
			e.getOtherEntities().add( other );
			other.setEmployee( e );
			em.persist( e );
			em.persist( other );
		} );
	}

	@Test
	public void testJoinFetchWithEnhancement(SessionFactoryScope scope) {
		Employee myEmployee = scope.fromTransaction( em -> {
			Employee localEmployee = em.createQuery( "from Employee e left join fetch e.otherEntities", Employee.class )
					.getResultList().get( 0 );
			assertTrue( Hibernate.isPropertyInitialized( localEmployee, "otherEntities" ) );
			return localEmployee;
		} );

		assertEquals( "test", myEmployee.getOtherEntities().iterator().next().getId() );
	}

	@Entity(name = "Employee")
	static class Employee {

		@Id
		private String name;

		private Set<OtherEntity> otherEntities = new HashSet<>();

		public Employee(String name) {
			this();
			setName( name );
		}

		protected Employee() {
		}

		@SuppressWarnings("unused")
		public String getName() {
			return name;
		}

		@OneToMany(targetEntity=OtherEntity.class, mappedBy="employee", fetch=FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		@LazyGroup("pOtherEntites")
		@Access(AccessType.PROPERTY)
		public Set<OtherEntity> getOtherEntities() {
			if ( otherEntities == null ) {
				otherEntities = new LinkedHashSet<>();
			}
			return otherEntities;
		}

		public void setName(String name) {
			this.name = name;
		}

		@SuppressWarnings("unused")
		public void setOtherEntities(Set<OtherEntity> pOtherEntites) {
			otherEntities = pOtherEntites;
		}
	}

	@Entity(name = "OtherEntity")
	static class OtherEntity {

		@Id
		private String id;

		@ManyToOne
		@LazyToOne(LazyToOneOption.NO_PROXY)
		@LazyGroup("Employee")
		@JoinColumn(name = "Employee_Id")
		private Employee employee = null;

		@SuppressWarnings("unused")
		protected OtherEntity() {
		}

		public OtherEntity(String id) {
			setId( id );
		}

		@SuppressWarnings("unused")
		public Employee getEmployee() {
			return employee;
		}

		public void setEmployee(Employee employee) {
			this.employee = employee;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}
}
