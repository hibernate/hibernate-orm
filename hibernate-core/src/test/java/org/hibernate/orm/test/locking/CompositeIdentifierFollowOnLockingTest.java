package org.hibernate.orm.test.locking;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
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
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.StaleObjectStateException;
import org.hibernate.annotations.SecondaryRow;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.SelectionQuery;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static jakarta.persistence.PessimisticLockScope.EXTENDED;
import static jakarta.persistence.PessimisticLockScope.NORMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.hibernate.LockMode.PESSIMISTIC_FORCE_INCREMENT;
import static org.hibernate.LockMode.PESSIMISTIC_READ;
import static org.hibernate.LockMode.PESSIMISTIC_WRITE;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/// Verifies complete-key follow-on acquisition, state adoption and required-row
/// validation across composite identifier representations at READ_COMMITTED.
///
/// @author Steve Ebersole
@DomainModel(overrideCacheStrategy = false, annotatedClasses = {
		CompositeIdentifierFollowOnLockingTest.EmbeddedItem.class,
		CompositeIdentifierFollowOnLockingTest.SingleItem.class,
		CompositeIdentifierFollowOnLockingTest.NestedItem.class,
		CompositeIdentifierFollowOnLockingTest.IdClassItem.class,
		CompositeIdentifierFollowOnLockingTest.AssociationItem.class,
		CompositeIdentifierFollowOnLockingTest.DerivedItem.class,
		CompositeIdentifierFollowOnLockingTest.VirtualItem.class,
		CompositeIdentifierFollowOnLockingTest.SecondaryItem.class,
		CompositeIdentifierFollowOnLockingTest.JoinedRoot.class,
		CompositeIdentifierFollowOnLockingTest.JoinedItem.class,
		CompositeIdentifierFollowOnLockingTest.BinaryItem.class,
		CompositeIdentifierFollowOnLockingTest.TypedItem.class,
		CompositeIdentifierFollowOnLockingTest.Parent.class
})
@SessionFactory(useCollectingStatementObserver = true, generateStatistics = true)
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.connection.isolation", value = "2"),
		@Setting(name = "hibernate.use_sql_comments", value = "false")
})
@ExtendWith(FollowOnLockingTestSupport.Eligibility.class)
@Tag("db-locking")
class CompositeIdentifierFollowOnLockingTest {
	@BeforeEach
	void createData(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		scope.inTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			final var parent = new Parent();
			parent.id = 1;
			session.persist( parent );
			for ( int local = 2; local <= 3; local++ ) {
				final var embedded = new EmbeddedItem();
				embedded.id = new Pair( 1, local );
				embedded.tags.add( "tag" );
				final var single = new SingleItem();
				single.id = new SingleKey( local );
				final var nested = new NestedItem();
				nested.id = new NestedKey( new Pair( 1, local ), 7 );
				final var idClass = new IdClassItem();
				idClass.tenant = 1;
				idClass.local = local;
				final var association = new AssociationItem();
				association.id = new AssociationKey( parent, local );
				final var derived = new DerivedItem();
				derived.parent = parent;
				derived.sequence = local;
				final var virtual = new VirtualItem();
				virtual.tenant = 1;
				virtual.local = local;
				final var secondary = new SecondaryItem();
				secondary.id = new Pair( 1, local );
				secondary.detail = "detail" + local;
				secondary.note = local == 2 ? "present" : null;
				final var joined = new JoinedItem();
				joined.id = new Pair( 1, local );
				joined.detail = "detail" + local;
				final var binary = new BinaryItem();
				binary.id = new byte[] { 1, (byte) local };
				final var typed = new TypedItem();
				typed.id = new TypedKey( new BigDecimal( "1.0" ), new byte[] { 1, (byte) local } );
				for ( var item : List.of( embedded, single, nested, idClass, association, derived, virtual, secondary, joined, binary, typed ) ) {
					item.title = "item" + local;
					session.persist( item );
				}
			}
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	static Stream<Arguments> identifierMappings() {
		return Stream.of( EmbeddedItem.class, SingleItem.class, NestedItem.class, IdClassItem.class,
				AssociationItem.class, DerivedItem.class, VirtualItem.class, SecondaryItem.class, JoinedItem.class, BinaryItem.class, TypedItem.class )
				.flatMap( type -> Stream.of( PESSIMISTIC_READ, PESSIMISTIC_WRITE, PESSIMISTIC_FORCE_INCREMENT )
						.flatMap( mode -> Stream.of( false, true ).map( readOnly -> arguments( type, mode, readOnly ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("identifierMappings")
	void acquiresCompleteIdentifiersAndKeepsVersionsCoherent(
			Class<? extends VersionedItem> type, LockMode mode, boolean readOnly, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setDefaultReadOnly( readOnly );
			scope.getCollectingStatementObserver().clear();
			final var items = query( session, type, mode, NORMAL ).getResultList();
			assertThat( items ).extracting( item -> item.title ).containsExactly( "item3", "item2" );
			for ( var item : items ) {
				assertVersion( session, item, mode == PESSIMISTIC_FORCE_INCREMENT ? 1 : 0 );
				assertThat( session.getCurrentLockMode( item ) ).isEqualTo( mode );
				assertThat( entry( session, item ).isReadOnly() ).isEqualTo( readOnly );
				if ( readOnly ) {
					assertThat( entry( session, item ).getLoadedState() ).isNull();
				}
				if ( item instanceof SecondaryItem secondary ) {
					assertThat( secondary.detail ).isEqualTo( "detail" + secondary.id.local );
					assertThat( secondary.note ).isEqualTo( secondary.id.local == 2 ? "present" : null );
				}
				if ( item instanceof JoinedItem joined ) {
					assertThat( joined.detail ).isEqualTo( "detail" + joined.id.local );
				}
			}
			assertThat( updates( scope ) ).isEqualTo( mode == PESSIMISTIC_FORCE_INCREMENT ? 2 : 0 );
		} );
	}

	@ParameterizedTest
	@CsvSource({ "PESSIMISTIC_READ, NORMAL", "PESSIMISTIC_READ, EXTENDED", "PESSIMISTIC_WRITE, NORMAL",
			"PESSIMISTIC_WRITE, EXTENDED", "PESSIMISTIC_FORCE_INCREMENT, NORMAL", "PESSIMISTIC_FORCE_INCREMENT, EXTENDED" })
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
	void physicallyProtectsTheRequestedScope(LockMode mode, PessimisticLockScope lockScope, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var items = query( session, EmbeddedItem.class, mode, lockScope ).getResultList();
			assertThat( items ).hasSize( 2 );
			assertThat( Hibernate.isInitialized( items.get( 0 ).tags ) ).isFalse();
			assertProtection( scope, "composite_embedded", "tenant_key", "local_key", true );
			assertProtection( scope, "composite_tags", "tenant_fk", "local_fk", lockScope == EXTENDED );
		} );
	}

	@ParameterizedTest
	@CsvSource({ "false, false", "false, true", "true, false", "true, true" })
	void adoptsAcquiredVersionsBeforeForceIncrement(boolean force, boolean readOnly, SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireDeletionRace( scope );
		final var changed = new AtomicBoolean();
		final List<String> selects = new ArrayList<>();
		final UnaryOperator<String> inspector = sql -> {
			if ( sql.startsWith( "select " ) && sql.contains( "composite_embedded" ) ) {
				selects.add( sql );
				if ( selects.size() == 2 ) {
					scope.inTransaction( writer -> writer.doWork( connection -> {
						try ( var statement = connection.createStatement() ) {
							assertThat( statement.executeUpdate( "update composite_embedded set version = 5, title = 'acquired'" ) ).isEqualTo( 2 );
						}
					} ) );
					changed.set( true );
				}
			}
			return sql;
		};
		try ( var reader = scope.getSessionFactory().withOptions().statementInspector( inspector ).openSession() ) {
			reader.setDefaultReadOnly( readOnly );
			final var transaction = reader.beginTransaction();
			try {
				final var items = query( reader, EmbeddedItem.class, force ? PESSIMISTIC_FORCE_INCREMENT : PESSIMISTIC_WRITE, NORMAL ).getResultList();
				assertThat( changed ).isTrue();
				assertThat( items ).hasSize( 2 ).allSatisfy( item -> {
					assertThat( item.title ).isEqualTo( "acquired" );
					assertVersion( reader, item, force ? 6 : 5 );
				} );
			}
			finally {
				transaction.rollback();
			}
		}
	}

	@Test
	void preservesDirtyManagedStateInAMixedResult(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var managed = session.find( EmbeddedItem.class, new Pair( 1, 2 ) );
			managed.title = "local change";
			session.setHibernateFlushMode( FlushMode.MANUAL );
			scope.getCollectingStatementObserver().clear();
			final var items = query( session, EmbeddedItem.class, PESSIMISTIC_FORCE_INCREMENT, NORMAL ).getResultList();
			assertThat( items ).hasSize( 2 );
			assertThat( items.get( 1 ) ).isSameAs( managed );
			assertThat( managed.title ).isEqualTo( "local change" );
			final var title = entry( session, managed ).getPersister().findAttributeMapping( "title" );
			assertThat( entry( session, managed ).getLoadedState()[title.getStateArrayPosition()] ).isEqualTo( "item2" );
			items.forEach( item -> assertVersion( session, item, 1 ) );
			assertThat( updates( scope ) ).isEqualTo( 2 );
		} );
	}

