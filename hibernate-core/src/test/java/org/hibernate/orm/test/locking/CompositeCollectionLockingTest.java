package org.hibernate.orm.test.locking;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.Timeouts;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.lock.internal.LockingSqlRewriterSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.SelectionQuery;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.lock.LockingHelper;

import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProviderSettingProvider;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static jakarta.persistence.PessimisticLockScope.EXTENDED;
import static jakarta.persistence.PessimisticLockScope.FETCHED;
import static jakarta.persistence.PessimisticLockScope.NORMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.LockMode.PESSIMISTIC_FORCE_INCREMENT;
import static org.hibernate.LockMode.PESSIMISTIC_READ;
import static org.hibernate.LockMode.PESSIMISTIC_WRITE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/// Exercises complete collection keys through both acquisition overloads and
/// direct/follow-on entity locking. Physical probes use READ_COMMITTED.
///
/// @author Steve Ebersole
@DomainModel(overrideCacheStrategy = false, annotatedClasses = {
		CompositeCollectionLockingTest.Owner.class,
		CompositeCollectionLockingTest.Parent.class,
		CompositeCollectionLockingTest.AssociationOwner.class,
		CompositeCollectionLockingTest.SingleOwner.class
})
@SessionFactory(useCollectingStatementObserver = true)
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.connection.isolation", value = "2"),
		@Setting(name = "hibernate.use_sql_comments", value = "false")
}, settingProviders = @SettingProvider(
		settingName = "hibernate.connection.provider_class",
		provider = PreparedStatementSpyConnectionProviderSettingProvider.class))
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSelectLocking.class)
@ExtendWith(FollowOnLockingTestSupport.ReadCommittedEligibility.class)
@Tag("db-locking")
class CompositeCollectionLockingTest {
	static final List<Key> SELECTED = List.of( new Key( 1, 10 ), new Key( 1, 20 ), new Key( 2, 30 ) );
	static final List<Key> CONTROLS = List.of( new Key( 1, 30 ), new Key( 2, 10 ), new Key( 2, 20 ) );

