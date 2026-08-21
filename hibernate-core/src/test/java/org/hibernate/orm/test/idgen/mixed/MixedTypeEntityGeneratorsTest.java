/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.mixed;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = MixedTypeEntityGeneratorsTest.Event.class )
@SessionFactory
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = CurrentTimestampGeneration.CLOCK_SETTING_NAME,
				provider = MutableClockSettingProvider.class
		)
)
class MixedTypeEntityGeneratorsTest {

	private MutableClock clock;

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		clock = CurrentTimestampGeneration.getClock( scope.getSessionFactory() );
		clock.reset();
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testMixedGenerators(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Event event = new Event();
			event.id = 1L;
			event.name = "conference";
			session.persist( event );
		} );

		final LocalDateTime[] timestamps = scope.fromTransaction( session -> {
			Event event = session.find( Event.class, 1L );
			assertThat( event.createdOn ).isNotNull();
			assertThat( event.updatedOn ).isNotNull();
			return new LocalDateTime[] { event.createdOn, event.updatedOn };
		} );

		clock.tick();

		scope.inTransaction( session -> {
			Event event = session.find( Event.class, 1L );
			event.name = "concert";
		} );

		scope.inTransaction( session -> {
			Event event = session.find( Event.class, 1L );
			assertThat( event.createdOn ).isEqualTo( timestamps[0] );
			assertThat( event.updatedOn ).isAfter( timestamps[1] );
		} );
	}

	@Entity(name = "Event")
	public static class Event {
		@Id
		public Long id;

		public String name;

		@CreationTimestamp(source = SourceType.DB)
		public LocalDateTime createdOn;

		@UpdateTimestamp(source = SourceType.VM)
		public LocalDateTime updatedOn;
	}
}
