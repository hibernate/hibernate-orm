/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetchprofiles;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.LazyInitializationException;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.StatisticsSettings.GENERATE_STATISTICS;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey( value = "HHH-12297")
@ServiceRegistry(
		settings = @Setting( name = GENERATE_STATISTICS, value = "true" )
)
@DomainModel( annotatedClasses = {
		EntityLoadedInTwoPhaseLoadTest.Start.class,
		EntityLoadedInTwoPhaseLoadTest.Mid.class,
		EntityLoadedInTwoPhaseLoadTest.Finish.class,
		EntityLoadedInTwoPhaseLoadTest.Via1.class,
		EntityLoadedInTwoPhaseLoadTest.Via2.class
} )
@SessionFactory
public class EntityLoadedInTwoPhaseLoadTest {
	static final String FETCH_PROFILE_NAME = "fp1";

	@Test
	public void testIfAllRelationsAreInitialized(SessionFactoryScope sessions) {
		final StatisticsImplementor statistics = sessions.getSessionFactory().getStatistics();
		statistics.clear();

		final Start start = sessions.fromTransaction( (session) -> {
			session.enableFetchProfile( FETCH_PROFILE_NAME );
			return session.find( Start.class, 1 );
		} );

		// should have loaded all the data
		assertThat( statistics.getEntityLoadCount() ).isEqualTo( 4 );
		// should have loaded it in one query (join fetch)
		assertThat( statistics.getPrepareStatementCount() ).isEqualTo( 1 );

		try {
			// access the data which was supposed to have been fetched
			//noinspection ResultOfMethodCallIgnored
			start.getVia2().getMid().getFinish().getValue();
		}
		catch (LazyInitializationException e) {
			fail( "Everything should be initialized" );
		}
	}

	@BeforeEach
	void createTestData(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			Finish finish = new Finish( 1, "foo" );
			Mid mid = new Mid( 1, finish );
			Via2 via2 = new Via2( 1, mid );
			Start start = new Start( 1, null, via2 );

			session.persist( start );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope sessions) {
		sessions.dropData();
	}

	@Entity(name = "FinishEntity")
	public static class Finish {
		@Id
		private Integer id;
		@Column(name = "val", nullable = false)
		private String value;

		public Finish() {
		}

		public Finish(Integer id, String value) {
			this.id = id;
			this.value = value;
		}

		public Integer getId() {
			return id;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	@Entity(name = "MidEntity")
	@FetchProfile(name = FETCH_PROFILE_NAME, fetchOverrides = {
			@FetchProfile.FetchOverride(entity = Mid.class, association = "finish")
	})
	public static class Mid {
		@Id
		private Integer id;
		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private Finish finish;

		public Mid() {
		}

		public Mid(Integer id, Finish finish) {
			this.id = id;
			this.finish = finish;
		}

		public Integer getId() {
			return id;
		}

		public Finish getFinish() {
			return finish;
		}

		public void setFinish(Finish finish) {
			this.finish = finish;
		}

	}

	@Entity(name = "StartEntity")
	@FetchProfile(name = FETCH_PROFILE_NAME, fetchOverrides = {
			@FetchProfile.FetchOverride(entity = Start.class, association = "via1"),
			@FetchProfile.FetchOverride(entity = Start.class, association = "via2")
	})
	public static class Start {
		@Id
		private Integer id;
		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private Via1 via1;
		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private Via2 via2;

		public Start() {
		}

		public Start(Integer id, Via1 via1, Via2 via2) {
			this.id = id;
			this.via1 = via1;
			this.via2 = via2;
		}

		public Integer getId() {
			return id;
		}

		public Via1 getVia1() {
			return via1;
		}

		public void setVia1(Via1 via1) {
			this.via1 = via1;
		}

		public Via2 getVia2() {
			return via2;
		}

		public void setVia2(Via2 via2) {
			this.via2 = via2;
		}

	}

	@Entity(name = "Via1Entity")
	@FetchProfile(name = FETCH_PROFILE_NAME, fetchOverrides = {
			@FetchProfile.FetchOverride(entity = Via1.class, association = "mid")
	})
	public static class Via1 {
		@Id
		private Integer id;
		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private Mid mid;

		public Via1() {
		}

		public Via1(Integer id, Mid mid) {
			this.id = id;
			this.mid = mid;
		}

		public Integer getId() {
			return id;
		}

		public Mid getMid() {
			return mid;
		}

		public void setMid(Mid mid) {
			this.mid = mid;
		}

	}

	@Entity(name = "Via2Entity")
	@FetchProfile(name = FETCH_PROFILE_NAME, fetchOverrides = {
			@FetchProfile.FetchOverride(entity = Via2.class, association = "mid")
	})
	public static class Via2 {
		@Id
		private Integer id;
		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private Mid mid;

		public Via2() {
		}

		public Via2(Integer id, Mid mid) {
			this.id = id;
			this.mid = mid;
		}

		public Integer getId() {
			return id;
		}

		public Mid getMid() {
			return mid;
		}

		public void setMid(Mid mid) {
			this.mid = mid;
		}

	}
}
