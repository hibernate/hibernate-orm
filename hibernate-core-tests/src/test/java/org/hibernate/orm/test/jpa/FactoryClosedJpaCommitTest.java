package org.hibernate.orm.test.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.RollbackException;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionImplementor;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// JPA commit must preserve the shutdown validation failure as its cause.
///
/// @author Steve Ebersole
@Jpa(annotatedClasses = FactoryClosedJpaCommitTest.Item.class)
class FactoryClosedJpaCommitTest {
	@AfterEach
	void releaseFactory(EntityManagerFactoryScope scope) {
		scope.releaseEntityManagerFactory();
	}

	@Test
	void commitException(EntityManagerFactoryScope scope) {
		final var factory = scope.getEntityManagerFactory();
		try (var em = factory.createEntityManager()) {
			final var tx = em.getTransaction();
			tx.begin();
			em.persist( new Item() );
			factory.close();
			final var failure = assertThrows( RollbackException.class, tx::commit );
			assertInstanceOf( IllegalStateException.class, failure.getCause() );
			assertTrue( failure.getCause().getMessage().contains( "may contain unflushed changes" ) );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void sharedSessionWaitingForAutoClose(boolean closeFactory, EntityManagerFactoryScope scope) {
		final var factory = scope.getEntityManagerFactory().unwrap( SessionFactory.class );
		try (var parent = factory.openSession()) {
			final var tx = parent.beginTransaction();
			final var child = parent.sessionWithOptions().connection().openSession();
			child.persist( new Item() );
			child.close();
			assertTrue( !child.isOpen() );
			assertTrue( ((SessionImplementor) child).isOpenOrWaitingForAutoClose() );
			if ( closeFactory ) {
				factory.close();
			}
			tx.commit();
		}
	}

	@Entity(name = "JpaShutdownItem")
	static class Item {
		@Id
		Long id = 1L;
	}
}
