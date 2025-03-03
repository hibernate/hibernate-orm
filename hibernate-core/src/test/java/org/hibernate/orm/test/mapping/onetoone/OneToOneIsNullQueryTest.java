/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetoone;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.TypedQuery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		OneToOneIsNullQueryTest.Thing.class,
		OneToOneIsNullQueryTest.ThingStats.class
})
@JiraKey("HHH-16080")
public class OneToOneIsNullQueryTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Thing thing1 = new Thing( 1L );
			final ThingStats stats = new ThingStats( thing1.getPk(), 10 );
			thing1.setThingStats( stats );
			final Thing thing2 = new Thing( 2L );
			session.persist( thing1 );
			session.persist( thing2 );
			session.persist( stats );
		} );
	}

	@Test
	public void testIsNullQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			String ql = "select thing from Thing thing"
					+ " left join thing.thingStats thingStats "
					+ " where thingStats is null or thingStats.countRejected = 0";
			TypedQuery<Thing> q = session.createQuery( ql, Thing.class );
			assertThat( q.getSingleResult().getPk() ).isEqualTo( 2L );
		} );
	}

	@Entity(name = "Thing")
	public static class Thing {
		@Id
		@Column(name = "thing_pk")
		private Long pk;

		@OneToOne
		@JoinColumn(name = "thing_pk")
		private ThingStats thingStats;

		public Thing() {
		}

		public Thing(Long pk) {
			this.pk = pk;
		}

		public Long getPk() {
			return pk;
		}

		public ThingStats getThingStats() {
			return thingStats;
		}

		public void setThingStats(ThingStats thingStats) {
			this.thingStats = thingStats;
		}
	}

	@Entity(name = "ThingStats")
	public static class ThingStats {
		@Id
		@Column(name = "thing_fk", nullable = false)
		private Long thingPk;

		private Integer countRejected;

		public ThingStats() {
		}

		public ThingStats(Long thingPk, Integer countRejected) {
			this.thingPk = thingPk;
			this.countRejected = countRejected;
		}

		public Long getThingPk() {
			return thingPk;
		}

		public Integer getCountRejected() {
			return countRejected;
		}
	}
}
