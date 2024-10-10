package org.hibernate.orm.test.bytecode.enhancement.saveupdate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				SaveUpdateTest.Parent.class,
				SaveUpdateTest.Child.class,
				SaveUpdateTest.Owned.class,
				SaveUpdateTest.Owner.class,
		}
)
@SessionFactory
@BytecodeEnhanced(runNotEnhancedAsWell = true)
public class SaveUpdateTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Child" ).executeUpdate();
					session.createQuery( "delete from Parent" ).executeUpdate();
				}
		);
	}

	@Test
	@JiraKey("HHH-18713")
	public void testSaveUpdate(SessionFactoryScope scope) {
		Parent parent = scope.fromTransaction(
				session -> {
					Parent p = new Parent( "a" );
					Child child = new Child( p, "b" );
					session.persist( p );
					return p;

				}
		);

		scope.inTransaction( session -> {
			Child child2 = new Child( parent, "c" );
			session.saveOrUpdate( parent );
		} );

		scope.inTransaction( session -> {
			Parent saved = session.get( Parent.class, parent.getId() );
			assertThat( saved.getChildren().size() ).isEqualTo( 2 );
		} );
	}

	@Test
	@JiraKey("HHH-18614")
	public void testUpdate(SessionFactoryScope scope) {
		Parent parent = scope.fromTransaction(
				session -> {
					Parent p = new Parent( "a" );
					Child child = new Child( p, "b" );
					session.persist( p );
					return p;

				}
		);

		scope.inTransaction( session -> {
			Child child2 = new Child( parent, "c" );
			session.update( parent );
		} );

		scope.inTransaction( session -> {
			Parent saved = session.get( Parent.class, parent.getId() );
			assertThat( saved.getChildren().size() ).isEqualTo( 2 );
		} );
	}

	@Test
	@JiraKey("HHH-18614")
	void testUpdate2(SessionFactoryScope scope) {
		Long ownerId = scope.fromTransaction( session -> {
			Owner owner = new Owner( "a" );
			owner.addOwned( new Owned() );
			session.persist( owner );
			return owner.getId();
		} );

		scope.inTransaction( session -> {
			Owner owner2 = new Owner( ownerId, "a" );
			owner2.addOwned( new Owned() );
			session.update( owner2 );
			session.flush();
		} );
	}

	@Test
	@JiraKey("HHH-18614")
	void testUpdate3(SessionFactoryScope scope) {
		Long ownerId = scope.fromTransaction( session -> {
			Owner owner = new Owner(  );
			owner.addOwned( new Owned() );
			session.persist( owner );
			return owner.getId();
		} );

		scope.inTransaction( session -> {
			Owner owner2 = new Owner(  );
			owner2.id = ownerId;
			owner2.owneds.add( new Owned() );
			session.update( owner2 );
			session.flush();
		} );
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		@GeneratedValue
		public Long id;

		public Long getId() {
			return id;
		}

		private String name;

		@Version
		@Column(name = "VERSION_COLUMN")
		private long version;

		public Parent() {
		}

		public Parent(String name) {
			this.name = name;
		}

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
		private Set<Child> children = new LinkedHashSet<>();

		public Set<Child> getChildren() {
			return children;
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue
		public Long id;

		public Long getId() {
			return id;
		}

		@ManyToOne
		private Parent parent;

		private String name;

		public Child() {
		}

		public Child(Parent parent, String name) {
			this.parent = parent;
			parent.children.add( this );
			this.name = name;
		}
	}

	@Entity(name = "Owned")
	public static class Owned {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Owned() {
		}

		public Owned(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Owner")
	public static class Owner {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToMany(cascade = CascadeType.ALL)
		private List<Owned> owneds = new ArrayList<>();

		public Owner() {
		}

		public Owner(String name) {
			this.name = name;
		}

		public Owner(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public void addOwned(Owned owned) {
			owneds.add( owned );
		}

		public Long getId() {
			return id;
		}
	}

}
