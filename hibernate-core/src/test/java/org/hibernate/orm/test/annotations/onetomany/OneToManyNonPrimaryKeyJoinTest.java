/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.onetomany;

import java.io.Serializable;
import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@JiraKey("HHH-12752")
@Jpa (
		annotatedClasses = {
				OneToManyNonPrimaryKeyJoinTest.Parent.class,
				OneToManyNonPrimaryKeyJoinTest.Child_PK.class,
				OneToManyNonPrimaryKeyJoinTest.Child_NonPK.class
		},
		generateStatistics = true
)
public class OneToManyNonPrimaryKeyJoinTest {

	@BeforeAll
	protected void setup(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final Parent parent = new Parent( 123L );
					entityManager.persist( parent );

					entityManager.persist( new Child_PK( parent ) );
					entityManager.persist( new Child_PK( parent ) );

					entityManager.persist( new Child_NonPK( parent ) );
					entityManager.persist( new Child_NonPK( parent ) );
				}
		);
	}

	@AfterAll
	protected void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Child_NonPK" ).executeUpdate();
					entityManager.createQuery( "delete from Child_PK" ).executeUpdate();
					entityManager.createQuery( "delete from Parent" ).executeUpdate();
				}
		);
	}

	@Test
	public void testJoinOnPrimaryKey(EntityManagerFactoryScope scope) {
		Statistics stats = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class ).getStatistics();
		stats.clear();
		scope.inTransaction(
				entityManager -> entityManager.createQuery( "select p from Parent p left join fetch p.primaryKeySet", Parent.class )
							.getResultList()
		);
		Assertions.assertEquals( 1L, stats.getPrepareStatementCount() );
	}

	@Test
	public void testJoinOnNonPrimaryKey(EntityManagerFactoryScope scope) {
		Statistics stats = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class ).getStatistics();
		stats.clear();
		scope.inTransaction(
				entityManager -> entityManager.createQuery( "select p from Parent p left join fetch p.nonPrimaryKeySet", Parent.class )
							.getResultList()
		);
		Assertions.assertEquals( 1L, scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class ).getStatistics().getPrepareStatementCount() );
	}

	@Entity(name = "Parent")
	@Access(AccessType.PROPERTY)
	// The unique constraint is only here to avoid a stacktrace during schema generation
	@Table(
			name="Parent",
			uniqueConstraints=
			@UniqueConstraint(columnNames={"otherId"})
	)
	public static class Parent implements Serializable {

		private Long id;
		private Long otherId;
		private Set<Child_PK> primaryKeySet;
		private Set<Child_NonPK> nonPrimaryKeySet;

		Parent() {
		}

		public Parent(final Long otherId) {
			this.otherId = otherId;
		}

		@Id
		@GeneratedValue
		private Long getId() {
			return id;
		}

		private void setId(final Long id) {
			this.id = id;
		}

		private Long getOtherId() {
			return otherId;
		}

		private void setOtherId(final Long otherId) {
			this.otherId = otherId;
		}

		@OneToMany(mappedBy = "parent")
		private Set<Child_PK> getPrimaryKeySet() {
			return primaryKeySet;
		}

		private void setPrimaryKeySet(
				final Set<Child_PK> primaryKeySet) {
			this.primaryKeySet = primaryKeySet;
		}

		@OneToMany(mappedBy = "parent")
		private Set<Child_NonPK> getNonPrimaryKeySet() {
			return nonPrimaryKeySet;
		}

		private void setNonPrimaryKeySet(final Set<Child_NonPK> nonPrimaryKeySet) {
			this.nonPrimaryKeySet = nonPrimaryKeySet;
		}
	}

	@Entity(name = "Child_PK")
	@Access(AccessType.PROPERTY)
	public static class Child_PK implements Serializable {

		private Long id;
		private Parent parent;

		Child_PK() {
		}

		public Child_PK(final Parent parent) {
			this.parent = parent;
		}

		@Id
		@GeneratedValue
		private Long getId() {
			return id;
		}

		private void setId(final Long id) {
			this.id = id;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "parent_id")
		private Parent getParent() {
			return parent;
		}

		private void setParent(final Parent parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "Child_NonPK")
	@Access(AccessType.PROPERTY)
	public static class Child_NonPK implements Serializable {

		private Long id;
		private Parent parent;

		Child_NonPK() {
		}

		public Child_NonPK(final Parent parent) {
			this.parent = parent;
		}

		@Id
		@GeneratedValue
		private Long getId() {
			return id;
		}

		private void setId(final Long id) {
			this.id = id;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "parent_id", referencedColumnName = "otherId")
		private Parent getParent() {
			return parent;
		}

		private void setParent(final Parent parent) {
			this.parent = parent;
		}
	}
}