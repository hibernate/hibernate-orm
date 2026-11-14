package org.hibernate.orm.test.dialect;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies that translating SQL ASTs does not ask the [org.hibernate.dialect.Dialect]
/// for its [CteSupport] and [MutationSyntaxSupport] again for every translation.
/// Dialects typically build these objects anew on each call.
@DomainModel(annotatedClasses = DialectCapabilityLookupTests.Thing.class)
@ServiceRegistry(settings = @Setting(
		name = AvailableSettings.DIALECT,
		value = "org.hibernate.orm.test.dialect.DialectCapabilityLookupTests$CountingDialect"
))
@SessionFactory
@RequiresDialect(H2Dialect.class)
public class DialectCapabilityLookupTests {
	@Test
	void retranslationDoesNotQueryDialectCapabilitiesAgain(SessionFactoryScope scope) {
		// warm up, so that any lazy initialization has happened
		runQueries( scope, 1, 3 );

		final int cteCalls = CountingDialect.CTE_SUPPORT_CALLS.get();
		final int mutationSyntaxCalls = CountingDialect.MUTATION_SYNTAX_SUPPORT_CALLS.get();

		// a multi-valued parameter forces a new translation whenever the number of values changes
		runQueries( scope, 3, 20 );

		assertThat( CountingDialect.CTE_SUPPORT_CALLS.get() ).isEqualTo( cteCalls );
		assertThat( CountingDialect.MUTATION_SYNTAX_SUPPORT_CALLS.get() ).isEqualTo( mutationSyntaxCalls );
	}

	private static void runQueries(SessionFactoryScope scope, int fromSize, int toSize) {
		scope.inTransaction( session -> {
			for ( int size = fromSize; size < toSize; size++ ) {
				final List<Long> ids = IntStream.range( 0, size ).mapToObj( Long::valueOf ).toList();
				session.createSelectionQuery( "from Thing where id in :ids", Thing.class )
						.setParameter( "ids", ids )
						.getResultList();
				session.createMutationQuery( "update Thing set name = 'x' where id in :ids" )
						.setParameter( "ids", ids )
						.executeUpdate();
			}
		} );
	}

	@Entity(name = "Thing")
	public static class Thing {
		@Id
		private Long id;
		private String name;
	}

	public static class CountingDialect extends H2Dialect {
		static final AtomicInteger CTE_SUPPORT_CALLS = new AtomicInteger();
		static final AtomicInteger MUTATION_SYNTAX_SUPPORT_CALLS = new AtomicInteger();

		@Override
		public CteSupport getCteSupport() {
			CTE_SUPPORT_CALLS.incrementAndGet();
			return super.getCteSupport();
		}

		@Override
		public MutationSyntaxSupport getMutationSyntaxSupport() {
			MUTATION_SYNTAX_SUPPORT_CALLS.incrementAndGet();
			return super.getMutationSyntaxSupport();
		}
	}
}
