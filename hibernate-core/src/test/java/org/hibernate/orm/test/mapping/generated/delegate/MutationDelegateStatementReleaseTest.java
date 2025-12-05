/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated.delegate;

import java.util.Date;
import java.util.Set;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.engine.jdbc.JdbcLogging;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.resource.jdbc.ResourceRegistry;

import org.hibernate.testing.logger.LogInspectionHelper;
import org.hibernate.testing.logger.TriggerOnPrefixLogListener;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.resource.jdbc.internal.ResourceRegistryLogger.RESOURCE_REGISTRY_LOGGER;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		MutationDelegateStatementReleaseTest.IdentityOnly.class,
		MutationDelegateStatementReleaseTest.IdentityAndValues.class,
		MutationDelegateStatementReleaseTest.BaseEntity.class,
		MutationDelegateStatementReleaseTest.ChildEntity.class,
} )
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsIdentityColumns.class )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17688" )
public class MutationDelegateStatementReleaseTest {
	private TriggerOnPrefixLogListener trigger;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		trigger = new TriggerOnPrefixLogListener( Set.of( "Exception clearing", "Unable to release" ) );
		LogInspectionHelper.registerListener( trigger, RESOURCE_REGISTRY_LOGGER );
	}

	@BeforeEach
	public void reset() {
		trigger.reset();
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		LogInspectionHelper.clearAllListeners( JdbcLogging.JDBC_LOGGER );
	}

	@Test
	public void testInsertGeneratedIdentityOnly(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final IdentityOnly entity = new IdentityOnly();
			session.persist( entity );
			session.flush();
			assertNoOrphanStatements( session );
			assertThat( entity.getId() ).isNotNull();
			assertThat( entity.getName() ).isNull();
		} );

		assertThat( trigger.wasTriggered() ).as( "Exception encountered while releasing statement" ).isFalse();
	}

	@Test
	public void testInsertGeneratedValuesAndIdentity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final IdentityAndValues entity = new IdentityAndValues();
			session.persist( entity );
			session.flush();
			assertNoOrphanStatements( session );
			assertThat( entity.getId() ).isNotNull();
			assertThat( entity.getName() ).isEqualTo( "default_name" );
		} );

		assertThat( trigger.wasTriggered() ).as( "Exception encountered while releasing statement" ).isFalse();
	}

	@Test
	public void testUpdateGeneratedValuesAndIdentity(SessionFactoryScope scope) {
		final Long id = scope.fromTransaction( session -> {
			final IdentityAndValues entity = new IdentityAndValues();
			session.persist( entity );
			session.flush();
			return entity.getId();
		} );

		scope.inTransaction( session -> {
			final IdentityAndValues entity = session.find( IdentityAndValues.class, id );
			entity.setData( "changed" );
			session.flush();
			assertNoOrphanStatements( session );
			assertThat( entity.getUpdateDate() ).isNotNull();
		} );

		assertThat( trigger.wasTriggered() ).as( "Exception encountered while releasing statement" ).isFalse();
	}

	@Test
	public void testInsertChildEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ChildEntity entity = new ChildEntity();
			session.persist( entity );
			session.flush();
			assertNoOrphanStatements( session );
			assertThat( entity.getId() ).isNotNull();
			assertThat( entity.getName() ).isEqualTo( "default_name" );
			assertThat( entity.getChildName() ).isEqualTo( "default_child_name" );
		} );

		assertThat( trigger.wasTriggered() ).as( "Exception encountered while releasing update statement" ).isFalse();
	}

	@Test
	public void testUpdateChildEntity(SessionFactoryScope scope) {
		final Long id = scope.fromTransaction( session -> {
			final ChildEntity entity = new ChildEntity();
			session.persist( entity );
			session.flush();
			return entity.getId();
		} );

		scope.inTransaction( session -> {
			final ChildEntity entity = session.find( ChildEntity.class, id );
			entity.setData( "changed" );
			session.flush();
			assertNoOrphanStatements( session );
			assertThat( entity.getUpdateDate() ).isNotNull();
			assertThat( entity.getChildUpdateDate() ).isNotNull();
		} );

		assertThat( trigger.wasTriggered() ).as( "Exception encountered while releasing update statement" ).isFalse();
	}

	private void assertNoOrphanStatements(SessionImplementor session) {
		final ResourceRegistry resourceRegistry = session.getJdbcCoordinator()
				.getLogicalConnection()
				.getResourceRegistry();
		assertThat( resourceRegistry.hasRegisteredResources() ).as( "Expected no registered resources" ).isFalse();
	}

	@Entity( name = "IdentityOnly" )
	public static class IdentityOnly {
		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "IdentityAndValues" )
	@SuppressWarnings( "unused" )
	public static class IdentityAndValues {
		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Long id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default_name'" )
		private String name;

		@UpdateTimestamp( source = SourceType.DB )
		private Date updateDate;

		private String data;

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Date getUpdateDate() {
			return updateDate;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	@Entity( name = "BaseEntity" )
	@Inheritance( strategy = InheritanceType.JOINED )
	@SuppressWarnings( "unused" )
	public static class BaseEntity {
		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Long id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default_name'" )
		private String name;

		@UpdateTimestamp( source = SourceType.DB )
		private Date updateDate;

		@SuppressWarnings( "FieldCanBeLocal" )
		private String data;

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Date getUpdateDate() {
			return updateDate;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	@Entity( name = "ChildEntity" )
	@SuppressWarnings( "unused" )
	public static class ChildEntity extends BaseEntity {
		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default_child_name'" )
		private String childName;

		@UpdateTimestamp( source = SourceType.DB )
		private Date childUpdateDate;

		public String getChildName() {
			return childName;
		}

		public Date getChildUpdateDate() {
			return childUpdateDate;
		}
	}
}
