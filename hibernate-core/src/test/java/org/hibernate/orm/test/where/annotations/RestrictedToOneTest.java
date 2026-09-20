/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import java.io.Serializable;
import java.util.stream.Stream;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.engine.internal.FilteredAssociationState;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A filtered null is an incomplete view of a stored reference, not an instruction to erase it.
 */
@ServiceRegistry(settings =
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "false"))
@SessionFactory
@DomainModel(annotatedClasses = {
		RestrictedToOneTest.SqlTarget.class, RestrictedToOneTest.FilterTarget.class,
		RestrictedToOneTest.SqlFk.class, RestrictedToOneTest.SqlFkSelect.class,
		RestrictedToOneTest.SqlFkLazy.class,
		RestrictedToOneTest.SqlJoin.class, RestrictedToOneTest.SqlJoinSelect.class,
		RestrictedToOneTest.SqlJoinLazy.class,
		RestrictedToOneTest.SqlImplicitJoin.class, RestrictedToOneTest.SqlImplicitJoinSelect.class,
		RestrictedToOneTest.SqlImplicitJoinLazy.class,
		RestrictedToOneTest.FilterFk.class, RestrictedToOneTest.FilterFkSelect.class,
		RestrictedToOneTest.FilterFkLazy.class,
		RestrictedToOneTest.FilterJoin.class, RestrictedToOneTest.FilterJoinSelect.class,
		RestrictedToOneTest.FilterJoinLazy.class,
		RestrictedToOneTest.FilterImplicitJoin.class, RestrictedToOneTest.FilterImplicitJoinSelect.class,
		RestrictedToOneTest.FilterImplicitJoinLazy.class,
		RestrictedToOneTest.SqlOneFk.class,
		RestrictedToOneTest.SqlOneFkSelect.class,
		RestrictedToOneTest.SqlOneFkLazy.class,
		RestrictedToOneTest.SqlOneJoin.class,
		RestrictedToOneTest.SqlOneJoinSelect.class,
		RestrictedToOneTest.SqlOneJoinLazy.class,
		RestrictedToOneTest.FilterOneFk.class,
		RestrictedToOneTest.FilterOneFkSelect.class,
		RestrictedToOneTest.FilterOneFkLazy.class,
		RestrictedToOneTest.FilterOneJoin.class,
		RestrictedToOneTest.FilterOneJoinSelect.class,
		RestrictedToOneTest.FilterOneJoinLazy.class,
		RestrictedToOneTest.SqlCustomFk.class,
		RestrictedToOneTest.SqlAllFk.class,
		RestrictedToOneTest.SqlDirtyFk.class,
		RestrictedToOneTest.FilterCustomFk.class,
		RestrictedToOneTest.FilterAllFk.class,
		RestrictedToOneTest.FilterDirtyFk.class
})
@JiraKey("HHH-19568")
class RestrictedToOneTest {
	private Mapping preparedMapping;

	record Mapping(Class<? extends Owner> ownerType, Class<? extends Target> targetType, boolean lazy) {
		@Override
		public String toString() {
			return ownerType.getSimpleName();
		}
	}

