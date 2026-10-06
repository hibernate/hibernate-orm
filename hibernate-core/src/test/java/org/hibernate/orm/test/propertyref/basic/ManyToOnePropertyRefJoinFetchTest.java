package org.hibernate.orm.test.propertyref.basic;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies that fetching a many-to-one by a non-primary-key property does not require another select.
///
/// @author Steve Ebersole
@DomainModel(xmlMappings = "org/hibernate/orm/test/propertyref/basic/ManyToOnePropertyRefJoinFetch.hbm.xml")
@SessionFactory(useCollectingStatementObserver = true)
@JiraKey("HHH-2473")
public class ManyToOnePropertyRefJoinFetchTest {

	@BeforeEach
	void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var target = new Target();
			target.id = 10L;
			target.code = "target-code";
			target.name = "Fetched target";
			session.persist( target );

			for ( long id = 1; id <= 2; id++ ) {
				var owner = new Owner();
				owner.id = id;
				owner.target = target;
				session.persist( owner );
			}
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testMappingJoinFetch(SessionFactoryScope scope) {
		var observer = scope.getCollectingStatementObserver();
		observer.clear();
		scope.inTransaction( session -> {
			var owner = session.find( Owner.class, 1L );
			assertNotNull( owner );
			assertTarget( owner.target );
			observer.assertQueries().hasSize( 1 );
			observer.assertQuery( 0 ).containsToken( " join ", 1 );
		} );
	}

	@Test
	void testHqlJoinFetch(SessionFactoryScope scope) {
		var observer = scope.getCollectingStatementObserver();
		observer.clear();
		scope.inTransaction( session -> {
			var owners = session.createQuery(
					"from " + Owner.class.getName() + " o join fetch o.target order by o.id",
					Owner.class
			).getResultList();
			assertEquals( 2, owners.size() );
			assertTarget( owners.get( 0 ).target );
			assertTarget( owners.get( 1 ).target );
			assertSame( owners.get( 0 ).target, owners.get( 1 ).target );
			observer.assertQueries().hasSize( 1 );
			observer.assertQuery( 0 ).containsToken( " join ", 1 );
		} );
	}

	private static void assertTarget(Target target) {
		assertNotNull( target );
		assertTrue( Hibernate.isInitialized( target ) );
		assertEquals( 10L, target.id );
		assertEquals( "target-code", target.code );
		assertEquals( "Fetched target", target.name );
	}

	public static class Owner {
		private Long id;
		private Target target;
	}

	public static class Target {
		private Long id;
		private String code;
		private String name;
	}
}
