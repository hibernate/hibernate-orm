package org.hibernate.orm.test.refresh;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		NestedCascadeRefreshCompositeIdTest.Root.class,
		NestedCascadeRefreshCompositeIdTest.Parent.class,
		NestedCascadeRefreshCompositeIdTest.Child.class,
		NestedCascadeRefreshCompositeIdTest.Tenant.class
})
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false"))
@JiraKey("HHH-21009")
class NestedCascadeRefreshCompositeIdTest {
	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void refreshInitializedChildren(boolean initializeRoot, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Tenant tenant = new Tenant();
			tenant.id = 10L;
			session.persist( tenant );
			final Root root = new Root();
			root.id = 1L;
			final Parent parent = new Parent();
			parent.sequence = 2L;
			parent.tenant = tenant;
			parent.root = root;
			root.parents.add( parent );
			final Child child = new Child();
			child.id = 3L;
			child.label = "old";
			child.parent = parent;
			parent.children.add( child );
			session.persist( root );
		} );
		scope.inTransaction( session -> {
			final Root root = session.find( Root.class, 1L );
			final Parent parent = session.find( Parent.class, new ParentId( 2L, 10L ) );
			Hibernate.initialize( parent.children );
			final Child child = parent.children.get( 0 );
			if ( initializeRoot ) {
				Hibernate.initialize( root.parents );
			}
			assertThat( Hibernate.isInitialized( root.parents ) ).isEqualTo( initializeRoot );
			assertThat( child.label ).isEqualTo( "old" );
			session.doWork( connection -> {
				try ( var statement = connection.createStatement() ) {
					assertThat( statement.executeUpdate(
							"update composite_refresh_child set label = 'new' where id = 3" ) ).isEqualTo( 1 );
					assertThat( statement.executeUpdate(
							"insert into composite_refresh_child (id, label, parent_sequence, tenant_id)"
									+ " values (4, 'added', 2, 10)" ) ).isEqualTo( 1 );
				}
			} );

			session.refresh( root );

			assertThat( root.parents ).containsExactly( parent );
			assertThat( session.find( Parent.class, new ParentId( 2L, 10L ) ) ).isSameAs( parent );
			assertThat( session.find( Child.class, 3L ) ).isSameAs( child );
			assertThat( child.label ).isEqualTo( "new" );
			assertThat( parent.children ).contains( child );
			assertThat( parent.children ).extracting( element -> element.id ).containsExactlyInAnyOrder( 3L, 4L );
		} );
	}

	@Entity(name = "CompositeRefreshRoot")
	@Table(name = "composite_refresh_root")
	public static class Root {
		@Id
		private Long id;

		@OneToMany(mappedBy = "root", cascade = CascadeType.ALL)
		private List<Parent> parents = new ArrayList<>();
	}

	@Entity(name = "CompositeRefreshTenant")
	@Table(name = "composite_refresh_tenant")
	public static class Tenant {
		@Id
		private Long id;
	}

	@Entity(name = "CompositeRefreshParent")
	@Table(name = "composite_refresh_parent")
	@IdClass(ParentId.class)
	public static class Parent {
		@Id
		@Column(name = "parent_sequence")
		private Long sequence;

		@Id
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "tenant_id")
		private Tenant tenant;

		@ManyToOne(fetch = FetchType.LAZY)
		private Root root;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
		private List<Child> children = new ArrayList<>();
	}

	public static class ParentId implements Serializable {
		private Long sequence;
		private Long tenant;

		public ParentId() {
		}

		public ParentId(Long sequence, Long tenant) {
			this.sequence = sequence;
			this.tenant = tenant;
		}

		@Override
		public boolean equals(Object object) {
			return object instanceof ParentId other
					&& Objects.equals( sequence, other.sequence )
					&& Objects.equals( tenant, other.tenant );
		}

		@Override
		public int hashCode() {
			return Objects.hash( sequence, tenant );
		}
	}

	@Entity(name = "CompositeRefreshChild")
	@Table(name = "composite_refresh_child")
	public static class Child {
		@Id
		private Long id;

		private String label;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumns({
				@JoinColumn(name = "parent_sequence", referencedColumnName = "parent_sequence"),
				@JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id")
		})
		private Parent parent;
	}
}
