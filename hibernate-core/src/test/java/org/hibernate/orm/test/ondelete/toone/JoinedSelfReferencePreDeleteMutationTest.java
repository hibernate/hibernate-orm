package org.hibernate.orm.test.ondelete.toone;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.event.spi.EventType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = {
		JoinedSelfReferencePreDeleteMutationTest.Base.class,
		JoinedSelfReferencePreDeleteMutationTest.Child.class
})
@ServiceRegistry(settings = @Setting(name = "hibernate.flush.queue.type", value = "graph"))
@SessionFactory
@RequiresDialect(H2Dialect.class)
public class JoinedSelfReferencePreDeleteMutationTest {
	@Test
	void removeAfterPreDeleteListenerCreatesPhysicalSelfReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var child = new Child();
			child.id = 1;
			session.persist( child );
		} );

		final var listenerCount = new AtomicInteger();
		scope.getSessionFactory().getEventListenerRegistry().appendListeners( EventType.PRE_DELETE, event -> {
			if ( event.getEntity() instanceof Child && Integer.valueOf( 1 ).equals( event.getId() ) ) {
				event.getSession().doWork( connection -> {
					try (var statement = connection.prepareStatement(
							"update JoinedPreDeleteBase set self_id = id where id = ?" )) {
						statement.setInt( 1, 1 );
						assertEquals( 1, statement.executeUpdate() );
					}
				} );
				listenerCount.incrementAndGet();
			}
			return false;
		} );

		scope.inTransaction( session -> session.remove( session.find( Child.class, 1 ) ) );
		assertEquals( 1, listenerCount.get() );
		scope.inTransaction( session -> {
			assertEquals( 0L, session.createNativeQuery(
					"select count(*) from JoinedPreDeleteBase", Long.class
			).getSingleResult() );
			assertEquals( 0L, session.createNativeQuery(
					"select count(*) from JoinedPreDeleteChild", Long.class
			).getSingleResult() );
		} );
	}

	@Entity(name = "JoinedPreDeleteBase")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Base {
		@Id
		Integer id;
		@ManyToOne
		@JoinColumn(name = "self_id")
		@OnDelete(action = OnDeleteAction.CASCADE)
		Child self;
	}

	@Entity(name = "JoinedPreDeleteChild")
	public static class Child extends Base {
		String description;
	}
}
