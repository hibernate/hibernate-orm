/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.dialect.Dialect;
import org.hibernate.orm.test.dialect.resolver.TestingDialectResolutionInfo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.DatabaseVersion.NO_VERSION;

public class SingleStoreDialectTest {

	@Test
	public void testSpecificProperties() {
		final Dialect dialect = resolveDialect( "SingleStore", values -> {
			values.put( SingleStoreDialect.SINGLE_STORE_TABLE_TYPE, "rowStOre " );
			values.put( SingleStoreDialect.SINGLE_STORE_FOR_UPDATE_LOCK_ENABLED, "true" );
		} );

		assertThat( dialect ).isInstanceOf( SingleStoreDialect.class );
		SingleStoreDialect singleStoreDialect = (SingleStoreDialect) dialect;
		assertThat( singleStoreDialect.getExplicitTableType() ).isEqualTo( SingleStoreDialect.SingleStoreTableType.ROWSTORE );
		assertThat( singleStoreDialect.isForUpdateLockingEnabled() ).isTrue();
	}

	private static Dialect resolveDialect(String productName, Consumer<Map<String, Object>> configurationProvider) {
		final Map<String, Object> configurationValues = new HashMap<>();
		configurationProvider.accept( configurationValues );
		final TestingDialectResolutionInfo info = TestingDialectResolutionInfo.forDatabaseInfo( productName,
																								null,
																								NO_VERSION,
																								NO_VERSION,
																								configurationValues
		);

		assertThat( info.getDatabaseMetadata() ).isNull();

		return new CommunityDialectResolver().resolveDialect( info );
	}
}
