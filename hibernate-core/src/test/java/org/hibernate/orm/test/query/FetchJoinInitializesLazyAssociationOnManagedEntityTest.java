package org.hibernate.orm.test.query;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static jakarta.persistence.FetchType.LAZY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = {
		FetchJoinInitializesLazyAssociationOnManagedEntityTest.Parent.class,
		FetchJoinInitializesLazyAssociationOnManagedEntityTest.ChildWithLong.class,
		FetchJoinInitializesLazyAssociationOnManagedEntityTest.ChildWithEmbeddedId.class
})
@SessionFactory
@JiraKey("HHH-21054")
class FetchJoinInitializesLazyAssociationOnManagedEntityTest {
	@BeforeEach
	void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var childWithLong = new ChildWithLong();
			childWithLong.id = 1L;
			final var childWithEmbeddedId = new ChildWithEmbeddedId();
			childWithEmbeddedId.id = new ChildId();
			childWithEmbeddedId.id.first = 1L;
			final var parent = new Parent();
			parent.id = 1L;
			parent.childWithLong = childWithLong;
			parent.childWithEmbeddedId = childWithEmbeddedId;
			session.persist( childWithLong );
			session.persist( childWithEmbeddedId );
			session.persist( parent );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Parent" ).executeUpdate();
			session.createMutationQuery( "delete from ChildWithLong" ).executeUpdate();
			session.createMutationQuery( "delete from ChildWithEmbeddedId" ).executeUpdate();
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"childWithLong",
			"childWithEmbeddedId",
			"childWithLong left join fetch p.childWithEmbeddedId"
	})
	void testFetchJoinInitializesLazyAssociation(String fetch, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var parent = session.createQuery( "from Parent p where p.id = 1", Parent.class )
					.getSingleResult();
			assertFalse( Hibernate.isInitialized( parent.childWithLong ) );
			assertFalse( Hibernate.isInitialized( parent.childWithEmbeddedId ) );

			final var fetchedParent = session.createQuery(
							"from Parent p left join fetch p." + fetch + " where p.id = 1", Parent.class )
					.getSingleResult();
			assertSame( parent, fetchedParent );
			if ( fetch.contains( "childWithLong" ) ) {
				assertTrue( Hibernate.isInitialized( parent.childWithLong ) );
			}
			else {
				assertFalse( Hibernate.isInitialized( parent.childWithLong ) );
			}
			if ( fetch.contains( "childWithEmbeddedId" ) ) {
				assertTrue( Hibernate.isInitialized( parent.childWithEmbeddedId ) );
			}
			else {
				assertFalse( Hibernate.isInitialized( parent.childWithEmbeddedId ) );
			}
		} );
	}

	@Entity(name = "Parent")
	static class Parent {
		@Id
		private long id;
		@ManyToOne(fetch = LAZY)
		private ChildWithLong childWithLong;
		@ManyToOne(fetch = LAZY)
		private ChildWithEmbeddedId childWithEmbeddedId;
	}

	@Entity(name = "ChildWithLong")
	static class ChildWithLong {
		@Id
		private long id;
	}

	@Entity(name = "ChildWithEmbeddedId")
	static class ChildWithEmbeddedId {
		@EmbeddedId
		private ChildId id;
	}

	@Embeddable
	static class ChildId {
		private long first;
	}
}
