/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.generator.internal.CurrentTimestampGeneration;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.stream.IntStream;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = InMemoryTimestampGenerationBatchTest.Person.class)
@SessionFactory(generateStatistics = true)
@ServiceRegistry(settings = @Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "5"),
		settingProviders = @SettingProvider(settingName = CurrentTimestampGeneration.CLOCK_SETTING_NAME,
				provider = InMemoryTimestampGenerationBatchTest.MutableClockProvider.class))
@Jira("https://hibernate.atlassian.net/browse/HHH-19840")
public class InMemoryTimestampGenerationBatchTest {
	private static final MutableClock clock = new MutableClock();

	private static final int PERSON_COUNT = 8;

	@Test
	public void test(SessionFactoryScope scope) throws InterruptedException {
		final var statistics = scope.getSessionFactory().getStatistics();
		scope.inTransaction( session -> {
			Person person = null;
			for ( int i = 1; i <= PERSON_COUNT; i++ ) {
				person = new Person();
				person.setId( (long) i );
				person.setName( "person_" + i );
				session.persist( person );
			}

			statistics.clear();
			session.flush();

			assertEquals( 1, statistics.getPrepareStatementCount(), "Expected updates to execute in batches" );

			assertNotNull( person.getCreatedOn() );
			assertNotNull( person.getUpdatedOn() );
		} );


		clock.tick();
		sleep( 1 );

		scope.inTransaction( session -> {
			final var persons = session.findMultiple( Person.class,
					IntStream.rangeClosed( 1, PERSON_COUNT )
							.mapToObj( i -> (long) i )
							.toList() );

			assertThat( persons ).hasSize( PERSON_COUNT );
			assertThat( persons ).doesNotContainNull();

			Person person = null;
			for ( final Person p : persons ) {
				p.setName( p.getName() + "_updated" );
				person = p;
			}

			final var createdOn = person.getCreatedOn();
			final var updatedOn = person.getUpdatedOn();

			statistics.clear();
			session.flush();

			assertEquals( 1, statistics.getPrepareStatementCount(), "Expected updates to execute in batches" );

			assertEquals( person.getCreatedOn(), createdOn );
			assertTrue( person.getUpdatedOn().isAfter( updatedOn ) );
		} );
	}

	public static class MutableClockProvider implements SettingProvider.Provider<Object> {
		@Override
		public Object getSetting() {
			return clock;
		}
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private Long id;

		private String name;

		@Column(nullable = false)
		@CreationTimestamp(source = SourceType.VM)
		private Instant createdOn;

		@Column(nullable = false)
		@UpdateTimestamp(source = SourceType.VM)
		private Instant updatedOn;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Instant getCreatedOn() {
			return createdOn;
		}

		public Instant getUpdatedOn() {
			return updatedOn;
		}
	}
}
