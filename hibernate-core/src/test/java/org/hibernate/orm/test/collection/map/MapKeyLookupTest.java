package org.hibernate.orm.test.collection.map;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKey;

import org.hibernate.Hibernate;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.internal.CollectionLookupLoader;
import org.hibernate.loader.ast.internal.CollectionLookupLoader.Operation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A lookup in an uninitialized map mapped by {@code @MapKey} restricts the attribute
 * of the element, in the table of the element.
 */
@DomainModel(annotatedClasses = { MapKeyLookupTest.Library.class, MapKeyLookupTest.Volume.class })
@SessionFactory
class MapKeyLookupTest {

	@Test
	void lookupByMapKeyOfManyToMany(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var first = new Volume();
			first.id = 1;
			first.code = "a";
			session.persist( first );
			final var second = new Volume();
			second.id = 2;
			second.code = "b";
			session.persist( second );
			final var library = new Library();
			library.volumes.put( "a", first );
			session.persist( library );
		} );
		scope.inSession( session -> {
			final var volumes = session.find( Library.class, 1 ).volumes;
			assertSame( session.find( Volume.class, 1 ), Hibernate.get( volumes, "a" ) );
			assertNull( Hibernate.get( volumes, "b" ) );
			// the lookup of a key, which only an extra-lazy map performs, and so no map
			// mapped by @MapKey, since only a collection mapped in XML is extra-lazy
			final var implementor = (SharedSessionContractImplementor) session;
			final var collection =
					implementor.getFactory().getMappingMetamodel()
							.getCollectionDescriptor( Library.class.getName() + ".volumes" )
							.getAttributeMapping();
			final var lookup = new CollectionLookupLoader( collection, Operation.INDEX_EXISTS,
					implementor.getLoadQueryInfluencers() );
			assertTrue( lookup.exists( 1, "a", implementor ) );
			assertFalse( lookup.exists( 1, "b", implementor ) );
			assertFalse( Hibernate.isInitialized( volumes ) );
		} );
	}

	@Entity(name = "BookLibrary")
	static class Library {
		@Id
		int id = 1;
		@ManyToMany
		@MapKey(name = "code")
		Map<String, Volume> volumes = new HashMap<>();
	}

	@Entity(name = "Volume")
	static class Volume {
		@Id
		int id;
		String code;
	}
}
