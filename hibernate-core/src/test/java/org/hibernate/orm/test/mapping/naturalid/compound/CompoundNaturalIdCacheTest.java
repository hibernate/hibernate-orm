/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.compound;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.NaturalIdStatistics;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hibernate.testing.cache.CachingRegionFactory.DEFAULT_ACCESSTYPE;

/**
 * @author Sylvain Dusart
 */
@JiraKey(value = "HHH-16218")
@DomainModel(
		annotatedClasses = {
				CompoundNaturalIdCacheTest.EntityWithSimpleNaturalId.class,
				CompoundNaturalIdCacheTest.EntityWithCompoundNaturalId.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = DEFAULT_ACCESSTYPE, value = "nonstrict-read-write"),
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.SHOW_SQL, value = "false"),
		}
)
@SessionFactory(generateStatistics = true)
public class CompoundNaturalIdCacheTest {

	private static final int OBJECT_NUMBER = 3;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for ( int i = 0; i < OBJECT_NUMBER; i++ ) {
						EntityWithCompoundNaturalId compoundNaturalIdEntity = new EntityWithCompoundNaturalId();
						final String str = String.valueOf( i );
						compoundNaturalIdEntity.setFirstname( str );
						compoundNaturalIdEntity.setLastname( str );
						session.persist( compoundNaturalIdEntity );

						EntityWithSimpleNaturalId withSimpleNaturalIdEntity = new EntityWithSimpleNaturalId();
						withSimpleNaturalIdEntity.setName( str );
						session.persist( withSimpleNaturalIdEntity );
					}
				}
		);
	}

	@Test
	public void createThenLoadTest(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		NaturalIdStatistics naturalIdStatistics = statistics.getNaturalIdStatistics( EntityWithCompoundNaturalId.class.getName() );

		loadEntityWithCompoundNaturalId( "0", "0", scope );
		assertThat( naturalIdStatistics.getCacheHitCount() ).isEqualTo( 1 );

		loadEntityWithCompoundNaturalId( "1", "1", scope );
		assertThat( naturalIdStatistics.getCacheHitCount() ).isEqualTo( 2 );

		loadEntityWithCompoundNaturalId( "2", "2", scope );
		assertThat( naturalIdStatistics.getCacheHitCount() ).isEqualTo( 3 );

	}

	private void loadEntityWithCompoundNaturalId(String firstname, String lastname, SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					session.byNaturalId( EntityWithCompoundNaturalId.class )
							.using( "firstname", firstname )
							.using( "lastname", lastname )
							.load();
				}
		);
	}

	@Entity(name = "SimpleNaturalId")
	@NaturalIdCache
	public static class EntityWithSimpleNaturalId {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		private String name;

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}
	}

	@Entity(name = "CompoundNaturalId")
	@NaturalIdCache
	public static class EntityWithCompoundNaturalId {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		private String firstname;

		@NaturalId
		private String lastname;

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		public String getFirstname() {
			return firstname;
		}

		public void setFirstname(final String firstname) {
			this.firstname = firstname;
		}

		public String getLastname() {
			return lastname;
		}

		public void setLastname(final String lastname) {
			this.lastname = lastname;
		}
	}
}
