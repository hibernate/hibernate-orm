package org.hibernate.orm.test.secondarytable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The row of an optional secondary table is deleted when it has no data. An
 * update which does not assign every column of the table cannot tell whether
 * the row has data, and so must not delete it.
 */
public abstract class AbstractOptionalSecondaryRowTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Shipment( 1, "code", "label" ) );
			session.persist( new DynamicShipment( 1, "code", "label" ) );
			session.persist( new Memo( 1, "text" ) );
		} );
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
	void clearDirtyColumnInDynamicUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.find( DynamicShipment.class, 1 ).label = null );
		scope.inTransaction( session -> {
			final var shipment = session.find( DynamicShipment.class, 1 );
			assertThat( shipment.label ).isNull();
			assertThat( shipment.code ).isEqualTo( "code" );
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
