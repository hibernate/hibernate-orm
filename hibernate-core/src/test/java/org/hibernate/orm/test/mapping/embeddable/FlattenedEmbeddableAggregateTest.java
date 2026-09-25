/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// An aggregate nested inside a flattened embeddable must use its aggregate JDBC mapping.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		FlattenedEmbeddableAggregateTest.Person.class,
		FlattenedEmbeddableAggregateTest.Details.class,
		FlattenedEmbeddableAggregateTest.Address.class
})
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonAggregate.class)
class FlattenedEmbeddableAggregateTest {
	@Test
	@FailureExpected(jiraKey = "HHH-20913", reason = "The JSON aggregate is bound using its String member JDBC mapping")
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
