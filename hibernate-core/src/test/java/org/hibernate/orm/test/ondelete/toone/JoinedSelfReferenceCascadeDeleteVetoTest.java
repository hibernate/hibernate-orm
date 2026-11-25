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
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = {
		JoinedSelfReferenceCascadeDeleteVetoTest.Base.class,
		JoinedSelfReferenceCascadeDeleteVetoTest.Child.class
})
@ServiceRegistry(settings = @Setting(name = "hibernate.flush.queue.type", value = "graph"))
@SessionFactory
@RequiresDialect(H2Dialect.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCircularCascadeDeleteCheck.class)
public class JoinedSelfReferenceCascadeDeleteVetoTest {
	@Test
	void vetoedDeleteKeepsRowsAndSelfReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var child = new Child();
			child.id = 1;
			session.persist( child );
			session.flush();
			child.self = child;
		} );

		final var vetoCount = new AtomicInteger();
		scope.getSessionFactory().getEventListenerRegistry().appendListeners( EventType.PRE_DELETE, event -> {
			if ( event.getEntity() instanceof Child && Integer.valueOf( 1 ).equals( event.getId() ) ) {
				vetoCount.incrementAndGet();
				return true;
			}
			return false;
		} );

		scope.inTransaction( session -> session.remove( session.find( Child.class, 1 ) ) );
		assertEquals( 1, vetoCount.get() );
		scope.inTransaction( session -> {
			assertEquals( 1L, session.createNativeQuery(
					"select count(*) from JoinedVetoBase where id = 1", Long.class
			).getSingleResult() );
			assertEquals( 1L, session.createNativeQuery(
					"select count(*) from JoinedVetoChild where id = 1", Long.class
			).getSingleResult() );
			assertEquals( 1, session.createNativeQuery(
					"select self_id from JoinedVetoBase where id = 1", Integer.class
			).getSingleResult() );
		} );
	}

	@Entity(name = "JoinedVetoBase")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Base {
		@Id
		Integer id;
		@ManyToOne
		@JoinColumn(name = "self_id")
		@OnDelete(action = OnDeleteAction.CASCADE)
		Child self;
	}

	@Entity(name = "JoinedVetoChild")
	public static class Child extends Base {
		String description;
	}
}
