package org.hibernate.orm.test.merge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OneToMany;

import static jakarta.persistence.CascadeType.ALL;
import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		ManagedCollectionCascadeMergeTest.Parent.class,
		ManagedCollectionCascadeMergeTest.Child.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-20973")
class ManagedCollectionCascadeMergeTest {
	@ParameterizedTest
	@EnumSource(Kind.class)
	void replacesDetachedElementsOfUnmodifiableCollections(Kind kind, SessionFactoryScope scope) {
		Long[] ids = scope.fromTransaction( session -> {
			var parent = new Parent();
			var child = new Child();
			child.name = "before";
			kind.initialize( parent, child );
			session.persist( parent );
			session.flush();
			session.detach( child );
			child.name = "after";
			assertThat( session.merge( parent ) ).isSameAs( parent );
			var managed = kind.child( parent );
			// Child.equals() compares ids, so equality would miss this replacement.
			assertThat( managed ).isEqualTo( child ).isNotSameAs( child );
			assertThat( session.contains( managed ) ).isTrue();
			assertThat( managed.name ).isEqualTo( "after" );
			if ( kind == Kind.MAP ) {
				assertThat( parent.map.keySet().iterator().next() ).isSameAs( managed );
			}
			session.flush();
			return new Long[] { parent.id, child.id };
		} );
		scope.inTransaction( session -> {
			var child = kind.child( session.find( Parent.class, ids[0] ) );
			assertThat( child.id ).isEqualTo( ids[1] );
			assertThat( child.name ).isEqualTo( "after" );
		} );
	}

	@Test
	void replacesDetachedMapKeyEvenWhenValueIsUnchanged(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var parent = new Parent();
			var key = new Child();
			var value = new Child();
			session.persist( key );
			parent.map = Collections.unmodifiableMap( Map.of( key, value ) );
			session.persist( parent );
			session.flush();
			session.detach( key );
			session.merge( parent );
			var entry = parent.map.entrySet().iterator().next();
			assertThat( entry.getValue() ).isSameAs( value );
			assertThat( entry.getKey() ).isEqualTo( key ).isNotSameAs( key );
			assertThat( session.contains( entry.getKey() ) ).isTrue();
		} );
	}

	@Test
	void retainsBackingCollectionChangesWhenReplacingDetachedElements(SessionFactoryScope scope) {
		Long[] ids = scope.fromTransaction( session -> {
			var parent = new Parent();
			var removed = new Child();
			var retained = new Child();
			var backing = new ArrayList<>( List.of( removed, retained ) );
			parent.list = Collections.unmodifiableList( backing );
			session.persist( parent );
			session.flush();
			backing.remove( removed );
			session.detach( retained );
			session.merge( parent );
			assertThat( parent.list ).hasSize( 1 );
			assertThat( parent.list.get( 0 ) ).isEqualTo( retained ).isNotSameAs( retained );
			session.flush();
			session.flush();
			return new Long[] { parent.id, retained.id };
		} );
		scope.inTransaction( session -> {
			var parent = session.find( Parent.class, ids[0] );
			assertThat( parent.list ).hasSize( 1 );
			assertThat( parent.list.get( 0 ).id ).isEqualTo( ids[1] );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	enum Kind {
		LIST, SET, MAP;

		void initialize(Parent parent, Child child) {
			switch ( this ) {
				case LIST -> parent.list = Collections.unmodifiableList( List.of( child ) );
				case SET -> parent.set = Collections.unmodifiableSet( Set.of( child ) );
				case MAP -> parent.map = Collections.unmodifiableMap( Map.of( child, child ) );
			}
		}

		Child child(Parent parent) {
			return switch ( this ) {
				case LIST -> parent.list.get( 0 );
				case SET -> parent.set.iterator().next();
				case MAP -> parent.map.values().iterator().next();
			};
		}
	}

	@Entity(name = "CascadeMergeParent")
	static class Parent {
		@Id
		@GeneratedValue
		Long id;
		@OneToMany(cascade = ALL)
		@JoinTable(name = "cascade_merge_list")
		List<Child> list = new ArrayList<>();
		@OneToMany(cascade = ALL)
		@JoinTable(name = "cascade_merge_set")
		Set<Child> set = new LinkedHashSet<>();
		@OneToMany(cascade = ALL)
		@JoinTable(name = "cascade_merge_map")
		@MapKeyJoinColumn(name = "map_key_id")
		Map<Child, Child> map = new LinkedHashMap<>();
	}

	@Entity(name = "CascadeMergeChild")
	static class Child {
		@Id
		@GeneratedValue
		Long id;
		String name;

		@Override
		public boolean equals(Object object) {
			return object == this || object instanceof Child other && id != null && id.equals( other.id );
		}

		@Override
		public int hashCode() {
			return Child.class.hashCode();
		}
	}
}
