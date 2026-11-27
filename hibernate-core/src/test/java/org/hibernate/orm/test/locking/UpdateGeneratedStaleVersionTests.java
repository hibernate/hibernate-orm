package org.hibernate.orm.test.locking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Version;
import org.hibernate.StaleObjectStateException;
import org.hibernate.annotations.Generated;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.VersionMatchMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;

@DomainModel(annotatedClasses = {
		UpdateGeneratedStaleVersionTests.Generation.class,
		UpdateGeneratedStaleVersionTests.SecondaryGeneration.class
})
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Fails at SERIALIZABLE isolation")
public class UpdateGeneratedStaleVersionTests {
	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 11, minorVersion = 6, microVersion = 2,
			versionMatchMode = VersionMatchMode.SAME_OR_NEWER,
			reason = "MariaDB will throw an error DB_RECORD_CHANGED when acquiring a lock on a record that have changed")
	void staleUpdateFailsOnVersion(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var created = new Generation();
			created.id = 1L;
			session.persist( created );
		} );

		assertThatThrownBy( () -> scope.inTransaction( session -> {
			final var stale = session.find( Generation.class, 1L );
			scope.inTransaction( other -> other.find( Generation.class, 1L ).counter++ );
			stale.counter++;
			session.flush();
		} ) )
				.isInstanceOf( OptimisticLockException.class )
				.hasCauseInstanceOf( StaleObjectStateException.class );
	}

	@Test
	@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 11, minorVersion = 6, microVersion = 2,
			versionMatchMode = VersionMatchMode.SAME_OR_NEWER,
			reason = "MariaDB will throw an error DB_RECORD_CHANGED when acquiring a lock on a record that have changed")
	void staleUpdateWithSecondaryTableFailsOnVersion(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var created = new SecondaryGeneration();
			created.id = 1L;
			session.persist( created );
		} );

		assertThatThrownBy( () -> scope.inTransaction( session -> {
			final var stale = session.find( SecondaryGeneration.class, 1L );
			scope.inTransaction( other -> other.find( SecondaryGeneration.class, 1L ).counter++ );
			stale.counter++;
			stale.detail++;
			session.flush();
		} ) )
				.isInstanceOf( OptimisticLockException.class )
				.hasCauseInstanceOf( StaleObjectStateException.class );
	}

	@Test
	void forcedVersionIncrement(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var created = new Generation();
			created.id = 1L;
			session.persist( created );
		} );

		scope.inTransaction( session ->
				session.lock( session.find( Generation.class, 1L ), LockModeType.PESSIMISTIC_FORCE_INCREMENT ) );
		scope.inTransaction( session ->
				session.lock( session.find( Generation.class, 1L ), LockModeType.OPTIMISTIC_FORCE_INCREMENT ) );

		scope.inTransaction( session -> assertThat( session.find( Generation.class, 1L ).version ).isEqualTo( 2 ) );
	}

	@Test
	@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 11, minorVersion = 6, microVersion = 2,
			versionMatchMode = VersionMatchMode.SAME_OR_NEWER,
			reason = "MariaDB will throw an error DB_RECORD_CHANGED when acquiring a lock on a record that have changed")
	void staleForcedVersionIncrementFailsOnVersion(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var created = new Generation();
			created.id = 1L;
			session.persist( created );
		} );

		assertThatThrownBy( () -> scope.inTransaction( session -> {
			final var stale = session.find( Generation.class, 1L );
			scope.inTransaction( other -> other.find( Generation.class, 1L ).counter++ );
			session.lock( stale, LockModeType.PESSIMISTIC_FORCE_INCREMENT );
		} ) )
				.isInstanceOf( StaleObjectStateException.class );
	}

	@Entity(name = "Generation")
	public static class Generation {
		@Id Long id;
		@Version int version;
		int counter;
		@Generated(event = {INSERT, UPDATE}, sql = "1") int generatedValue;
	}

	@Entity(name = "SecondaryGeneration")
	@SecondaryTable(name = "SecondaryGenerationDetail")
	public static class SecondaryGeneration {
		@Id Long id;
		@Version int version;
		int counter;
		@Column(table = "SecondaryGenerationDetail") int detail;
		@Generated(event = {INSERT, UPDATE}, sql = "1") int generatedValue;
	}
}
