/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				HqlSubselectParameterTest.Resource.class,
				HqlSubselectParameterTest.Bookmark.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-15647")
public class HqlSubselectParameterTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Resource resource = new Resource( 1L, null );
					session.persist( resource );
					Bookmark bookmark = new Bookmark( 1L, null );
					session.persist( bookmark );

				}
		);
	}

	@Test
	public void testHqlQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String query = "select rsrc, (select count(mrk.key) from bookmark as mrk where mrk.key=:identityKey) as myBookmarks from resource as rsrc";
					List<Object[]> objects = session.createQuery( query, Object[].class )
							.setParameter( "identityKey", 100L )
							.getResultList();

					assertThat( objects.size() ).isEqualTo( 1 );
				}
		);
	}

	@Entity(name = "bookmark")
	@Table(name = "o_bookmark")
	public static class Bookmark {
		@Id
		@Column(name = "id", nullable = false, unique = true, updatable = false)
		private Long key;
		private String name;

		public Bookmark() {
		}

		public Bookmark(Long key, String name) {
			this.key = key;
			this.name = name;
		}
	}

	@Entity(name = "resource")
	@Table(name = "o_resource")
	public static class Resource {
		@Id
		@Column(name = "id", nullable = false, unique = true, updatable = false)
		private Long key;
		private String name;

		public Resource() {
		}

		public Resource(Long key, String name) {
			this.key = key;
			this.name = name;
		}
	}
}
