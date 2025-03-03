/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.records;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		RecordEmbeddedPropertyNamesTest.Fee.class,
		RecordEmbeddedPropertyNamesTest.Getaway.class,
		RecordEmbeddedPropertyNamesTest.Vacation.class,
		RecordEmbeddedPropertyNamesTest.TestFee.class,
		RecordEmbeddedPropertyNamesTest.TestGetaway.class,
		RecordEmbeddedPropertyNamesTest.TestVacation.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18445" )
public class RecordEmbeddedPropertyNamesTest {
	@Test
	public void testFee(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestFee result = session.find( TestFee.class, 1L );
			assertThat( result.getFee().issuedA() ).isFalse();
			assertThat( result.getFee().issuedB() ).isTrue();
		} );
	}

	@Test
	public void testGetaway(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestGetaway result = session.find( TestGetaway.class, 1L );
			assertThat( result.getGetaway().getawayA() ).isEqualTo( "A" );
			assertThat( result.getGetaway().getawayB() ).isEqualTo( "B" );
		} );
	}

	@Test
	public void testVacation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestVacation result = session.find( TestVacation.class, 1L );
			assertThat( result.getVacation().amount() ).isEqualTo( 7 );
			assertThat( result.getVacation().issued() ).isTrue();
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TestFee( 1L, new Fee( true, false ) ) );
			session.persist( new TestGetaway( 1L, new Getaway( "B", "A" ) ) );
			session.persist( new TestVacation( 1L, new Vacation( true, 7 ) ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Embeddable
	public record Fee(Boolean issuedB, Boolean issuedA) {
	}

	@Entity( name = "TestFee" )
	static class TestFee {
		@Id
		private Long id;
		@Embedded
		private Fee fee;

		public TestFee() {
		}

		public TestFee(Long id, Fee fee) {
			this.id = id;
			this.fee = fee;
		}

		public Fee getFee() {
			return fee;
		}
	}

	@Embeddable
	record Getaway(String getawayB, String getawayA) {
	}

	@Entity( name = "TestGetaway" )
	static class TestGetaway {
		@Id
		private Long id;

		@Embedded
		private Getaway getaway;

		public TestGetaway() {
		}

		public TestGetaway(Long id, Getaway getaway) {
			this.id = id;
			this.getaway = getaway;
		}

		public Getaway getGetaway() {
			return getaway;
		}
	}

	@Embeddable
	record Vacation(Boolean issued, Integer amount) {
	}

	@Entity( name = "TestVacation" )
	static class TestVacation {
		@Id
		private Long id;

		@Embedded
		private Vacation vacation;

		public TestVacation() {
		}

		public TestVacation(Long id, Vacation vacation) {
			this.id = id;
			this.vacation = vacation;
		}

		public Vacation getVacation() {
			return vacation;
		}
	}
}
