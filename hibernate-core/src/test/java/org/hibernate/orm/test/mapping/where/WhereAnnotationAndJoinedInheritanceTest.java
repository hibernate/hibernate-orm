/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.where;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				WhereAnnotationAndJoinedInheritanceTest.Parent.class,
				WhereAnnotationAndJoinedInheritanceTest.Child.class,
				WhereAnnotationAndJoinedInheritanceTest.PrimaryObject.class
		}
)
@Jira("HHH-16967")
public class WhereAnnotationAndJoinedInheritanceTest {

	private final static String PRIMARY_OBJECT_WITH_DELETED_CHILD = "with deleted child";
	private final static String DELETED_CHILD = "deleted child";
	private final static String NOT_DELETED_CHILD = "not deleted child";

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Child child = new Child( 1l, NOT_DELETED_CHILD, null );
					Child deletedChild = new Child( 2l, DELETED_CHILD, "deleted" );

					PrimaryObject primaryObject = new PrimaryObject( 3l, child, "with not deleted child" );
					PrimaryObject primaryObjectWithDeletedChild = new PrimaryObject(
							4l,
							deletedChild,
							PRIMARY_OBJECT_WITH_DELETED_CHILD
					);
					entityManager.persist( child );
					entityManager.persist( deletedChild );
					entityManager.persist( primaryObject );
					entityManager.persist( primaryObjectWithDeletedChild );
				}
		);
	}

	@Test
	public void testCriteriaQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<PrimaryObject> query = builder.createQuery( PrimaryObject.class );
					Root<PrimaryObject> root = query.from( PrimaryObject.class );

					query.where(
							builder.equal(
									root.get( "child" ).get( "primaryObjects" ).get( "data" ),
									PRIMARY_OBJECT_WITH_DELETED_CHILD
							) );

					// the child
					List<PrimaryObject> resultList = entityManager.createQuery( query ).getResultList();
					assertThat( resultList.size() ).isEqualTo( 0 );
				}
		);
	}

	@Test
	public void testCriteriaQueryWithoutWhereClause(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<PrimaryObject> query = builder.createQuery( PrimaryObject.class );
					Root<PrimaryObject> root = query.from( PrimaryObject.class );

					// the child
					List<PrimaryObject> resultList = entityManager.createQuery( query ).getResultList();
					assertThat( resultList.size() ).isEqualTo( 2 );

					resultList.forEach(
							primaryObject -> {
								Child child = primaryObject.getChild();
								if ( primaryObject.getData().equals( PRIMARY_OBJECT_WITH_DELETED_CHILD ) ) {
									assertThat( child ).isNull();
								}
								else {
									assertThat( child ).isNotNull();
								}
							}
					);
				}
		);
	}

	@Test
	public void testCriteriaQuery2(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<PrimaryObject> query = builder.createQuery( PrimaryObject.class );
					Root<PrimaryObject> root = query.from( PrimaryObject.class );

					query.where(
							builder.equal(
									root.get( "child" ).get( "data" ), DELETED_CHILD ) );

					List<PrimaryObject> resultList = entityManager.createQuery( query ).getResultList();
					assertThat( resultList.size() ).isEqualTo( 0 );

					builder = entityManager.getCriteriaBuilder();
					query = builder.createQuery( PrimaryObject.class );
					root = query.from( PrimaryObject.class );

					query.where(
							builder.equal(
									root.get( "child" ).get( "data" ), NOT_DELETED_CHILD ) );

					resultList = entityManager.createQuery( query ).getResultList();
					assertThat( resultList.size() ).isEqualTo( 1 );
				}
		);
	}


	@Entity(name = "Parent")
	@Inheritance(strategy = InheritanceType.JOINED)
	@SQLRestriction("deleted_column is null")
	public static abstract class Parent {

		@Id
		private Long id;

		private String data;

		@Column(name = "deleted_column")
		private String deleted;

		public Parent() {
		}

		public Parent(Long id, String data, String deleted) {
			this.id = id;
			this.data = data;
			this.deleted = deleted;
		}
	}

	@Entity(name = "Child")
	public static class Child extends Parent {

		@Column(name = "child_data")
		private String childData;

		@OneToMany(mappedBy = "child")
		private List<PrimaryObject> primaryObjects = new ArrayList<>();

		public Child() {
		}

		public Child(Long id, String data, String deleted) {
			super( id, data, deleted );
		}
	}

	@Entity(name = "PrimaryObject")
	public static class PrimaryObject {

		@Id
		private Long id;

		@ManyToOne
		private Child child;

		private String data;

		public PrimaryObject() {
		}

		public PrimaryObject(Long id, Child child, String data) {
			this.id = id;
			this.child = child;
			this.data = data;
		}

		public Long getId() {
			return id;
		}

		public Child getChild() {
			return child;
		}

		public String getData() {
			return data;
		}
	}

}
