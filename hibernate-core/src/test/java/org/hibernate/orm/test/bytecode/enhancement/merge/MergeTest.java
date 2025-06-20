/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.merge;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				MergeTest.Parent.class,
				MergeTest.Child.class,
				MergeTest.Owned.class,
				MergeTest.Owner.class,
		}
)
@SessionFactory
@BytecodeEnhanced(runNotEnhancedAsWell = true)
public class MergeTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testMerge(SessionFactoryScope scope) {
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
			session.merge( parent );
		} );

		scope.inTransaction( session -> {
			Parent saved = session.get( Parent.class, parent.getId() );
			assertThat( saved.getChildren().size() ).isEqualTo( 2 );
		} );
	}

	@Test
	public void testMerge2(SessionFactoryScope scope) {
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
			session.merge( parent );
		} );

		scope.inTransaction( session -> {
			Parent saved = session.get( Parent.class, parent.getId() );
			assertThat( saved.getChildren().size() ).isEqualTo( 2 );
		} );
	}

	@Test
	void testMerge3(SessionFactoryScope scope) {
		Long ownerId = scope.fromTransaction( session -> {
			Owner owner = new Owner( "a" );
			owner.addOwned( new Owned() );
			session.persist( owner );
			return owner.getId();
		} );

		scope.inTransaction( session -> {
			Owner owner2 = new Owner( ownerId, "a" );
			owner2.addOwned( new Owned() );
			session.merge( owner2 );
			session.flush();
		} );
	}

	@Test
	void testMerge4(SessionFactoryScope scope) {
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
			session.merge( owner2 );
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
