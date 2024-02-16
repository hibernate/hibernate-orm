/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cache;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProviderSettingProvider;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cache;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Jan Schatteman
 */
@Jpa(
		annotatedClasses = L2CacheAccessNoCommitTest.Person.class,
		sharedCacheMode = SharedCacheMode.ALL,
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.CONNECTION_PROVIDER,
						provider = PreparedStatementSpyConnectionProviderSettingProvider.class)
		},
		integrationSettings = @Setting(name = AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, value = "true"),
		generateStatistics = true
)
@JiraKey( value = "HHH-14867" )
public class L2CacheAccessNoCommitTest {

	@BeforeAll
	public void setup(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Person p1 = new Person(1, "John");
					Person p2 = new Person(2, "Jane");
					entityManager.persist( p1 );
					entityManager.persist( p2 );
				}
		);
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> entityManager.createQuery( "delete from Person" ).executeUpdate()
		);
	}

	@Test
	public void test(EntityManagerFactoryScope scope) {
		final PreparedStatementSpyConnectionProvider connectionProvider =
				(PreparedStatementSpyConnectionProvider) scope.getEntityManagerFactory().getProperties()
				.get( AvailableSettings.CONNECTION_PROVIDER );
		connectionProvider.clear();

		final Statistics statistics = scope.getEntityManagerFactory().unwrap( SessionFactory.class ).getStatistics();
		statistics.clear();

		Cache cache = scope.getEntityManagerFactory().getCache();
		cache.evictAll();

		scope.inEntityManager(
				entityManager -> entityManager.find(Person.class, 1)
		);
		assertEquals( 1, connectionProvider.getReleasedConnections().size() );
		assertEquals(1, statistics.getSecondLevelCachePutCount());
		assertTrue( cache.contains(Person.class, 1) );
		connectionProvider.clear();

		scope.inTransaction(
				entityManager -> entityManager.find(Person.class, 1)
		);
		assertEquals( 0, connectionProvider.getReleasedConnections().size() );
		assertEquals(1, statistics.getSecondLevelCacheHitCount());
	}

	@Entity(name = "Person")
	@Table(name = "PERSON_TABLE")
	public static class Person {
		@Id
		private Integer id;

		private String name;

		public Person() {
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

}