	@ParameterizedTest
	@CsvSource({ "false", "true" })
	void identifierCorrelationDoesNotInitializeAssociations(boolean derived, SessionFactoryScope scope) {
		final var type = derived ? DerivedItem.class : AssociationItem.class;
		final var entityName = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( type ).getEntityName();
		final var initialStates = scope.fromTransaction( session -> {
			scope.getCollectingStatementObserver().clear();
			final var items = session.createQuery( "from " + entityName + " order by title desc", type ).getResultList();
			assertThat( items ).hasSize( 2 );
			assertThat( scope.getCollectingStatementObserver().getSqlQueries() ).hasSize( 1 );
			return items.stream().map( item -> Hibernate.isInitialized( parent( item ) ) ).toList();
		} );
		assertThat( initialStates ).containsOnly( false );
		scope.inTransaction( session -> {
			// Observe from before the initial query through identity collection,
			// acquisition and state publication, including any intervening SQL.
			scope.getCollectingStatementObserver().clear();
			final var items = query( session, type, PESSIMISTIC_WRITE, NORMAL ).getResultList();
			assertThat( items ).hasSize( 2 );
			assertThat( items.stream().map( item -> Hibernate.isInitialized( parent( item ) ) ).toList() )
					.isEqualTo( initialStates );
			assertThat( scope.getCollectingStatementObserver().getSqlQueries() ).hasSize( 2 )
					.noneMatch( sql -> sql.contains( "composite_parent" ) || sql.contains( " join " ) );
		} );
	}

