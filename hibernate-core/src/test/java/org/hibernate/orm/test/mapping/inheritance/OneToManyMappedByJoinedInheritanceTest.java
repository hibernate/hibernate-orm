/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.mapping.Column;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
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
		OneToManyMappedByJoinedInheritanceTest.Company.class,
		OneToManyMappedByJoinedInheritanceTest.CustomerCompany.class,
		OneToManyMappedByJoinedInheritanceTest.DistributorCompany.class,
		OneToManyMappedByJoinedInheritanceTest.ComputerSystem.class,
		OneToManyMappedByJoinedInheritanceTest.CustomerComputerSystem.class,
		OneToManyMappedByJoinedInheritanceTest.DistributorComputerSystem.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16860" )
public class OneToManyMappedByJoinedInheritanceTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CustomerComputerSystem customerComputer = new CustomerComputerSystem();
			session.persist( customerComputer );
			final CustomerCompany customerCompany = new CustomerCompany( 1L );
			customerCompany.addComputerSystem( customerComputer );
			session.persist( customerCompany );
			final DistributorComputerSystem distributorComputer = new DistributorComputerSystem();
			session.persist( distributorComputer );
			final DistributorCompany distributorCompany = new DistributorCompany( 2L );
			distributorCompany.addComputerSystem( distributorComputer );
			session.persist( distributorCompany );
		} );
	}

	@Test
	public void testFkMapping(SessionFactoryScope scope) {
		StreamSupport.stream( scope.getMetadataImplementor().getDatabase().getNamespaces().spliterator(), false )
				.flatMap( namespace -> namespace.getTables().stream() )
				.forEach( t -> {
					// assert that the 'owner_id' column is only found in the 'computer_system' table
					final Column column = t.getColumn( Identifier.toIdentifier( "owner_id " ) );
					if ( t.getName().equals( "computer_system" ) ) {
						assertThat( column ).isNotNull();
					}
					else {
						assertThat( column ).isNull();
					}
				} );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CustomerCompany result = session.createQuery(
					"from CustomerCompany",
					CustomerCompany.class
			).getSingleResult();
			assertThat( result.getComputerSystems() ).isNotEmpty();
		} );
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final DistributorCompany result = session.find( DistributorCompany.class, 2L );
			assertThat( result.getComputerSystems() ).isNotEmpty();
		} );
	}

	@Entity( name = "Company" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static abstract class Company {
		@Id
		private long id;

		public Company() {
		}

		public Company(long id) {
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
	public static abstract class ComputerSystem {
		@Id
		@GeneratedValue
		private long id;

		@ManyToOne
		@JoinColumn( name = "owner_id" )
		private Company owner;

		public void setOwner(Company owner) {
			this.owner = owner;
		}
	}

	@Entity( name = "CustomerComputerSystem" )
	public static class CustomerComputerSystem extends ComputerSystem {
	}

	@Entity( name = "DistributorComputerSystem" )
	public static class DistributorComputerSystem extends ComputerSystem {
	}
}
