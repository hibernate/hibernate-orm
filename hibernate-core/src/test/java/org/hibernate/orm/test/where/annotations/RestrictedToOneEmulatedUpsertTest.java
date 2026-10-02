package org.hibernate.orm.test.where.annotations;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.jdbc.OptionalTableUpdateOperation;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

@ServiceRegistry(settings = @Setting(name = AvailableSettings.DIALECT,
		value = "org.hibernate.orm.test.where.annotations.RestrictedToOneEmulatedUpsertTest$EmulatedUpsertDialect"))
@RequiresDialect(H2Dialect.class)
@JiraKey("HHH-19498")
class RestrictedToOneEmulatedUpsertTest extends RestrictedToOneTest {
	public static class EmulatedUpsertDialect extends H2Dialect {
		@Override
		public MutationOperation createOptionalTableUpdateOperation(OptionalTableUpdateOperationRequest request) {
			return new OptionalTableUpdateOperation( request.mutationTarget(), request.update() );
		}
	}
}
