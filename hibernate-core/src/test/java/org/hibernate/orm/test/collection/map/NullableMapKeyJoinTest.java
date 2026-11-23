package org.hibernate.orm.test.collection.map;

import java.util.List;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = { NullableMapKeyJoinTest.Owner.class, NullableMapKeyJoinTest.Child.class,
		NullableMapKeyJoinTest.MapKeyEntity.class })
@SessionFactory
@JiraKey("HHH-21216")
class NullableMapKeyJoinTest {
	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void onConditionPreservesChildWithNullKey(boolean filterKeys, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owner = new Owner();
			session.persist( owner );
			final var key = new MapKeyEntity();
			key.caption = "visible";
			session.persist( key );
			session.persist( new Child( 1, owner, null ) );
			session.persist( new Child( 2, owner, key ) );
			final var hidden = new MapKeyEntity();
			hidden.id = 3;
			hidden.caption = "third";
			session.persist( hidden );
			session.persist( new Child( 3, owner, hidden ) );
		} );
		scope.inSession( session -> {
			if ( filterKeys ) {
				session.enableFilter( "visibleMapKeys" );
			}
			final List<Object[]> rows = session.createSelectionQuery(
					"select c.id, key(c).caption from MapOwner o left join o.children c on c.id > 0 order by c.id",
					Object[].class ).list();
			assertEquals( filterKeys ? 2 : 3, rows.size() );
			assertArrayEquals( new Object[]{ 1, null }, rows.get( 0 ) );
			assertArrayEquals( new Object[]{ 2, "visible" }, rows.get( 1 ) );
			if ( !filterKeys ) {
				assertArrayEquals( new Object[]{ 3, "third" }, rows.get( 2 ) );
			}
		} );
	}

	@Entity(name = "MapOwner")
	static class Owner {
		@Id int id = 1;
		@OneToMany(mappedBy = "owner") @MapKey(name = "mapKey") Map<MapKeyEntity, Child> children;
	}

	@Entity(name = "MapChild")
	static class Child {
		@Id int id;
		@ManyToOne Owner owner;
		@ManyToOne MapKeyEntity mapKey;

		Child() {
		}

		Child(int id, Owner owner, MapKeyEntity key) {
			this.id = id;
			this.owner = owner;
			this.mapKey = key;
		}
	}

	@FilterDef(name = "visibleMapKeys", applyToLoadByKey = true)
	@Filter(name = "visibleMapKeys", condition = "id <> 3")
	@Entity(name = "MapKeyEntity")
	static class MapKeyEntity {
		@Id int id = 1;
		String caption;
	}
}
