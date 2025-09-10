/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.op;

import jakarta.persistence.Timeout;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.ScrollMode;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.internal.LockTimeoutHandler;
import org.hibernate.sql.exec.internal.StandardStatementCreator;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.ops.internal.DatabaseSelectImpl;
import org.hibernate.sql.ops.internal.lock.FollowOnLockingAction;
import org.hibernate.sql.ops.internal.LoadedValuesCollectorImpl;
import org.hibernate.sql.ops.internal.SingleRootLoadedValuesCollector;
import org.hibernate.sql.ops.spi.DatabaseSelect;
import org.hibernate.sql.ops.spi.LoadedValuesCollector;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.testing.orm.junit.DomainModel;
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
@SessionFactory
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
	void testFollowOnLockingFlow(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel().findEntityDescriptor( Team.class );

		final Helper.SelectByIdQuery selectByIdQuery = Helper.createSelectByIdQuery(
				entityDescriptor,
				sessionFactory
		);

		final QueryOptionsImpl queryOptions = new QueryOptionsImpl();
		queryOptions.getLockOptions()
				.setLockMode( LockMode.PESSIMISTIC_WRITE )
				.setFollowOnStrategy( Locking.FollowOn.FORCE )
				.setTimeout( Timeout.seconds( 2 ) )
				.setScope( Locking.Scope.INCLUDE_COLLECTIONS );

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
					QueryOptions.NONE,
					null
			);

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

			assertThat( team.getMembers() ).hasSize( 3 );
			assertThat( loadedValuesCollector.getCollectedRootEntities() ).hasSize( 1 );
			assertThat( loadedValuesCollector.getCollectedNonRootEntities() ).hasSize( 3 );
			assertThat( loadedValuesCollector.getCollectedCollections() ).hasSize( 1 );

			// let's look a little deeper...

			// the root
			final LoadedValuesCollectorImpl.LoadedEntityRegistration rootEntityDetails = loadedValuesCollector.getCollectedRootEntities().get( 0 );
			assertThat( session.getPersistenceContext().getEntity( rootEntityDetails.entityKey() ) ).isSameAs( team );
			assertThat( rootEntityDetails.navigablePath() ).isEqualTo( rootPath );

			// the collection
			final NavigablePath expectedMembersPath = rootPath.append( "members" );
			final LoadedValuesCollectorImpl.LoadedCollectionRegistration membersDetails = loadedValuesCollector.getCollectedCollections().get( 0 );
			assertThat( membersDetails.navigablePath() ).isEqualTo( expectedMembersPath );
			assertThat( membersDetails.collectionKey().getKey() ).isEqualTo( 1 );
		} );
	}

	@Test
	void testFollowOnLockingParadigm(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel().findEntityDescriptor( Team.class );

		final Helper.SelectByIdQuery selectByIdQuery = Helper.createSelectByIdQuery(
				entityDescriptor,
				sessionFactory
		);

		final QueryOptionsImpl queryOptions = new QueryOptionsImpl();
		queryOptions.getLockOptions()
				.setLockMode( LockMode.PESSIMISTIC_WRITE )
				.setFollowOnStrategy( Locking.FollowOn.FORCE )
				.setTimeout( Timeout.seconds( 2 ) )
				.setScope( Locking.Scope.INCLUDE_COLLECTIONS );

		final NavigablePath rootPath = selectByIdQuery.sqlAst()
				.getQuerySpec()
				.getFromClause()
				.getRoots()
				.get( 0 )
				.getNavigablePath();
		final var loadedValuesCollector = new SingleRootLoadedValuesCollector( rootPath );

		final DatabaseSelect databaseSelect = DatabaseSelectImpl
				.builder( selectByIdQuery.jdbcOperation() )
				.setLoadedValuesCollector( loadedValuesCollector )
				.build();

		factoryScope.inTransaction( (session) -> {
			final SingleIdExecutionContext executionContext = new SingleIdExecutionContext(
					session,
					null,
					1,
					entityDescriptor,
					QueryOptions.NONE,
					null
			);

			databaseSelect.execute(
					Team.class,
					1,
					StandardStatementCreator.getStatementCreator( ScrollMode.FORWARD_ONLY ),
					selectByIdQuery.jdbcParameterBindings(),
					row -> (Team) row[0],
					ListResultsConsumer.instance( ListResultsConsumer.UniqueSemantic.FILTER ),
					executionContext
			);
		} );
	}

	// todo (JdbcOperation) : for this to work properly, we'd really need a way
	//		to understand the association relationships between entities being loaded.
	//  	+
	//		e.g. `from Team t join fetch t.members` would load both Team and Person
	//		data.  But given the current Callback/AfterLoadAction structure we can't
	//		understand that Team refs we see are the roots and Person refs relate
	//		to Team#members and may need to be separately locked.
	//		+
	//		so here we fake it and base it simply on a few assumptions :
	//			1. there is only one root, which is not necessarily valid for Query
	//			2. loading entities which match the root entity descriptor are considered a root,
	//				which is not valid for self-referential associations e.g.
	//		and a few others - basically this Callback/AfterLoadAction structure really
	//		needs redesigned.

	@Test
	void testFollowOnLockingParadigm2(SessionFactoryScope factoryScope) {
		// same as testFollowOnLockingParadigm, but here playing with collections
		// and "secondary tables" - i.e., JPA extended locking scope

		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel().findEntityDescriptor( Team.class );

		final Helper.SelectByIdQuery selectByIdQuery = Helper.createSelectByIdQuery(
				entityDescriptor,
				sessionFactory
		);
		final JdbcOperationQuerySelect jdbcOperation = selectByIdQuery.jdbcOperation();
		final JdbcParameterBindings jdbcParameterBindings = selectByIdQuery.jdbcParameterBindings();

		final NavigablePath rootPath = selectByIdQuery.sqlAst()
				.getQuerySpec()
				.getFromClause()
				.getRoots()
				.get( 0 )
				.getNavigablePath();
		final var loadedValuesCollector = new SingleRootLoadedValuesCollector( rootPath );

		final Timeout lockTimeout = Timeout.seconds( 2 );
		factoryScope.inTransaction( (session) -> {

			final QueryOptionsImpl queryOptions = new QueryOptionsImpl();
			queryOptions.getLockOptions()
					.setLockMode( LockMode.PESSIMISTIC_WRITE )
					.setFollowOnStrategy( Locking.FollowOn.FORCE )
					.setTimeout( lockTimeout )
					.setScope( Locking.Scope.INCLUDE_COLLECTIONS );

			final SingleIdExecutionContext executionContext = new SingleIdExecutionContext(
					session,
					null,
					1,
					entityDescriptor,
					QueryOptions.NONE,
					null
			);


			final DatabaseSelectImpl.Builder operationBuilder = DatabaseSelectImpl.builder( jdbcOperation )
					.setLoadedValuesCollector( loadedValuesCollector );

			final ConnectionLockTimeoutStrategy lockTimeoutStrategy = session
					.getDialect()
					.getLockingSupport()
					.getConnectionLockTimeoutStrategy();
			if ( lockTimeoutStrategy.getSupportedLevel() != ConnectionLockTimeoutStrategy.Level.NONE ) {
				final LockTimeoutHandler lockTimeoutHandler = new LockTimeoutHandler( lockTimeout, lockTimeoutStrategy );
				operationBuilder.addSecondaryActionPair( lockTimeoutHandler, lockTimeoutHandler );
			}

			final DatabaseSelectImpl databaseOperation = operationBuilder.build();
			final List<Team> results = databaseOperation.execute(
					Team.class,
					1,
					StandardStatementCreator.getStatementCreator( ScrollMode.FORWARD_ONLY ),
					jdbcParameterBindings,
					row -> (Team) row[0],
					ListResultsConsumer.instance( ListResultsConsumer.UniqueSemantic.FILTER ),
					executionContext
			);

			// for visual confirmation...
			FollowOnLockingAction.logLoadedValues( loadedValuesCollector );

			final Team team = results.get( 0 );

			assertThat( team.getMembers() ).hasSize( 3 );
			assertThat( loadedValuesCollector.getCollectedRootEntities() ).hasSize( 1 );
			assertThat( loadedValuesCollector.getCollectedNonRootEntities() ).hasSize( 3 );
			assertThat( loadedValuesCollector.getCollectedCollections() ).hasSize( 1 );

			// let's look a little deeper...

			// the root
			final LoadedValuesCollectorImpl.LoadedEntityRegistration rootEntityDetails = loadedValuesCollector.getCollectedRootEntities().get( 0 );
			assertThat( session.getPersistenceContext().getEntity( rootEntityDetails.entityKey() ) ).isSameAs( team );
			assertThat( rootEntityDetails.navigablePath() ).isEqualTo( rootPath );

			// the collection
			final NavigablePath expectedMembersPath = rootPath.append( "members" );
			final LoadedValuesCollectorImpl.LoadedCollectionRegistration membersDetails = loadedValuesCollector.getCollectedCollections().get( 0 );
			assertThat( membersDetails.navigablePath() ).isEqualTo( expectedMembersPath );
			assertThat( membersDetails.collectionKey().getKey() ).isEqualTo( 1 );
		} );
	}
}