	static Stream<Arguments> missingRows() {
		return Stream.of( IdClassItem.class, SecondaryItem.class, JoinedItem.class )
				.flatMap( type -> Stream.of( false, true ).map( all -> arguments( type, all ) ) );
	}

	@ParameterizedTest
	@MethodSource("missingRows")
	void missingRequiredRowsFailBeforeApplyingTheirState(
			Class<? extends VersionedItem> type, boolean deleteAll, SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireDeletionRace( scope );
		final var persister = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( type );
		final String property = type == IdClassItem.class ? "title" : "detail";
		final var attribute = persister.findAttributeMapping( property );
		final var basic = (BasicValuedModelPart) attribute;
		final var table = tableFor( persister, basic.getContainingTableExpression() );
		final var readerRef = new AtomicReference<SessionImplementor>();
		final List<Snapshot> snapshots = new ArrayList<>();
		final int[] selects = { 0 };
		final UnaryOperator<String> inspector = sql -> {
			if ( FollowOnLockingTestSupport.isEntitySelect( sql, persister ) && ++selects[0] == 2 ) {
				readerRef.get().getPersistenceContextInternal().getEntitiesByKey().values().stream()
						.filter( type::isInstance ).map( type::cast )
						.sorted( (a, b) -> b.title.compareTo( a.title ) ).forEach( item -> {
							final var entry = entry( readerRef.get(), item );
							snapshots.add( new Snapshot( item, entry, attribute.getValue( item ), entry.getLockMode() ) );
						} );
				assertThat( snapshots ).hasSize( 2 );
				scope.inTransaction( writer -> writer.doWork( connection -> {
					final var predicates = new StringJoiner( " and " );
					table.getKeyDetails().forEachKeyColumn( (i, column) -> predicates.add( column.getColumnName() + " = ?" ) );
					if ( !deleteAll ) {
						try ( var update = connection.prepareStatement( "update " + table.getTableName() + " set "
								+ basic.getSelectionExpression() + " = 'changed' where " + predicates ) ) {
							persister.getIdentifierMapping().breakDownJdbcValues( snapshots.get( 1 ).entry.getId(),
									(i, value, mapping) -> {
										try { update.setObject( i + 1, value ); }
										catch (java.sql.SQLException e) { throw new RuntimeException( e ); }
									}, writer );
							assertThat( update.executeUpdate() ).isEqualTo( 1 );
						}
					}
					for ( int index = 0; index < (deleteAll ? 2 : 1); index++ ) {
						try ( var delete = connection.prepareStatement( "delete from " + table.getTableName() + " where " + predicates ) ) {
							persister.getIdentifierMapping().breakDownJdbcValues( snapshots.get( index ).entry.getId(),
									(i, value, mapping) -> {
										try { delete.setObject( i + 1, value ); }
										catch (java.sql.SQLException e) { throw new RuntimeException( e ); }
									}, writer );
							assertThat( delete.executeUpdate() ).isEqualTo( 1 );
						}
					}
				} ) );
			}
			return sql;
		};
		try ( var reader = scope.getSessionFactory().withOptions().statementInspector( inspector ).openSession() ) {
			readerRef.set( reader );
			final var transaction = reader.beginTransaction();
			try {
				final var failure = catchThrowableOfType( OptimisticLockException.class,
						() -> query( reader, type, PESSIMISTIC_WRITE, NORMAL ).getResultList() );
				assertThat( failure ).hasCauseInstanceOf( StaleObjectStateException.class );
				final var stale = (StaleObjectStateException) failure.getCause();
				assertThat( stale.getIdentifier() ).isSameAs( snapshots.get( 0 ).entry.getId() );
				final var missing = new StringJoiner( ", ", "[", "]" );
				snapshots.stream().limit( deleteAll ? 2 : 1 ).forEach( snapshot -> missing.add(
						persister.getIdentifierType().toLoggableString( snapshot.entry.getId(), scope.getSessionFactory() ) ) );
				assertThat( stale ).hasMessageContaining( "on table '" + table.getTableName() + "': missing identifiers " + missing );
				for ( var snapshot : snapshots ) {
					assertThat( attribute.getValue( snapshot.item ) ).isEqualTo( snapshot.value );
					assertThat( snapshot.entry.getLoadedState()[attribute.getStateArrayPosition()] ).isEqualTo( snapshot.value );
					assertThat( snapshot.entry.getLockMode() ).isEqualTo( snapshot.mode );
					assertVersion( reader, snapshot.item, 0 );
				}
			}
			finally {
				transaction.rollback();
			}
		}
	}

