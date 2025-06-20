/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.naturalid;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DomainModel(
		annotatedClasses = {
				NaturalIdOnSingleManyToOneTest.NaturalIdOnManyToOne.class,
				NaturalIdOnSingleManyToOneTest.State.class,
				NaturalIdOnSingleManyToOneTest.Citizen.class,
		}
)

@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"))
@JiraKey(value = "HHH-14943")
public class NaturalIdOnSingleManyToOneTest {

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testManyToOneNaturalLoadByNaturalId(SessionFactoryScope scope) {
		NaturalIdOnManyToOne singleManyToOne1 = new NaturalIdOnManyToOne();
		NaturalIdOnManyToOne singleManyToOne2 = new NaturalIdOnManyToOne();

		Citizen c1 = new Citizen();
		c1.setFirstname( "Emmanuel" );
		c1.setLastname( "Bernard" );
		c1.setSsn( "1234" );

		State france = new State();
		france.setName( "Ile de France" );
		c1.setState( france );

		singleManyToOne1.setCitizen( c1 );
		singleManyToOne2.setCitizen( null );

		scope.inTransaction(
				session -> {
					session.persist( france );
					session.persist( c1 );
					session.persist( singleManyToOne1 );
					session.persist( singleManyToOne2 );
				}
		);

		scope.getSessionFactory().getCache().evictNaturalIdData(); // we want to go to the database

		scope.inTransaction(
				session -> {
					NaturalIdOnManyToOne instance1 = session.byNaturalId( NaturalIdOnManyToOne.class )
							.using( "citizen", c1 )
							.load();
					assertNotNull( instance1 );
					assertNotNull( instance1.getCitizen() );

					NaturalIdOnManyToOne instance2 = session.byNaturalId( NaturalIdOnManyToOne.class )
							.using( "citizen", null ).load();

					assertNotNull( instance2 );
					assertNull( instance2.getCitizen() );
				}
		);
	}

	@Entity(name = "Citizen")
	@NaturalIdCache
	public static class Citizen {
		@Id
		@GeneratedValue
		private Integer id;
		private String firstname;
		private String lastname;
		@NaturalId
		@ManyToOne
		private State state;
		@NaturalId
		private String ssn;


		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getFirstname() {
			return firstname;
		}

		public void setFirstname(String firstname) {
			this.firstname = firstname;
		}

		public String getLastname() {
			return lastname;
		}

		public void setLastname(String lastname) {
			this.lastname = lastname;
		}

		public State getState() {
			return state;
		}

		public void setState(State state) {
			this.state = state;
		}

		public String getSsn() {
			return ssn;
		}

		public void setSsn(String ssn) {
			this.ssn = ssn;
		}
	}

	@Entity(name = "State")
	public static class State {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;

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
	}

	@Entity(name = "NaturalIdOnManyToOne")
	@NaturalIdCache
	public static class NaturalIdOnManyToOne {

		@Id
		@GeneratedValue
		int id;

		@NaturalId
		@ManyToOne(fetch = FetchType.LAZY)
		Citizen citizen;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public Citizen getCitizen() {
			return citizen;
		}

		public void setCitizen(Citizen citizen) {
			this.citizen = citizen;
		}
	}
}
