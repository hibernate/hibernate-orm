package org.hibernate.orm.test.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityAgent;
import jakarta.persistence.ExcludedFromVersioning;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.annotations.Generated;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;

@DomainModel(annotatedClasses = {
		ExcludedGeneratedVersioningTests.VersionedGeneration.class,
		ExcludedGeneratedVersioningTests.ExcludedGeneration.class,
		ExcludedGeneratedVersioningTests.InsertGeneration.class
})
@SessionFactory(useCollectingStatementObserver = true)
@JiraKey("HHH-20828")
public class ExcludedGeneratedVersioningTests {
	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void nonExcludedUpdateGenerationUsesNormalVersioning(SessionFactoryScope scope) {
		final var entity = scope.fromTransaction( session -> {
			final var created = new VersionedGeneration();
			created.id = 1L;
			session.persist( created );
			return created;
		} );
		for ( int expectedVersion = 1; expectedVersion <= 2; expectedVersion++ ) {
			entity.counter++;
			scope.getCollectingStatementObserver().clear();
			scope.getSessionFactory().runInTransaction( EntityAgent.class, agent -> agent.update( entity ) );
			scope.getCollectingStatementObserver().assertQueries()
					.isNotEmpty().allSatisfy( sql -> assertThat( sql ).doesNotContain( "case when" ) );
			assertThat( entity.version ).isEqualTo( expectedVersion );
			scope.inTransaction( session -> {
				final var reloaded = session.find( VersionedGeneration.class, entity.id );
				assertThat( reloaded.version ).isEqualTo( entity.version );
				assertThat( reloaded.counter ).isEqualTo( entity.counter );
				assertThat( reloaded.generatedValue ).isEqualTo( 1 );
			} );
		}
	}

	@Test
	void excludedUpdateGenerationKeepsConditionalVersioning(SessionFactoryScope scope) {
		final var entity = scope.fromTransaction( session -> {
			final var created = new ExcludedGeneration();
			created.id = 1L;
			created.name = "before";
			session.persist( created );
			return created;
		} );
		for ( int i = 0; i < 3; i++ ) {
			if ( i == 1 ) {
				entity.name = "after";
			}
			scope.getCollectingStatementObserver().clear();
			scope.getSessionFactory().runInTransaction( EntityAgent.class, agent -> agent.update( entity ) );
			scope.getCollectingStatementObserver().assertQueries()
					.anySatisfy( sql -> assertThat( sql ).contains( "case when" ) );
			assertThat( entity.version ).isEqualTo( i == 0 ? 0 : 1 );
			scope.inTransaction( session -> {
				final var reloaded = session.find( ExcludedGeneration.class, entity.id );
				assertThat( reloaded.version ).isEqualTo( entity.version );
				assertThat( reloaded.name ).isEqualTo( entity.name );
				assertThat( reloaded.generatedValue ).isEqualTo( 1 );
			} );
		}
	}

	@Test
	void statefulUpdateRetrievesExcludedGeneratedValue(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = new ExcludedGeneration();
			entity.id = 1L;
			entity.name = "before";
			session.persist( entity );
		} );
		scope.inTransaction( session -> {
			final var entity = session.find( ExcludedGeneration.class, 1L );
			entity.name = "after";
			session.flush();
			assertThat( entity.version ).isOne();
			assertThat( entity.generatedValue ).isOne();
		} );
	}

	@Test
	void insertGenerationDoesNotDisableConditionalVersioning(SessionFactoryScope scope) {
		final var entity = scope.fromTransaction( session -> {
			final var created = new InsertGeneration();
			created.id = 1L;
			session.persist( created );
			return created;
		} );
		entity.counter++;
		scope.getSessionFactory().runInTransaction( EntityAgent.class, agent -> agent.update( entity ) );
		assertThat( entity.version ).isZero();
		scope.inTransaction( session -> {
			final var reloaded = session.find( InsertGeneration.class, entity.id );
			assertThat( reloaded.version ).isZero();
			assertThat( reloaded.counter ).isOne();
		} );
	}

	@Entity(name = "VersionedGeneration")
	public static class VersionedGeneration {
		@Id long id;
		@Version int version;
		@ExcludedFromVersioning int counter;
		@Generated(event = UPDATE, sql = "1") int generatedValue;
	}

	@Entity(name = "ExcludedGeneration")
	public static class ExcludedGeneration {
		@Id long id;
		@Version int version;
		String name;
		@ExcludedFromVersioning
		@Generated(event = UPDATE, sql = "1") int generatedValue;
	}

	@Entity(name = "InsertGeneration")
	public static class InsertGeneration {
		@Id long id;
		@Version int version;
		@ExcludedFromVersioning int counter;
		@Generated(event = INSERT, sql = "1") int generatedValue;
	}
}
