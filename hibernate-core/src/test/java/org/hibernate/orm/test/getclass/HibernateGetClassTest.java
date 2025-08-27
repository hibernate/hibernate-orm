/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.getclass;

import org.hibernate.Hibernate;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				HibernateGetClassTest.TestEntity.class,
				HibernateGetClassTest.TestSubEntity.class,
				HibernateGetClassTest.TestRegularEntity.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@JiraKey("HHH-15453")
public class HibernateGetClassTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity e1 = new TestSubEntity( 1, "E1" );
					TestRegularEntity e2 = new TestRegularEntity( 2, "E2" );
					TestEntity e3 = new TestEntity( 3, "E2", e1, e2 );

					session.persist( e1 );
					session.persist( e2 );
					session.persist( e3 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSelectUserWithRole(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inSession(
				session -> {
					TestEntity e3 = session.find( TestEntity.class, 3 );
					statementInspector.clear();
					assertThat( Hibernate.getClassLazy( e3 ) ).isEqualTo( TestEntity.class );
					assertThat( Hibernate.getClassLazy( e3.regularToOne ) ).isEqualTo( TestRegularEntity.class );
					assertThat( statementInspector.getSqlQueries().size() ).isEqualTo( 0 );
					assertThat( Hibernate.getClassLazy( e3.manyToOne ) ).isEqualTo( TestSubEntity.class );
					assertThat( statementInspector.getSqlQueries().size() ).isEqualTo( 1 );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		private Integer id;
		private String name;
		private TestEntity manyToOne;
		private TestRegularEntity regularToOne;

		public TestEntity() {
		}

		public TestEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public TestEntity(Integer id, String name, TestEntity manyToOne, TestRegularEntity regularToOne) {
			this.id = id;
			this.name = name;
			this.manyToOne = manyToOne;
			this.regularToOne = regularToOne;
		}

		@Id
		@Column(name = "USER_ID")
		public Integer getId() {
			return id;
		}

		public void setId(Integer userId) {
			this.id = userId;
		}

		@Column(name = "USER_NAME")
		public String getName() {
			return name;
		}

		public void setName(String userName) {
			this.name = userName;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		public TestEntity getManyToOne() {
			return manyToOne;
		}

		public void setManyToOne(TestEntity manyToOne) {
			this.manyToOne = manyToOne;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		public TestRegularEntity getRegularToOne() {
			return regularToOne;
		}

		public void setRegularToOne(TestRegularEntity regularToOne) {
			this.regularToOne = regularToOne;
		}
	}

	@Entity(name = "TestSubEntity")
	public static class TestSubEntity extends TestEntity {
		public TestSubEntity() {
		}

		public TestSubEntity(Integer id, String name) {
			super( id, name );
		}

		public TestSubEntity(Integer id, String name, TestEntity manyToOne, TestRegularEntity regularToOne) {
			super( id, name, manyToOne, regularToOne );
		}
	}

	@Entity(name = "TestRegularEntity")
	public static class TestRegularEntity {

		private Integer id;
		private String name;

		public TestRegularEntity() {
		}

		public TestRegularEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		@Column(name = "USER_ID")
		public Integer getId() {
			return id;
		}

		public void setId(Integer userId) {
			this.id = userId;
		}

		@Column(name = "USER_NAME")
		public String getName() {
			return name;
		}

		public void setName(String userName) {
			this.name = userName;
		}
	}
}
