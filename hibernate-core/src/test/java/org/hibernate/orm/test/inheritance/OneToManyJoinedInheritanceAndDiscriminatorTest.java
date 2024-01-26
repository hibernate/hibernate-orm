/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		OneToManyJoinedInheritanceAndDiscriminatorTest.Company.class,
		OneToManyJoinedInheritanceAndDiscriminatorTest.CustomerCompany.class,
		OneToManyJoinedInheritanceAndDiscriminatorTest.DistributorCompany.class,
		OneToManyJoinedInheritanceAndDiscriminatorTest.ComputerSystem.class,
		OneToManyJoinedInheritanceAndDiscriminatorTest.CustomerComputerSystem.class,
		OneToManyJoinedInheritanceAndDiscriminatorTest.DistributorComputerSystem.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17483" )
public class OneToManyJoinedInheritanceAndDiscriminatorTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CustomerComputerSystem customerComputer = new CustomerComputerSystem();
			customerComputer.setId( 1L );
			session.persist( customerComputer );
			final CustomerCompany customerCompany = new CustomerCompany( 2L );
			customerCompany.addComputerSystem( customerComputer );
			session.persist( customerCompany );
			final DistributorComputerSystem distributorComputer = new DistributorComputerSystem();
			distributorComputer.setId( 3L );
			session.persist( distributorComputer );
			final DistributorCompany distributorCompany = new DistributorCompany( 4L );
			distributorCompany.addComputerSystem( distributorComputer );
			session.persist( distributorCompany );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from ComputerSystem" ).executeUpdate();
			session.createMutationQuery( "delete from Company" ).executeUpdate();
		} );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CustomerCompany result = session.createQuery(
					"from CustomerCompany",
					CustomerCompany.class
			).getSingleResult();
			assertThat( result.getComputerSystems() ).hasSize( 1 );
		} );
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final DistributorCompany result = session.find( DistributorCompany.class, 4L );
			assertThat( result.getComputerSystems() ).hasSize( 1 );
		} );
	}

	@Test
	public void testJoinSelectId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Long result = session.createQuery(
					"select s.id from CustomerCompany c join c.computerSystems s",
					Long.class
			).getSingleResult();
			assertThat( result ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testJoinSelectEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final DistributorComputerSystem result = session.createQuery(
					"select s from DistributorCompany c join c.computerSystems s",
					DistributorComputerSystem.class
			).getSingleResult();
			assertThat( result.getId() ).isEqualTo( 3L );
		} );
	}

	@Entity( name = "Company" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static abstract class Company {
		@Id
		private Long id;

		public Company() {
		}

		public Company(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "CustomerCompany" )
	public static class CustomerCompany extends Company {
		@OneToMany( mappedBy = "owner" )
		private List<CustomerComputerSystem> computerSystems = new ArrayList<>();

		public CustomerCompany() {
		}

		public CustomerCompany(long id) {
			super( id );
		}

		public void addComputerSystem(CustomerComputerSystem computerSystem) {
			computerSystems.add( computerSystem );
			computerSystem.setOwner( this );
		}

		public List<CustomerComputerSystem> getComputerSystems() {
			return computerSystems;
		}
	}

	@Entity( name = "DistributorCompany" )
	public static class DistributorCompany extends Company {
		@OneToMany( mappedBy = "owner" )
		private List<DistributorComputerSystem> computerSystems = new ArrayList<>();

		public DistributorCompany() {
		}

		public DistributorCompany(long id) {
			super( id );
		}

		public void addComputerSystem(DistributorComputerSystem computerSystem) {
			computerSystems.add( computerSystem );
			computerSystem.setOwner( this );
		}

		public List<DistributorComputerSystem> getComputerSystems() {
			return computerSystems;
		}
	}

	@Entity( name = "ComputerSystem" )
	@Table( name = "computer_system" )
	@Inheritance( strategy = InheritanceType.JOINED )
	@DiscriminatorColumn( name = "disc_col" )
	public static abstract class ComputerSystem {
		@Id
		private Long id;

		@ManyToOne
		@JoinColumn( name = "owner_id" )
		private Company owner;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setOwner(Company owner) {
			this.owner = owner;
		}
	}

	@Entity( name = "CustomerComputerSystem" )
	@Table( name = "computer_system_sub" )
	public static class CustomerComputerSystem extends ComputerSystem {
	}

	@Entity( name = "DistributorComputerSystem" )
	@Table( name = "computer_system_sub" )
	public static class DistributorComputerSystem extends ComputerSystem {
	}
}
