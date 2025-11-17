/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.orphan.onetomany;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@DomainModel(
		annotatedClasses = {
				PersistAndQueryingInSameTransactionTest.Parent.class,
				PersistAndQueryingInSameTransactionTest.Child.class,
		}
)
@SessionFactory
@JiraKey(value = "HHH-15512")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
public class PersistAndQueryingInSameTransactionTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSelectingThePersistedEntity(SessionFactoryScope scope) {
		String parentId = SafeRandomUUIDGenerator.safeRandomUUIDAsString();
		scope.inTransaction(
				session -> {
					Child child = new Child();
					child.setName( "Andrew" );
					child.setAge( 5 );
					Parent parent = new Parent( parentId );
					parent.addChild( child );
					session.persist( parent );

					session.createQuery( "select p from Parent p where p.id = :id", Parent.class )
							.setParameter( "id", parentId )
							.getSingleResult();

					Parent p = session.find( Parent.class, parentId );
					assertThat( p, notNullValue() );

					List<Child> children = p.getChildren();
					assertThat( children.size(), is( 1 ) );
					Child c = children.get( 0 );

					assertThat( c.getAge(), equalTo( 5 ) );
				}
		);

		scope.inTransaction(
				session -> {
					Parent parent1 = session.find( Parent.class, parentId );
					assertThat( parent1, notNullValue() );

					List<Child> children = parent1.getChildren();
					assertThat( children.size(), is( 1 ) );
					Child c = children.get( 0 );

					assertThat( c.getAge(), equalTo( 5 ) );
				}
		);
	}

	@Test
	public void testSelectingAndModifying(SessionFactoryScope scope) {
		String parentId = SafeRandomUUIDGenerator.safeRandomUUIDAsString();
		scope.inTransaction(
				session -> {
					Child child = new Child();
					child.setName( "Andrew" );
					child.setAge( 5 );
					Parent parent = new Parent( parentId );
					parent.addChild( child );
					session.persist( parent );

					Parent p = session.createQuery( "select p from Parent p where p.id = :id", Parent.class )
							.setParameter( "id", parentId )
							.getSingleResult();

					p.getChildren().get( 0 ).setAge( 10 );

					Parent parent1 = session.find( Parent.class, parentId );
					assertThat( parent1, notNullValue() );

					List<Child> children = parent1.getChildren();
					assertThat( children.size(), is( 1 ) );
					Child c = children.get( 0 );

					assertThat( c.getAge(), equalTo( 10 ) );
				}
		);

		scope.inTransaction(
				session -> {
					Parent parent1 = session.find( Parent.class, parentId );
					assertThat( parent1, notNullValue() );

					List<Child> children = parent1.getChildren();
					assertThat( children.size(), is( 1 ) );
					Child c = children.get( 0 );

					assertThat( c.getAge(), equalTo( 10 ) );
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private String id;

		@Cascade(CascadeType.ALL)
		@OneToMany(orphanRemoval = true, mappedBy = "parent")
		private final List<Child> children = new ArrayList<>();

		public Parent() {
		}

		public Parent(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public void addChild(Child c) {
			c.parent = this;
			children.add( c );
		}

		public List<Child> getChildren() {
			return children;
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		private Integer age;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		private Parent parent;

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getAge() {
			return age;
		}

		public void setAge(Integer age) {
			this.age = age;
		}
	}

}
