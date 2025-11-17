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
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

@SessionFactory
@DomainModel(annotatedClasses = MySQLCollateTest.Message.class)
@RequiresDialect(MySQLDialect.class)
@SkipForDialect(dialectClass = TiDBDialect.class)
public class MySQLCollateTest {

	@Test void test(SessionFactoryScope scope) {
		scope.inTransaction(session -> session.persist(new Message("Hello, world!")));
	}

	@Entity(name = "msgs")
	static class Message {
		@Id @GeneratedValue
		Long id;
		@Basic(optional = false)
		@Collate("utf8mb4_spanish2_ci")
		@Column(length = 200)
		String text;

		public Message(String text) {
			this.text = text;
		}
	}
}
