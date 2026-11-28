package org.hibernate.orm.test.mapping.embeddable;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// An aggregate nested inside a flattened embeddable must use its aggregate JDBC mapping.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		FlattenedEmbeddableAggregateTest.Person.class,
		FlattenedEmbeddableAggregateTest.Details.class,
		FlattenedEmbeddableAggregateTest.Address.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-20913")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonAggregate.class)
class FlattenedEmbeddableAggregateTest {
	@BeforeEach
	void seed(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var person = new Person();
			person.id = 10;
			person.details = details( "Main" );
			session.persist( person );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void loadSeeded(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( "Main", session.find( Person.class, 10L ).details.address.street ) );
	}

	@Test
	void selectDetails(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( "Main", session.createQuery(
				"select p.details from PersonWithAggregateAddress p where p.id=10", Details.class ).getSingleResult().address.street ) );
	}

	@Test
	void selectAddress(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( "Main", session.createQuery(
				"select p.details.address from PersonWithAggregateAddress p where p.id=10", Address.class ).getSingleResult().street ) );
	}

	@Test
	void selectStreet(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( "Main", session.createQuery(
				"select p.details.address.street from PersonWithAggregateAddress p where p.id=10", String.class ).getSingleResult() ) );
	}

	@Test
	void predicateDetails(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var query = session.createQuery( "from PersonWithAggregateAddress p where p.details=:value", Person.class );
			assertEquals( 1, query.setParameter( "value", details( "Main" ) ).getResultList().size() );
			assertEquals( 0, query.setParameter( "value", details( "Other" ) ).getResultList().size() );
		} );
	}

	@Test
	void replaceAddress(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.find( Person.class, 10L ).details.address = details( "Other" ).address );
		assertStreet( scope, "Other" );
	}

	@Test
	void mutateStreet(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.find( Person.class, 10L ).details.address.street = "Other" );
		assertStreet( scope, "Other" );
	}

	@Test
	void nullAddress(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.find( Person.class, 10L ).details.address = null );
		scope.inTransaction( session -> assertNull( session.find( Person.class, 10L ).details ) );
	}

	@Test
	void nullToAddress(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.find( Person.class, 10L ).details = null );
		scope.inTransaction( session -> session.find( Person.class, 10L ).details = details( "Other" ) );
		assertStreet( scope, "Other" );
	}

	@Test
	void insertNullDetails(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var person = new Person();
			person.id = 2;
			session.persist( person );
		} );
		scope.inTransaction( session -> assertNull( session.find( Person.class, 2L ).details ) );
	}

	@Test
	void bulkUpdateDetails(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery(
				"update PersonWithAggregateAddress p set p.details=:value where p.id=10" )
				.setParameter( "value", details( "Other" ) ).executeUpdate() );
		assertStreet( scope, "Other" );
	}

	private static Details details(String street) {
		final var details = new Details();
		details.address = new Address();
		details.address.street = street;
		return details;
	}

	private static void assertStreet(SessionFactoryScope scope, String street) {
		scope.inTransaction( session -> assertEquals( street, session.find( Person.class, 10L ).details.address.street ) );
	}

	@Test
	void persistAndLoad(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var person = new Person();
			person.id = 1L;
			person.details = new Details();
			person.details.address = new Address();
			person.details.address.street = "Main";
			session.persist( person );
		} );

		scope.inTransaction( session -> {
			final var person = session.find( Person.class, 1L );
			assertEquals( "Main", person.details.address.street );
		} );
	}

	@Entity(name = "PersonWithAggregateAddress")
	@Table(name = "person_with_aggregate_address")
	static class Person {
		@Id
		long id;

		@Embedded
		Details details;
	}

	@Embeddable
	static class Details {
		@JdbcTypeCode(SqlTypes.JSON)
		Address address;
	}

	@Embeddable
	static class Address {
		String street;
	}
}
