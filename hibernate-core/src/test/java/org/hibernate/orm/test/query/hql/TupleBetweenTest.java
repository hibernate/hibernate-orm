package org.hibernate.orm.test.query.hql;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.ORDERING_COMPARISON;

/// A multi-column BETWEEN compares lexicographically, and uses the comparison
/// emulation on databases without row-value ordering syntax.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = TupleBetweenTest.Row.class)
@SessionFactory(useCollectingStatementInspector = true)
class TupleBetweenTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int id = 0;
			for ( int major = 1; major <= 3; major++ ) {
				for ( int minor = 1; minor <= 3; minor++ ) {
					final var row = new Row();
					row.id = ++id;
					row.version = new Version( major, minor );
					session.persist( row );
				}
			}
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void between(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> assertThat( session.createSelectionQuery(
						"select e.id from TupleBetweenRow e where e.version between :low and :high order by e.id", Integer.class )
				.setParameter( "low", new Version( 1, 3 ) )
				.setParameter( "high", new Version( 3, 1 ) )
				.getResultList() ).containsExactly( 3, 4, 5, 6, 7 ) );
		assertEmulatedWithoutRowOrdering( scope, inspector.getSqlQueries().get( 0 ) );
	}

	@Test
	void notBetween(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> assertThat( session.createSelectionQuery(
						"select e.id from TupleBetweenRow e where e.version not between :low and :high order by e.id", Integer.class )
				.setParameter( "low", new Version( 1, 3 ) )
				.setParameter( "high", new Version( 3, 1 ) )
				.getResultList() ).containsExactly( 1, 2, 8, 9 ) );
		assertEmulatedWithoutRowOrdering( scope, inspector.getSqlQueries().get( 0 ) );
	}

	private static void assertEmulatedWithoutRowOrdering(SessionFactoryScope scope, String sql) {
		if ( !scope.getSessionFactory().getJdbcServices().getDialect()
				.getRowValueSupport().supports( ORDERING_COMPARISON ) ) {
			assertThat( sql ).doesNotContain( " between " );
		}
	}

	@Entity(name = "TupleBetweenRow")
	@Table(name = "tuple_between_row")
	static class Row {
		@Id int id;
		Version version;
	}

	@Embeddable
	static class Version {
		int major;
		int minor;

		Version() {
		}

		Version(int major, int minor) {
			this.major = major;
			this.minor = minor;
		}
	}
}
