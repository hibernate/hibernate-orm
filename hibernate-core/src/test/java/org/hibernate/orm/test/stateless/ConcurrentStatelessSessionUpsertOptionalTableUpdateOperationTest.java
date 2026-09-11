/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.jdbc.OptionalTableUpdateOperation;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.JdbcSettings.DIALECT;


@ServiceRegistry(settings = {
		@Setting(name = DIALECT,
				value = "org.hibernate.orm.test.stateless.ConcurrentStatelessSessionUpsertOptionalTableUpdateOperationTest$TestDialect")
})
@RequiresDialect(H2Dialect.class)
public class ConcurrentStatelessSessionUpsertOptionalTableUpdateOperationTest extends ConcurrentStatelessSessionUpsertTest {

	public static class TestDialect extends H2Dialect {

		public TestDialect(DialectResolutionInfo info) {
			super( info );
		}

		public TestDialect() {
		}

		public TestDialect(DatabaseVersion version) {
			super( version );
		}

		@Override
		public MutationOperation createOptionalTableUpdateOperation(OptionalTableUpdateOperationRequest request) {
			return new OptionalTableUpdateOperation( request.mutationTarget(), request.update() );
		}
	}
}
