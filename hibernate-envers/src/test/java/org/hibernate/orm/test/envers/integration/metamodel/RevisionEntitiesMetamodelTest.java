/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.metamodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.Audited;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.internal.HEMLogging;
import org.hibernate.metamodel.internal.MetadataContext;
import org.hibernate.testing.logger.LogInspectionHelper;
import org.hibernate.testing.logger.TriggerOnPrefixLogListener;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-17612" )
@TestInstance( TestInstance.Lifecycle.PER_CLASS )
public class RevisionEntitiesMetamodelTest {
	private TriggerOnPrefixLogListener trigger;

	@BeforeAll
	public void setUp() {
		trigger = new TriggerOnPrefixLogListener( "HHH015007: Illegal argument on static metamodel field injection" );
		LogInspectionHelper.registerListener( trigger, HEMLogging.messageLogger( MetadataContext.class ) );
	}

	@Test
	public void testDefaultRevisionEntity() {
		try (final SessionFactoryImplementor ignored = buildSessionFactory( false, true )) {
			assertThat( trigger.wasTriggered() ).isFalse();
		}
	}

	@Test
	public void testSequenceIdRevisionEntity() {
		try (final SessionFactoryImplementor ignored = buildSessionFactory( false, false )) {
			assertThat( trigger.wasTriggered() ).isFalse();
		}
	}

	@Test
	public void testDefaultTrackingModifiedEntitiesRevisionEntity() {
		try (final SessionFactoryImplementor ignored = buildSessionFactory( true, true )) {
			assertThat( trigger.wasTriggered() ).isFalse();
		}
	}

	@Test
	public void testSequenceIdTrackingModifiedEntitiesRevisionEntity() {
		try (final SessionFactoryImplementor ignored = buildSessionFactory( true, false )) {
			assertThat( trigger.wasTriggered() ).isFalse();
		}
	}

	@SuppressWarnings( "resource" )
	private static SessionFactoryImplementor buildSessionFactory(boolean trackEntities, boolean nativeId) {
		final StandardServiceRegistryBuilder registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		registryBuilder.applySetting( EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION, trackEntities );
		registryBuilder.applySetting( EnversSettings.USE_REVISION_ENTITY_WITH_NATIVE_ID, nativeId );
		return new MetadataSources( registryBuilder.build() )
				.addAnnotatedClasses( Customer.class )
				.buildMetadata()
				.buildSessionFactory()
				.unwrap( SessionFactoryImplementor.class );
	}

	@Audited
	@Entity( name = "Customer" )
	@SuppressWarnings( "unused" )
	public static class Customer {
		@Id
		private Long id;

		private String firstName;

		private String lastName;

		@Column( name = "created_on" )
		@CreationTimestamp
		private Instant createdOn;
	}
}
