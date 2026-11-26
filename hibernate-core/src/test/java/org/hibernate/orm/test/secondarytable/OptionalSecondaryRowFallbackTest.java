package org.hibernate.orm.test.secondarytable;

import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.jdbc.OptionalTableUpdateOperation;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = @Setting(name = JdbcSettings.DIALECT,
		value = "org.hibernate.orm.test.secondarytable.OptionalSecondaryRowFallbackTest$FallbackDialect"))
public class OptionalSecondaryRowFallbackTest extends OptionalSecondaryRowTest {
	public static class FallbackDialect extends H2Dialect {
		@Override
		public MutationOperation createOptionalTableUpdateOperation(OptionalTableUpdateOperationRequest request) {
			return new OptionalTableUpdateOperation( request.mutationTarget(), request.update() );
		}
	}
}