	private static Parent parent(Object item) {
		return item instanceof DerivedItem derived ? derived.parent : ((AssociationItem) item).id.parent;
	}

	@ParameterizedTest
	@CsvSource({ "false", "true" })
	void repeatedEntitySelectionsIncrementEachIdentityOnce(boolean binary, SessionFactoryScope scope) {
		final var type = binary ? BinaryItem.class : AssociationItem.class;
		scope.inTransaction( session -> {
			final var name = session.getFactory().getMappingMetamodel().getEntityDescriptor( type ).getEntityName();
			final var query = session.createQuery( "select a, b from " + name + " a join " + name
					+ " b on a.id = b.id order by a.title desc", Object[].class )
					.setHibernateLockMode( PESSIMISTIC_FORCE_INCREMENT ).setFollowOnStrategy( Locking.FollowOn.FORCE );
			FollowOnLockingTestSupport.assertFollowOnPlan( session, query );
			scope.getCollectingStatementObserver().clear();
			assertThat( query.getResultList() ).hasSize( 2 ).allSatisfy( row -> {
				assertThat( row[0] ).isSameAs( row[1] );
				assertVersion( session, (VersionedItem) row[0], 1 );
			} );
			assertThat( updates( scope ) ).isEqualTo( 2 );
		} );
	}

	private static TableDetails tableFor(EntityPersister persister, String tableName) {
		final var table = new AtomicReference<TableDetails>();
		persister.forEachTableDetails( details -> { if ( details.getTableName().equals( tableName ) ) { table.set( details ); } } );
		return table.get();
	}

	private static <T> SelectionQuery<T> query(SessionImplementor session, Class<T> type, LockMode mode, PessimisticLockScope scope) {
		final var persister = session.getFactory().getMappingMetamodel().getEntityDescriptor( type );
		final var query = session.createQuery( "from " + persister.getEntityName() + " order by title desc", type )
				.setHibernateLockMode( mode ).setLockScope( scope ).setFollowOnStrategy( Locking.FollowOn.FORCE );
		FollowOnLockingTestSupport.assertFollowOnPlan( session, query );
		return query;
	}

