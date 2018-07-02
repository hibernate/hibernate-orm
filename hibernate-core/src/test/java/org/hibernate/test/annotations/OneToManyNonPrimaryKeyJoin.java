package org.hibernate.test.annotations.onetomany;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

import static org.hamcrest.core.Is.is;

@TestForIssue(jiraKey = "HHH-12752")
public class OneToManyNonPrimaryKeyJoin extends BaseEntityManagerFunctionalTestCase {

	@Test
	public void testJoinOnPrimaryKey() {
		clearStatistics();
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createQuery( "select p from Parent p left join fetch p.primaryKeySet", Parent.class )
					.getResultList();

		} );
		Assert.assertThat( entityManagerFactory().getStatistics().getPrepareStatementCount(), is( 1L ) );
	}

	@Test
	@FailureExpected(jiraKey = "HHH-12752")
	public void testJoinOnNonPrimaryKey() {
		clearStatistics();
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createQuery( "select p from Parent p left join fetch p.nonPrimaryKeySet", Parent.class )
					.getResultList();

		} );
		Assert.assertThat( entityManagerFactory().getStatistics().getPrepareStatementCount(), is( 1L ) );
	}

	private void clearStatistics() {
		entityManagerFactory().getStatistics().clear();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Parent.class,
				Child_PK.class,
				Child_NonPK.class
		};
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		super.afterEntityManagerFactoryBuilt();

		entityManagerFactory().getStatistics().setStatisticsEnabled( true );

		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final Parent parent = new Parent( 123L );
			entityManager.persist( parent );

			entityManager.persist( new Child_PK( parent ) );
			entityManager.persist( new Child_PK( parent ) );

			entityManager.persist( new Child_NonPK( parent ) );
			entityManager.persist( new Child_NonPK( parent ) );
		} );
	}

	@Entity(name = "Parent")
	@Access(AccessType.PROPERTY)
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
