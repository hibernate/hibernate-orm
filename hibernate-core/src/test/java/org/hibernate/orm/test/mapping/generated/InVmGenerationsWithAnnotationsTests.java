/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import java.sql.Timestamp;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
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

/**
 * Tests for using {@link CreationTimestamp} and {@link UpdateTimestamp}
 * annotations with Timestamp-valued attributes
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = InVmGenerationsWithAnnotationsTests.AuditedEntity.class )
@SessionFactory
@ServiceRegistry(settingProviders = @SettingProvider(settingName = CurrentTimestampGeneration.CLOCK_SETTING_NAME, provider = MutableClockSettingProvider.class))
public class InVmGenerationsWithAnnotationsTests {

	private MutableClock clock;

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		clock = CurrentTimestampGeneration.getClock( scope.getSessionFactory() );
		clock.reset();
	}

	@Test
	public void testGenerations(SessionFactoryScope scope) {
		scope.inSession( (session) -> {
			// first creation
			final AuditedEntity saved = scope.fromTransaction( session, s -> {
				final AuditedEntity entity = new AuditedEntity( 1, "it" );
				s.persist( entity );
				return entity;
			} );

			assertThat( saved ).isNotNull();
			assertThat( saved.createdOn ).isNotNull();
			assertThat( saved.lastUpdatedOn ).isNotNull();

			saved.name = "changed";
			clock.tick();

			// then changing
			final AuditedEntity merged = scope.fromTransaction( session, s -> s.merge( saved ) );

			assertThat( merged ).isNotNull();
			assertThat( merged.createdOn ).isNotNull();
			assertThat( merged.lastUpdatedOn ).isNotNull();
			assertThat( merged.lastUpdatedOn ).isNotEqualTo( merged.createdOn );

			// lastly, make sure we can load it..
			final AuditedEntity loaded = scope.fromTransaction( session, s -> s.find( AuditedEntity.class, 1 ) );

			assertThat( loaded ).isNotNull();
			assertThat( loaded.createdOn ).isEqualTo( merged.createdOn );
			assertThat( loaded.lastUpdatedOn ).isEqualTo( merged.lastUpdatedOn );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "AuditedEntity" )
	@Table( name = "ann_generated_simple" )
	public static class AuditedEntity {
		@Id
		public Integer id;
		@Basic
		public String name;
		@CreationTimestamp
		public Timestamp createdOn;
		@UpdateTimestamp
		public Timestamp lastUpdatedOn;

		public AuditedEntity() {
		}

		public AuditedEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
