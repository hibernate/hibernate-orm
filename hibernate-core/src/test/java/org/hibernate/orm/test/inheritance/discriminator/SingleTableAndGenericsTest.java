/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator;

import org.hibernate.annotations.JdbcTypeCode;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Table;

import static jakarta.persistence.InheritanceType.SINGLE_TABLE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hibernate.type.SqlTypes.JSON;

@DomainModel(
		annotatedClasses = {
				// the order is important to reproduce the issue
				SingleTableAndGenericsTest.B.class,
				SingleTableAndGenericsTest.A.class,
		}
)
@SessionFactory
@JiraKey("HHH-17644")
public class SingleTableAndGenericsTest {

	@Test
	public void testIt(SessionFactoryScope scope) {
		String payload = "{\"book\": \"1\"}";
		String aId = "1";

		scope.inTransaction(
				session -> {
					A a = new A();
					a.setId( aId );
					session.persist( a );
					a.setPayload( payload );
				}
		);

		scope.inTransaction(
				session -> {
					A a = session.find( A.class, aId );
					assertThat( a ).isNotNull();
					String payload1 = a.getPayload();
					assertThat( payload1 ).isNotNull();
					assertThat( payload1 ).contains( "book" );
				}
		);
	}

	@Entity(name = "B")
	@Table(name = "table_b")
	@Inheritance(strategy = SINGLE_TABLE)
	@DiscriminatorColumn(name = "demo_type")
	public static abstract class B<T> {

		@Id
		@Column(name = "id", nullable = false)
		private String id;

		@Column(name = "payload")
		@JdbcTypeCode(JSON)
		private T payload;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public T getPayload() {
			return payload;
		}

		public void setPayload(T payload) {
			this.payload = payload;
		}
	}

	@Entity(name = "C")
	@DiscriminatorValue("child")
	public static class A extends B<String> {
	}
}
