package org.hibernate.orm.test.cache;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Removing an owner must evict its READ_ONLY collection cache entry.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		ReadOnlyCollectionOwnerDeletionTest.Post.class,
		ReadOnlyCollectionOwnerDeletionTest.Comment.class
})
@ServiceRegistry(settings = @Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"))
@SessionFactory
@JiraKey("HHH-10753")
class ReadOnlyCollectionOwnerDeletionTest {
	@BeforeEach
	void populate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var post = new Post();
			post.id = 1L;
			for ( long id = 1; id <= 2; id++ ) {
				var comment = new Comment();
				comment.id = id;
				comment.post = post;
				post.comments.add( comment );
			}
			session.persist( post );
		} );
		scope.inTransaction( session -> assertThat( session.find( Post.class, 1L ).comments ).hasSize( 2 ) );
		var cache = scope.getSessionFactory().getCache();
		assertThat( cache.containsCollection( Post.class.getName() + ".comments", 1L ) ).isTrue();
		assertThat( cache.containsEntity( Post.class, 1L ) ).isTrue();
		assertThat( cache.containsEntity( Comment.class, 1L ) ).isTrue();
		assertThat( cache.containsEntity( Comment.class, 2L ) ).isTrue();
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	@Test
	void testInitializedCollection(SessionFactoryScope scope) {
		assertDeletion( scope, true );
	}

	@Test
	void testUninitializedCollection(SessionFactoryScope scope) {
		assertDeletion( scope, false );
	}

	private void assertDeletion(SessionFactoryScope scope, boolean initialize) {
		scope.inTransaction( session -> {
			var post = session.find( Post.class, 1L );
			assertThat( Hibernate.isInitialized( post.comments ) ).isFalse();
			if ( initialize ) {
				assertThat( post.comments ).hasSize( 2 );
			}
			session.remove( post );
		} );
		var cache = scope.getSessionFactory().getCache();
		assertThat( cache.containsCollection( Post.class.getName() + ".comments", 1L ) ).isFalse();
		assertThat( cache.containsEntity( Post.class, 1L ) ).isFalse();
		assertThat( cache.containsEntity( Comment.class, 1L ) ).isFalse();
		assertThat( cache.containsEntity( Comment.class, 2L ) ).isFalse();
		scope.inTransaction( session -> {
			assertThat( session.find( Post.class, 1L ) ).isNull();
			assertThat( session.createQuery( "from ReadOnlyDeleteComment", Comment.class ).getResultList() ).isEmpty();
		} );
	}

	@Entity(name = "ReadOnlyDeletePost")
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
	public static class Post {
		@Id
		Long id;
		@Version
		int version;
		@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
		@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
		List<Comment> comments = new ArrayList<>();
	}

	@Entity(name = "ReadOnlyDeleteComment")
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
	public static class Comment {
		@Id
		Long id;
		@ManyToOne(fetch = FetchType.LAZY)
		Post post;
	}
}
