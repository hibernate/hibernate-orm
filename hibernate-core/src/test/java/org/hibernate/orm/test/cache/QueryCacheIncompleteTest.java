/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.cache;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		QueryCacheIncompleteTest.Admin.class,
})
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.QUERY_CACHE_LAYOUT, value = "FULL")
		}
)
@JiraKey(value = "HHH-18689")
public class QueryCacheIncompleteTest {

	private Long adminId;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		adminId = scope.fromTransaction(
				session -> {
					Admin admin = new Admin();
					admin.setAge( 42 );
					session.persist( admin );
					return admin.getId();
				}
		);
	}

	@Test
	void testQueryWithEmbeddableParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// load uninitialized proxy
					session.getReference( Admin.class, adminId );
					// load entity
					var multiLoader = session.byMultipleIds( Admin.class );
					multiLoader.with( CacheMode.NORMAL );
					multiLoader.multiLoad( adminId );

					// store in query cache
					Admin admin = queryAdmin( session );
					assertThat( admin.getAge() ).isEqualTo( 42 );
				}
		);

		scope.inTransaction(
				session -> {
					// use query cache
					Admin admin = queryAdmin( session );
					assertThat( admin.getAge() ).isEqualTo( 42 );
				}
		);
	}

	private Admin queryAdmin(Session s) {
		return s.createQuery( "from Admin", Admin.class ).setCacheable( true ).getSingleResult();
	}

	@Entity(name = "Admin")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Admin {

		@Id
		@GeneratedValue
		private Long id;

		@Column(nullable = false)
		private int age;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}
}
