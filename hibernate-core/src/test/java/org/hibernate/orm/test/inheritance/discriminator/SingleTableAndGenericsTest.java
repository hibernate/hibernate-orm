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

import java.util.Map;

import static jakarta.persistence.InheritanceType.SINGLE_TABLE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hibernate.type.SqlTypes.JSON;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				// the order is important to reproduce the issue
				SingleTableAndGenericsTest.B.class,
				SingleTableAndGenericsTest.A.class,
				SingleTableAndGenericsTest.C.class,
		}
)
@SessionFactory
@JiraKey("HHH-17644")
@JiraKey("HHH-19978")
public class SingleTableAndGenericsTest {

	@Test
	public void testIt(SessionFactoryScope scope) {
		Map<String,String> payload = Map.of("book","1");
		String aId = "1";
		String cId = "2";

		scope.inTransaction(
				session -> {
					A a = new A();
					a.setId( aId );
					session.persist( a );
					a.setPayload( payload );

					C c = new C();
					c.setId( cId );
					session.persist( c );
					c.setPayload( "{\"book\":\"2\"}" );
				}
		);

		scope.inTransaction(
				session -> {
					A a = session.find( A.class, aId );
					assertThat( a ).isNotNull();
					Map<?,?> payload1 = a.getPayload();
					assertThat( payload1 ).isNotNull();
					assertTrue( payload1.containsKey("book") );

					C c = session.find( C.class, cId );
					assertThat( c ).isNotNull();
					String payload2 = c.getPayload();
					assertThat( payload2 ).isNotNull();
					assertThat( payload2 ).contains( "book" );
					assertThat( payload2 ).contains( "2" );
				}
		);

		scope.inTransaction(
				session -> {
					Object aPayload = session.createQuery( "select a.payload from A a where a.id = :id").setParameter( "id", aId ).getSingleResult();
					assertThat( aPayload ).isNotNull();
					assertThat( aPayload ).isInstanceOf( Map.class );
					Map<?,?> payload1 = (Map<?, ?>) aPayload;
					assertThat( payload1 ).isNotNull();
					assertTrue( payload1.containsKey("book") );

					Object cPayload = session.createQuery( "select c.payload from C c where c.id = :id").setParameter( "id", cId ).getSingleResult();
					assertThat( cPayload ).isNotNull();
					assertThat( cPayload ).isInstanceOf( String.class );
					String payload2 = (String) cPayload;
					assertThat( payload2 ).isNotNull();
					assertThat( payload2 ).contains( "book" );
					assertThat( payload2 ).contains( "2" );
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

	@Entity(name = "A")
	@DiscriminatorValue("child")
	public static class A extends B<Map<?,?>> {
	}

	@Entity(name = "C")
	@DiscriminatorValue("child2")
	public static class C extends B<String> {
	}
}
