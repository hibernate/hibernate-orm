package org.hibernate.orm.test.refresh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		NestedCascadeRefreshMapKeyTest.Root.class,
		NestedCascadeRefreshMapKeyTest.MapKeyEntity.class,
		NestedCascadeRefreshMapKeyTest.MapValueEntity.class,
		NestedCascadeRefreshMapKeyTest.KeyChild.class
})
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false"))
@JiraKey("HHH-21009")
class NestedCascadeRefreshMapKeyTest {
	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void refreshValuesDoesNotCascadeThroughMapKeys(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Root root = new Root();
			root.id = 1L;
			final MapKeyEntity key = new MapKeyEntity();
			key.id = 2L;
			final MapValueEntity value = new MapValueEntity();
			value.id = 3L;
			value.label = "old";
			final KeyChild child = new KeyChild();
			child.id = 4L;
			child.label = "old";
			child.keyOwner = key;
			key.children.add( child );
			root.values.put( key, value );
			session.persist( key );
			session.persist( value );
			session.persist( child );
			session.persist( root );
		} );
		scope.inTransaction( session -> {
			final Root root = session.find( Root.class, 1L );
			final MapKeyEntity key = session.find( MapKeyEntity.class, 2L );
			final MapValueEntity value = session.find( MapValueEntity.class, 3L );
			Hibernate.initialize( key.children );
			final List<KeyChild> originalChildren = key.children;
			final KeyChild child = key.children.get( 0 );
			assertThat( Hibernate.isInitialized( root.values ) ).isFalse();
			assertThat( key.children ).containsExactly( child );
			assertThat( value.label ).isEqualTo( "old" );
			assertThat( child.label ).isEqualTo( "old" );
			session.doWork( connection -> {
				try ( var statement = connection.createStatement() ) {
					assertThat( statement.executeUpdate(
							"update refresh_map_value set label = 'new' where id = 3" ) ).isEqualTo( 1 );
					assertThat( statement.executeUpdate(
							"update refresh_map_key_child set label = 'new' where id = 4" ) ).isEqualTo( 1 );
				}
			} );

			session.refresh( root );

			assertThat( key.children ).isSameAs( originalChildren );
			assertThat( Hibernate.isInitialized( key.children ) ).isTrue();

			assertThat( root.values ).containsOnlyKeys( key );
			assertThat( root.values.get( key ) ).isSameAs( value );
			assertThat( value.label ).isEqualTo( "new" );
			assertThat( session.find( MapKeyEntity.class, 2L ) ).isSameAs( key );
			assertThat( session.find( KeyChild.class, 4L ) ).isSameAs( child );
			assertThat( key.children ).containsExactly( child );
			assertThat( child.label ).isEqualTo( "old" );
		} );
	}

	@Entity(name = "MapRefreshRoot")
	@Table(name = "refresh_map_root")
	public static class Root {
		@Id
		private Long id;

		@OneToMany(cascade = CascadeType.REFRESH)
		@JoinTable(name = "refresh_map_entry", joinColumns = @JoinColumn(name = "root_id"),
				inverseJoinColumns = @JoinColumn(name = "value_id"))
		@MapKeyJoinColumn(name = "key_id")
		private Map<MapKeyEntity, MapValueEntity> values = new HashMap<>();
	}

	@Entity(name = "MapRefreshKey")
	@Table(name = "refresh_map_key")
	public static class MapKeyEntity {
		@Id
		private Long id;

		@OneToMany(mappedBy = "keyOwner", cascade = CascadeType.REFRESH)
		private List<KeyChild> children = new ArrayList<>();
	}

	@Entity(name = "MapRefreshValue")
	@Table(name = "refresh_map_value")
	public static class MapValueEntity {
		@Id
		private Long id;

		private String label;
	}

	@Entity(name = "MapRefreshKeyChild")
	@Table(name = "refresh_map_key_child")
	public static class KeyChild {
		@Id
		private Long id;

		private String label;

		@ManyToOne(fetch = FetchType.LAZY)
		private MapKeyEntity keyOwner;
	}
}
