package org.hibernate.orm.test.softdelete;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Hibernate;
import org.hibernate.annotations.SoftDelete;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link Hibernate#get(Map, Object)} of an uninitialized map does not find a
 * soft-deleted entry, which the initialized map does not contain.
 */
@DomainModel(annotatedClasses = SoftDeleteMapLookupTests.Owner.class)
@SessionFactory
@JiraKey("HHH-21218")
public class SoftDeleteMapLookupTests {

	@Test
	void getSoftDeletedEntry(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owner = new Owner();
			owner.id = 1;
			owner.entries.put( "a", "1" );
			owner.entries.put( "b", "2" );
			session.persist( owner );
		} );
		scope.inTransaction( session -> session.find( Owner.class, 1 ).entries.remove( "a" ) );
		scope.inSession( session -> {
			final var owner = session.find( Owner.class, 1 );
			assertNull( Hibernate.get( owner.entries, "a" ) );
			assertEquals( "2", Hibernate.get( owner.entries, "b" ) );
			assertFalse( Hibernate.isInitialized( owner.entries ) );
			assertEquals( Map.of( "b", "2" ), owner.entries );
		} );
	}

	@Entity(name = "Owner")
	static class Owner {
		@Id
		int id;
		@ElementCollection
		@SoftDelete
		Map<String, String> entries = new HashMap<>();
	}
}
