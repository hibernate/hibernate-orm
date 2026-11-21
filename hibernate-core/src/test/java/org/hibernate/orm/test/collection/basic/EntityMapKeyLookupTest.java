package org.hibernate.orm.test.collection.basic;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyJoinColumn;
import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@DomainModel(annotatedClasses = { EntityMapKeyLookupTest.Owner.class, EntityMapKeyLookupTest.Tag.class })
@SessionFactory
@JiraKey("HHH-21224")
class EntityMapKeyLookupTest {
	@Test
	void keyAndValueOfSameEntityType(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var key = new Tag();
			key.id = 10;
			var value = new Tag();
			value.id = 20;
			session.persist( key );
			session.persist( value );
			var owner = new Owner();
			owner.tags.put( key, value );
			session.persist( owner );
		} );
		scope.inSession( session -> {
			var tags = session.find( Owner.class, 1 ).tags;
			var key = session.find( Tag.class, 10 );
			var value = session.find( Tag.class, 20 );
			assertSame( value, Hibernate.get( tags, key ) );
			assertNull( Hibernate.get( tags, value ) );
			assertFalse( Hibernate.isInitialized( tags ) );
		} );
	}

	@Entity(name = "MapOwner")
	static class Owner {
		@Id int id = 1;
		@ManyToMany @MapKeyJoinColumn(name = "tag_key") Map<Tag, Tag> tags = new HashMap<>();
	}

	@Entity(name = "Tag")
	static class Tag {
		@Id int id;
	}
}
