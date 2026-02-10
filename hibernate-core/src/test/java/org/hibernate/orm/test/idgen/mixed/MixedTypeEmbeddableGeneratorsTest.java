/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.mixed;

import java.time.LocalDateTime;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.dialect.Dialect;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.generator.internal.CurrentTimestampGeneration;
import org.hibernate.orm.test.annotations.MutableClock;
import org.hibernate.orm.test.annotations.MutableClockSettingProvider;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = {
				MixedTypeEmbeddableGeneratorsTest.Event.class,
				MixedTypeEmbeddableGeneratorsTest.History.class,
		}
)
@SessionFactory( useCollectingStatementInspector = true )
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = CurrentTimestampGeneration.CLOCK_SETTING_NAME,
				provider = MutableClockSettingProvider.class
		)
)
class MixedTypeEmbeddableGeneratorsTest {

	private MutableClock clock;

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		clock = CurrentTimestampGeneration.getClock( scope.getSessionFactory() );
		clock.reset();
	}

	@Test
	void testMixedTiming(SessionFactoryScope scope) {
		final var statementInspector = scope.getCollectingStatementInspector();

		statementInspector.clear();
		scope.inTransaction( session -> {
			Event event = new Event();
			event.id = 1L;
			event.name = "conference";
			event.history = new History();
			session.persist( event );
		} );

		assertEquals( "insert into Event (created,updated,name,id) values (%s,?,?,?)"
						.formatted( getDialect( scope ).currentTimestamp() ),
				statementInspector.getSqlQueries().get( 0 ) );

		final LocalDateTime[] timestamps = scope.fromTransaction( session -> {
			Event event = session.get( Event.class, 1L );
			assertThat( event.history.created ).isNotNull();
			assertThat( event.history.updated ).isNotNull();
			return new LocalDateTime[] { event.history.created, event.history.updated };
		} );

		clock.tick();

		statementInspector.clear();
		scope.inTransaction( session -> {
			Event event = session.get( Event.class, 1L );
			event.name = "concert";
		} );

		assertEquals( "update Event set updated=?,name=? where id=?",
				statementInspector.getSqlQueries().get( 1 ) );

		scope.inTransaction( session -> {
			Event event = session.get( Event.class, 1L );
			assertThat( event.history.created ).isEqualTo( timestamps[0] );
			assertThat( event.history.updated ).isAfter( timestamps[1] );
			assertThat( event.name ).isEqualTo( "concert" );
		} );
	}

	@Entity(name = "Event")
	public static class Event {
		@Id
		public Long id;

		public String name;

		@Embedded
		public History history;
	}

	@Embeddable
	public static class History {
		@CreationTimestamp(source = SourceType.DB)
		public LocalDateTime created;

		@UpdateTimestamp(source = SourceType.VM)
		public LocalDateTime updated;
	}

	private static Dialect getDialect(SessionFactoryScope scope) {
		return scope.getSessionFactory().getJdbcServices().getJdbcEnvironment().getDialect();
	}

}
