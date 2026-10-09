package org.hibernate.orm.test.locking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Version;
import org.hibernate.StaleObjectStateException;
import org.hibernate.annotations.Generated;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
