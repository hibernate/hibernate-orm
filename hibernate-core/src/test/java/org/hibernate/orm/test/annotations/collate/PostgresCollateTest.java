/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collate;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.Collate;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@SessionFactory
@DomainModel(annotatedClasses = PostgresCollateTest.Message.class)
@RequiresDialect(PostgreSQLDialect.class)
public class PostgresCollateTest {

	@Test void test(SessionFactoryScope scope) {
		scope.inTransaction(session -> session.persist(new Message("Hello, world!")));
	}

	@Entity(name = "msgs")
	static class Message {
		@Id @GeneratedValue
		Long id;
		@Basic(optional = false)
		@Collate("en_US")
		@Column(length = 200)
		String text;

		public Message(String text) {
			this.text = text;
		}
	}
}
