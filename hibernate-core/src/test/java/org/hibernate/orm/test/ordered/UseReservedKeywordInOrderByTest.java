/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ordered;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				UseReservedKeywordInOrderByTest.Person.class,
				UseReservedKeywordInOrderByTest.Location.class
		}
)
@SessionFactory
public class UseReservedKeywordInOrderByTest {


	@Test
	public void testOrderBy(SessionFactoryScope scope) {
		scope.inSession(
				session ->
						session.createQuery( "from Person p order by p.update" )
		);
	}

	@Test
	public void testMultipleOrderBy(SessionFactoryScope scope) {
		scope.inSession(
				session ->
						session.createQuery( "from Person p order by p.name,p.update" )
		);
	}

	@Test
	public void testOrderByOfAssociationEntityField(SessionFactoryScope scope) {
		scope.inSession(
				session ->
						session.createQuery( "from Person p order by p.location.update" )
		);
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private Integer id;

		private String name;

		@Column(name = "update_date")
		private Date update;

		@OneToOne
		Location location;
	}

	@Entity(name = "Location")
	public static class Location {
		@Id
		private Integer id;

		private String city;

		@Column(name = "update_date")
		private Date update;

	}
}
