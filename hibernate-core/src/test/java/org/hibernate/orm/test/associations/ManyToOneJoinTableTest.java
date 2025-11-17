/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@SessionFactory
@DomainModel(annotatedClasses =
		{ManyToOneJoinTableTest.Book.class, ManyToOneJoinTableTest.Publisher.class, ManyToOneJoinTableTest.Company.class})
public class ManyToOneJoinTableTest {

	@Test void test(SessionFactoryScope scope) {
		scope.inSession( s -> s.createSelectionQuery( "from Book join publisher", Object[].class ).getResultList() );
		scope.inSession( s -> s.createSelectionQuery( "from Book join publisher join company", Object[].class ).getResultList() );
	}

	@Entity(name="Book")
	static class Book {
		@Id
		@GeneratedValue
		long id;
		@ManyToOne
		@JoinTable // no explicit table name!
		Publisher publisher;
	}
	@Entity(name="Publisher")
	static class Publisher {
		@Id
		@GeneratedValue
		long id;
		@OneToOne
		@JoinTable // no explicit table name!
		Company company;
	}
	@Entity(name="Company")
	static class Company {
		@Id
		@GeneratedValue
		long id;
	}
}
