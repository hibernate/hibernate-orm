/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.subquery;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andreas Asplund
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		SubqueryMultipleLeftJoinsTest.MyUnrelatedEntity.class,
		SubqueryMultipleLeftJoinsTest.MyEntity.class,
		SubqueryMultipleLeftJoinsTest.AnotherEntity.class,
		SubqueryMultipleLeftJoinsTest.AgainAnotherEntity.class
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-16413" )
public class SubqueryMultipleLeftJoinsTest {
	private static final long ENTITY_WITH_ASSOCIATION_ID_1 = 1L;
	private static final long ENTITY_WITH_ASSOCIATION_ID_2 = 2L;
	private static final long ANOTHER_ENTITY_ID_1 = 3L;
	private static final long ANOTHER_ENTITY_ID_2 = 4L;
	private static final long AGAIN_ANOTHER_ENTITY_ID = 5L;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final AgainAnotherEntity againAnotherEntity = new AgainAnotherEntity(
					AGAIN_ANOTHER_ENTITY_ID,
					"again"
			);
			session.persist( againAnotherEntity );
			final AnotherEntity anotherEntity1 = new AnotherEntity(
					ANOTHER_ENTITY_ID_1,
					"another 1",
					true,
					null
			);
			session.persist( anotherEntity1 );
			final AnotherEntity anotherEntity2 = new AnotherEntity(
					ANOTHER_ENTITY_ID_2,
					"another 2",
					false,
					againAnotherEntity
			);
			session.persist( anotherEntity2 );
			session.persist( new MyEntity( ENTITY_WITH_ASSOCIATION_ID_1, "without association", anotherEntity1 ) );
			session.persist( new MyEntity( ENTITY_WITH_ASSOCIATION_ID_2, "with association", anotherEntity2 ) );
			session.persist( new MyUnrelatedEntity( ENTITY_WITH_ASSOCIATION_ID_1, "unrelated 1" ) );
			session.persist( new MyUnrelatedEntity( ENTITY_WITH_ASSOCIATION_ID_2, "unrelated 2" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from MyUnrelatedEntity" ).executeUpdate();
			session.createMutationQuery( "delete from MyEntity" ).executeUpdate();
			session.createMutationQuery( "delete from AnotherEntity" ).executeUpdate();
			session.createMutationQuery( "delete from AgainAnotherEntity" ).executeUpdate();
		} );
	}

	@Test
	public void subqueryWithLeftJoinsCriteriaApi(SessionFactoryScope sessionFactoryScope) {
		sessionFactoryScope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<MyUnrelatedEntity> cq = cb.createQuery( MyUnrelatedEntity.class );
			final Root<MyUnrelatedEntity> root = cq.from( MyUnrelatedEntity.class );
			final Subquery<Long> subquery = cq.subquery( Long.class );
			final Root<MyEntity> myEntityRoot = subquery.from( MyEntity.class );
			final Join<MyEntity, AnotherEntity> anotherEntityJoin = myEntityRoot.join(
					"otherEntity",
					JoinType.LEFT
			);
			final Join<AnotherEntity, AgainAnotherEntity> againAnotherEntityJoin = anotherEntityJoin.join(
					"otherEntity",
					JoinType.LEFT
			);
			subquery.select( myEntityRoot.get( "id" ) ).where( cb.and(
					cb.equal( anotherEntityJoin.get( "aString" ), "another 1" ),
					cb.or(
							cb.and(
									cb.equal( anotherEntityJoin.get( "aBoolean" ), false ),
									cb.equal( againAnotherEntityJoin.get( "aString" ), "again" )
							),
							cb.and(
									// This should be true since "another 1" has no association and the join is LEFT
									cb.equal( anotherEntityJoin.get( "aBoolean" ), true ),
									cb.isNull( againAnotherEntityJoin.get( "aString" ) )
							)
					)
			) );
			final MyUnrelatedEntity result = session.createQuery(
					cq.select( root ).where( root.get( "id" ).in( subquery ) )
			).getSingleResult();
			assertThat( result.getId() ).isEqualTo( 1L );
			assertThat( result.getaString() ).isEqualTo( "unrelated 1" );
		} );
	}

	@Entity( name = "MyUnrelatedEntity" )
	public static class MyUnrelatedEntity {
		@Id
		private Long id;

		private String aString;

		public MyUnrelatedEntity() {
		}

		public MyUnrelatedEntity(Long id, String aString) {
			this.id = id;
			this.aString = aString;
		}

		public Long getId() {
			return id;
		}

		public String getaString() {
			return aString;
		}
	}

	@Entity( name = "MyEntity" )
	public static class MyEntity {
		@Id
		private Long id;

		private String aString;

		@ManyToOne
		private AnotherEntity otherEntity;

		public MyEntity() {
		}

		public MyEntity(Long id, String aString, AnotherEntity otherEntity) {
			this.id = id;
			this.aString = aString;
			this.otherEntity = otherEntity;
		}

		public Long getId() {
			return id;
		}

		public String getaString() {
			return aString;
		}

		public AnotherEntity getOtherEntity() {
			return otherEntity;
		}
	}

	@Entity( name = "AnotherEntity" )
	public static class AnotherEntity {
		@Id
		private Long id;

		private String aString;

		private boolean aBoolean;
		@ManyToOne
		private AgainAnotherEntity otherEntity;

		public AnotherEntity() {
		}

		public AnotherEntity(Long id, String aString, boolean aBoolean, AgainAnotherEntity otherEntity) {
			this.id = id;
			this.aString = aString;
			this.otherEntity = otherEntity;
			this.aBoolean = aBoolean;
		}

		public String getaString() {
			return aString;
		}
	}

	@Entity( name = "AgainAnotherEntity" )
	public static class AgainAnotherEntity {
		@Id
		private Long id;

		private String aString;

		@ManyToOne
		private AnotherEntity otherEntity;

		public AgainAnotherEntity() {
		}

		public AgainAnotherEntity(Long id, String aString) {
			this.id = id;
			this.aString = aString;
		}
	}
}