	private static EntityEntry entry(SessionImplementor session, Object item) {
		return session.getPersistenceContextInternal().getEntry( item );
	}

	private static void assertVersion(SessionImplementor session, VersionedItem item, int version) {
		assertThat( item.version ).isEqualTo( version );
		final var entry = entry( session, item );
		assertThat( entry.getVersion() ).isEqualTo( version );
		if ( entry.getLoadedState() != null ) {
			assertThat( entry.getLoadedState()[entry.getPersister().getVersionPropertyIndex()] ).isEqualTo( version );
		}
	}

	private static long updates(SessionFactoryScope scope) {
		return scope.getCollectingStatementObserver().getSqlQueries().stream().filter( sql -> sql.startsWith( "update " ) ).count();
	}

	private static void assertProtection(SessionFactoryScope scope, String table, String first, String second, boolean locked) {
		final var rows = scope.fromTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			final var sql = FollowOnLockingTestSupport.writeProtectionProbeSql( session,
					"select 1 from " + table + " t where " + first + " = 1 and " + second + " = 2",
					Map.of( "t", new String[] { first, second } ) );
			return session.createNativeQuery( sql, Integer.class ).getResultList().size();
		} );
		assertThat( rows ).as( table + " physical protection" ).isEqualTo( locked ? 0 : 1 );
	}

	private record Snapshot(VersionedItem item, EntityEntry entry, Object value, LockMode mode) {}

	@MappedSuperclass
	static abstract class VersionedItem {
		@Version int version;
		String title;
	}

	@Embeddable
	static class Pair implements Serializable {
		@Column(name = "tenant_key") int tenant;
		@Column(name = "local_key") int local;
		Pair() {}
		Pair(int tenant, int local) { this.tenant = tenant; this.local = local; }
		@Override public boolean equals(Object other) { return other instanceof Pair p && tenant == p.tenant && local == p.local; }
		@Override public int hashCode() { return Objects.hash( tenant, local ); }
	}

	@Entity(name = "CompositeEmbeddedItem")
	@Table(name = "composite_embedded")
	static class EmbeddedItem extends VersionedItem {
		@EmbeddedId Pair id;
		@ElementCollection
		@CollectionTable(name = "composite_tags", joinColumns = {
				@JoinColumn(name = "tenant_fk", referencedColumnName = "tenant_key"),
				@JoinColumn(name = "local_fk", referencedColumnName = "local_key")
		})
		Set<String> tags = new HashSet<>();
	}

	@Embeddable
	static class SingleKey implements Serializable {
		int code;
		SingleKey() {}
		SingleKey(int code) { this.code = code; }
		@Override public boolean equals(Object other) { return other instanceof SingleKey key && code == key.code; }
		@Override public int hashCode() { return code; }
	}

	@Entity(name = "CompositeSingleItem")
	@Table(name = "composite_single")
	static class SingleItem extends VersionedItem { @EmbeddedId SingleKey id; }

	@Embeddable
	static class NestedKey implements Serializable {
		@Embedded Pair pair;
		int shard;
		NestedKey() {}
		NestedKey(Pair pair, int shard) { this.pair = pair; this.shard = shard; }
		@Override public boolean equals(Object other) { return other instanceof NestedKey key && shard == key.shard && Objects.equals( pair, key.pair ); }
		@Override public int hashCode() { return Objects.hash( pair, shard ); }
	}

	@Entity(name = "CompositeNestedItem")
	@Table(name = "composite_nested")
	static class NestedItem extends VersionedItem { @EmbeddedId NestedKey id; }

	static class PairId implements Serializable {
		int tenant;
		int local;
		PairId() {}
		@Override public boolean equals(Object other) { return other instanceof PairId key && tenant == key.tenant && local == key.local; }
		@Override public int hashCode() { return Objects.hash( tenant, local ); }
	}

	@Entity(name = "CompositeIdClassItem")
	@Table(name = "composite_idclass")
	@IdClass(PairId.class)
	static class IdClassItem extends VersionedItem { @Id int tenant; @Id int local; }

	@Entity(name = "CompositeParent")
	@Table(name = "composite_parent")
	static class Parent {
		@Id int id;
		public int getId() { return id; }
		@Override public boolean equals(Object other) { return other instanceof Parent parent && getId() == parent.getId(); }
		@Override public int hashCode() { return 31; }
	}

	@Embeddable
	static class AssociationKey implements Serializable {
		@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_fk") Parent parent;
		int sequence;
		AssociationKey() {}
		AssociationKey(Parent parent, int sequence) { this.parent = parent; this.sequence = sequence; }
		@Override public boolean equals(Object other) { return other instanceof AssociationKey key && sequence == key.sequence && Objects.equals( parent, key.parent ); }
		@Override public int hashCode() { return Objects.hash( parent, sequence ); }
	}

	@Entity(name = "CompositeAssociationItem")
	@Table(name = "composite_association")
	static class AssociationItem extends VersionedItem { @EmbeddedId AssociationKey id; }

	static class DerivedKey implements Serializable {
		int parent;
		int sequence;
		DerivedKey() {}
		@Override public boolean equals(Object other) { return other instanceof DerivedKey key && parent == key.parent && sequence == key.sequence; }
		@Override public int hashCode() { return Objects.hash( parent, sequence ); }
	}

	@Entity(name = "CompositeDerivedItem")
	@Table(name = "composite_derived")
	@IdClass(DerivedKey.class)
	static class DerivedItem extends VersionedItem {
		@Id @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_fk") Parent parent;
		@Id int sequence;
	}

	@Entity(name = "CompositeVirtualItem")
	@Table(name = "composite_virtual")
	static class VirtualItem extends VersionedItem implements Serializable {
		@Id int tenant;
		@Id int local;
		@Override public boolean equals(Object other) { return other instanceof VirtualItem item && tenant == item.tenant && local == item.local; }
		@Override public int hashCode() { return Objects.hash( tenant, local ); }
	}

	@Entity(name = "CompositeSecondaryItem")
	@Table(name = "composite_secondary")
	@SecondaryTables({
			@SecondaryTable(name = "composite_required", pkJoinColumns = {
					@PrimaryKeyJoinColumn(name = "detail_tenant", referencedColumnName = "tenant_key"),
					@PrimaryKeyJoinColumn(name = "detail_local", referencedColumnName = "local_key") }),
			@SecondaryTable(name = "composite_optional", pkJoinColumns = {
					@PrimaryKeyJoinColumn(name = "optional_local", referencedColumnName = "local_key"),
					@PrimaryKeyJoinColumn(name = "optional_tenant", referencedColumnName = "tenant_key") })
	})
	@SecondaryRow(table = "composite_required", optional = false)
	static class SecondaryItem extends VersionedItem {
		@EmbeddedId Pair id;
		@Column(table = "composite_required") String detail;
		@Column(table = "composite_optional") String note;
	}

	@Entity(name = "CompositeJoinedRoot")
	@Table(name = "composite_joined")
	@Inheritance(strategy = InheritanceType.JOINED)
	static abstract class JoinedRoot extends VersionedItem { @EmbeddedId Pair id; }

	@Entity(name = "CompositeJoinedItem")
	@Table(name = "composite_joined_detail")
	@PrimaryKeyJoinColumns({
			@PrimaryKeyJoinColumn(name = "joined_tenant", referencedColumnName = "tenant_key"),
			@PrimaryKeyJoinColumn(name = "joined_local", referencedColumnName = "local_key")
	})
	static class JoinedItem extends JoinedRoot { String detail; }

	@Entity(name = "CompositeControlBinaryItem")
	@Table(name = "composite_binary")
	static class BinaryItem extends VersionedItem { @Id byte[] id; }
	@Embeddable
	static class TypedKey implements Serializable {
		@Column(precision = 12, scale = 2) BigDecimal amount;
		byte[] token;
		TypedKey() {}
		TypedKey(BigDecimal amount, byte[] token) { this.amount = amount; this.token = token; }
		@Override public boolean equals(Object other) { return other instanceof TypedKey key && amount.compareTo( key.amount ) == 0 && Arrays.equals( token, key.token ); }
		@Override public int hashCode() { return 31 * amount.stripTrailingZeros().hashCode() + Arrays.hashCode( token ); }
	}

	@Entity(name = "CompositeTypedItem")
	@Table(name = "composite_typed")
	static class TypedItem extends VersionedItem { @EmbeddedId TypedKey id; }

}
