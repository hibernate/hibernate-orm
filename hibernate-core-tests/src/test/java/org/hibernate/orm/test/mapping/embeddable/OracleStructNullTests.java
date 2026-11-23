package org.hibernate.orm.test.mapping.embeddable;

import org.hibernate.annotations.Struct;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Whole-object SQL nulls require the Oracle object type name.
/// A non-temporal member shows the binding is independent of member types.
///
/// @author Steve Ebersole
@RequiresDialect(OracleDialect.class)
@DomainModel(annotatedClasses = OracleStructNullTests.Row.class)
@SessionFactory
class OracleStructNullTests {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void nullLifecycle(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var row = new Row();
			row.id = 1;
			session.persist( row );
		} );
		scope.inTransaction( session -> {
			var row = session.find( Row.class, 1 );
			assertThat( row.payload ).isNull();
			row.payload = new Payload( "first" );
		} );
		scope.inTransaction( session -> {
			var row = session.find( Row.class, 1 );
			assertThat( row.payload.text ).isEqualTo( "first" );
			row.payload = null;
		} );
		scope.inTransaction( session -> {
			var row = session.find( Row.class, 1 );
			assertThat( row.payload ).isNull();
			row.payload = new Payload( "restored" );
		} );
		scope.inTransaction( session ->
				assertThat( session.find( Row.class, 1 ).payload.text ).isEqualTo( "restored" ) );
	}

	@Entity(name = "OracleStructNullRow")
	@Table(name = "oracle_struct_null_row")
	static class Row {
		@Id int id;
		@Struct(name = "struct_null_payload") Payload payload;
	}

	@Embeddable
	static class Payload {
		String text;

		Payload() {
		}

		Payload(String text) {
			this.text = text;
		}
	}
}
