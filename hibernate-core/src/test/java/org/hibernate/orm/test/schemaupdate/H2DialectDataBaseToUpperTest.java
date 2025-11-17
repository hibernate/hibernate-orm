/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.EnumSet;

@RequiresDialect(H2Dialect.class)
@JiraKey(value = "HHH-13597")
public class H2DialectDataBaseToUpperTest {
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testToUpper(boolean toUpper) {
		try (var registry = createServiceRegistry(toUpper)) {
			final Metadata metadata = new MetadataSources( registry ).buildMetadata();
			new SchemaUpdate().setHaltOnError( true ).execute( EnumSet.of( TargetType.DATABASE ), metadata );
		}
	}

	private StandardServiceRegistry createServiceRegistry(boolean toUpper) {
		return ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting(
						AvailableSettings.URL,
						"jdbc:h2:mem:databaseToUpper;DATABASE_TO_UPPER=" + toUpper + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
				)
				.build();
	}
}
