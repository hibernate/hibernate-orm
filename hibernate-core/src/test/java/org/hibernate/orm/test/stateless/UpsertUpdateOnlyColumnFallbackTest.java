package org.hibernate.orm.test.stateless;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLUpdate;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jdbc.Expectation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialects;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiresDialects(@RequiresDialect(H2Dialect.class))
@ServiceRegistry(settings = @Setting(name = DIALECT,
		value = "org.hibernate.orm.test.secondarytable.OptionalSecondaryRowFallbackTest$FallbackDialect"))
class UpsertUpdateOnlyColumnFallbackTest extends UpsertUpdateOnlyColumnTest {
	@Entity
	@Table(name = "upsert_with_marker")
	@SQLUpdate(sql = "update upsert_with_marker set marker = ?, quantity = ? where id = ?", verify = MarkerExpectation.class)
	static class MarkedItem {
		@Id int id;
		int quantity;
		@Column(insertable = false, updatable = false) Integer marker;
	}

	public static class MarkerExpectation extends Expectation.RowCount {
		@Override
		public int getNumberOfParametersUsed() {
			return 1;
		}

		@Override
		public int prepare(PreparedStatement statement) throws SQLException {
			statement.setInt( 1, 42 );
			return 1;
		}
	}

	@Test
	@DomainModel(annotatedClasses = MarkedItem.class)
	@SessionFactory
	void updateRespectsPositionReservedByExpectation(SessionFactoryScope scope) {
		var item = new MarkedItem();
		item.id = 7;
		scope.inTransaction( session -> session.persist( item ) );
		item.quantity = 23;
		scope.inStatelessTransaction( session -> session.upsert( item ) );
		scope.inSession( session -> {
			var stored = session.find( MarkedItem.class, 7 );
			assertEquals( 42, stored.marker );
			assertEquals( 23, stored.quantity );
		} );
	}
}
