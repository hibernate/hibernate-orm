/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import java.util.stream.Stream;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityAgent;
import jakarta.persistence.ExcludedFromVersioning;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import org.hibernate.HibernateException;
import org.hibernate.annotations.Generated;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.generator.EventType.UPDATE;

@DomainModel(annotatedClasses = {
		ExcludedLobVersioningTests.VersionedClob.class,
		ExcludedLobVersioningTests.VersionedBlob.class,
		ExcludedLobVersioningTests.ExcludedLobs.class,
		ExcludedLobVersioningTests.GeneratedValueWithLob.class,
		ExcludedLobVersioningTests.ReadOnlyLob.class,
		ExcludedLobVersioningTests.LobWithoutExclusions.class
})
@SessionFactory(useCollectingStatementObserver = true)
@JiraKey("HHH-20828")
public class ExcludedLobVersioningTests {
	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	static Stream<ExcludedCounter> versionedLobs() {
		return Stream.of( new VersionedClob(), new VersionedBlob() );
	}

	@ParameterizedTest
	@MethodSource("versionedLobs")
	void agentRejectsNonExcludedLobBeforeUpdating(ExcludedCounter entity, SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( entity ) );
		entity.counter++;
		scope.getCollectingStatementObserver().clear();
		assertThatThrownBy( () -> scope.getSessionFactory()
				.runInTransaction( EntityAgent.class, agent -> agent.update( entity ) ) )
				.isInstanceOf( HibernateException.class )
				.hasMessageContainingAll( entity.getClass().getSimpleName(), "lob", "ExcludedFromVersioning" );
		assertThat( entity.version ).isZero();
		scope.getCollectingStatementObserver().assertQueries().isEmpty();
		scope.inTransaction( session -> {
			final var reloaded = session.find( entity.getClass(), entity.id );
			assertThat( reloaded.version ).isZero();
			assertThat( reloaded.counter ).isZero();
		} );
	}

	@ParameterizedTest
	@MethodSource("versionedLobs")
	void statefulUpdateCanUseItsSnapshot(ExcludedCounter entity, SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( entity ) );
		scope.inTransaction( session -> {
			final var managed = session.find( entity.getClass(), entity.id );
			managed.counter++;
		} );
		scope.inTransaction( session -> {
			final var managed = session.find( entity.getClass(), entity.id );
			assertThat( managed.counter ).isOne();
			assertThat( managed.version ).isZero();
			if ( managed instanceof VersionedClob clob ) {
				clob.lob = "after";
			}
			else if ( managed instanceof VersionedBlob blob ) {
				blob.lob = new byte[] { 2 };
			}
		} );
		scope.inTransaction( session -> {
			final var reloaded = session.find( entity.getClass(), entity.id );
			assertThat( reloaded.version ).isOne();
			if ( reloaded instanceof VersionedClob clob ) {
				assertThat( clob.lob ).isEqualTo( "after" );
			}
			else if ( reloaded instanceof VersionedBlob blob ) {
				assertThat( blob.lob ).containsExactly( (byte) 2 );
			}
		} );
	}

	@Test
	void excludedLobsDoNotParticipateInComparison(SessionFactoryScope scope) {
		final var entity = new ExcludedLobs();
		scope.inTransaction( session -> session.persist( entity ) );
		for ( int i = 0; i < 5; i++ ) {
			entity.textData = i == 2 || i == 3 ? null : "after";
			entity.binaryData = i == 2 || i == 3 ? null : new byte[] { 2 };
			if ( i == 4 ) {
				entity.title = "after";
			}
			scope.getCollectingStatementObserver().clear();
			scope.getSessionFactory().runInTransaction( EntityAgent.class, agent -> agent.update( entity ) );
			assertThat( entity.version ).isEqualTo( i == 4 ? 1 : 0 );
			scope.getCollectingStatementObserver().assertQueries()
					.anySatisfy( sql -> {
						assertThat( sql ).contains( "case when" );
						final String condition = sql.substring( sql.indexOf( "case when" ), sql.indexOf( " then " ) );
						assertThat( condition ).doesNotContain( "textData", "binaryData" );
					} );
			scope.inTransaction( session -> {
				final var reloaded = session.find( ExcludedLobs.class, entity.id );
				assertThat( reloaded.version ).isEqualTo( entity.version );
				assertThat( reloaded.textData ).isEqualTo( entity.textData );
				assertThat( reloaded.binaryData ).isEqualTo( entity.binaryData );
				assertThat( reloaded.title ).isEqualTo( entity.title );
			} );
		}
	}

	@Test
	void nonExcludedUpdateGenerationStillUsesOrdinaryVersioning(SessionFactoryScope scope) {
		final var entity = new GeneratedValueWithLob();
		scope.inTransaction( session -> session.persist( entity ) );
		entity.counter++;
		scope.getSessionFactory().runInTransaction( EntityAgent.class, agent -> agent.update( entity ) );
		assertThat( entity.version ).isOne();
		scope.inTransaction( session -> {
			final var reloaded = session.find( GeneratedValueWithLob.class, entity.id );
			assertThat( reloaded.version ).isOne();
			assertThat( reloaded.counter ).isOne();
			assertThat( reloaded.stamp ).isOne();
		} );
	}

	@Test
	void nonUpdateableLobDoesNotRequireComparison(SessionFactoryScope scope) {
		final var entity = new ReadOnlyLob();
		scope.inTransaction( session -> session.persist( entity ) );
		entity.counter++;
		scope.getSessionFactory().runInTransaction( EntityAgent.class, agent -> agent.update( entity ) );
		assertThat( entity.version ).isZero();
		scope.inTransaction( session -> {
			final var reloaded = session.find( ReadOnlyLob.class, entity.id );
			assertThat( reloaded.version ).isZero();
			assertThat( reloaded.counter ).isOne();
		} );
	}

	@Test
	void lobWithoutExclusionsStillUsesOrdinaryVersioning(SessionFactoryScope scope) {
		final var entity = new LobWithoutExclusions();
		scope.inTransaction( session -> session.persist( entity ) );
		entity.lob = "after";
		scope.getSessionFactory().runInTransaction( EntityAgent.class, agent -> agent.update( entity ) );
		assertThat( entity.version ).isOne();
		scope.inTransaction( session -> {
			final var reloaded = session.find( LobWithoutExclusions.class, entity.id );
			assertThat( reloaded.version ).isOne();
			assertThat( reloaded.lob ).isEqualTo( "after" );
		} );
	}

	@MappedSuperclass
	public static class VersionedEntity {
		@Id long id = 1L;
		@Version int version;
	}

	@MappedSuperclass
	public static class ExcludedCounter extends VersionedEntity {
		@ExcludedFromVersioning int counter;
	}

	@Entity(name = "VersionedClob")
	public static class VersionedClob extends ExcludedCounter {
		@Lob String lob = "before";
	}

	@Entity(name = "VersionedBlob")
	public static class VersionedBlob extends ExcludedCounter {
		@Lob byte[] lob = { 1 };
	}

	@Entity(name = "ExcludedLobs")
	public static class ExcludedLobs extends VersionedEntity {
		String title = "before";
		@Lob @ExcludedFromVersioning String textData;
		@Lob @ExcludedFromVersioning byte[] binaryData;
	}

	@Entity(name = "GeneratedValueWithLob")
	public static class GeneratedValueWithLob extends ExcludedCounter {
		@Lob String lob = "before";
		@Generated(event = UPDATE, sql = "1") int stamp;
	}

	@Entity(name = "ReadOnlyLob")
	public static class ReadOnlyLob extends ExcludedCounter {
		@Lob @Column(updatable = false) String lob = "before";
	}

	@Entity(name = "LobWithoutExclusions")
	public static class LobWithoutExclusions extends VersionedEntity {
		@Lob String lob = "before";
	}
}
