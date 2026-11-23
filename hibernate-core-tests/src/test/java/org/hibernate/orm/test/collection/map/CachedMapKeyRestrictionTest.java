package org.hibernate.orm.test.collection.map;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyJoinColumn;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.cfg.CacheSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = { CachedMapKeyRestrictionTest.Owner.class, CachedMapKeyRestrictionTest.Tag.class })
@ServiceRegistry(settings = @Setting(name = CacheSettings.USE_SECOND_LEVEL_CACHE, value = "true"))
@SessionFactory
@JiraKey("HHH-21216")
class CachedMapKeyRestrictionTest {
	@Test
	void changingKeyVisibility(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var key = new Tag( 1 );
			final var value = new Tag( 2 );
			final var owner = new Owner();
			session.persist( key );
			session.persist( value );
			owner.tags.put( key, value );
			session.persist( owner );
		} );
		scope.inSession( session -> {
			final var tags = session.find( Owner.class, 1 ).tags;
			Hibernate.initialize( tags );
			assertEquals( 1, tags.size() );
		} );
		scope.inTransaction( session -> session.find( Tag.class, 1 ).visible = 0 );
		scope.inSession( session -> {
			final var tags = session.find( Owner.class, 1 ).tags;
			assertEquals( 0, session.createSelectionQuery(
					"select size(o.tags) from MapOwner o", Integer.class ).getSingleResult() );
			Hibernate.initialize( tags );
			assertEquals( 0, tags.size() );
		} );
	}

	@Entity(name = "MapOwner")
	static class Owner {
		@Id int id = 1;
		@ManyToMany
		@MapKeyJoinColumn(name = "key_id")
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		Map<Tag, Tag> tags = new HashMap<>();
	}

	@Entity(name = "Tag")
	@SQLRestriction("visible = 1")
	static class Tag {
		@Id int id;
		int visible = 1;

		Tag() {
		}

		Tag(int id) {
			this.id = id;
		}
	}
}
