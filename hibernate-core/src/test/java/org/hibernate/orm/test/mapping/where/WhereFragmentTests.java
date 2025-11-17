/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.where;

import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.jpa.SpecHints;

import org.hibernate.testing.jdbc.SQLStatementInspector;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
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
@SessionFactory( useCollectingStatementInspector = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16019" )
@RequiresDialect( H2Dialect.class )
public class WhereFragmentTests {
	/**
	 * Loads a User, fetching their detail and skills using an entity-graph
	 */
	public User findUserByIdUsingEntityGraph(Integer id, SessionFactoryScope factoryScope) {
		return factoryScope.fromTransaction( (session) -> session.find( User.class, id,
				Map.of( SpecHints.HINT_SPEC_FETCH_GRAPH, session.getEntityGraph("user-entity-graph") ) ) );
	}

	/**
	 * Loads a User (via HQL), fetching their detail and skills using an entity-graph
	 */
	public User findUserByNameUsingEntityGraph(String name, SessionFactoryScope factoryScope) {
		return factoryScope.fromTransaction( (session) -> {
			final RootGraphImplementor<?> entityGraph = session.getEntityGraph( "user-entity-graph" );
			return session.createSelectionQuery( "from User where name = :name", User.class )
					.setParameter( "name", name )
					.setHint( SpecHints.HINT_SPEC_FETCH_GRAPH, entityGraph )
					.uniqueResult();
		} );
	}

	/**
	 * Loads a User (via HQL), fetching their detail and skills using HQL fetch joins
	 */
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
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Alice - no detail and no skills

	/**
	 * Create the user Alice who has no details and no skills
	 */
	private void createAlice(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			User user = new User( 1, "Alice");
			session.persist( user );
		} );
	}

	private void verifyAlice(User alice) {
		assertThat( alice ).isNotNull();
		assertThat( alice.getName() ).isEqualTo( "Alice" );
		assertThat( alice.getDetail() ).isNull();
		assertThat( alice.getSkills() ).isEmpty();
	}

	@Test
	public void testFindAliceById(SessionFactoryScope scope) {
		createAlice( scope );

		User alice = findUserByIdUsingEntityGraph( 1, scope );
		verifyAlice( alice );
	}

	@Test
	public void testFindAliceByHql(SessionFactoryScope scope) {
		createAlice( scope );

		User alice = findUserByNameUsingEntityGraph("Alice", scope);
		verifyAlice( alice );
	}

	@Test
	public void testFindAliceByHqlWithFetchJoin(SessionFactoryScope scope) {
		createAlice( scope );

		User alice = findUserByNameUsingHqlFetches("Alice", scope);
		verifyAlice( alice );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Bob - an inactive detail and no skills

	/**
	 * Create the user Bob who has an inactive detail and no skills
	 */
	private static void createBob(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			User user = new User( 1, "Bob" );
			session.persist( user );

			UserDetail detail = new UserDetail( 1, "New York", false, user );
			session.persist( detail );
		} );
	}

	private static void verifyBob(User bob) {
		assertThat( bob ).isNotNull();
		assertThat( bob.getName() ).isEqualTo( "Bob" );
		assertThat( bob.getDetail() ).isNull();
		assertThat( bob.getSkills() ).isEmpty();
	}

	@Test
	public void testFindBobById(SessionFactoryScope scope) {
		createBob( scope );

		User bob = findUserByIdUsingEntityGraph( 1, scope );
		verifyBob( bob );
	}

	@Test
	public void testFindBobByHql(SessionFactoryScope scope) {
		createBob( scope );

		User bob = findUserByNameUsingEntityGraph( "Bob", scope );
		verifyBob( bob );
	}

	@Test
	public void testFindBobByHqlWithFetchJoin(SessionFactoryScope scope) {
		createBob( scope );

		User bob = findUserByNameUsingHqlFetches( "Bob", scope );
		verifyBob( bob );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Charlie - an active detail and a deleted skill

	/**
	 * Create the user Bob who has an active detail and a deleted skill
	 */
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

	private void verifyCharlie(User user) {
		assertThat( user ).isNotNull();
		assertThat( user.getName() ).isEqualTo( "Charlie" );
		assertThat( user.getDetail() ).isNotNull();
		assertThat( user.getDetail().getActive() ).isTrue();
		assertThat( user.getDetail().getCity() ).isEqualTo( "Paris" );
		assertThat( user.getSkills() ).isEmpty();
	}

	@Test
	public void testFindCharlieById(SessionFactoryScope scope) {
		createCharlie( scope );
		verifyCharlie( findUserByIdUsingEntityGraph( 1, scope ) );
	}

	@Test
	public void testFindCharlieByHqlWithGraph(SessionFactoryScope scope) {
		createCharlie( scope );
		verifyCharlie( findUserByNameUsingEntityGraph( "Charlie", scope ) );
	}

	@Test
	public void testFindCharlieByHqlWithJoinFetch(SessionFactoryScope scope) {
		createCharlie( scope );
		verifyCharlie( findUserByNameUsingHqlFetches( "Charlie", scope ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// David - multiple details, single skill

	/**
	 * Create the user David with multiple details, single skill
	 */
	private static void createDavid(SessionFactoryScope scope) {
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
	}

	private static void verifyDavid(User david) {
		assertNotNull( david );
		assertEquals("David", david.getName());

		assertNotNull( david.getDetail());
		assertTrue( david.getDetail().getActive());
		assertEquals("Rome", david.getDetail().getCity());

		assertFalse( david.getSkills().isEmpty());
		assertTrue( david.getSkills().stream().noneMatch(UserSkill::getDeleted));
		assertEquals(1, david.getSkills().size());
		assertEquals("Kotlin", david.getSkills().stream().findFirst().orElseThrow(IllegalStateException::new).getSkillName());
	}

	@Test
	public void testFindDavidById(SessionFactoryScope scope) {
		createDavid( scope );

		User david = findUserByIdUsingEntityGraph( 1, scope );
		verifyDavid( david );
	}

	@Test
	public void testFindDavidByHqlWithGraph(SessionFactoryScope scope) {
		createDavid( scope );

		User david = findUserByNameUsingEntityGraph("David", scope );
		verifyDavid( david );
	}

	@Test
	public void testFindDavidByHqlWithJoinFetch(SessionFactoryScope scope) {
		createDavid( scope );

		User david = findUserByNameUsingHqlFetches("David", scope );
		verifyDavid( david );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Eve
	//		- 1 active and 2 inactive details
	//		- 1 active and 1 inactive skills

	private static void createEve(SessionFactoryScope scope) {
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
	}

	private static void verifyEve(User eve) {
		assertNotNull( eve );
		assertEquals("Eve", eve.getName());

		assertNotNull( eve.getDetail());
		assertTrue( eve.getDetail().getActive());
		assertEquals("Berlin", eve.getDetail().getCity());

		assertFalse( eve.getSkills().isEmpty());
		assertTrue( eve.getSkills().stream().noneMatch(UserSkill::getDeleted));
		assertEquals(1, eve.getSkills().size());
		assertEquals("Ruby", eve.getSkills().stream().findFirst().orElseThrow(IllegalStateException::new).getSkillName());
	}

	@Test
	public void testFindEveById(SessionFactoryScope scope) {
		createEve( scope );

		User eve = findUserByIdUsingEntityGraph( 1, scope );
		verifyEve( eve );
	}

	@Test
	public void testFindEveByHqlWithGraph(SessionFactoryScope scope) {
		createEve( scope );

		User eve = findUserByNameUsingEntityGraph( "Eve", scope );
		verifyEve( eve );
	}

	@Test
	public void testFindEveByHqlWithJoinFetch(SessionFactoryScope scope) {
		createEve( scope );

		User eve = findUserByNameUsingHqlFetches( "Eve", scope );
		verifyEve( eve );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Frank
	//		- 1 active detail
	//		- 2 deleted and 2 active skills

	/**
	 * Create the user Frank with a single active detail and 2 deleted and 2 active skills
	 */
	private static void createFrank(SessionFactoryScope scope) {
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
	}

	private static void verifyFrank(User frank) {
		assertNotNull( frank );
		assertEquals("Frank", frank.getName());

		assertNotNull( frank.getDetail());
		assertTrue( frank.getDetail().getActive());
		assertEquals("Madrid", frank.getDetail().getCity());

		assertFalse( frank.getSkills().isEmpty());
		assertTrue( frank.getSkills().stream().noneMatch(UserSkill::getDeleted));
		assertEquals(2, frank.getSkills().size());
	}

	@Test
	public void testFindFrankById(SessionFactoryScope scope) {
		createFrank( scope );

		User frank = findUserByIdUsingEntityGraph( 1, scope );
		verifyFrank( frank );
	}

	@Test
	public void testFindFrankByHqlWithGraph(SessionFactoryScope scope) {
		createFrank( scope );

		User frank = findUserByNameUsingEntityGraph( "Frank", scope );
		verifyFrank( frank );
	}

	@Test
	public void testFindFrankByHqlWithJoinFetch(SessionFactoryScope scope) {
		createFrank( scope );

		User frank = findUserByNameUsingHqlFetches( "Frank", scope );
		verifyFrank( frank );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Grace
	//		- 1 active and 1 inactive details
	//		- 4 inactive skills

	private static void createGrace(SessionFactoryScope scope) {
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
	}

	private static void verifyGrace(User grace) {
		assertNotNull( grace );
		assertEquals("Grace", grace.getName());

		assertNotNull( grace.getDetail());
		assertTrue( grace.getDetail().getActive());
		assertEquals("Barcelona", grace.getDetail().getCity());

		assertFalse( grace.getSkills().isEmpty());
		assertTrue( grace.getSkills().stream().noneMatch(UserSkill::getDeleted));
		assertEquals(4, grace.getSkills().size());
	}

	@Test
	public void testFindGraceById(SessionFactoryScope scope) {
		createGrace( scope );

		User grace = findUserByIdUsingEntityGraph( 1, scope );
		verifyGrace( grace );
	}

	@Test
	public void testFindGraceByHqlWithGraph(SessionFactoryScope scope) {
		createGrace( scope );

		User grace = findUserByNameUsingEntityGraph( "Grace", scope );
		verifyGrace( grace );
	}

	@Test
	public void testFindGraceByHqlWithJoinFetch(SessionFactoryScope scope) {
		createGrace( scope );

		User grace = findUserByNameUsingHqlFetches( "Grace", scope );
		verifyGrace( grace );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Zurg
	//		- 1 active and 1 inactive detail
	//		- 1 active and 1 inactive skill

	private void createZurg(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final User user = new User( 1, "Zurg" );
			session.persist( user );

			UserDetail detail1 = new UserDetail( 1, "Infinity", false, user );
			session.persist( detail1 );

			UserDetail detail2 = new UserDetail( 2, "Beyond", true, user );
			session.persist( detail2 );

			UserSkill skill1 = new UserSkill( 1, "Plundering", false, user );
			session.persist( skill1 );

			UserSkill skill2 = new UserSkill( 2, "Pillaging", true, user );
			session.persist( skill2 );
		} );
	}

	@Test
	public void testLoadEntity(SessionFactoryScope scope) {
		createZurg( scope );

		scope.inTransaction( (session) -> {
			final UserSkill skill1 = session.find( UserSkill.class, 1 );
			assertThat( skill1 ).isNotNull();

			final UserSkill skill2 = session.find( UserSkill.class, 2 );
			assertThat( skill2 ).isNull();
		} );

		scope.inTransaction( (session) -> {
			final UserDetail detail1 = session.find( UserDetail.class, 1 );
			assertThat( detail1 ).isNull();

			final UserDetail detail2 = session.find( UserDetail.class, 2 );
			assertThat( detail2 ).isNotNull();
		} );
	}

	@Test
	public void testSubsequentInitialization(SessionFactoryScope scope) {
		createZurg( scope );

		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			User user = session.find( User.class, 1 );

			// this should have initialized User & User#detail in 2 separate selects
			assertThat( Hibernate.isInitialized( user.getDetail() ) ).isTrue();
			assertThat( Hibernate.isInitialized( user.getSkills() ) ).isFalse();
			assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );

			// trigger load of User#skills
			statementInspector.clear();
			Hibernate.initialize( user.getSkills() );
			assertThat( Hibernate.isInitialized( user.getSkills() ) ).isTrue();
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( user.getSkills() ).hasSize( 1 );
		} );
	}
}
