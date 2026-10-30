package org.hibernate.orm.test.locking;

import java.io.Serializable;
import java.util.ArrayList;
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
import org.hibernate.Timeouts;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.lock.internal.LockingSqlRewriterSupport;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.SelectionQuery;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.lock.LockingHelper;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;

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
})
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSelectLocking.class)
@ExtendWith(FollowOnLockingTestSupport.ReadCommittedEligibility.class)
@Tag("db-locking")
class CompositeCollectionLockingTest {
	static final List<Key> SELECTED = List.of( new Key( 1, 10 ), new Key( 1, 20 ), new Key( 2, 30 ) );
	static final List<Key> CONTROLS = List.of( new Key( 1, 30 ), new Key( 2, 10 ), new Key( 2, 20 ) );

	@BeforeEach
	void createData(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
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
	@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "READPAST does not reliably skip all locked rows for these probes")
	void protectsCompleteKeysAndOnlyTheRequestedScope(Route route, SessionFactoryScope scope) {
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
	@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "READPAST does not reliably skip all locked rows for these probes")
	void helperOverloadsHandleEmptySingleAndDuplicateKeys(boolean collected, boolean propertyRef, int count, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owners = session.createQuery( "from CollectionKeyOwner o where o.chosen = true order by o.id.tenant, o.id.local", Owner.class ).getResultList();
			final var selected = new ArrayList<>( owners.subList( 0, Math.min( count, 2 ) ) );
			if ( count == 3 ) {
				selected.add( selected.get( 0 ) );
			}
			lock( session, selected, propertyRef, collected );
			for ( int i = 0; i < owners.size(); i++ ) {
				assertThat( Hibernate.isInitialized( owners.get( i ).tags ) ).isFalse();
				assertProtection( scope, propertyRef, owners.get( i ).id, i < Math.min( count, 2 ) );
			}
		} );
	}

	@ParameterizedTest
	@CsvSource({ "false,false,false", "false,false,true", "false,true,false", "false,true,true",
			"true,false,false", "true,false,true", "true,true,false", "true,true,true" })
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
	@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "READPAST does not reliably skip all locked rows for these probes")
	void propertyRefUsesPersistedKeyDespiteLocalCollectionChanges(boolean readOnly, boolean clear, boolean initialized, SessionFactoryScope scope) {
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
			lock( session, List.of( owner ), true, false );
			assertThat( (Object) owner.labels ).isSameAs( replacement );
			assertThat( Hibernate.isInitialized( original ) ).isEqualTo( initialized );
			assertThat( entry( session, owner ).getLoadedState() ).isSameAs( snapshot );
			assertThat( owner.business.region ).isEqualTo( -1 );
			assertThat( owner.version ).isZero();
			assertProtection( scope, true, owner.id, true );
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
	@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "READPAST does not reliably skip all locked rows for these probes")
	void fetchedPropertyRefCollectionsUseTheirOwnKeys(boolean readOnly, SessionFactoryScope scope) {
		requireRoute( Route.FOLLOW_ON_FETCHED, scope );
		scope.inTransaction( session -> {
			session.setDefaultReadOnly( readOnly );
			final var query = session.createQuery( "from CollectionKeyOwner o join fetch o.labels where o.chosen = true", Owner.class )
					.setHibernateLockMode( PESSIMISTIC_WRITE ).setLockScope( FETCHED ).setFollowOnStrategy( Locking.FollowOn.FORCE );
			FollowOnLockingTestSupport.assertFollowOnPlan( session, query );
			final var owners = query.getResultList();
			assertThat( owners ).hasSize( 3 );
			owners.forEach( owner -> {
				assertThat( Hibernate.isInitialized( owner.labels ) ).isTrue();
				assertThat( Hibernate.isInitialized( owner.tags ) ).isFalse();
				assertProtection( scope, true, owner.id, true );
				assertProtection( scope, false, owner.id, false );
			} );
			CONTROLS.forEach( id -> assertProtection( scope, true, id, false ) );
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

	private static void assertProtection(SessionFactoryScope scope, boolean propertyRef, Key id, boolean locked) {
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
