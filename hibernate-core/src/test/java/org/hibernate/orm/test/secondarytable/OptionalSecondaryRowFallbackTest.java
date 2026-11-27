package org.hibernate.orm.test.secondarytable;

import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.jdbc.OptionalTableUpdateOperation;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = @Setting(name = JdbcSettings.DIALECT,
		value = "org.hibernate.orm.test.secondarytable.OptionalSecondaryRowFallbackTest$FallbackDialect"))
public class OptionalSecondaryRowFallbackTest extends OptionalSecondaryRowTest {
	@Test
	void fallbackUpsertReusesStatementsAcrossRows(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Memo( 2, "second" ) ) );

		scope.inStatelessTransaction( session -> {
			var first = new Memo( 1, "first update" );
			session.upsert( first );
			session.upsert( new Memo( 2, "second update" ) );
			first.text = "last update";
			session.upsert( first );
		} );

		scope.inSession( session -> {
			var first = session.find( Memo.class, 1 );
			assertThat( first.text ).isEqualTo( "last update" );
			assertThat( first.version ).isEqualTo( 2 );
			var second = session.find( Memo.class, 2 );
			assertThat( second.text ).isEqualTo( "second update" );
			assertThat( second.version ).isEqualTo( 1 );
		} );
	}

	public static class FallbackDialect extends H2Dialect {
		@Override
		public MutationOperation createOptionalTableUpdateOperation(OptionalTableUpdateOperationRequest request) {
			return new OptionalTableUpdateOperation( request.mutationTarget(), request.update() );
		}
	}
}
