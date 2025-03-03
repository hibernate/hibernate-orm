/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.JdbcLogging;

import org.hibernate.testing.logger.LogInspectionHelper;
import org.hibernate.testing.logger.TriggerOnPrefixLogListener;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		BatchRowCountWarningTest.BaseEntity.class,
		BatchRowCountWarningTest.SubEntity.class,
		BatchRowCountWarningTest.MyEntity.class,
		BatchRowCountWarningTest.SpamEntity.class,
		BatchRowCountWarningTest.JoinTableEntity.class,
} )
@ServiceRegistry( settings = @Setting( name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "3" ) )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16713" )
public class BatchRowCountWarningTest {
	private TriggerOnPrefixLogListener trigger;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		trigger = new TriggerOnPrefixLogListener( "HHH100001" );
		LogInspectionHelper.registerListener( trigger, JdbcLogging.JDBC_MESSAGE_LOGGER );
		scope.inTransaction( session -> {
			session.persist( new MyEntity( 1L, "Nicola", null ) );
			session.persist( new MyEntity( 2L, "Stefano", "Ste" ) );
			session.persist( new JoinTableEntity( 1L, new SpamEntity() ) );
		} );
	}

	@BeforeEach
	public void reset() {
		trigger.reset();
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		LogInspectionHelper.clearAllListeners( JdbcLogging.JDBC_MESSAGE_LOGGER );
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from BaseEntity" ).executeUpdate();
			session.createQuery( "from JoinTableEntity", JoinTableEntity.class )
					.getResultList()
					.forEach( session::remove );
		} );
	}

	@Test
	public void testPersistBase(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new BaseEntity( 10L ) ) );
		scope.inTransaction( session -> assertThat( session.find( BaseEntity.class, 10L ) ).isNotNull() );
		assertThat( trigger.wasTriggered() ).as( "Warning message was triggered" ).isFalse();
	}

	@Test
	public void testPersistJoined(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new MyEntity( 3L, "Vittorio", "Vitto" ) ) );
		scope.inTransaction( session -> assertThat( session.find( MyEntity.class, 3L )
															.getName() ).isEqualTo( "Vittorio" ) );
		assertThat( trigger.wasTriggered() ).as( "Warning message was triggered" ).isFalse();
	}

	@Test
	public void testPersistJoinTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new JoinTableEntity( 2L, new SpamEntity() ) ) );
		scope.inTransaction( session -> assertThat( session.find( JoinTableEntity.class, 2L )
															.getSpamEntity() ).isNotNull() );
		assertThat( trigger.wasTriggered() ).as( "Warning message was triggered" ).isFalse();
	}

	@Test
	public void testPersistMultipleJoined(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new MyEntity( 96L, "multi_1", null ) );
			session.persist( new MyEntity( 97L, "multi_2", null ) );
			session.persist( new MyEntity( 98L, "multi_3", null ) );
			session.persist( new MyEntity( 99L, "multi_4", null ) );
		} );
		assertThat( trigger.wasTriggered() ).as( "Warning message was triggered" ).isFalse();
	}

	@Test
	public void testUpdateJoined(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MyEntity entity = session.find( MyEntity.class, 1L );
			entity.setNickname( "Nico" );
		} );
		scope.inTransaction( session -> assertThat( session.find( MyEntity.class, 1L )
															.getNickname() ).isEqualTo( "Nico" ) );
		assertThat( trigger.wasTriggered() ).as( "Warning message was triggered" ).isFalse();
	}

	@Test
	public void testDeleteJoined(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MyEntity entity = session.find( MyEntity.class, 2L );
			session.remove( entity );
		} );
		scope.inTransaction( session -> assertThat( session.find( MyEntity.class, 2L ) ).isNull() );
		assertThat( trigger.wasTriggered() ).as( "Warning message was triggered" ).isFalse();
	}

	@Test
	public void testDeleteJoinTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final JoinTableEntity entity = session.find( JoinTableEntity.class, 1L );
			session.remove( entity );
		} );
		scope.inTransaction( session -> assertThat( session.find( JoinTableEntity.class, 1L ) ).isNull() );
		assertThat( trigger.wasTriggered() ).as( "Warning message was triggered" ).isFalse();
	}

	@Entity( name = "BaseEntity" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static class BaseEntity {
		@Id
		private Long id;

		public BaseEntity() {
		}

		public BaseEntity(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "SubEntity" )
	public static class SubEntity extends BaseEntity {
		private String name;

		public SubEntity() {
		}

		public SubEntity(Long id, String name) {
			super( id );
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "MyEntity" )
	public static class MyEntity extends SubEntity {
		private String nickname;

		public MyEntity() {
		}

		public MyEntity(Long id, String name, String nickname) {
			super( id, name );
			this.nickname = nickname;
		}

		public String getNickname() {
			return nickname;
		}

		public void setNickname(String nickname) {
			this.nickname = nickname;
		}
	}

	@Entity( name = "SpamEntity" )
	public static class SpamEntity {
		@Id
		@GeneratedValue
		private Long id;
	}

	@Entity( name = "JoinTableEntity" )
	public static class JoinTableEntity {
		@Id
		private Long id;

		@ManyToOne( cascade = CascadeType.ALL )
		@JoinTable( name = "foo_spam", joinColumns = @JoinColumn( name = "foo_id" ), inverseJoinColumns = @JoinColumn( name = "spam_id" ) )
		private SpamEntity spamEntity;

		public JoinTableEntity() {

		}

		public JoinTableEntity(Long id, SpamEntity spam) {
			this.id = id;
			this.spamEntity = spam;
		}

		public SpamEntity getSpamEntity() {
			return spamEntity;
		}
	}
}
