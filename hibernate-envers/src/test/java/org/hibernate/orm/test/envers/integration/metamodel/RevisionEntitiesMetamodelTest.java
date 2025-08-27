/*
 * SPDX-License-Identifier: Apache-2.0
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
import org.hibernate.envers.RevisionMapping_;
import org.hibernate.envers.TrackingModifiedEntitiesRevisionMapping_;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.enhanced.SequenceIdRevisionMapping_;
import org.hibernate.envers.enhanced.SequenceIdTrackingModifiedEntitiesRevisionMapping_;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.internal.MetadataContext;
import org.hibernate.testing.logger.LogInspectionHelper;
import org.hibernate.testing.logger.TriggerOnPrefixLogListener;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.invoke.MethodHandles;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@Jira("https://hibernate.atlassian.net/browse/HHH-17612")
@Jira("https://hibernate.atlassian.net/browse/HHH-19258")
@Jira("https://hibernate.atlassian.net/browse/HHH-10068")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RevisionEntitiesMetamodelTest {
	private CoreMessageLogger logger;
	private TriggerOnPrefixLogListener t1;
	private TriggerOnPrefixLogListener t2;

	@BeforeAll
	public void setUp() {
		logger = Logger.getMessageLogger(
				MethodHandles.lookup(),
				CoreMessageLogger.class,
				MetadataContext.class.getName()
		);
		// HHH-17612 - Injecting the class_ type field fails
		t1 = new TriggerOnPrefixLogListener( "HHH015007: Illegal argument on static metamodel field injection" );
		LogInspectionHelper.registerListener( t1, logger );
		// HHH-19259 - Injecting the actual attributes fails
		t2 = new TriggerOnPrefixLogListener( "HHH015011: Unable to locate static metamodel field" );
		LogInspectionHelper.registerListener( t2, logger );
	}

	@BeforeEach
	public void clear() {
		t1.reset();
		t2.reset();
	}

	@AfterAll
	public void tearDown() {
		LogInspectionHelper.clearAllListeners( logger );
	}

	@Test
	public void testDefaultRevisionEntity() {
		try (final SessionFactoryImplementor ignored = buildSessionFactory( false, true, Customer.class )) {
			assertListeners();
			assertThat( RevisionMapping_.class ).isNotNull();
		}
	}

	@Test
	public void testSequenceIdRevisionEntity() {
		try (final SessionFactoryImplementor ignored = buildSessionFactory( false, false, Customer.class )) {
			assertListeners();
			assertThat( SequenceIdRevisionMapping_.class ).isNotNull();
		}
	}

	@Test
	public void testDefaultTrackingModifiedEntitiesRevisionEntity() {
		try (final SessionFactoryImplementor ignored = buildSessionFactory( true, true, Customer.class )) {
			assertListeners();
			assertThat( TrackingModifiedEntitiesRevisionMapping_.class ).isNotNull();
		}
	}

	@Test
	public void testSequenceIdTrackingModifiedEntitiesRevisionEntity() {
		try (final SessionFactoryImplementor ignored = buildSessionFactory( true, false, Customer.class )) {
			assertListeners();
			assertListeners();
			assertThat( SequenceIdTrackingModifiedEntitiesRevisionMapping_.class ).isNotNull().isNotNull();
		}
	}

	@Test
	public void testCustomRevisionEntity() {
		try (final SessionFactoryImplementor ignored = buildSessionFactory( false, true, Customer.class,
				CustomRevisionEntity.class )) {
			assertListeners();
			assertThat( CustomRevisionEntity_.class ).isNotNull();
		}
	}

	@SuppressWarnings("resource")
	private static SessionFactoryImplementor buildSessionFactory(boolean trackEntities, boolean nativeId, Class<?>... classes) {
		final StandardServiceRegistryBuilder registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		registryBuilder.applySetting( EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION, trackEntities );
		registryBuilder.applySetting( EnversSettings.USE_REVISION_ENTITY_WITH_NATIVE_ID, nativeId );
		return new MetadataSources( registryBuilder.build() )
				.addAnnotatedClasses( classes )
				.buildMetadata()
				.buildSessionFactory()
				.unwrap( SessionFactoryImplementor.class );
	}

	private void assertListeners() {
		assertThat( t1.wasTriggered() ).as( "HHH015007 was triggered" ).isFalse();
		assertThat( t2.wasTriggered() ).as( "HHH015011 was triggered" ).isFalse();
	}

	@Audited
	@Entity(name = "Customer")
	@SuppressWarnings("unused")
	static class Customer {
		@Id
		private Long id;

		private String firstName;

		private String lastName;

		@Column(name = "created_on")
		@CreationTimestamp
		private Instant createdOn;
	}
}
