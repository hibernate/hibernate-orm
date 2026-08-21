/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.Date;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DomainModel(annotatedClasses = UnionSameJavaTypeDifferentPathTest.EntityA.class)
@SessionFactory
@JiraKey("HHH-20661")
public class UnionSameJavaTypeDifferentPathTest {

	@Test
	public void testUnionOfSameJavaTypeFromDifferentPaths(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Object[]> results = session.createQuery(
					"select a.code, a.eventDate as d from EntityA a " +
					"union " +
					"select a2.code, a2.createdAt as d from EntityA a2",
					Object[].class
			).list();
			assertThat( results ).isNotNull();
		} );
	}

	@Test
	public void testUnionOfGenuinelyDifferentJavaTypesStillRejected(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThatThrownBy( () -> session.createQuery(
				"select a.code from EntityA a " +
				"union " +
				"select a2.eventDate from EntityA a2",
				Object.class
		).list() ).hasMessageContaining( "must have the same java type across all query parts" ) );
	}

	@MappedSuperclass
	public static class BaseAuditable {
		private Date createdAt;

		public Date getCreatedAt() {
			return createdAt;
		}

		public void setCreatedAt(Date createdAt) {
			this.createdAt = createdAt;
		}
	}

	@Entity(name = "EntityA")
	public static class EntityA extends BaseAuditable {
		@Id
		private Long id;

		private String code;

		private Date eventDate;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public Date getEventDate() {
			return eventDate;
		}

		public void setEventDate(Date eventDate) {
			this.eventDate = eventDate;
		}
	}
}
