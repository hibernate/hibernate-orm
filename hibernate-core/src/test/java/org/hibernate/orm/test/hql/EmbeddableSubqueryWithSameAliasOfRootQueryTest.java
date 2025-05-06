/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Tuple;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				EmbeddableSubqueryWithSameAliasOfRootQueryTest.MyEntity.class,
				EmbeddableSubqueryWithSameAliasOfRootQueryTest.AnotherEntity.class,
				EmbeddableSubqueryWithSameAliasOfRootQueryTest.AgainAnotherEntity.class
		}
)
@SessionFactory
@JiraKey( value = "HHH-16396")
public class EmbeddableSubqueryWithSameAliasOfRootQueryTest {

	private static final Long ENTITY_WITH_ASSOCIATION_ID = 1L;
	private static final Long ENTITY_WITHOUT_ASSOCIATION_ID = 2L;
	private static final long ANOTHER_ENTITY_ID = 3l;
	private static final long AGAIN_ANOTHER_ENTITY_ID = 4l;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					AgainAnotherEntity againAnotherEntity = new AgainAnotherEntity(AGAIN_ANOTHER_ENTITY_ID, "again");
					session.persist( againAnotherEntity );

					AnotherEntity anotherEntity = new AnotherEntity(ANOTHER_ENTITY_ID, "another", againAnotherEntity);
					session.persist( anotherEntity );

					MyEntity entity = new MyEntity( ENTITY_WITH_ASSOCIATION_ID, "with association", anotherEntity );
					session.persist( entity );

					MyEntity entity2 = new MyEntity( ENTITY_WITHOUT_ASSOCIATION_ID, "without any association", null );
					session.persist( entity2 );
				}
		);
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Tuple> results = session.createQuery(
							"select " +
									" e.myEntityEmbeddable.otherEntity.id," +
									" (select e.aString from MyEntity e where e.myEntityEmbeddable.otherEntity.id = :anotherEntityId)" +
									"from MyEntity e where e.id = :id", Tuple.class )
							.setParameter( "anotherEntityId", ANOTHER_ENTITY_ID )
							.setParameter( "id", ENTITY_WITHOUT_ASSOCIATION_ID ).list();
					assertThat( results.size() ).isEqualTo( 1 );
					Tuple tuple = results.get( 0 );
					assertThat( tuple.get( 0 ) ).isEqualTo( null );
					assertThat( tuple.get( 1 ) ).isEqualTo( "with association" );
				}
		);
	}

	@Entity(name = "MyEntity")
	public static class MyEntity {

		@Id
		private Long id;

		private String aString;

		MyEntityEmbeddable myEntityEmbeddable;

		public MyEntity() {
		}

		public MyEntity(Long id, String aString, AnotherEntity anotherEntity) {
			this.id = id;
			this.aString = aString;
			this.myEntityEmbeddable = new MyEntityEmbeddable( anotherEntity );
		}

		public Long getId() {
			return id;
		}

		public String getaString() {
			return aString;
		}

	}

	@Embeddable
	public static class MyEntityEmbeddable{
		@ManyToOne
		private AnotherEntity otherEntity;


		public MyEntityEmbeddable() {
		}

		public MyEntityEmbeddable(AnotherEntity otherEntity) {
			this.otherEntity = otherEntity;
		}

		public AnotherEntity getOtherEntity() {
			return otherEntity;
		}

	}

	@Entity(name = "AnotherEntity")
	public static class AnotherEntity{
		@Id
		private Long id;

		private String aString;

		@ManyToOne
		private AgainAnotherEntity otherEntity;

		public AnotherEntity() {
		}

		public AnotherEntity(Long id, String aString, AgainAnotherEntity otherEntity) {
			this.id = id;
			this.aString = aString;
			this.otherEntity = otherEntity;
		}

		public String getaString() {
			return aString;
		}
	}

	@Entity(name = "AgainAnotherEntity")
	public static class AgainAnotherEntity{
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
