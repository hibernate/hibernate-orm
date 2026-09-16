/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.immutable;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel(
		annotatedClasses = {
				ImmutableAndSubselectTest.TestEntity.class,
				ImmutableAndSubselectTest.ImmutableEntity.class,
				ImmutableAndSubselectTest.SubselectEntity.class
		}
)
@JiraKey( "HHH-20892" )
public class ImmutableAndSubselectTest {

	public static final long ID = 1L;

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testDeleteImmutable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TestEntity( ID, "test" ) );
			session.persist( new ImmutableEntity( ID, "immutable" ) );
		} );

		scope.inTransaction( session -> {
			TestEntity testEntity = session.find( TestEntity.class, ID );
			ImmutableEntity immutableEntity = session.find( ImmutableEntity.class, ID );
			SubselectEntity subselectEntity = session.find( SubselectEntity.class, ID );

			session.remove( testEntity );
		} );

		scope.inTransaction( session -> {
			assertThat( session.find( TestEntity.class, ID ) ).isNull();
		});
	}


	@Entity(name = "SubselectEntity")
	@Immutable
	@Subselect("Select id, 'myname' as name, id as testentity_id from TestEntity")
	public static class SubselectEntity {

		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "testentity_id")
		private TestEntity testEntity;

		private String name;

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public TestEntity getTestEntity() {
			return testEntity;
		}
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		private Long id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private ImmutableEntity immmutableEntity;

		@ManyToOne(fetch = FetchType.LAZY)
		private SubselectEntity subselectEntity;

		public TestEntity() {
		}

		public TestEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public ImmutableEntity getImmmutableEntity() {
			return immmutableEntity;
		}

		public void setImmmutableEntity(ImmutableEntity immmutableEntity) {
			this.immmutableEntity = immmutableEntity;
		}

		public SubselectEntity getSubselectEntity() {
			return subselectEntity;
		}

		public void setSubselectEntity(SubselectEntity subselectEntity) {
			this.subselectEntity = subselectEntity;
		}
	}


	@Entity(name = "ImmutableEntity")
	@Immutable
	public static class ImmutableEntity {

		@Id
		private Long id;

		private String name;

		public ImmutableEntity() {
		}

		public ImmutableEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