	@BeforeEach
	void createData(SessionFactoryScope scope) {
		// Only the contention tests require an explicit update-lock-read guarantee.
		// Mapping, binding and state checks also run on HSQLDB.
		scope.inTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			Stream.concat( SELECTED.stream(), CONTROLS.stream() ).forEach( id -> {
				final var owner = new Owner( id );
				owner.chosen = SELECTED.contains( id );
				owner.tags.addAll( Set.of( "first", "second" ) );
				owner.labels.addAll( Set.of( "first", "second" ) );
				session.persist( owner );
			} );
			session.persist( new Owner( new Key( 3, 40 ) ) );
			for ( int code = 1; code <= 2; code++ ) {
				final var owner = new SingleOwner();
				owner.id = new SingleKey( code );
				owner.tags.add( "tag" );
				session.persist( owner );
			}
			final var parent = new Parent();
			parent.id = 1;
			session.persist( parent );
			for ( int slot = 1; slot <= 2; slot++ ) {
				final var owner = new AssociationOwner();
				owner.id = new AssociationKey( parent, slot );
				owner.tags.add( "tag" );
				session.persist( owner );
			}
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	enum Route {
		DIRECT_NORMAL( false, NORMAL ), DIRECT_EXTENDED( false, EXTENDED ),
		FOLLOW_ON_NORMAL( true, NORMAL ), FOLLOW_ON_EXTENDED( true, EXTENDED ), FOLLOW_ON_FETCHED( true, FETCHED );

		final boolean followOn;
		final PessimisticLockScope scope;
		Route(boolean followOn, PessimisticLockScope scope) { this.followOn = followOn; this.scope = scope; }
	}

	static Stream<Arguments> routesAndModes() {
		return Stream.of( Route.values() ).flatMap( route -> Stream.of( PESSIMISTIC_READ, PESSIMISTIC_WRITE, PESSIMISTIC_FORCE_INCREMENT )
				.flatMap( mode -> Stream.of( false, true ).map( readOnly -> arguments( route, mode, readOnly ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("routesAndModes")
	void acquiresCollectionsWithoutChangingState(Route route, LockMode mode, boolean readOnly, SessionFactoryScope scope) {
		requireRoute( route, scope );
		scope.inTransaction( session -> {
			session.setDefaultReadOnly( readOnly );
			scope.getCollectingStatementObserver().clear();
			final var owners = query( session, route, mode ).getResultList();
			assertThat( owners ).hasSize( 3 );
			owners.forEach( owner -> {
				assertThat( Hibernate.isInitialized( owner.tags ) ).isEqualTo( route.scope == FETCHED );
				assertThat( Hibernate.isInitialized( owner.labels ) ).isFalse();
				assertThat( session.getCurrentLockMode( owner ) ).isEqualTo( mode );
				assertThat( entry( session, owner ).isReadOnly() ).isEqualTo( readOnly );
				assertThat( owner.version ).isEqualTo( mode == PESSIMISTIC_FORCE_INCREMENT ? 1 : 0 );
				assertThat( entry( session, owner ).getVersion() ).isEqualTo( owner.version );
				if ( readOnly ) {
					assertThat( entry( session, owner ).getLoadedState() ).isNull();
				}
			} );
			final var sql = scope.getCollectingStatementObserver().getSqlQueries();
			assertThat( sql.stream().filter( CompositeCollectionLockingTest::collectionSelect ).count() )
					.isEqualTo( route.scope == EXTENDED ? 2 : route.scope == FETCHED ? 1 : 0 );
			assertThat( sql.stream().filter( statement -> statement.startsWith( "update cc_owner " ) ).count() )
					.isEqualTo( mode == PESSIMISTIC_FORCE_INCREMENT ? 3 : 0 );
			if ( mode == PESSIMISTIC_FORCE_INCREMENT && route.followOn && route.scope != NORMAL ) {
				final int firstUpdate = sql.indexOf( sql.stream().filter( statement -> statement.startsWith( "update cc_owner " ) ).findFirst().orElseThrow() );
				assertThat( sql.subList( firstUpdate, sql.size() ) ).noneMatch( CompositeCollectionLockingTest::collectionSelect );
			}
		} );
	}

	@ParameterizedTest
	@EnumSource(Route.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
	void protectsCompleteKeysAndOnlyTheRequestedScope(Route route, SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		requireRoute( route, scope );
		scope.inTransaction( session -> {
			assertThat( query( session, route, PESSIMISTIC_WRITE ).getResultList() ).hasSize( 3 );
			for ( var key : SELECTED ) {
				assertProtection( scope, false, key, route.scope != NORMAL );
				assertProtection( scope, true, key, route.scope == EXTENDED );
			}
			for ( var key : CONTROLS ) {
				assertProtection( scope, false, key, false );
				assertProtection( scope, true, key, false );
			}
		} );
	}

	@ParameterizedTest
	@CsvSource({ "false,false,0", "false,false,1", "false,false,2", "false,false,3",
			"false,true,0", "false,true,1", "false,true,2", "false,true,3",
			"true,false,0", "true,false,1", "true,false,2", "true,false,3",
			"true,true,0", "true,true,1", "true,true,2", "true,true,3" })
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
	void helperOverloadsProtectEmptySingleAndDuplicateKeys(boolean collected, boolean propertyRef, int count, SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		checkHelperOverloads( collected, propertyRef, count, true, scope );
	}

	@ParameterizedTest
	@CsvSource({ "false,false,0", "false,false,1", "false,false,2", "false,false,3",
			"false,true,0", "false,true,1", "false,true,2", "false,true,3",
			"true,false,0", "true,false,1", "true,false,2", "true,false,3",
			"true,true,0", "true,true,1", "true,true,2", "true,true,3" })
	void helperOverloadsHandleEmptySingleAndDuplicateKeys(boolean collected, boolean propertyRef, int count, SessionFactoryScope scope) {
		checkHelperOverloads( collected, propertyRef, count, false, scope );
	}

	private void checkHelperOverloads(boolean collected, boolean propertyRef, int count, boolean checkProtection, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owners = session.createQuery( "from CollectionKeyOwner o where o.chosen = true order by o.id.tenant, o.id.local", Owner.class ).getResultList();
			final var selected = new ArrayList<>( owners.subList( 0, Math.min( count, 2 ) ) );
			if ( count == 3 ) {
				selected.add( selected.get( 0 ) );
			}
			connectionProvider( scope ).clear();
			lock( session, selected, propertyRef, collected );
			final var keys = selected.stream().map( owner -> owner.id );
			assertCollectionBindings( session, scope, propertyRef, (collected ? keys : keys.distinct()).toList() );
			for ( int i = 0; i < owners.size(); i++ ) {
				assertThat( Hibernate.isInitialized( owners.get( i ).tags ) ).isFalse();
				if ( checkProtection ) {
					assertProtection( scope, propertyRef, owners.get( i ).id, i < Math.min( count, 2 ) );
				}
			}
		} );
	}

	@ParameterizedTest
	@CsvSource({ "false,false,false", "false,false,true", "false,true,false", "false,true,true",
			"true,false,false", "true,false,true", "true,true,false", "true,true,true" })
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
	void propertyRefProtectsPersistedKeyDespiteLocalCollectionChanges(boolean readOnly, boolean clear, boolean initialized, SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		checkPersistedPropertyRef( readOnly, clear, initialized, true, scope );
	}

	@ParameterizedTest
	@CsvSource({ "false,false,false", "false,false,true", "false,true,false", "false,true,true",
			"true,false,false", "true,false,true", "true,true,false", "true,true,true" })
	void propertyRefUsesPersistedKeyDespiteLocalCollectionChanges(boolean readOnly, boolean clear, boolean initialized, SessionFactoryScope scope) {
		checkPersistedPropertyRef( readOnly, clear, initialized, false, scope );
	}

	private void checkPersistedPropertyRef(boolean readOnly, boolean clear, boolean initialized, boolean checkProtection, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setHibernateFlushMode( FlushMode.MANUAL );
			final var owner = session.find( Owner.class, SELECTED.get( 0 ) );
			final var original = owner.labels;
			if ( initialized ) {
				Hibernate.initialize( original );
			}
			session.setReadOnly( owner, readOnly );
			final var snapshot = entry( session, owner ).getLoadedState();
			final var replacement = clear ? null : new HashSet<>( Set.of( "local" ) );
			owner.labels = replacement;
			owner.business = new BusinessKey( -1, -2 );
			connectionProvider( scope ).clear();
			lock( session, List.of( owner ), true, false );
			assertCollectionBindings( session, scope, true, List.of( owner.id ) );
			assertThat( (Object) owner.labels ).isSameAs( replacement );
			assertThat( Hibernate.isInitialized( original ) ).isEqualTo( initialized );
			assertThat( entry( session, owner ).getLoadedState() ).isSameAs( snapshot );
			assertThat( owner.business.region ).isEqualTo( -1 );
			assertThat( owner.version ).isZero();
			if ( checkProtection ) {
				assertProtection( scope, true, owner.id, true );
			}
		} );
	}

	@ParameterizedTest
	@EnumSource(value = Route.class, names = { "DIRECT_EXTENDED", "FOLLOW_ON_EXTENDED", "FOLLOW_ON_FETCHED" })
	void doesNotInitializeIdentifierAssociationsDuringTheWholePhase(Route route, SessionFactoryScope scope) {
		requireRoute( route, scope );
		final String hql = "from CollectionAssociationOwner o" + (route.scope == FETCHED ? " join fetch o.tags" : "");
		final var baseline = scope.fromTransaction( session -> session.createQuery( hql, AssociationOwner.class ).getResultList().stream()
				.map( owner -> Hibernate.isInitialized( owner.id.parent ) ).toList() );
		assertThat( baseline ).containsOnly( false );
		scope.inTransaction( session -> {
			scope.getCollectingStatementObserver().clear();
			final var query = session.createQuery( hql, AssociationOwner.class ).setHibernateLockMode( PESSIMISTIC_WRITE )
					.setLockScope( route.scope ).setFollowOnStrategy( route.followOn ? Locking.FollowOn.FORCE : Locking.FollowOn.DISALLOW );
			if ( route.followOn ) {
				FollowOnLockingTestSupport.assertFollowOnPlan( session, query );
			}
			final var owners = query.getResultList();
			assertThat( owners ).hasSize( 2 );
			assertThat( owners.stream().map( owner -> Hibernate.isInitialized( owner.id.parent ) ).toList() ).isEqualTo( baseline );
			assertThat( scope.getCollectingStatementObserver().getSqlQueries() ).noneMatch( sql -> sql.contains( "cc_parent" ) );
			assertThat( owners ).allSatisfy( owner -> assertThat( Hibernate.isInitialized( owner.tags ) ).isEqualTo( route.scope == FETCHED ) );
		} );
	}

	@ParameterizedTest
	@CsvSource({ "false", "true" })
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
	void fetchedPropertyRefCollectionsProtectTheirOwnKeys(boolean readOnly, SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		checkFetchedPropertyRef( readOnly, true, scope );
	}

	@ParameterizedTest
	@CsvSource({ "false", "true" })
	void fetchedPropertyRefCollectionsUseTheirOwnKeys(boolean readOnly, SessionFactoryScope scope) {
		checkFetchedPropertyRef( readOnly, false, scope );
	}

	private void checkFetchedPropertyRef(boolean readOnly, boolean checkProtection, SessionFactoryScope scope) {
		requireRoute( Route.FOLLOW_ON_FETCHED, scope );
		scope.inTransaction( session -> {
			session.setDefaultReadOnly( readOnly );
			final var query = session.createQuery( "from CollectionKeyOwner o join fetch o.labels where o.chosen = true", Owner.class )
					.setHibernateLockMode( PESSIMISTIC_WRITE ).setLockScope( FETCHED ).setFollowOnStrategy( Locking.FollowOn.FORCE );
			FollowOnLockingTestSupport.assertFollowOnPlan( session, query );
			connectionProvider( scope ).clear();
			final var owners = query.getResultList();
			assertThat( owners ).hasSize( 3 );
			assertCollectionBindings( session, scope, true, SELECTED );
			owners.forEach( owner -> {
				assertThat( Hibernate.isInitialized( owner.labels ) ).isTrue();
				assertThat( Hibernate.isInitialized( owner.tags ) ).isFalse();
				if ( checkProtection ) {
					assertProtection( scope, true, owner.id, true );
					assertProtection( scope, false, owner.id, false );
				}
			} );
			if ( checkProtection ) {
				CONTROLS.forEach( id -> assertProtection( scope, true, id, false ) );
			}
		} );
	}

	@ParameterizedTest
	@EnumSource(value = Route.class, names = { "DIRECT_EXTENDED", "FOLLOW_ON_EXTENDED", "FOLLOW_ON_FETCHED" })
	void oneColumnCompositeKeysKeepScalarPredicates(Route route, SessionFactoryScope scope) {
		requireRoute( route, scope );
		scope.inTransaction( session -> {
			scope.getCollectingStatementObserver().clear();
			final String hql = "from CollectionSingleOwner o" + (route.scope == FETCHED ? " join fetch o.tags" : "");
			final var query = session.createQuery( hql, SingleOwner.class ).setHibernateLockMode( PESSIMISTIC_WRITE )
					.setLockScope( route.scope ).setFollowOnStrategy( route.followOn ? Locking.FollowOn.FORCE : Locking.FollowOn.DISALLOW );
			if ( route.followOn ) {
				FollowOnLockingTestSupport.assertFollowOnPlan( session, query );
			}
			final var owners = query.getResultList();
			assertThat( owners ).hasSize( 2 ).allSatisfy( owner ->
					assertThat( Hibernate.isInitialized( owner.tags ) ).isEqualTo( route.scope == FETCHED ) );
			assertThat( scope.getCollectingStatementObserver().getSqlQueries().stream()
					.filter( sql -> sql.contains( " from cc_single_tags tbl" ) ).toList() )
					.hasSize( 1 ).allSatisfy( sql -> assertThat( sql ).contains( "tbl.code_fk in (" ).doesNotContain( " or " ) );
		} );
	}

	@ParameterizedTest
	@EnumSource(value = Route.class, names = { "DIRECT_EXTENDED", "FOLLOW_ON_EXTENDED", "FOLLOW_ON_FETCHED" })
	void emptyCollectionsCompleteSuccessfully(Route route, SessionFactoryScope scope) {
		requireRoute( route, scope );
		scope.inTransaction( session -> {
			final String hql = "from CollectionKeyOwner o" + (route.scope == FETCHED ? " left join fetch o.tags" : "") + " where o.id.tenant = 3";
			final var query = session.createQuery( hql, Owner.class ).setHibernateLockMode( PESSIMISTIC_WRITE )
					.setLockScope( route.scope ).setFollowOnStrategy( route.followOn ? Locking.FollowOn.FORCE : Locking.FollowOn.DISALLOW );
			assertThat( query.getResultList() ).hasSize( 1 );
		} );
	}

	static SelectionQuery<Owner> query(SessionImplementor session, Route route, LockMode mode) {
		assumeFalse( !route.followOn && session.getDialect() instanceof InformixDialect,
				"Informix does not support direct locking with ORDER BY" );
		final String hql = "select o from CollectionKeyOwner o" + (route.scope == FETCHED ? " join fetch o.tags" : "")
				+ " where o.chosen = true order by o.id.tenant, o.id.local";
		final var query = session.createQuery( hql, Owner.class ).setHibernateLockMode( mode ).setLockScope( route.scope )
				.setFollowOnStrategy( route.followOn ? Locking.FollowOn.FORCE : Locking.FollowOn.DISALLOW );
		if ( route.followOn ) {
			FollowOnLockingTestSupport.assertFollowOnPlan( session, query );
		}
		return query;
	}

	private static void requireRoute(Route route, SessionFactoryScope scope) {
		if ( route.followOn ) {
			final var dialect = scope.getSessionFactory().getJdbcServices().getDialect();
			assumeFalse( dialect instanceof SQLServerDialect || dialect instanceof SybaseDialect,
					"Dialect deliberately chooses direct entity locking even with FORCE" );
		}
	}

	static PluralAttributeMapping attribute(SessionImplementor session, boolean propertyRef) {
		return (PluralAttributeMapping) session.getFactory().getMappingMetamodel().getEntityDescriptor( Owner.class )
				.findAttributeMapping( propertyRef ? "labels" : "tags" );
	}

	static void lock(SessionImplementor session, List<Owner> owners, boolean propertyRef, boolean collected) {
		final var attribute = attribute( session, propertyRef );
		final var context = new BaseExecutionContext( session );
		if ( collected ) {
			final var keys = owners.stream().map( owner -> new CollectionKey( attribute.getCollectionDescriptor(),
					LockingHelper.resolveCollectionKey( attribute, owner, session ) ) ).toList();
			LockingHelper.lockCollectionTable( attribute, PESSIMISTIC_WRITE, Timeouts.WAIT_FOREVER, keys, context );
		}
		else {
			final var persister = session.getFactory().getMappingMetamodel().getEntityDescriptor( Owner.class );
			final var keys = owners.stream().map( owner -> session.generateEntityKey( owner.id, persister ) ).toList();
			LockingHelper.lockCollectionTable( attribute, PESSIMISTIC_WRITE, Timeouts.WAIT_FOREVER,
					LockingHelper.resolveEntityKeys( keys, context ), context );
		}
	}

	static boolean collectionSelect(String sql) {
		return sql.startsWith( "select " ) && (sql.contains( " from cc_tags tbl" ) || sql.contains( " from cc_labels tbl" ));
	}

	private static EntityEntry entry(SessionImplementor session, Owner owner) {
		return session.getPersistenceContextInternal().getEntry( owner );
	}

	private static PreparedStatementSpyConnectionProvider connectionProvider(SessionFactoryScope scope) {
		return (PreparedStatementSpyConnectionProvider) scope.getSessionFactory().getServiceRegistry()
				.getService( ConnectionProvider.class );
	}

	private static void assertCollectionBindings(
			SessionImplementor session, SessionFactoryScope scope, boolean propertyRef, List<Key> keys) {
		final var provider = connectionProvider( scope );
		final var statements = provider.getPreparedStatementsAndSql().entrySet().stream()
				.filter( entry -> collectionSelect( entry.getValue() ) ).toList();
		assertThat( statements ).hasSize( 1 );
		final var statement = statements.get( 0 );
		assertThat( statement.getValue() ).contains( propertyRef ? " from cc_labels tbl" : " from cc_tags tbl" );
		final var columns = new ArrayList<String>();
		attribute( session, propertyRef ).getKeyDescriptor()
				.forEachSelectable( (i, selectable) -> columns.add( selectable.getSelectionExpression() ) );
		final var expected = keys.stream().map( key -> columns.stream().map( column -> switch ( column ) {
			case "local_fk" -> key.local;
			case "tenant_fk" -> key.tenant;
			case "business_number_fk" -> key.tenant + 200;
			case "business_region_fk" -> key.local + 100;
			default -> throw new AssertionError( column );
		} ).toList() ).toList();
		try {
			final var calls = provider.spyContext.getCalls(
					PreparedStatement.class.getMethod( "setInt", int.class, int.class ), statement.getKey() ).stream()
					.sorted( Comparator.comparingInt( args -> (int) args[0] ) ).toList();
			assertThat( calls ).hasSize( keys.size() * columns.size() );
			final var actual = new ArrayList<List<Integer>>();
			for ( int i = 0; i < calls.size(); i += columns.size() ) {
				final var tuple = new ArrayList<Integer>();
				for ( int j = 0; j < columns.size(); j++ ) {
					assertThat( calls.get( i + j )[0] ).isEqualTo( i + j + 1 );
					tuple.add( (Integer) calls.get( i + j )[1] );
				}
				actual.add( tuple );
			}
			assertThat( actual ).containsExactlyInAnyOrderElementsOf( expected );
		}
		catch (NoSuchMethodException e) {
			throw new AssertionError( e );
		}
		if ( keys.isEmpty() ) {
			assertThat( statement.getValue() ).contains( "1=0" );
		}
		else if ( session.getDialect().getRowValueSupport() == RowValueSupport.NONE ) {
			assertThat( statement.getValue() ).doesNotContain( " in (" );
			if ( keys.size() > 1 ) {
				assertThat( statement.getValue() ).contains( " or " );
			}
		}
	}

	private static void assertProtection(SessionFactoryScope scope, boolean propertyRef, Key id, boolean locked) {
		final var dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		if ( dialect instanceof SQLServerDialect || dialect instanceof H2Dialect ) {
			assertUpdateProtection( scope, propertyRef, id, locked );
			return;
		}
		final int count = scope.fromTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			final String table = propertyRef ? "cc_labels" : "cc_tags";
			final String[] columns = propertyRef ? new String[] { "business_number_fk", "business_region_fk" } : new String[] { "local_fk", "tenant_fk" };
			final int first = propertyRef ? id.tenant + 200 : id.local;
			final int second = propertyRef ? id.local + 100 : id.tenant;
			final var sql = LockingSqlRewriterSupport.rewrite( session.getDialect().getLockingSupport(),
					"select 1 from " + table + " t where " + columns[0] + " = " + first + " and " + columns[1] + " = " + second,
					new LockOptions( PESSIMISTIC_WRITE ).setTimeout( Timeouts.SKIP_LOCKED ), Map.of( "t", columns ) ).sql();
			return session.createNativeQuery( sql, Integer.class ).getResultList().size();
		} );
		assertThat( count ).as( "%s collection for (%s,%s)", propertyRef ? "property-ref" : "identifier", id.tenant, id.local )
				.isEqualTo( locked ? 0 : 2 );
	}

	/// SQL Server's READPAST can skip a conflicting page or index lock even for an unrelated
	/// collection. An actual UPDATE must instead succeed or report a native lock
	/// timeout for each existing element while the acquiring transaction is open.
	/// H2 also exercises this probe in the default test run.
	private static void assertUpdateProtection(SessionFactoryScope scope, boolean propertyRef, Key id, boolean locked) {
		final String table = propertyRef ? "cc_labels" : "cc_tags";
		final String hint = scope.getSessionFactory().getJdbcServices().getDialect() instanceof SQLServerDialect
				? " with (rowlock)" : "";
		final String column = propertyRef ? "labels" : "tags";
		final String firstColumn = propertyRef ? "business_number_fk" : "local_fk";
		final String secondColumn = propertyRef ? "business_region_fk" : "tenant_fk";
		for ( String element : List.of( "first", "second" ) ) {
			scope.inSession( session -> {
				final var transaction = session.beginTransaction();
				try {
					FollowOnLockingTestSupport.assertReadCommitted( session );
					session.doWork( connection -> {
						try ( var lockWait = ConcurrencyTestSupport.boundedLockWait( connection, session.getFactory() ) ) {
							try ( var update = connection.prepareStatement( "update " + table + hint + " set "
									+ column + "=? where " + firstColumn + "=? and " + secondColumn + "=? and " + column + "=?" ) ) {
								update.setQueryTimeout( 10 );
								update.setString( 1, "contender" );
								update.setInt( 2, propertyRef ? id.tenant + 200 : id.local );
								update.setInt( 3, propertyRef ? id.local + 100 : id.tenant );
								update.setString( 4, element );
								if ( locked ) {
									ConcurrencyTestSupport.assertLockConflict(
											assertThrows( SQLException.class, update::executeUpdate ), session.getFactory() );
								}
								else {
									assertThat( update.executeUpdate() ).isOne();
								}
							}
							finally {
								connection.rollback();
							}
						}
					} );
				}
				finally {
					transaction.rollback();
				}
			} );
		}
	}

	@Embeddable
	static class Key implements Serializable {
		@Column(name = "owner_tenant") int tenant;
		@Column(name = "owner_local") int local;
		Key() {}
		Key(int tenant, int local) { this.tenant = tenant; this.local = local; }
		@Override public boolean equals(Object other) { return other instanceof Key key && tenant == key.tenant && local == key.local; }
		@Override public int hashCode() { return Objects.hash( tenant, local ); }
	}

	@Embeddable
	static class BusinessKey implements Serializable {
		@Column(name = "code_region") int region;
		@Column(name = "code_number") int number;
		BusinessKey() {}
		BusinessKey(int region, int number) { this.region = region; this.number = number; }
		@Override public boolean equals(Object other) { return other instanceof BusinessKey key && region == key.region && number == key.number; }
		@Override public int hashCode() { return Objects.hash( region, number ); }
	}

	@Entity(name = "CollectionKeyOwner")
	@Table(name = "cc_owner", uniqueConstraints = @UniqueConstraint(columnNames = { "code_region", "code_number" }))
	static class Owner {
		@EmbeddedId Key id;
		@Embedded BusinessKey business;
		boolean chosen;
		@Version int version;
		@ElementCollection
		@CollectionTable(name = "cc_tags", joinColumns = {
				@JoinColumn(name = "local_fk", referencedColumnName = "owner_local"),
				@JoinColumn(name = "tenant_fk", referencedColumnName = "owner_tenant") })
		Set<String> tags = new HashSet<>();
		@ElementCollection
		@CollectionTable(name = "cc_labels", joinColumns = {
				@JoinColumn(name = "business_number_fk", referencedColumnName = "code_number"),
				@JoinColumn(name = "business_region_fk", referencedColumnName = "code_region") })
		Set<String> labels = new HashSet<>();
		Owner() {}
		Owner(Key id) { this.id = id; this.business = new BusinessKey( id.local + 100, id.tenant + 200 ); }
	}

	@Embeddable
	static class SingleKey implements Serializable {
		int code;
		SingleKey() {}
		SingleKey(int code) { this.code = code; }
		@Override public boolean equals(Object other) { return other instanceof SingleKey key && code == key.code; }
		@Override public int hashCode() { return code; }
	}

	@Entity(name = "CollectionSingleOwner")
	@Table(name = "cc_single_owner")
	static class SingleOwner {
		@EmbeddedId SingleKey id;
		@ElementCollection
		@CollectionTable(name = "cc_single_tags", joinColumns = @JoinColumn(name = "code_fk"))
		Set<String> tags = new HashSet<>();
	}

	@Entity(name = "CollectionKeyParent")
	@Table(name = "cc_parent")
	static class Parent {
		@Id int id;
		public int getId() { return id; }
		@Override public boolean equals(Object other) { return other instanceof Parent parent && getId() == parent.getId(); }
		@Override public int hashCode() { return 31; }
	}

	@Embeddable
	static class AssociationKey implements Serializable {
		@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_key") Parent parent;
		int slot;
		AssociationKey() {}
		AssociationKey(Parent parent, int slot) { this.parent = parent; this.slot = slot; }
		@Override public boolean equals(Object other) { return other instanceof AssociationKey key && slot == key.slot && Objects.equals( parent, key.parent ); }
		@Override public int hashCode() { return Objects.hash( parent, slot ); }
	}

	@Entity(name = "CollectionAssociationOwner")
	@Table(name = "cc_association")
	static class AssociationOwner {
		@EmbeddedId AssociationKey id;
		@ElementCollection
		@CollectionTable(name = "cc_association_tags", joinColumns = {
				@JoinColumn(name = "slot_fk", referencedColumnName = "slot"),
				@JoinColumn(name = "parent_fk", referencedColumnName = "parent_key") })
		Set<String> tags = new HashSet<>();
	}
}
