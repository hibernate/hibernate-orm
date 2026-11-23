package org.hibernate.orm.test.stateless;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static jakarta.persistence.FetchType.EAGER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// @author Steve Ebersole
@SessionFactory
@DomainModel(annotatedClasses = EagerCollectionInStatelessTest.WithEagerCollection.class)
public class EagerCollectionInStatelessTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	@JiraKey("HHH-13404")
	void testScrollWithoutFetchJoin(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			for ( long id = 1; id <= 3; id++ ) {
				final var entity = new WithEagerCollection();
				entity.id = id;
				if ( id == 1 ) {
					entity.eager.addAll( Set.of( "Hello", "World" ) );
				}
				else if ( id == 2 ) {
					entity.eager.add( "Goodbye" );
				}
				entity.lazy.add( "lazy-" + id );
				session.insert( entity );
			}
		} );

		final List<WithEagerCollection> loaded = new ArrayList<>();
		scope.inStatelessTransaction( session -> {
			try ( var results = session.createSelectionQuery(
					"from WithEagerCollection order by id", WithEagerCollection.class
			).scroll( ScrollMode.FORWARD_ONLY ) ) {
				while ( results.next() ) {
					final var entity = results.get();
					assertTrue( Hibernate.isInitialized( entity.eager ) );
					assertFalse( Hibernate.isInitialized( entity.lazy ) );
					session.fetch( entity.lazy );
					assertTrue( Hibernate.isInitialized( entity.lazy ) );
					assertEquals( Set.of( "lazy-" + entity.id ), entity.lazy );
					loaded.add( entity );
				}
			}
		} );

		assertEquals( List.of( 1L, 2L, 3L ), loaded.stream().map( entity -> entity.id ).toList() );
		assertEquals( Set.of( "Hello", "World" ), loaded.get( 0 ).eager );
		assertEquals( Set.of( "Goodbye" ), loaded.get( 1 ).eager );
		assertEquals( Set.of(), loaded.get( 2 ).eager );
	}

	@Test
	void test(SessionFactoryScope scope) {
		scope.inStatelessTransaction(s-> {
			WithEagerCollection entity = new WithEagerCollection();
			entity.eager.add("Hello");
			entity.eager.add("World");
			entity.lazy.add("Goodbye");
			entity.lazy.add("World");
			s.insert(entity);
		});
		scope.inStatelessSession(s-> {
			WithEagerCollection entity = s.get(WithEagerCollection.class, 69L);
			assertTrue(Hibernate.isInitialized(entity.eager));
			assertFalse(Hibernate.isInitialized(entity.lazy));
			s.fetch(entity.lazy);
			assertTrue(Hibernate.isInitialized(entity.lazy));
			assertEquals(2, entity.eager.size());
			assertEquals(2, entity.lazy.size());
		});
		scope.inStatelessSession(s-> {
			WithEagerCollection entity =
					s.createSelectionQuery("where id= 69L", WithEagerCollection.class)
							.getSingleResult();
			assertTrue(Hibernate.isInitialized(entity.eager));
			assertFalse(Hibernate.isInitialized(entity.lazy));
			s.fetch(entity.lazy);
			assertTrue(Hibernate.isInitialized(entity.lazy));
			assertEquals(2, entity.eager.size());
			assertEquals(2, entity.lazy.size());
		});
	}

	@Entity(name = "WithEagerCollection")
	static class WithEagerCollection {
		@Id long id = 69L;
		@ElementCollection(fetch = EAGER)
		Set<String> eager = new HashSet<>();
		@ElementCollection
		Set<String> lazy = new HashSet<>();
	}
}
