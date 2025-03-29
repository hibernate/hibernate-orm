/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;

import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		MappedSuperclassTemporalAccessorTest.AbstractSuperclass.class,
		MappedSuperclassTemporalAccessorTest.TestEntity.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16784" )
public class MappedSuperclassTemporalAccessorTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new TestEntity(
				1L,
				"Marco",
				LocalDateTime.of( 2023, 6, 16, 11, 41 )
		) ) );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from TestEntity" ).executeUpdate() );
	}

	@Test
	public void testGenericTemporalAccessorPath(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Object> cq = cb.createQuery( Object.class );
			final Root<TestEntity> root = cq.from( TestEntity.class );
			final Path<Object> createTime = root.get( "createTime" );
			assertThat( createTime.getModel() ).isSameAs( root.getModel().getAttribute( "createTime" ) );
			assertThat( ( (SqmPath<?>) createTime ).getResolvedModel()
								.getBindableJavaType() ).isEqualTo( LocalDateTime.class );
			final Object result = session.createQuery( cq.select( createTime ) ).getSingleResult();
			assertThat( result ).isEqualTo( LocalDateTime.of( 2023, 6, 16, 11, 41 ) );
		} );
	}

	@Test
	public void testGenericSerializablePath(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Object> cq = cb.createQuery( Object.class );
			final Root<TestEntity> root = cq.from( TestEntity.class );
			final Path<Object> id = root.get( "id" );
			assertThat( id.getModel() ).isSameAs( root.getModel().getAttribute( "id" ) );
			assertThat( ( (SqmPath<?>) id ).getResolvedModel()
								.getBindableJavaType() ).isEqualTo( Long.class );
			final Object result = session.createQuery( cq.select( id ) ).getSingleResult();
			assertThat( result ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testGenericObjectPath(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Object> cq = cb.createQuery( Object.class );
			final Root<TestEntity> root = cq.from( TestEntity.class );
			final Path<Object> createUser = root.get( "createUser" );
			assertThat( createUser.getModel() ).isSameAs( root.getModel().getAttribute( "createUser" ) );
			assertThat( ( (SqmPath<?>) createUser ).getResolvedModel()
								.getBindableJavaType() ).isEqualTo( String.class );
			final Object result = session.createQuery( cq.select( createUser ) ).getSingleResult();
			assertThat( result ).isEqualTo( "Marco" );
		} );
	}

	@MappedSuperclass
	public static abstract class AbstractSuperclass<I extends Serializable, U, T extends TemporalAccessor> {
		@Id
		private I id;
		private U createUser;
		private T createTime;

		public AbstractSuperclass() {
		}

		public AbstractSuperclass(I id, U createUser, T createTime) {
			this.id = id;
			this.createUser = createUser;
			this.createTime = createTime;
		}
	}

	@Entity( name = "TestEntity" )
	public static class TestEntity extends AbstractSuperclass<Long, String, LocalDateTime> {
		public TestEntity() {
		}

		public TestEntity(Long id, String createUser, LocalDateTime createTime) {
			super( id, createUser, createTime );
		}
	}
}
