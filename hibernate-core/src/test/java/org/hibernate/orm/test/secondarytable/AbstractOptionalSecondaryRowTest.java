package org.hibernate.orm.test.secondarytable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.sql.spi.mutation.jdbc.MergeOperation;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * An optional secondary row is deleted when all its mapped state is null
 * after the update, including the values of columns left untouched.
 */
public abstract class AbstractOptionalSecondaryRowTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Shipment( 1, "code", "label" ) );
			session.persist( new DynamicShipment( 1, "code", "label" ) );
			session.persist( new Memo( 1, "text" ) );
		} );
		scope.getCollectingStatementObserver().clear();
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void clearColumnOfRowWithNonUpdatableColumn(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.find( Shipment.class, 1 ).label = null );
		scope.inTransaction( session -> {
			final var shipment = session.find( Shipment.class, 1 );
			assertThat( shipment.label ).isNull();
			assertThat( shipment.code ).isEqualTo( "code" );
		} );
	}

	@Test
	void clearColumnOfRowWithNonUpdatableColumnInStatelessSession(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			final var shipment = session.get( Shipment.class, 1 );
			shipment.label = null;
			session.update( shipment );
		} );
		scope.inTransaction( session -> {
			final var shipment = session.find( Shipment.class, 1 );
			assertThat( shipment.label ).isNull();
			assertThat( shipment.code ).isEqualTo( "code" );
		} );
	}

	@Test
	void clearLastValueWhenNonUpdatableColumnIsNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Shipment( 2, null, "label" ) ) );
		scope.inTransaction( session -> session.find( Shipment.class, 2 ).label = null );
		assertNoDetailRow( scope, 2 );
	}

	@Test
	void statelessUpdateUsesStoredNonUpdatableColumn(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> session.update( new Shipment( 1, null, null ) ) );
		if ( usesOptionalTableMerge( scope ) ) {
			assertSingleDetailMerge( scope );
		}
		else {
			assertThat( scope.getCollectingStatementObserver().getSqlQueries() )
					.filteredOn( sql -> sql.contains( "optional_shipment_detail" ) )
					.singleElement().asString().startsWith( "update optional_shipment_detail " )
					.contains( "code is not null" );
		}
		scope.inTransaction( session -> {
			final var shipment = session.find( Shipment.class, 1 );
			assertThat( shipment.code ).isEqualTo( "code" );
			assertThat( shipment.label ).isNull();
		} );
	}

	@Test
	void statelessUpdateInsertsMissingDetailRow(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Shipment( 2, null, null ) ) );
		assertNoDetailRow( scope, 2 );
		scope.getCollectingStatementObserver().clear();
		scope.inStatelessTransaction( session -> session.update( new Shipment( 2, null, "new" ) ) );
		if ( usesOptionalTableMerge( scope ) ) {
			assertSingleDetailMerge( scope );
		}
		else {
			assertThat( scope.getCollectingStatementObserver().getSqlQueries() )
					.filteredOn( sql -> sql.contains( "optional_shipment_detail" ) )
					.satisfiesExactly(
							sql -> assertThat( sql ).startsWith( "update optional_shipment_detail " ),
							sql -> assertThat( sql ).startsWith( "insert into optional_shipment_detail " )
					);
		}
		scope.inTransaction( session -> assertThat( session.find( Shipment.class, 2 ).label ).isEqualTo( "new" ) );
	}

	@Test
	void statelessUpdateLeavesEmptyDetailRowAbsent(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Shipment( 2, null, null ) ) );
		scope.inStatelessTransaction( session -> session.update( new Shipment( 2, null, null ) ) );
		assertNoDetailRow( scope, 2 );
		scope.inStatelessTransaction( session -> session.update( new Shipment( 2, "ignored", null ) ) );
		assertNoDetailRow( scope, 2 );
	}

	@Test
	void statelessUpdateLeavesMissingMemoDetailAbsent(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Memo( 2, null ) ) );
		scope.inStatelessTransaction( session -> session.update( new Memo( 2, null ) ) );
		scope.inTransaction( session -> assertThat( session
				.createNativeQuery( "select count(*) from optional_memo_detail where id = 2", Long.class )
				.getSingleResult() ).isZero() );
	}

	@Test
	void staleUpdateWithMissingDetailIsRejected(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Memo( 2, null ) ) );
		scope.inStatelessTransaction( session -> session.update( new Memo( 2, null ) ) );
		assertThatThrownBy( () -> scope.inStatelessTransaction( session -> session.update( new Memo( 2, null ) ) ) )
				.isInstanceOf( OptimisticLockException.class );
	}

	@Test
	void statelessUpsertUsesStoredCodeForMatchedRows(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Shipment( 2, null, "label" ) ) );
		scope.inStatelessTransaction( session -> {
			session.upsert( new Shipment( 1, null, null ) );
			session.upsert( new Shipment( 2, "ignored", null ) );
			session.upsert( new Shipment( 3, "new", null ) );
		} );
		scope.inTransaction( session -> {
			final var existing = session.find( Shipment.class, 1 );
			assertThat( existing.code ).isEqualTo( "code" );
			assertThat( existing.label ).isNull();
			assertThat( session.find( Shipment.class, 3 ).code ).isEqualTo( "new" );
		} );
		assertNoDetailRow( scope, 2 );
	}

	private static void assertNoDetailRow(SessionFactoryScope scope, int id) {
		scope.inTransaction( session -> assertThat( session
				.createNativeQuery( "select count(*) from optional_shipment_detail where id = :id", Long.class )
				.setParameter( "id", id ).getSingleResult() ).isZero() );
	}

	private static void assertSingleDetailMerge(SessionFactoryScope scope) {
		assertThat( scope.getCollectingStatementObserver().getSqlQueries() )
				.filteredOn( sql -> sql.contains( "optional_shipment_detail" ) )
				.singleElement().asString().startsWith( "merge into optional_shipment_detail " );
	}

	// Memo has no omitted columns, so its mutation identifies the dialect's usual strategy.
	private static boolean usesOptionalTableMerge(SessionFactoryScope scope) {
		return scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Memo.class )
				.getUpdateCoordinator().getStaticMutationOperationGroup()
				.getOperation( "optional_memo_detail" ) instanceof MergeOperation;
	}

	@Test
	void clearDirtyColumnInDynamicUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.find( DynamicShipment.class, 1 ).label = null;
			scope.getCollectingStatementObserver().clear();
		} );
		assertThat( scope.getCollectingStatementObserver().getSqlQueries() )
				.filteredOn( sql -> sql.contains( "optional_dynamic_shipment_detail" ) )
				.singleElement().asString().startsWith( "update optional_dynamic_shipment_detail " );
		scope.inTransaction( session -> {
			final var shipment = session.find( DynamicShipment.class, 1 );
			assertThat( shipment.label ).isNull();
			assertThat( shipment.code ).isEqualTo( "code" );
		} );
	}

	@Test
	void deleteLastDirtyColumnWithOptimisticLock(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new DynamicShipment( 2, null, "label" ) ) );
		scope.inTransaction( session -> {
			session.find( DynamicShipment.class, 2 ).label = null;
			scope.getCollectingStatementObserver().clear();
		} );
		assertThat( scope.getCollectingStatementObserver().getSqlQueries() )
				.filteredOn( sql -> sql.contains( "optional_dynamic_shipment_detail" ) )
				.singleElement().asString().startsWith( "delete from optional_dynamic_shipment_detail " );
		scope.inTransaction( session -> assertThat( session
				.createNativeQuery( "select count(*) from optional_dynamic_shipment_detail where id = 2", Long.class )
				.getSingleResult() ).isZero() );
	}

	@Test
	void clearLastDirtyColumnAfterAnotherSessionSetsCode(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new DynamicShipment( 2, null, "label" ) ) );
		scope.inSession( firstSession -> {
			final var shipment = scope.fromTransaction( firstSession, session -> session.find( DynamicShipment.class, 2 ) );
			scope.inTransaction( session -> session.find( DynamicShipment.class, 2 ).code = "added" );
			scope.inTransaction( firstSession, session -> shipment.label = null );
		} );
		scope.inTransaction( session -> {
			final var shipment = session.find( DynamicShipment.class, 2 );
			assertThat( shipment.code ).isEqualTo( "added" );
			assertThat( shipment.label ).isNull();
		} );
	}

	@ParameterizedTest
	@CsvSource({ ", false", "code, false", "code, true" })
	void staleLabelClearIsRejected(String code, boolean clearCode, SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new DynamicShipment( 2, code, "label" ) ) );
		scope.inSession( firstSession -> {
			final var shipment = scope.fromTransaction( firstSession, session -> session.find( DynamicShipment.class, 2 ) );
			scope.inTransaction( session -> session.find( DynamicShipment.class, 2 ).label = "other" );
			assertThatThrownBy( () -> scope.inTransaction( firstSession, session -> {
				shipment.label = null;
				if ( clearCode ) {
					shipment.code = null;
				}
				session.flush();
			} ) ).isInstanceOf( OptimisticLockException.class );
		} );
		scope.inTransaction( session -> {
			final var shipment = session.find( DynamicShipment.class, 2 );
			assertThat( shipment.label ).isEqualTo( "other" );
			assertThat( shipment.code ).isEqualTo( code );
		} );
	}

	@Test
	void staleLabelUpdateDoesNotRecreateDeletedDetailRow(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new DynamicShipment( 2, null, "label" ) ) );
		scope.inSession( firstSession -> {
			final var shipment = scope.fromTransaction( firstSession, session -> session.find( DynamicShipment.class, 2 ) );
			scope.inTransaction( session -> session.find( DynamicShipment.class, 2 ).label = null );
			assertThatThrownBy( () -> scope.inTransaction( firstSession, session -> {
				shipment.label = "mine";
				session.flush();
			} ) ).isInstanceOf( OptimisticLockException.class );
		} );
		scope.inTransaction( session -> assertThat( session
				.createNativeQuery( "select count(*) from optional_dynamic_shipment_detail where id = 2", Long.class )
				.getSingleResult() ).isZero() );
	}

	@Test
	void setLabelAfterAnotherSessionClearsLastCode(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new DynamicShipment( 2, "code", null ) ) );
		scope.inSession( firstSession -> {
			final var shipment = scope.fromTransaction( firstSession, session -> session.find( DynamicShipment.class, 2 ) );
			scope.inTransaction( session -> session.find( DynamicShipment.class, 2 ).code = null );
			scope.inTransaction( firstSession, session -> shipment.label = "mine" );
		} );
		scope.inTransaction( session -> {
			final var shipment = session.find( DynamicShipment.class, 2 );
			assertThat( shipment.label ).isEqualTo( "mine" );
			assertThat( shipment.code ).isNull();
		} );
	}

	@Test
	void restoreColumnOfRowWithNonUpdatableColumn(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.find( Shipment.class, 1 ).label = null );
		scope.inTransaction( session -> session.find( Shipment.class, 1 ).label = "restored" );
		scope.inTransaction( session -> {
			final var shipment = session.find( Shipment.class, 1 );
			assertThat( shipment.label ).isEqualTo( "restored" );
			assertThat( shipment.code ).isEqualTo( "code" );
		} );
	}

	/**
	 * When every column of the table is assigned, a row with no data is still deleted.
	 */
	@Test
	void deleteRowWithNoData(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.find( Memo.class, 1 ).text = null );
		scope.inTransaction( session -> {
			assertThat( session.find( Memo.class, 1 ).text ).isNull();
			assertThat( session.createNativeQuery( "select count(*) from optional_memo_detail", Long.class )
					.getSingleResult() ).isZero();
		} );
		scope.inTransaction( session -> session.find( Memo.class, 1 ).text = "restored" );
		scope.inTransaction( session -> assertThat( session.find( Memo.class, 1 ).text ).isEqualTo( "restored" ) );
	}

	@Entity(name = "Shipment")
	@Table(name = "optional_shipment")
	@SecondaryTable(name = "optional_shipment_detail")
	public static class Shipment {
		@Id
		Integer id;
		@Column(table = "optional_shipment_detail", updatable = false)
		String code;
		@Column(table = "optional_shipment_detail")
		String label;

		Shipment() {
		}

		Shipment(Integer id, String code, String label) {
			this.id = id;
			this.code = code;
			this.label = label;
		}
	}

	@Entity(name = "DynamicShipment")
	@Table(name = "optional_dynamic_shipment")
	@SecondaryTable(name = "optional_dynamic_shipment_detail")
	@DynamicUpdate
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	public static class DynamicShipment {
		@Id
		Integer id;
		@Column(table = "optional_dynamic_shipment_detail")
		String code;
		@Column(table = "optional_dynamic_shipment_detail")
		String label;

		DynamicShipment() {
		}

		DynamicShipment(Integer id, String code, String label) {
			this.id = id;
			this.code = code;
			this.label = label;
		}
	}

	@Entity(name = "Memo")
	@Table(name = "optional_memo")
	@SecondaryTable(name = "optional_memo_detail")
	public static class Memo {
		@Id
		Integer id;
		@Version
		int version;
		@Column(table = "optional_memo_detail")
		String text;

		Memo() {
		}

		Memo(Integer id, String text) {
			this.id = id;
			this.text = text;
		}
	}
}
