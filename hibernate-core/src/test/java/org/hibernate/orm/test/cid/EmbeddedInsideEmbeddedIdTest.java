/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cid;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				EmbeddedInsideEmbeddedIdTest.DomainEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-16560")
public class EmbeddedInsideEmbeddedIdTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DomainEntity entity = new DomainEntity( InnerWrappingId.of( 1L ), "key" );
					session.persist( entity );
				}
		);
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select d.id from DomainEntity d" ).getSingleResult();
				}
		);
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DomainEntity domainEntity = session.get(
							DomainEntity.class,
							new OuterWrappingId( InnerWrappingId.of( 1L ) )
					);
					assertThat( domainEntity ).isNotNull();
				}
		);
	}

	@Test
	public void testQuery2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select id from DomainEntity" ).getSingleResult();
				}
		);
	}

	@Entity(name = "DomainEntity")
	@Table(name = "domain_entity")
	public static class DomainEntity {
		@EmbeddedId
		private OuterWrappingId id;

		private String name;

		protected DomainEntity() {
		}

		public DomainEntity(InnerWrappingId artifactId, String name) {
			this();
			this.id = new OuterWrappingId( artifactId );
			this.name = name;
		}

		public OuterWrappingId getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Embeddable
	public static class OuterWrappingId {
		private InnerWrappingId artifactId;
		private String idType;

		public static OuterWrappingId of(InnerWrappingId id) {
			return new OuterWrappingId( id );
		}

		protected OuterWrappingId() {
		}

		public OuterWrappingId(InnerWrappingId artifactId) {
			this();
			this.artifactId = artifactId;
			this.idType = artifactId.getClass().getSimpleName();
		}

		public String getIdType() {
			return idType;
		}
	}

	@Embeddable
	public static class InnerWrappingId {
		private Long id;

		protected InnerWrappingId() {
		}

		protected InnerWrappingId(Long id) {
			this();
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public static InnerWrappingId of(Long id) {
			return new InnerWrappingId( id );
		}
	}
}
