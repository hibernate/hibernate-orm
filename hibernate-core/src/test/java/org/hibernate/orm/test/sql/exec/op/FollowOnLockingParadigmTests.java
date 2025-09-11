/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.op;

import jakarta.persistence.Timeout;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.exec.internal.StandardStatementCreator;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.ops.spi.DatabaseSelect;
import org.hibernate.sql.ops.spi.LoadedValuesCollector;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Initial look at support for JPA extended locking scope.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {Person.class, Team.class})
@SessionFactory( useCollectingStatementInspector = true )
public class FollowOnLockingParadigmTests {
	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Person steve = new Person( 1, "Steve" );
			session.persist( steve );

			final Person john = new Person( 2, "John" );
			session.persist( john );

			final Person brian = new Person( 3, "Brian" );
			session.persist( brian );

			final Team team1 = new Team( 1, "Space Ballerz" );
			team1.addMember( steve ).addMember( john ).addMember( brian );
			session.persist( team1 );

			assertThat( team1.getMembers() ).hasSize( 3 );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	/**
	 * This is the test that actually leverages the full DatabaseOperation flow,
	 * starting from SqlAstTranslator, to achieve follow-on locking.
	 */
	@Test
	void testFollowOnLockingFlowNormalScope(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel().findEntityDescriptor( Team.class );

		final LockMode lockMode = LockMode.PESSIMISTIC_WRITE;
		final Locking.Scope lockScope = Locking.Scope.ROOT_ONLY;
		final Timeout lockTimeout = Timeout.seconds( 2 );

		final Helper.SelectByIdQuery selectByIdQuery = Helper.createSelectByIdQuery(
				entityDescriptor,
				lockMode,
				sessionFactory
		);

		final QueryOptionsImpl queryOptions = new QueryOptionsImpl();
		queryOptions.getLockOptions()
				.setLockMode( lockMode )
				.setFollowOnStrategy( Locking.FollowOn.FORCE )
				.setTimeout( lockTimeout )
				.setScope( lockScope );

		final NavigablePath rootPath = selectByIdQuery.sqlAst()
				.getQuerySpec()
				.getFromClause()
				.getRoots()
				.get( 0 )
				.getNavigablePath();

		final SqlAstTranslatorFactory translatorFactory = sessionFactory.getJdbcServices().getDialect().getSqlAstTranslatorFactory();
		final SqlAstTranslator<JdbcOperationQuerySelect> translator = translatorFactory.buildSelectTranslator( sessionFactory, selectByIdQuery.sqlAst() );

		final DatabaseSelect databaseSelect = translator.translateLockingSelect(
				selectByIdQuery.jdbcParameterBindings(),
				queryOptions
		);

		final LoadedValuesCollector loadedValuesCollector = databaseSelect.getLoadedValuesCollector();

		factoryScope.inTransaction( (session) -> {
			final SingleIdExecutionContext executionContext = new SingleIdExecutionContext(
					session,
					null,
					1,
					entityDescriptor,
					queryOptions,
					null
			);

			sqlCollector.clear();
			final List<Team> results = databaseSelect.execute(
					Team.class,
					1,
					StandardStatementCreator.getStatementCreator( ScrollMode.FORWARD_ONLY ),
					selectByIdQuery.jdbcParameterBindings(),
					row -> (Team) row[0],
					ListResultsConsumer.instance( ListResultsConsumer.UniqueSemantic.FILTER ),
					executionContext
			);

			// the initial query and subsequent locking-select for the root
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );

			final Team team = results.get( 0 );

			assertThat( team.getMembers() ).hasSize( 3 );
			assertThat( loadedValuesCollector.getCollectedRootEntities() ).hasSize( 1 );
			assertThat( loadedValuesCollector.getCollectedNonRootEntities() ).isEmpty();
			assertThat( loadedValuesCollector.getCollectedCollections() ).isEmpty();

			// let's look a little deeper...

			// the root
			final LoadedValuesCollector.LoadedEntityRegistration rootEntityDetails = loadedValuesCollector.getCollectedRootEntities().get( 0 );
			assertThat( session.getPersistenceContext().getEntity( rootEntityDetails.entityKey() ) ).isSameAs( team );
			assertThat( rootEntityDetails.navigablePath() ).isEqualTo( rootPath );

		} );
	}

	/**
	 * This is the test that actually leverages the full DatabaseOperation flow,
	 * starting from SqlAstTranslator, to achieve follow-on locking.
	 */
	@Test
	@NotImplementedYet( reason = "collection scope (JPA extended scope) is not properly handled yet" )
	void testFollowOnLockingFlowCollectionScope(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel()
				.findEntityDescriptor( Team.class );

		final LockMode lockMode = LockMode.PESSIMISTIC_WRITE;
		final Locking.Scope lockScope = Locking.Scope.INCLUDE_COLLECTIONS;
		final Timeout lockTimeout = Timeout.seconds( 2 );

		final Helper.SelectByIdQuery selectByIdQuery = Helper.createSelectByIdQuery(
				entityDescriptor,
				lockMode,
				lockScope,
				sessionFactory
		);

		final QueryOptionsImpl queryOptions = new QueryOptionsImpl();
		queryOptions.getLockOptions()
				.setLockMode( lockMode )
				.setFollowOnStrategy( Locking.FollowOn.FORCE )
				.setTimeout( lockTimeout )
				.setScope( lockScope );

		final NavigablePath rootPath = selectByIdQuery.sqlAst()
				.getQuerySpec()
				.getFromClause()
				.getRoots()
				.get( 0 )
				.getNavigablePath();

		final SqlAstTranslatorFactory translatorFactory = sessionFactory.getJdbcServices().getDialect()
				.getSqlAstTranslatorFactory();
		final SqlAstTranslator<JdbcOperationQuerySelect> translator = translatorFactory.buildSelectTranslator(
				sessionFactory, selectByIdQuery.sqlAst() );

		final DatabaseSelect databaseSelect = translator.translateLockingSelect(
				selectByIdQuery.jdbcParameterBindings(),
				queryOptions
		);

		final LoadedValuesCollector loadedValuesCollector = databaseSelect.getLoadedValuesCollector();

		factoryScope.inTransaction( (session) -> {
			final SingleIdExecutionContext executionContext = new SingleIdExecutionContext(
					session,
					null,
					1,
					entityDescriptor,
					queryOptions,
					null
			);

			sqlCollector.clear();
			final List<Team> results = databaseSelect.execute(
					Team.class,
					1,
					StandardStatementCreator.getStatementCreator( ScrollMode.FORWARD_ONLY ),
					selectByIdQuery.jdbcParameterBindings(),
					row -> (Team) row[0],
					ListResultsConsumer.instance( ListResultsConsumer.UniqueSemantic.FILTER ),
					executionContext
			);

			final Team team = results.get( 0 );

			// 1. initial query
			// 2. lock of teams
			// 3. lock of persons
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 3 );

			assertThat( team.getMembers() ).hasSize( 3 );
			assertThat( loadedValuesCollector.getCollectedRootEntities() ).hasSize( 1 );
			assertThat( loadedValuesCollector.getCollectedNonRootEntities() ).hasSize( 3 );
			assertThat( loadedValuesCollector.getCollectedCollections() ).hasSize( 1 );

			// let's look a little deeper...

			// the root
			final LoadedValuesCollector.LoadedEntityRegistration rootEntityDetails = loadedValuesCollector.getCollectedRootEntities()
					.get( 0 );
			assertThat( session.getPersistenceContext().getEntity( rootEntityDetails.entityKey() ) ).isSameAs( team );
			assertThat( rootEntityDetails.navigablePath() ).isEqualTo( rootPath );

			// the collection
			final NavigablePath expectedMembersPath = rootPath.append( "members" );
			final LoadedValuesCollector.LoadedCollectionRegistration membersDetails = loadedValuesCollector.getCollectedCollections()
					.get( 0 );
			assertThat( membersDetails.navigablePath() ).isEqualTo( expectedMembersPath );
			assertThat( membersDetails.collectionKey().getKey() ).isEqualTo( 1 );
		} );
	}

	/**
	 * This is the test that actually leverages the full DatabaseOperation flow,
	 * starting from SqlAstTranslator, to achieve follow-on locking.
	 */
	@Test
	void testFollowOnLockingFlowFetchScope(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel().findEntityDescriptor( Team.class );

		final LockMode lockMode = LockMode.PESSIMISTIC_WRITE;
		final Locking.Scope lockScope = Locking.Scope.INCLUDE_FETCHES;
		final Timeout lockTimeout = Timeout.seconds( 2 );

		final Helper.SelectByIdQuery selectByIdQuery = Helper.createSelectByIdQuery(
				entityDescriptor,
				lockMode,
				lockScope,
				sessionFactory
		);

		final QueryOptionsImpl queryOptions = new QueryOptionsImpl();
		queryOptions.getLockOptions()
				.setLockMode( lockMode )
				.setFollowOnStrategy( Locking.FollowOn.FORCE )
				.setTimeout( lockTimeout )
				.setScope( lockScope );

		final NavigablePath rootPath = selectByIdQuery.sqlAst()
				.getQuerySpec()
				.getFromClause()
				.getRoots()
				.get( 0 )
				.getNavigablePath();

		final SqlAstTranslatorFactory translatorFactory = sessionFactory.getJdbcServices().getDialect().getSqlAstTranslatorFactory();
		final SqlAstTranslator<JdbcOperationQuerySelect> translator = translatorFactory.buildSelectTranslator( sessionFactory, selectByIdQuery.sqlAst() );

		final DatabaseSelect databaseSelect = translator.translateLockingSelect(
				selectByIdQuery.jdbcParameterBindings(),
				queryOptions
		);

		final LoadedValuesCollector loadedValuesCollector = databaseSelect.getLoadedValuesCollector();

		factoryScope.inTransaction( (session) -> {
			final SingleIdExecutionContext executionContext = new SingleIdExecutionContext(
					session,
					null,
					1,
					entityDescriptor,
					queryOptions,
					null
			);

			sqlCollector.clear();
			final List<Team> results = databaseSelect.execute(
					Team.class,
					1,
					StandardStatementCreator.getStatementCreator( ScrollMode.FORWARD_ONLY ),
					selectByIdQuery.jdbcParameterBindings(),
					row -> (Team) row[0],
					ListResultsConsumer.instance( ListResultsConsumer.UniqueSemantic.FILTER ),
					executionContext
			);

			final Team team = results.get( 0 );

			// initial query and the lock of the 2 tables
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 3 );

			assertThat( team.getMembers() ).hasSize( 3 );
			assertThat( loadedValuesCollector.getCollectedRootEntities() ).hasSize( 1 );
			assertThat( loadedValuesCollector.getCollectedNonRootEntities() ).hasSize( 3 );
			assertThat( loadedValuesCollector.getCollectedCollections() ).hasSize( 1 );

			// let's look a little deeper...

			// the root
			final LoadedValuesCollector.LoadedEntityRegistration rootEntityDetails = loadedValuesCollector.getCollectedRootEntities().get( 0 );
			assertThat( session.getPersistenceContext().getEntity( rootEntityDetails.entityKey() ) ).isSameAs( team );
			assertThat( rootEntityDetails.navigablePath() ).isEqualTo( rootPath );

			// the collection
			final NavigablePath expectedMembersPath = rootPath.append( "members" );
			final LoadedValuesCollector.LoadedCollectionRegistration membersDetails = loadedValuesCollector.getCollectedCollections().get( 0 );
			assertThat( membersDetails.navigablePath() ).isEqualTo( expectedMembersPath );
			assertThat( membersDetails.collectionKey().getKey() ).isEqualTo( 1 );
		} );
	}
}
