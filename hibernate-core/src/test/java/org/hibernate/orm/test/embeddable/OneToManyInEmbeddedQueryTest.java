package org.hibernate.orm.test.embeddable;

import java.io.Serializable;
import java.util.List;

import org.hibernate.query.SemanticException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@DomainModel( annotatedClasses = {
		OneToManyInEmbeddedQueryTest.EntityA.class,
		OneToManyInEmbeddedQueryTest.EntityC.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17528" )
public class OneToManyInEmbeddedQueryTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityC entityC1 = new EntityC( "entityc_1" );
			final EntityC entityC2 = new EntityC( "entityc_2" );
			final EntityC entityC3 = new EntityC( "entityc_3" );
			final EntityA entityA1 = new EntityA( 1, "entitya_1", new EmbeddedValue( List.of( entityC1, entityC2 ) ) );
			final EntityA entityA2 = new EntityA( 2, "entitya_2", new EmbeddedValue( List.of( entityC3 ) ) );
			session.persist( entityA1 );
			session.persist( entityA2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from EntityC" ).executeUpdate();
			session.createMutationQuery( "delete from EntityA" ).executeUpdate();
		} );
	}

	@Test
	public void testSelectEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA entityA = session.createQuery(
					"select a from EntityA a where a.id = 1",
					EntityA.class
			).getSingleResult();
			assertThat( entityA.getEmbedded().getEntityCList().stream().map( EntityC::getName ) ).containsOnly(
					"entityc_1",
					"entityc_2"
			);
			// test orphan removal
			entityA.getEmbedded().getEntityCList().clear();
		} );
		scope.inSession( session -> {
			final List<EntityC> entityCList = session.createQuery( "from EntityC", EntityC.class ).getResultList();
			assertThat( entityCList.stream().map( EntityC::getName ) ).doesNotContain( "entityc_1", "entityc_2" );
		} );
	}

	@Test
	public void testSelectEmbedded(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try {
				session.createQuery(
						"select a.embedded from EntityA a where a.id = 2",
						EmbeddedValue.class
				).getSingleResult();
				fail( "Should throw SemanticException" );
			}
			catch (Exception e) {
				final Throwable cause = e.getCause();
				assertThat( cause ).isInstanceOf( SemanticException.class );
				assertThat( cause.getMessage() ).contains(
						"selection of an embeddable containing collections is not supported"
				);
			}
		} );
	}

	@Entity( name = "EntityA" )
	public static class EntityA {
		@Id
		private Integer id;

		private String name;

		@Embedded
		private EmbeddedValue embedded;

		public EntityA() {
		}

		private EntityA(Integer id, String name, EmbeddedValue embedded) {
			this.id = id;
			this.name = name;
			this.embedded = embedded;
		}

		public Integer getId() {
			return id;
		}

		public EmbeddedValue getEmbedded() {
			return embedded;
		}
	}

	@Embeddable
	public static class EmbeddedValue implements Serializable {
		@OneToMany( cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER )
		@JoinColumn( name = "entityA_id" )
		private List<EntityC> entityCList;

		public EmbeddedValue() {
		}

		public EmbeddedValue(List<EntityC> entityCList) {
			this.entityCList = entityCList;
		}

		public List<EntityC> getEntityCList() {
			return entityCList;
		}
	}

	@Entity( name = "EntityC" )
	public static class EntityC {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		public EntityC() {
		}

		public EntityC(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
