package org.hibernate.orm.test.ondeletecascade;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.hibernate.action.queue.spi.QueueType;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.cfg.BatchSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

// MySQLDialect and a few others do not enable batching by default
@ServiceRegistry(settings = @Setting(name= BatchSettings.STATEMENT_BATCH_SIZE, value="15"))
@DomainModel( annotatedClasses = {
		OnDeleteJoinedInheritanceTest.A.class,
		OnDeleteJoinedInheritanceTest.B.class,
		OnDeleteJoinedInheritanceTest.C.class
} )
@SessionFactory( useCollectingStatementObserver = true)
class OnDeleteJoinedInheritanceTest {
	@Test
	void test(SessionFactoryScope scope) {
		var sqlObserver = scope.getCollectingStatementObserver();
		scope.inTransaction( em -> {
			B b = new B();
			b.id = 1;
			em.persist( b );
			C c = new C();
			c.id = 2;
			em.persist( c );
		} );
		// The GRAPH queue batches the inserts into the root table whereas the LEGACY queue
		// could not.
		assertThat( sqlObserver.getNumberOfBatches() ).isEqualTo( isGraphQueue( scope ) ? 3 : 4 );
		sqlObserver.clear();

		scope.inTransaction( em -> {
			A b = em.find( A.class, 1L );
			A c = em.getReference( A.class, 2L );
			assertThat( sqlObserver.getSqlQueries() ).hasSize( 1 );
			em.remove( b );
			em.remove( c );
		} );
		assertThat( sqlObserver.getSqlQueries() ).hasSize( usingDbDeleteCascade( scope ) ? 4 : 6 );

		scope.inTransaction( em -> {
			assertEquals( 0,
					em.createNativeQuery( "select count(*) from B", Integer.class )
							.getSingleResultOrNull() );
			assertEquals( 0,
					em.createNativeQuery( "select count(*) from C", Integer.class )
							.getSingleResultOrNull() );
			assertEquals( 0,
					em.createNativeQuery( "select count(*) from A", Integer.class )
							.getSingleResultOrNull() );
		} );
	}

	private static boolean isGraphQueue(SessionFactoryScope scope) {
		return scope.getSessionFactory().getActionQueueFactory().getConfiguredQueueType() == QueueType.GRAPH;
	}

	private static boolean usingDbDeleteCascade(SessionFactoryScope scope) {
		return scope.getSessionFactory()
				.getJdbcServices()
				.getDialect()
				.getForeignKeySupport()
				.supportsOnDeleteAction( org.hibernate.annotations.OnDeleteAction.CASCADE );
	}

	@Entity(name = "A")
	@Inheritance(strategy = InheritanceType.JOINED)
	static class A {
		@Id
		long id;
		boolean a;
	}

	@Entity(name = "B")
	@OnDelete(action = OnDeleteAction.CASCADE)
	static class B extends A {
		long b;
	}

	@Entity(name = "C")
	@OnDelete(action = OnDeleteAction.CASCADE)
	static class C extends A {
		int c;
	}
}
