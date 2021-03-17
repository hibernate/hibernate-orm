/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.joinfetch.enhanced;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@TestForIssue(jiraKey = "HHH-12298")
@RunWith(BytecodeEnhancerRunner.class)
public class JoinFetchWithEnhancementTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Employee.class, OtherEntity.class };
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.CLASSLOADERS, getClass().getClassLoader() );
	}

	@Before
	public void prepare() {
		doInJPA( this::entityManagerFactory, em -> {
			Employee e = new Employee( "John Smith" );
			OtherEntity other = new OtherEntity( "test" );
			e.getOtherEntities().add( other );
			other.setEmployee( e );
			em.persist( e );
			em.persist( other );
		} );
	}

	@Test
	public void testJoinFetchWithEnhancement() {
		Employee myEmployee = doInJPA( this::entityManagerFactory, em -> {
			Employee localEmployee = em.createQuery( "from Employee e left join fetch e.otherEntities", Employee.class )
					.getResultList().get( 0 );
			assertTrue( Hibernate.isPropertyInitialized( localEmployee, "otherEntities" ) );
			return localEmployee;
		} );

		assertEquals( "test", myEmployee.getOtherEntities().iterator().next().getId() );
	}

	@Entity(name = "Employee")
	private static class Employee {

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
	private static class OtherEntity {

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