	static Stream<Mapping> mappings() {
		return Stream.of(
				new Mapping( SqlFk.class, SqlTarget.class, false ),
				new Mapping( SqlFkSelect.class, SqlTarget.class, false ),
				new Mapping( SqlFkLazy.class, SqlTarget.class, true ),
				new Mapping( SqlJoin.class, SqlTarget.class, false ),
				new Mapping( SqlJoinSelect.class, SqlTarget.class, false ),
				new Mapping( SqlJoinLazy.class, SqlTarget.class, true ),
				new Mapping( SqlImplicitJoin.class, SqlTarget.class, false ),
				new Mapping( SqlImplicitJoinSelect.class, SqlTarget.class, false ),
				new Mapping( SqlImplicitJoinLazy.class, SqlTarget.class, true ),
				new Mapping( FilterFk.class, FilterTarget.class, false ),
				new Mapping( FilterFkSelect.class, FilterTarget.class, false ),
				new Mapping( FilterFkLazy.class, FilterTarget.class, true ),
				new Mapping( FilterJoin.class, FilterTarget.class, false ),
				new Mapping( FilterJoinSelect.class, FilterTarget.class, false ),
				new Mapping( FilterJoinLazy.class, FilterTarget.class, true ),
				new Mapping( FilterImplicitJoin.class, FilterTarget.class, false ),
				new Mapping( FilterImplicitJoinSelect.class, FilterTarget.class, false ),
				new Mapping( FilterImplicitJoinLazy.class, FilterTarget.class, true ),
				new Mapping( SqlOneFk.class, SqlTarget.class, false ),
				new Mapping( SqlOneFkSelect.class, SqlTarget.class, false ),
				new Mapping( SqlOneFkLazy.class, SqlTarget.class, true ),
				new Mapping( SqlOneJoin.class, SqlTarget.class, false ),
				new Mapping( SqlOneJoinSelect.class, SqlTarget.class, false ),
				new Mapping( SqlOneJoinLazy.class, SqlTarget.class, true ),
				new Mapping( FilterOneFk.class, FilterTarget.class, false ),
				new Mapping( FilterOneFkSelect.class, FilterTarget.class, false ),
				new Mapping( FilterOneFkLazy.class, FilterTarget.class, true ),
				new Mapping( FilterOneJoin.class, FilterTarget.class, false ),
				new Mapping( FilterOneJoinSelect.class, FilterTarget.class, false ),
				new Mapping( FilterOneJoinLazy.class, FilterTarget.class, true ),
				new Mapping( SqlCustomFk.class, SqlTarget.class, false ),
				new Mapping( SqlAllFk.class, SqlTarget.class, false ),
				new Mapping( SqlDirtyFk.class, SqlTarget.class, false ),
				new Mapping( FilterCustomFk.class, FilterTarget.class, false ),
				new Mapping( FilterAllFk.class, FilterTarget.class, false ),
				new Mapping( FilterDirtyFk.class, FilterTarget.class, false )
		);
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		if ( preparedMapping != null ) {
			// Delete only this fixture. Truncating the entire matrix repeatedly drops and
			// recreates foreign keys on some databases and can exhaust their transaction logs.
			final var metamodel = scope.getSessionFactory().getMappingMetamodel();
			final var owner = metamodel.getEntityDescriptor( preparedMapping.ownerType );
			final var target = metamodel.getEntityDescriptor( preparedMapping.targetType );
			final var association = (ToOneAttributeMapping) owner.findAttributeMapping( "target" );
			final String ownerTable = owner.getMappedTableDetails().getTableName();
			final String keyTable = association.getForeignKeyDescriptor().getKeyTable();
			scope.inTransaction( session -> {
				if ( !ownerTable.equals( keyTable ) ) {
					session.createNativeMutationQuery( "delete from " + keyTable + " where 1=1" ).executeUpdate();
				}
				session.createNativeMutationQuery( "delete from " + ownerTable + " where 1=1" ).executeUpdate();
				session.createNativeMutationQuery( "delete from " + target.getMappedTableDetails().getTableName() + " where 1=1" )
						.executeUpdate();
			} );
			preparedMapping = null;
		}
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	void prepare(SessionFactoryScope scope, Mapping mapping) {
		preparedMapping = mapping;
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 3; id++ ) {
				final Target target = instantiate( mapping.targetType );
				target.id = id;
				target.active = id != 2;
				session.persist( target );
			}
			for ( long id = 1; id <= 3; id++ ) {
				final Owner owner = instantiate( mapping.ownerType );
				owner.id = id;
				owner.name = "original";
				owner.setTarget( id == 3 ? null : session.find( mapping.targetType, id ) );
				session.persist( owner );
			}
		} );
	}

	private static <T> T instantiate(Class<T> type) {
		try {
			return type.getDeclaredConstructor().newInstance();
		}
		catch ( ReflectiveOperationException e ) {
			throw new AssertionError( e );
		}
	}

	static void enable(Session session) {
		session.enableFilter( "visibleTarget" ).setParameter( "active", true );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void findDistinguishesHiddenAndAbsent(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		scope.inTransaction( session -> {
			enable( session );
			assertThat( session.find( mapping.ownerType, 2L ).getTarget() ).isNull();
			assertThat( session.find( mapping.ownerType, 3L ).getTarget() ).isNull();
			final Target visible = session.find( mapping.ownerType, 1L ).getTarget();
			assertThat( visible ).isNotNull();
			if ( mapping.lazy ) {
				assertThat( Hibernate.isInitialized( visible ) ).isFalse();
			}
			assertThat( visible.getId() ).isEqualTo( 1L );
		} );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void bookkeepingIsSparseAndReleasesReplacedKeys(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		scope.inTransaction( session -> {
			enable( session );
			final var context = session.getPersistenceContextInternal();
			final Owner visible = session.find( mapping.ownerType, 1L );
			final Owner absent = session.find( mapping.ownerType, 3L );
			assertThat( context.getEntry( visible ).getExtraState( FilteredAssociationState.class ) ).isNull();
			assertThat( context.getEntry( absent ).getExtraState( FilteredAssociationState.class ) ).isNull();
			final Owner hidden = session.find( mapping.ownerType, 2L );
			final var entry = context.getEntry( hidden );
			final var state = entry.getExtraState( FilteredAssociationState.class );
			assertThat( state ).isNotNull();
			if ( !mapping.ownerType.isAnnotationPresent( SQLUpdate.class )
					&& !mapping.ownerType.isAnnotationPresent( OptimisticLocking.class ) ) {
				assertThat( state.physicalState( entry.getLoadedState(), entry.getPersister() ) )
						.isSameAs( entry.getLoadedState() );
			}
			final int position = entry.getPersister().findAttributeMapping( "target" ).getStateArrayPosition();
			assertThat( entry.getPersister().getFilteredAssociationMapping().isFiltered( state,
					(ToOneAttributeMapping) entry.getPersister().getAttributeMapping( position ) ) ).isTrue();
			assertThat( entry.getLoadedState()[position] ).isNull();
			hidden.setTarget( session.find( mapping.targetType, 3L ) );
			session.flush();
			assertThat( state.isEmpty() ).isTrue();
			assertThat( entry.getPersister().getFilteredAssociationMapping().isFiltered( state,
					(ToOneAttributeMapping) entry.getPersister().getAttributeMapping( position ) ) ).isFalse();
			// The replacement is visible and may now be cleared normally.
			hidden.setTarget( null );
		} );
		assertStoredReference( scope, mapping, 2L, null );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void queriesAndFetchJoinsAgree(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		for ( String fetch : new String[] { "", " left join fetch o.target" } ) {
			scope.inTransaction( session -> {
				enable( session );
				final var owners = session.createQuery(
						"from " + mapping.ownerType.getSimpleName() + " o" + fetch + " order by o.id",
						mapping.ownerType ).getResultList();
				assertThat( owners ).hasSize( 3 );
				assertThat( owners.get( 0 ).getTarget() ).isNotNull();
				assertThat( owners.get( 1 ).getTarget() ).isNull();
				assertThat( owners.get( 2 ).getTarget() ).isNull();
			} );
		}
	}

	@ParameterizedTest
	@MethodSource("mappings")
	@JiraKey("HHH-19566")
	void entityGraphsAgree(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		for ( boolean fetchTarget : new boolean[] { false, true } ) {
			scope.inTransaction( session -> {
				enable( session );
				final var graph = session.createEntityGraph( mapping.ownerType );
				if ( fetchTarget ) {
					graph.addAttributeNodes( "target" );
				}
				assertThat( session.find( graph, 2L ).getTarget() ).isNull();
			} );
		}
	}

	@ParameterizedTest
	@MethodSource("mappings")
	@JiraKey("HHH-19565")
	void unrelatedUpdatesPreserveHiddenReference(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		scope.inTransaction( session -> {
			enable( session );
			final Owner owner = session.find( mapping.ownerType, 2L );
			assertThat( owner.getTarget() ).isNull();

			owner.name = "first";
			session.flush();
			owner.name = "second";
		} );
		assertStoredReference( scope, mapping, 2L, 2L );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void assigningNullToHiddenReferencePreservesIt(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		scope.inTransaction( session -> {
			enable( session );
			final Owner owner = session.find( mapping.ownerType, 2L );
			assertThat( owner.getTarget() ).isNull();
			owner.setTarget( null );
			session.flush();
			owner.name = "explicit null";
		} );
		assertStoredReference( scope, mapping, 2L, 2L );
		assertHiddenTargetExists( scope, mapping );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void serializedSessionPreservesHiddenReference(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		final byte[] serialized = scope.fromSession( session -> {
			enable( session );
			assertThat( session.find( mapping.ownerType, 2L ).getTarget() ).isNull();
			session.getJdbcCoordinator().getLogicalConnection().manualDisconnect();
			return SerializationHelper.serialize( session );
		} );
		try ( var restored = (SessionImplementor) SerializationHelper.deserialize( serialized ) ) {
			scope.inTransaction( restored, session -> {
				final Owner owner = session.find( mapping.ownerType, 2L );
				assertThat( owner.getTarget() ).isNull();
				owner.name = "serialized session";
			} );
		}
		assertStoredReference( scope, mapping, 2L, 2L );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void mergingFilteredNullPreservesHiddenReference(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		final Owner detached = scope.fromTransaction( session -> {
			enable( session );
			final Owner owner = session.find( mapping.ownerType, 2L );
			assertThat( owner.getTarget() ).isNull();
			return owner;
		} );
		detached.name = "merged";
		scope.inTransaction( session -> {
			enable( session );
			session.merge( detached );
		} );
		assertStoredReference( scope, mapping, 2L, 2L );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void replacementThenClearIsNotSuppressed(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		scope.inTransaction( session -> {
			enable( session );
			final Owner owner = session.find( mapping.ownerType, 2L );
			assertThat( owner.getTarget() ).isNull();
			owner.setTarget( session.find( mapping.targetType, 3L ) );
			session.flush();
			owner.setTarget( null );
			session.flush();
		} );
		assertStoredReference( scope, mapping, 2L, null );
		assertHiddenTargetExists( scope, mapping );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void visibleReferenceCanBeClearedByMerge(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		final Owner detached = scope.fromTransaction( session -> {
			enable( session );
			return session.find( mapping.ownerType, 1L );
		} );
		detached.setTarget( null );
		scope.inTransaction( session -> {
			enable( session );
			session.merge( detached );
		} );
		assertStoredReference( scope, mapping, 1L, null );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void absentReferenceCanBeAssigned(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		scope.inTransaction( session -> {
			enable( session );
			final Owner owner = session.find( mapping.ownerType, 3L );
			assertThat( owner.getTarget() ).isNull();
			owner.setTarget( session.find( mapping.targetType, 3L ) );
		} );
		assertStoredReference( scope, mapping, 3L, 3L );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	@JiraKey("HHH-20806")
	void targetIdNavigationHonorsRestriction(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		scope.inTransaction( session -> {
			enable( session );
			assertThat( session.createQuery( "from " + mapping.ownerType.getSimpleName()
					+ " o where o.target.id = 2", mapping.ownerType ).getResultList() ).isEmpty();
			assertThat( session.createQuery( "select o.id from " + mapping.ownerType.getSimpleName()
					+ " o where fk(o.target) = 2", Long.class ).getSingleResult() ).isEqualTo( 2L );
		} );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void mergeDoesNotNeedDetachedMetadata(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		final Owner detached = instantiate( mapping.ownerType );
		detached.id = 2L;
		detached.name = "copy";
		scope.inTransaction( session -> {
			enable( session );
			session.merge( detached );
		} );
		assertStoredReference( scope, mapping, 2L, 2L );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void unrelatedUpdateDoesNotCreateAbsentAssociation(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		scope.inTransaction( session -> {
			enable( session );
			session.find( mapping.ownerType, 3L ).name = "changed";
		} );
		assertStoredReference( scope, mapping, 3L, null );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void refreshPreservesHiddenReference(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		scope.inTransaction( session -> {
			enable( session );
			final Owner owner = session.find( mapping.ownerType, 2L );
			session.refresh( owner );
			assertThat( owner.getTarget() ).isNull();
			owner.name = "refreshed";
		} );
		assertStoredReference( scope, mapping, 2L, 2L );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void readOnlyToModifiablePreservesHiddenReference(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		scope.inTransaction( session -> {
			enable( session );
			session.setDefaultReadOnly( true );
			final Owner owner = session.find( mapping.ownerType, 2L );
			assertThat( owner.getTarget() ).isNull();
			session.setReadOnly( owner, false );
			owner.name = "modifiable";
		} );
		assertStoredReference( scope, mapping, 2L, 2L );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void updatesInOneFlushKeepEachRowsNullness(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		scope.inTransaction( session -> {
			enable( session );
			session.find( mapping.ownerType, 1L ).setTarget( null );
			session.find( mapping.ownerType, 2L ).name = "hidden";
			session.find( mapping.ownerType, 3L ).name = "absent";
		} );
		assertStoredReference( scope, mapping, 1L, null );
		assertStoredReference( scope, mapping, 2L, 2L );
		assertStoredReference( scope, mapping, 3L, null );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void nativeAssociationFetchRetainsTheStoredKey(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		scope.inTransaction( session -> {
			enable( session );
			final var descriptor = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( mapping.ownerType );
			final var association = (ToOneAttributeMapping) descriptor.findAttributeMapping( "target" );
			final String keyTable = association.getForeignKeyDescriptor().getKeyTable();
			final String ownerTable = descriptor.getMappedTableDetails().getTableName();
			final String sql = ownerTable.equals( keyTable )
					? "select o.* from " + ownerTable + " o where o.id = 2"
					: "select o.*, j.target_id from " + ownerTable + " o left join " + keyTable
							+ " j on o.id = j.owner_id where o.id = 2";
			final Owner owner = session.createNativeQuery( sql, mapping.ownerType ).getSingleResult();
			assertThat( owner.getTarget() ).isNull();
			owner.name = "native";
		} );
		assertStoredReference( scope, mapping, 2L, 2L );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void deletingOwnerRemovesHiddenJoinRow(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		scope.inTransaction( session -> {
			enable( session );
			final Owner owner = session.find( mapping.ownerType, 2L );
			assertThat( owner.getTarget() ).isNull();
			session.remove( owner );
		} );
		scope.inTransaction( session -> {
			final var descriptor = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( mapping.ownerType );
			final String table = descriptor.getMappedTableDetails().getTableName();
			assertThat( session.createNativeQuery( "select count(*) from " + table + " where id=2", Long.class )
					.getSingleResult() ).isZero();
		} );
		assertStoredReference( scope, mapping, 2L, null );
		assertHiddenTargetExists( scope, mapping );
	}

	static Stream<Mapping> filterMappings() {
		return mappings().filter( mapping -> mapping.targetType == FilterTarget.class );
	}

	@ParameterizedTest
	@MethodSource("filterMappings")
	void changingFilterParametersDoesNotReuseThePreviousValue(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		for ( boolean active : new boolean[] { true, false, true } ) {
			scope.inTransaction( session -> {
				session.enableFilter( "visibleTarget" ).setParameter( "active", active );
				assertThat( session.find( mapping.ownerType, 1L ).getTarget() == null ).isEqualTo( !active );
				assertThat( session.find( mapping.ownerType, 2L ).getTarget() == null ).isEqualTo( active );
			} );
		}
	}

	@ParameterizedTest
	@MethodSource("filterMappings")
	void refreshCanRevealAReferenceWhichCanThenBeCleared(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		scope.inTransaction( session -> {
			enable( session );
			final Owner owner = session.find( mapping.ownerType, 2L );
			assertThat( owner.getTarget() ).isNull();
			session.disableFilter( "visibleTarget" );
			session.refresh( owner );
			assertThat( owner.getTarget() ).isNotNull();
			owner.setTarget( null );
		} );
		assertStoredReference( scope, mapping, 2L, null );
	}

	private static void assertHiddenTargetExists(SessionFactoryScope scope, Mapping mapping) {
		scope.inTransaction( session -> {
			final String table = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( mapping.targetType )
					.getMappedTableDetails().getTableName();
			assertThat( session.createNativeQuery( "select count(*) from " + table + " where id=2", Long.class )
					.getSingleResult() ).isEqualTo( 1 );
		} );
	}

	static Stream<Mapping> orphanMappings() {
		return mappings().filter( mapping -> mapping.ownerType.getSimpleName().contains( "One" ) );
	}

	@ParameterizedTest
	@MethodSource("orphanMappings")
	void onlyVisibleRemovalDeletesAnOrphan(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		scope.inTransaction( session -> {
			enable( session );
			final Owner hidden = session.find( mapping.ownerType, 2L );
			assertThat( hidden.getTarget() ).isNull();
			hidden.name = "changed";
			final Owner visible = session.find( mapping.ownerType, 1L );
			visible.setTarget( null );
		} );
		assertStoredReference( scope, mapping, 2L, 2L );
		scope.inTransaction( session -> {
			final String table = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( mapping.targetType )
					.getMappedTableDetails().getTableName();
			assertThat( session.createNativeQuery( "select id from " + table + " order by id", Long.class )
					.getResultList() ).containsExactly( 2L, 3L );
		} );
	}

	static void assertStoredReference(SessionFactoryScope scope, Mapping mapping, Long ownerId, Long targetId) {
		scope.inTransaction( session -> {
			final var descriptor = scope.getSessionFactory().getMappingMetamodel()
					.getEntityDescriptor( mapping.ownerType );
			final var association = (ToOneAttributeMapping) descriptor.findAttributeMapping( "target" );
			final String table = association.getForeignKeyDescriptor().getKeyTable();
			final String idColumn = mapping.ownerType.getSimpleName().contains( "Join" ) ? "owner_id" : "id";
			final var values = session.createNativeQuery(
					"select target_id from " + table + " where " + idColumn + " = :id", Long.class )
					.setParameter( "id", ownerId ).getResultList();
			if ( targetId == null ) {
				assertThat( values.isEmpty() || values.size() == 1 && values.get( 0 ) == null ).isTrue();
			}
			else {
				assertThat( values ).containsExactly( targetId );
			}
		} );
	}

	@MappedSuperclass
	public abstract static class Target implements Serializable {
		@Id
		Long id;
		boolean active;

		public Long getId() {
			return id;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "SqlTarget")
	@Table(name = "restricted_sql_target")
	@BatchSize(size = 8)
	@SQLRestriction("active = true")
	public static class SqlTarget extends Target {
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "FilterTarget")
	@Table(name = "restricted_filter_target")
	@BatchSize(size = 8)
	@FilterDef(name = "visibleTarget", applyToLoadByKey = true,
			parameters = @ParamDef(name = "active", type = Boolean.class))
	@Filter(name = "visibleTarget", condition = "active = :active")
	public static class FilterTarget extends Target {
	}

	@MappedSuperclass
	public abstract static class Owner implements Serializable {
		@Id
		Long id;
		String name;

		public abstract Target getTarget();
		public abstract void setTarget(Target target);
	}
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "SqlFk")
	@Table(name = "restricted_sqlfk")
	public static class SqlFk extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "target_id")
		SqlTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (SqlTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "SqlFkLazy")
	@Table(name = "restricted_sqlfklazy")
	public static class SqlFkLazy extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "target_id")
		SqlTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (SqlTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "SqlJoin")
	@Table(name = "restricted_sqljoin")
	public static class SqlJoin extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinTable(name = "restricted_sqljoin_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		SqlTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (SqlTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "SqlJoinLazy")
	@Table(name = "restricted_sqljoinlazy")
	public static class SqlJoinLazy extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinTable(name = "restricted_sqljoinlazy_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		SqlTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (SqlTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "SqlImplicitJoin")
	@Table(name = "restricted_sqlimplicitjoin")
	public static class SqlImplicitJoin extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinTable(joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		SqlTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (SqlTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "SqlImplicitJoinLazy")
	@Table(name = "restricted_sqlimplicitjoinlazy")
	public static class SqlImplicitJoinLazy extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinTable(joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		SqlTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (SqlTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "FilterFk")
	@Table(name = "restricted_filterfk")
	public static class FilterFk extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "target_id")
		FilterTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (FilterTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "FilterFkLazy")
	@Table(name = "restricted_filterfklazy")
	public static class FilterFkLazy extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "target_id")
		FilterTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (FilterTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "FilterJoin")
	@Table(name = "restricted_filterjoin")
	public static class FilterJoin extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinTable(name = "restricted_filterjoin_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		FilterTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (FilterTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "FilterJoinLazy")
	@Table(name = "restricted_filterjoinlazy")
	public static class FilterJoinLazy extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinTable(name = "restricted_filterjoinlazy_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		FilterTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (FilterTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "FilterImplicitJoin")
	@Table(name = "restricted_filterimplicitjoin")
	public static class FilterImplicitJoin extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinTable(joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		FilterTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (FilterTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "FilterImplicitJoinLazy")
	@Table(name = "restricted_filterimplicitjoinlazy")
	public static class FilterImplicitJoinLazy extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinTable(joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		FilterTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (FilterTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "SqlFkSelect")
	@Table(name = "restricted_sqlfkselect")
	public static class SqlFkSelect extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "target_id")
		SqlTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (SqlTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "SqlJoinSelect")
	@Table(name = "restricted_sqljoinselect")
	public static class SqlJoinSelect extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		@JoinTable(name = "restricted_sqljoinselect_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		SqlTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (SqlTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "SqlImplicitJoinSelect")
	@Table(name = "restricted_sqlimplicitjoinselect")
	public static class SqlImplicitJoinSelect extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		@JoinTable(joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		SqlTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (SqlTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "FilterFkSelect")
	@Table(name = "restricted_filterfkselect")
	public static class FilterFkSelect extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "target_id")
		FilterTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (FilterTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "FilterJoinSelect")
	@Table(name = "restricted_filterjoinselect")
	public static class FilterJoinSelect extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		@JoinTable(name = "restricted_filterjoinselect_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		FilterTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (FilterTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "FilterImplicitJoinSelect")
	@Table(name = "restricted_filterimplicitjoinselect")
	public static class FilterImplicitJoinSelect extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		@JoinTable(joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		FilterTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (FilterTarget) target;
		}
	}
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "SqlOneFk")
	@Table(name = "restricted_sqlonefk")
	public static class SqlOneFk extends Owner {
		@OneToOne(orphanRemoval = true, fetch = FetchType.EAGER)
		@JoinColumn(name = "target_id")
		SqlTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (SqlTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "SqlOneFkSelect")
	@Table(name = "restricted_sqlonefkselect")
	public static class SqlOneFkSelect extends Owner {
		@OneToOne(orphanRemoval = true, fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "target_id")
		SqlTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (SqlTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "SqlOneFkLazy")
	@Table(name = "restricted_sqlonefklazy")
	public static class SqlOneFkLazy extends Owner {
		@OneToOne(orphanRemoval = true, fetch = FetchType.LAZY)
		@JoinColumn(name = "target_id")
		SqlTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (SqlTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "SqlOneJoin")
	@Table(name = "restricted_sqlonejoin")
	public static class SqlOneJoin extends Owner {
		@OneToOne(orphanRemoval = true, fetch = FetchType.EAGER)
		@JoinTable(name = "restricted_sqlonejoin_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		SqlTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (SqlTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "SqlOneJoinSelect")
	@Table(name = "restricted_sqlonejoinselect")
	public static class SqlOneJoinSelect extends Owner {
		@OneToOne(orphanRemoval = true, fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		@JoinTable(name = "restricted_sqlonejoinselect_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		SqlTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (SqlTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "SqlOneJoinLazy")
	@Table(name = "restricted_sqlonejoinlazy")
	public static class SqlOneJoinLazy extends Owner {
		@OneToOne(orphanRemoval = true, fetch = FetchType.LAZY)
		@JoinTable(name = "restricted_sqlonejoinlazy_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		SqlTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (SqlTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "FilterOneFk")
	@Table(name = "restricted_filteronefk")
	public static class FilterOneFk extends Owner {
		@OneToOne(orphanRemoval = true, fetch = FetchType.EAGER)
		@JoinColumn(name = "target_id")
		FilterTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (FilterTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "FilterOneFkSelect")
	@Table(name = "restricted_filteronefkselect")
	public static class FilterOneFkSelect extends Owner {
		@OneToOne(orphanRemoval = true, fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "target_id")
		FilterTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (FilterTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "FilterOneFkLazy")
	@Table(name = "restricted_filteronefklazy")
	public static class FilterOneFkLazy extends Owner {
		@OneToOne(orphanRemoval = true, fetch = FetchType.LAZY)
		@JoinColumn(name = "target_id")
		FilterTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (FilterTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "FilterOneJoin")
	@Table(name = "restricted_filteronejoin")
	public static class FilterOneJoin extends Owner {
		@OneToOne(orphanRemoval = true, fetch = FetchType.EAGER)
		@JoinTable(name = "restricted_filteronejoin_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		FilterTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (FilterTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "FilterOneJoinSelect")
	@Table(name = "restricted_filteronejoinselect")
	public static class FilterOneJoinSelect extends Owner {
		@OneToOne(orphanRemoval = true, fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		@JoinTable(name = "restricted_filteronejoinselect_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		FilterTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (FilterTarget) target;
		}
	}

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "FilterOneJoinLazy")
	@Table(name = "restricted_filteronejoinlazy")
	public static class FilterOneJoinLazy extends Owner {
		@OneToOne(orphanRemoval = true, fetch = FetchType.LAZY)
		@JoinTable(name = "restricted_filteronejoinlazy_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		FilterTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (FilterTarget) target;
		}
	}

	@SQLUpdate(sql = "update restricted_sqlcustomfk set name=?, target_id=? where id=?")
	@Entity(name = "SqlCustomFk")
	@Table(name = "restricted_sqlcustomfk")
	public static class SqlCustomFk extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "target_id")
		SqlTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (SqlTarget) target;
		}
	}

	@DynamicUpdate
	@OptimisticLocking(type = OptimisticLockType.ALL)
	@Entity(name = "SqlAllFk")
	@Table(name = "restricted_sqlallfk")
	public static class SqlAllFk extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "target_id")
		SqlTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (SqlTarget) target;
		}
	}

	@DynamicUpdate
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	@Entity(name = "SqlDirtyFk")
	@Table(name = "restricted_sqldirtyfk")
	public static class SqlDirtyFk extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "target_id")
		SqlTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (SqlTarget) target;
		}
	}

	@SQLUpdate(sql = "update restricted_filtercustomfk set name=?, target_id=? where id=?")
	@Entity(name = "FilterCustomFk")
	@Table(name = "restricted_filtercustomfk")
	public static class FilterCustomFk extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "target_id")
		FilterTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (FilterTarget) target;
		}
	}

	@DynamicUpdate
	@OptimisticLocking(type = OptimisticLockType.ALL)
	@Entity(name = "FilterAllFk")
	@Table(name = "restricted_filterallfk")
	public static class FilterAllFk extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "target_id")
		FilterTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (FilterTarget) target;
		}
	}

	@DynamicUpdate
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	@Entity(name = "FilterDirtyFk")
	@Table(name = "restricted_filterdirtyfk")
	public static class FilterDirtyFk extends Owner {
		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "target_id")
		FilterTarget target;

		@Override
		public Target getTarget() {
			return target;
		}

		@Override
		public void setTarget(Target target) {
			this.target = (FilterTarget) target;
		}
	}

}
