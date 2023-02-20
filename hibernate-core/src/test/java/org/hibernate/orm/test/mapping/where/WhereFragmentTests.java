/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.mapping.where;

import java.util.Map;

import org.hibernate.annotations.Where;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.jpa.SpecHints;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.internal.util.collections.CollectionHelper.toSettingsMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Where} handling
 *
 * @implNote Requires H2 simply because we need hard-coded schema export.  The schema is simple and would
 * probably work on a larger number of databases; but there should really be nothing database specific in
 * these tests.
 */
@ServiceRegistry(
		settings = {
				@Setting(
						name = AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE,
						value = "org/hibernate/orm/test/mapping/schema.sql"
				),
				@Setting(
						name = AvailableSettings.JAKARTA_HBM2DDL_CREATE_SOURCE,
						value = "script"
				),
				@Setting(
						name = AvailableSettings.HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR,
						value = "org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor"
				),
		}
)
@DomainModel( annotatedClasses = { User.class, UserDetail.class, UserSkill.class } )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-16019" )
@RequiresDialect( H2Dialect.class )
public class WhereFragmentTests {
	public User findUserByIdUsingEntityGraph(Integer id, SessionFactoryScope factoryScope) {
		return factoryScope.fromTransaction( (session) -> {
			final Map<String, Object> properties = toSettingsMap(
					SpecHints.HINT_SPEC_FETCH_GRAPH,
					session.getEntityGraph("user-entity-graph")
			);

			return session.find(User.class, id, properties);
		} );
	}

	public User findUserByNameUsingEntityGraph(String name, SessionFactoryScope factoryScope) {
		return factoryScope.fromTransaction( (session) -> {
			final RootGraphImplementor<?> entityGraph = session.getEntityGraph( "user-entity-graph" );
			return session.createSelectionQuery( "from User where name = :name", User.class )
					.setParameter( "name", name )
					.setHint( SpecHints.HINT_SPEC_FETCH_GRAPH, entityGraph )
					.uniqueResult();
		} );
	}

