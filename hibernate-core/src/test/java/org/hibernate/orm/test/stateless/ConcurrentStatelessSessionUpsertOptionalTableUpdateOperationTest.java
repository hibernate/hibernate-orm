/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.jdbc.OptionalTableUpdateOperation;
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
		public MutationOperation createOptionalTableUpdateOperation(
				EntityMutationTarget mutationTarget,
				OptionalTableUpdate optionalTableUpdate,
				SessionFactoryImplementor factory) {
			return new OptionalTableUpdateOperation( mutationTarget, optionalTableUpdate, factory );
		}
	}
}
