package org.hibernate.test.annotations.onetoone;

import javax.persistence.AssociationOverride;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;

import org.hibernate.AnnotationException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Aresnii Skvortsov
 */
@TestForIssue(jiraKey = "HHH-4384")
public class OverrideOneToOneJoinColumnTest extends BaseUnitTestCase {

	@Test
	public void allowIfJoinColumnIsAbsent() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		try {
			Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( Person.class )
					.addAnnotatedClass( State.class )
					.buildMetadata();

			Table personTable = metadata.getDatabase().getDefaultNamespace().locateTable( Identifier.toIdentifier(
					"PERSON_TABLE" ) );
			ForeignKey foreignKey = personTable.getForeignKeyIterator().next();

			assertEquals(
					"Overridden join column name should be applied",
					"PERSON_ADDRESS_STATE",
					foreignKey.getColumn( 0 ).getName()
			);

		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void disallowOnSideWithMappedBy() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		try {
			new MetadataSources( ssr )
					.addAnnotatedClass( Employee.class )
					.addAnnotatedClass( PartTimeEmployee.class )
					.addAnnotatedClass( Desk.class )
					.buildMetadata();
			fail( "Should disallow @JoinColumn override on side with mappedBy" );
		}
		catch (AnnotationException ex) {
			assertTrue(
					"Should disallow exactly because of @JoinColumn override on side with mappedBy",
					ex
							.getMessage()
							.startsWith( "Illegal attempt to define a @JoinColumn with a mappedBy association:" )
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity(name = "Person")
	@javax.persistence.Table(name = "PERSON_TABLE")
	public static class Person {

		private String id;

		private Address address;

		@Id
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Embedded
		@AssociationOverride(name = "state", joinColumns = { @JoinColumn(name = "PERSON_ADDRESS_STATE") })
		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
	}

	@Embeddable
	public static class Address {

		private String street;

		private String city;

		private State state;

		@OneToOne
		public State getState() {
			return state;
		}

		public void setState(State state) {
			this.state = state;
		}

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}
	}

	@Entity(name = "State")
	@javax.persistence.Table(name = "STATE_TABLE")
	public static class State {

		private String id;

		private String name;

		@Id
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@MappedSuperclass
	public static class Employee {

		@Id
		private Long id;

		private String name;

		@OneToOne(mappedBy = "employee")
		protected Desk desk;
	}

	@Entity
	@AssociationOverride(name = "desk",
			joinColumns = @JoinColumn(name = "PARTTIMEEMPLOYEE_DESK"))
	public static class PartTimeEmployee extends Employee {

	}

	@Entity(name = "Desk")
	public static class Desk {
		@Id
		private Long id;

		@OneToOne
		private PartTimeEmployee employee;

		private String location;
	}


}