	public User findUserByNameUsingHqlFetches(String name, SessionFactoryScope factoryScope) {
		final String queryString = "from User u left join fetch u.detail left join fetch u.skills where u.name=?1";
		return factoryScope.fromTransaction( (session) ->session
				.createSelectionQuery( queryString, User.class )
				.setParameter( 1, name )
				.uniqueResult()
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createNativeMutationQuery( "delete from t_user_skills" ).executeUpdate();
			session.createNativeMutationQuery( "delete t_user_details" ).executeUpdate();
			session.createNativeMutationQuery( "delete t_users" ).executeUpdate();
		} );
	}

	@Test
	public void findUserByIdWithNoDetailAndNoSkillTest(SessionFactoryScope scope) {
		// Given
		scope.inTransaction( (session) -> {
			User user = new User( 1, "Alice");
			session.persist( user );
		} );

		// When
		User alice = findUserByIdUsingEntityGraph( 1, scope );

		// Then
		assertThat( alice ).isNotNull();
		assertThat( alice.getName() ).isEqualTo( "Alice" );
		assertThat( alice.getDetail() ).isNull();
		assertThat( alice.getSkills() ).isEmpty();
	}

	@Test
	public void findUserByNameWithNoDetailAndNoSkillTest(SessionFactoryScope scope) {
		// Given
		scope.inTransaction( (session) -> {
			User user = new User( 1, "Alice");
			session.persist(user);
		} );

		// When
		User alice = findUserByNameUsingEntityGraph("Alice", scope);

		// Then
		assertThat( alice ).isNotNull();
		assertThat( alice.getName() ).isEqualTo( "Alice" );
		assertThat( alice.getDetail() ).isNull();
		assertThat( alice.getSkills() ).isEmpty();
	}

	@Test
	public void findUserByIdWithInactiveDetailAndNoSkillTest(SessionFactoryScope scope) {
		// Given
		scope.inTransaction( (session) -> {
			User user = new User( 1, "Bob" );
			session.persist( user );

			UserDetail detail = new UserDetail( 1, "New York", false, user );
			session.persist( detail );
		} );

		// When
		User bob = findUserByIdUsingEntityGraph( 1, scope );

		// Then
		assertThat( bob ).isNotNull();
		assertThat( bob.getName() ).isEqualTo( "Bob" );
		assertThat( bob.getDetail() ).isNull();
		assertThat( bob.getSkills() ).isEmpty();
	}

	@Test
	public void findUserByNameWithInactiveDetailAndNoSkillTest(SessionFactoryScope scope) {
		// Given
		scope.inTransaction( (session) -> {
			User user = new User( 1, "Bob" );
			session.persist(user);

			UserDetail detail = new UserDetail( 1, "New York", false, user );
			session.persist(detail);
		} );


		// When
		User bob = findUserByNameUsingEntityGraph( "Bob", scope );

		// Then
		assertThat( bob ).isNotNull();
		assertThat( bob.getName() ).isEqualTo( "Bob" );
		assertThat( bob.getDetail() ).isNull();
		assertThat( bob.getSkills() ).isEmpty();
	}

	@Test
	public void testActiveDetailAndDeletedSkillFromLoad(SessionFactoryScope scope) {
		createCharlie( scope );
		verifyActiveDetailAndDeletedSkill( findUserByIdUsingEntityGraph( 1, scope ) );
	}

	@Test
	public void testActiveDetailAndDeletedSkillFromHqlWithGraph(SessionFactoryScope scope) {
		createCharlie( scope );
		verifyActiveDetailAndDeletedSkill( findUserByNameUsingEntityGraph( "Charlie", scope ) );
	}

	@Test
	public void testActiveDetailAndDeletedSkillFromHqlWithFetches(SessionFactoryScope scope) {
		createCharlie( scope );
		verifyActiveDetailAndDeletedSkill( findUserByNameUsingHqlFetches( "Charlie", scope ) );
	}

	private static void createCharlie(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			User user = new User( 1, "Charlie" );
			session.persist(user);

			UserDetail detail = new UserDetail( 1, "Paris", true, user );
			session.persist(detail);

			UserSkill skill = new UserSkill( 1, "Java", true, user );
			session.persist(skill);
		} );
	}

	private void verifyActiveDetailAndDeletedSkill(User user) {
		assertThat( user ).isNotNull();
		assertThat( user.getName() ).isEqualTo( "Charlie" );
		assertThat( user.getDetail() ).isNotNull();
		assertThat( user.getDetail().getActive() ).isTrue();
		assertThat( user.getDetail().getCity() ).isEqualTo( "Paris" );
		assertThat( user.getSkills() ).isEmpty();
	}

	@Test
	public void findUserByIdWithMultipleDetailsAndSingleSkillTest(SessionFactoryScope scope) {
		// Given
		scope.inTransaction( (entityManager) -> {
			User user = new User( 1, "David" );
			entityManager.persist( user );

			UserDetail detail1 = new UserDetail( 1, "London", false, user );
			entityManager.persist( detail1 );

			UserDetail detail2 = new UserDetail( 2, "Rome", true, user );
			entityManager.persist( detail2 );

			UserSkill skill = new UserSkill( 1, "Kotlin", false, user );
			entityManager.persist( skill );
		} );

		// When
		User david = findUserByIdUsingEntityGraph( 1, scope );

		// Then
		assertNotNull(david);
		assertEquals("David", david.getName());

		assertNotNull(david.getDetail());
		assertTrue(david.getDetail().getActive());
		assertEquals("Rome", david.getDetail().getCity());

		assertFalse(david.getSkills().isEmpty());
		assertTrue(david.getSkills().stream().noneMatch(UserSkill::getDeleted));
		assertEquals(1, david.getSkills().size());
		assertEquals("Kotlin", david.getSkills().stream().findFirst().orElseThrow(IllegalStateException::new).getSkillName());
	}

	@Test
	public void findUserByNameWithMultipleDetailsAndSingleSkillTest(SessionFactoryScope scope) {
		// Given
		scope.inTransaction( (entityManager) -> {
			User user = new User( 1, "David" );
			entityManager.persist( user );

			UserDetail detail1 = new UserDetail( 1, "London", false, user );
			entityManager.persist( detail1 );

			UserDetail detail2 = new UserDetail( 2, "Rome", true, user );
			entityManager.persist( detail2 );

			UserSkill skill = new UserSkill( 1, "Kotlin", false, user );
			entityManager.persist( skill );
		} );

		// When
		User david = findUserByNameUsingEntityGraph("David", scope );

		// Then
		assertNotNull(david);
		assertEquals("David", david.getName());

		assertNotNull(david.getDetail());
		assertTrue(david.getDetail().getActive());
		assertEquals("Rome", david.getDetail().getCity());

		assertFalse(david.getSkills().isEmpty());
		assertTrue(david.getSkills().stream().noneMatch(UserSkill::getDeleted));
		assertEquals(1, david.getSkills().size());
		assertEquals("Kotlin", david.getSkills().stream().findFirst().orElseThrow(IllegalStateException::new).getSkillName());
	}

	@Test
	public void findUserByIdWithSingleDetailAndMultipleSkillsTest(SessionFactoryScope scope) {
		// Given
		scope.inTransaction( (entityManager) -> {
			User user = new User( 1, "Frank" );
			entityManager.persist( user );

			UserDetail detail = new UserDetail( 1, "Madrid", true, user );
			entityManager.persist( detail );

			UserSkill skill1 = new UserSkill( 1, "Rust", true, user );
			entityManager.persist( skill1 );

			UserSkill skill2 = new UserSkill( 2, "Erlang", false, user );
			entityManager.persist( skill2 );

			UserSkill skill3 = new UserSkill( 3, "Go", false, user );
			entityManager.persist( skill3 );

			UserSkill skill4 = new UserSkill( 4, "C", true, user );
			entityManager.persist( skill4 );
		} );

		// When
		User frank = findUserByIdUsingEntityGraph( 1, scope );

		// Then
		assertNotNull(frank);
		assertEquals("Frank", frank.getName());

		assertNotNull(frank.getDetail());
		assertTrue(frank.getDetail().getActive());
		assertEquals("Madrid", frank.getDetail().getCity());

		assertFalse(frank.getSkills().isEmpty());
		assertTrue(frank.getSkills().stream().noneMatch(UserSkill::getDeleted));
		assertEquals(2, frank.getSkills().size());
	}

	@Test
	public void findUserByNameWithSingleDetailAndMultipleSkillsTest(SessionFactoryScope scope) {
		// Given
		scope.inTransaction( (entityManager) -> {
			User user = new User( 1, "Frank" );
			entityManager.persist( user );

			UserDetail detail = new UserDetail( 1, "Madrid", true, user );
			entityManager.persist( detail );

			UserSkill skill1 = new UserSkill( 1, "Rust", true, user );
			entityManager.persist( skill1 );

			UserSkill skill2 = new UserSkill( 2, "Erlang", false, user );
			entityManager.persist( skill2 );

			UserSkill skill3 = new UserSkill( 3, "Go", false, user );
			entityManager.persist( skill3 );

			UserSkill skill4 = new UserSkill( 4, "C", true, user );
			entityManager.persist( skill4 );
		} );

		// When
		User frank = findUserByNameUsingEntityGraph( "Frank", scope );

		// Then
		assertNotNull(frank);
		assertEquals("Frank", frank.getName());

		assertNotNull(frank.getDetail());
		assertTrue(frank.getDetail().getActive());
		assertEquals("Madrid", frank.getDetail().getCity());

		assertFalse(frank.getSkills().isEmpty());
		assertTrue(frank.getSkills().stream().noneMatch(UserSkill::getDeleted));
		assertEquals(2, frank.getSkills().size());
	}

	@Test
	public void findUserByIdWithMultipleDetailsAndMultipleSkillsTest1(SessionFactoryScope scope) {
		// Given
		scope.inTransaction( (entityManager) -> {
			User user = new User( 1, "Eve" );
			entityManager.persist( user );

			UserDetail detail1 = new UserDetail( 1, "Moscow", false, user );
			entityManager.persist( detail1 );

			UserDetail detail2 = new UserDetail( 2, "Istanbul", false, user );
			entityManager.persist( detail2 );

			UserDetail detail3 = new UserDetail( 3, "Berlin", true, user );
			entityManager.persist( detail3 );

			UserSkill skill1 = new UserSkill( 1, "Python", true, user );
			entityManager.persist( skill1 );

			UserSkill skill2 = new UserSkill( 2, "Ruby", false, user );
			entityManager.persist( skill2 );
		} );

		// When
		User eve = findUserByIdUsingEntityGraph( 1, scope );

		// Then
		assertNotNull(eve);
		assertEquals("Eve", eve.getName());

		assertNotNull(eve.getDetail());
		assertTrue(eve.getDetail().getActive());
		assertEquals("Berlin", eve.getDetail().getCity());

		assertFalse(eve.getSkills().isEmpty());
		assertTrue(eve.getSkills().stream().noneMatch(UserSkill::getDeleted));
		assertEquals(1, eve.getSkills().size());
		assertEquals("Ruby", eve.getSkills().stream().findFirst().orElseThrow(IllegalStateException::new).getSkillName());
	}

	@Test
	public void findUserByNameWithMultipleDetailsAndMultipleSkillsTest1(SessionFactoryScope scope) {
		// Given
		scope.inTransaction( (entityManager) -> {
			User user = new User( 1, "Eve" );
			entityManager.persist( user );

			UserDetail detail1 = new UserDetail( 1, "Moscow", false, user );
			entityManager.persist( detail1 );

			UserDetail detail2 = new UserDetail( 2, "Istanbul", false, user );
			entityManager.persist( detail2 );

			UserDetail detail3 = new UserDetail( 3, "Berlin", true, user );
			entityManager.persist( detail3 );

			UserSkill skill1 = new UserSkill( 1, "Python", true, user );
			entityManager.persist( skill1 );

			UserSkill skill2 = new UserSkill( 2, "Ruby", false, user );
			entityManager.persist( skill2 );
		} );

		// When
		User eve = findUserByNameUsingEntityGraph( "Eve", scope );

		// Then
		assertNotNull(eve);
		assertEquals("Eve", eve.getName());

		assertNotNull(eve.getDetail());
		assertTrue(eve.getDetail().getActive());
		assertEquals("Berlin", eve.getDetail().getCity());

		assertFalse(eve.getSkills().isEmpty());
		assertTrue(eve.getSkills().stream().noneMatch(UserSkill::getDeleted));
		assertEquals(1, eve.getSkills().size());
		assertEquals("Ruby", eve.getSkills().stream().findFirst().orElseThrow(IllegalStateException::new).getSkillName());
	}

	@Test
	public void findUserByIdWithMultipleDetailsAndMultipleSkillsTest2(SessionFactoryScope scope) {
		// Given
		scope.inTransaction( (entityManager) -> {
			User user = new User( 1, "Grace" );
			entityManager.persist( user );

			UserDetail detail1 = new UserDetail( 1, "Vienna", false, user );
			entityManager.persist( detail1 );

			UserDetail detail2 = new UserDetail( 2, "Barcelona", true, user );
			entityManager.persist( detail2 );

			UserSkill skill1 = new UserSkill( 1, "PHP", false, user );
			entityManager.persist( skill1 );

			UserSkill skill2 = new UserSkill( 2, "Swift", false, user );
			entityManager.persist( skill2 );

			UserSkill skill3 = new UserSkill( 3, "Dart", false, user );
			entityManager.persist( skill3 );

			UserSkill skill4 = new UserSkill( 4, "Scala", false, user );
			entityManager.persist( skill4 );
		} );

		// When
		User grace = findUserByIdUsingEntityGraph( 1, scope );

		// Then
		assertNotNull(grace);
		assertEquals("Grace", grace.getName());

		assertNotNull(grace.getDetail());
		assertTrue(grace.getDetail().getActive());
		assertEquals("Barcelona", grace.getDetail().getCity());

		assertFalse(grace.getSkills().isEmpty());
		assertTrue(grace.getSkills().stream().noneMatch(UserSkill::getDeleted));
		assertEquals(4, grace.getSkills().size());
	}

	@Test
	public void findUserByNameWithMultipleDetailsAndMultipleSkillsTest2(SessionFactoryScope scope) {
		// Given
		scope.inTransaction( (entityManager) -> {
			User user = new User( 1, "Grace" );
			entityManager.persist( user );

			UserDetail detail1 = new UserDetail( 1, "Vienna", false, user );
			entityManager.persist( detail1 );

			UserDetail detail2 = new UserDetail( 2, "Barcelona", true, user );
			entityManager.persist( detail2 );

			UserSkill skill1 = new UserSkill( 1, "PHP", false, user );
			entityManager.persist( skill1 );

			UserSkill skill2 = new UserSkill( 2, "Swift", false, user );
			entityManager.persist( skill2 );

			UserSkill skill3 = new UserSkill( 3, "Dart", false, user );
			entityManager.persist( skill3 );

			UserSkill skill4 = new UserSkill( 4, "Scala", false, user );
			entityManager.persist( skill4 );
		} );

		// When
		User grace = findUserByNameUsingEntityGraph( "Grace", scope );

		// Then
		assertNotNull(grace);
		assertEquals("Grace", grace.getName());

		assertNotNull(grace.getDetail());
		assertTrue(grace.getDetail().getActive());
		assertEquals("Barcelona", grace.getDetail().getCity());

		assertFalse(grace.getSkills().isEmpty());
		assertTrue(grace.getSkills().stream().noneMatch(UserSkill::getDeleted));
		assertEquals(4, grace.getSkills().size());
	}
}
