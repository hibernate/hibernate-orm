package org.hibernate.orm.test.polymorphic;

import java.util.List;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				PolymorphicQueriesTest.EntityA.class,
				PolymorphicQueriesTest.EntityB.class,
				PolymorphicQueriesTest.EntityC.class
		}
)
@SessionFactory
@TestForIssue( jiraKey = "HHH-15718")
public class PolymorphicQueriesTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityC c1 = new EntityC( 1l, "c1" );
					EntityA a = new EntityA( 2l, "a", c1 );
					EntityB b = new EntityB( 2l, "b", c1 );

					session.persist( c1 );
					session.persist( a );
					session.persist( b );
				}
		);
	}

	@BeforeEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from EntityA" ).executeUpdate();
					session.createMutationQuery( "delete from EntityB" ).executeUpdate();
					session.createMutationQuery( "delete from EntityC" ).executeUpdate();
				}
		);
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<I> results = session.createQuery( "from " + I.class.getName(), I.class ).list();
					assertThat( results.size() ).isEqualTo( 2 );
					assertThat(results.get(0)).isInstanceOf(EntityA.class);
					assertThat(results.get(1)).isInstanceOf(EntityB.class);
				}
		);

		scope.inTransaction(
				session -> {
					List<I> results = session.createQuery( "from " + I.class.getName() + " i", I.class ).list();
					assertThat( results.size() ).isEqualTo( 2 );
					assertThat(results.get(0)).isInstanceOf(EntityA.class);
					assertThat(results.get(1)).isInstanceOf(EntityB.class);
				}
		);

		scope.inTransaction(
				session -> {
					List<I> results = session.createQuery( "select i from " + I.class.getName() + " i", I.class )
							.list();
					assertThat( results.size() ).isEqualTo( 2 );
					assertThat(results.get(0)).isInstanceOf(EntityA.class);
					assertThat(results.get(1)).isInstanceOf(EntityB.class);
				}
		);
	}

	@Test
	public void testQueryWithProjection(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<String> results = session.createQuery(
							"select displayName from " + I.class.getName(),
							String.class
					).list();

					assertThat( results.size() ).isEqualTo( 2 );
					assertTrue( results.contains( "a" ) );
					assertTrue( results.contains( "b" ) );
				}
		);

		scope.inTransaction(
				session -> {
					List<String> results = session.createQuery(
							"select i.displayName from " + I.class.getName() + " i",
							String.class
					).list();
					assertThat( results.size() ).isEqualTo( 2 );
					assertTrue( results.contains( "a" ) );
					assertTrue( results.contains( "b" ) );
				}
		);
	}

	@Test
	public void testQueryWithProjection2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<EntityC> results = session.createQuery(
							"select entityC from " + I.class.getName(),
							EntityC.class
					).list();
					assertThat( results.size() ).isEqualTo( 2 );
					assertThat( results.get( 0 ) ).isEqualTo( results.get( 1 ) );
				}
		);
		scope.inTransaction(
				session -> {
					List<EntityC> results = session.createQuery(
							"select i.entityC from " + I.class.getName() + " i",
							EntityC.class
					).list();
					assertThat( results.size() ).isEqualTo( 2 );
					assertThat( results.get( 0 ) ).isEqualTo( results.get( 1 ) );
				}
		);

		scope.inTransaction(
				session -> {
					List<String> results = session.createQuery(
							"select entityC.name from " + I.class.getName(),
							String.class
					).list();
					assertThat( results.size() ).isEqualTo( 2 );
					assertThat( results.get( 0 ) ).isEqualTo( results.get( 1 ) );
					assertThat( results.get( 0 ) ).isEqualTo( "c1" );
				}
		);

		scope.inTransaction(
				session -> {
					List<String> results = session.createQuery(
									"select i.entityC.name from " + I.class.getName() + " i",
									String.class
							)
							.list();
					assertThat( results.size() ).isEqualTo( 2 );
					assertThat( results.get( 0 ) ).isEqualTo( results.get( 1 ) );
					assertThat( results.get( 0 ) ).isEqualTo( "c1" );
				}
		);

		scope.inTransaction(
				session -> {
					session.createQuery( "select cs from " + I.class.getName(), List.class ).list();
				}
		);

		scope.inTransaction(
				session -> {
					session.createQuery( "select cs from " + I.class.getName() + " i", List.class ).list();
				}
		);
	}

	@Test
	public void testQueryWithWhereClause(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<I> results = session.createQuery(
									"from " + I.class.getName() + " i where i.displayName is null",
									I.class
							)
							.list();
					assertThat( results.size() ).isEqualTo( 0 );
				}
		);

		scope.inTransaction(
				session -> {
					List<I> results = session.createQuery(
							"from " + I.class.getName() + " where displayName is null",
							I.class
					).list();
					assertThat( results.size() ).isEqualTo( 0 );
				}
		);

		scope.inTransaction(
				session -> {
					List<I> results = session.createQuery(
							"select i from " + I.class.getName() + " i where i.displayName is null",
							I.class
					).list();
					assertThat( results.size() ).isEqualTo( 0 );
				}
		);
	}

	@Test
	public void testQueryWithSelectionAndProjection(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery(
							"select i.displayName from " + I.class.getName() + " i where i.displayName is null",
							String.class
					).list();
				}
		);
	}

	public interface I {
		String getDisplayName();
	}

	@Entity(name = "EntityA")
	public static class EntityA implements I {
		@Id
		private Long id;

		private String displayName;

		@ManyToOne
		private EntityC entityC;

		@OneToMany
		private List<EntityC> cs;

		public EntityA() {
		}

		public EntityA(Long id, String displayName, EntityC entityC) {
			this.id = id;
			this.displayName = displayName;
			this.entityC = entityC;
		}

		public Long getId() {
			return id;
		}

		@Override
		public String getDisplayName() {
			return displayName;
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB implements I {
		@Id
		private Long id;

		private String displayName;

		@ManyToOne
		private EntityC entityC;

		@OneToMany
		private List<EntityC> cs;

		public EntityB() {
		}

		public EntityB(Long id, String displayName, EntityC entityC) {
			this.id = id;
			this.displayName = displayName;
			this.entityC = entityC;
		}

		public Long getId() {
			return id;
		}

		@Override
		public String getDisplayName() {
			return displayName;
		}
	}

	@Entity(name = "EntityC")
	public static class EntityC {
		@Id
		private Long id;

		private String name;

		public EntityC() {
		}

		public EntityC(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

}
