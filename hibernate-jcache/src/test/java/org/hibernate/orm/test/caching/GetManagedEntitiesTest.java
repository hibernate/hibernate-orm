package org.hibernate.orm.test.caching;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.Session;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(annotatedClasses = GetManagedEntitiesTest.ManagedEntity.class)
public class GetManagedEntitiesTest {

	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			for (int i=0; i<5; i++) {
				em.persist( new ManagedEntity() );
			}
		} );
		scope.inTransaction( em -> {
			final Session session = em.unwrap( Session.class );
			assertNoManagedEntities( session );
			assertEquals( 5, em.createQuery( "from ME", ManagedEntity.class ).getResultList().size() );
			assertEquals( 5, session.getManagedEntities().size() );
			assertEquals( 5, session.getManagedEntities( ManagedEntity.class ).size() );
			assertEquals( 5, session.getManagedEntities( ManagedEntity.class.getName() ).size() );
			session.getManagedEntities( ManagedEntity.class ).forEach(session::refresh);
			session.getManagedEntities( ManagedEntity.class ).forEach(session::detach);
			session.clear();
			assertNoManagedEntities( session );
		} );
	}

	private static void assertNoManagedEntities(Session session) {
		assertTrue( session.getManagedEntities().isEmpty() );
		assertTrue( session.getManagedEntities( ManagedEntity.class ).isEmpty() );
		assertTrue( session.getManagedEntities( ManagedEntity.class.getName() ).isEmpty() );
		assertTrue( session.getManagedEntities( session.getMetamodel().entity( ManagedEntity.class ) ).isEmpty() );
	}

	@Entity(name = "ME")
	static class ManagedEntity {
		@Id @GeneratedValue
		Long id;
	}
}
