/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.namespace.spi.NamespaceSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies provider-owned namespace lifecycle strategies in community Dialects.
///
/// @author Steve Ebersole
public class NamespaceSupportTest {
	@Test
	void informixPreservesAuthorizationCreateAndNoOpDropCommands() {
		final NamespaceSupport support = new InformixDialect().getNamespaceSupport();
		assertThat( support.canCreateCatalog() ).isFalse();
		assertThat( support.canCreateSchema() ).isTrue();
		assertThat( support.getCreateSchemaCommands( "orm" ) )
				.containsExactly( "create schema authorization orm" );
		assertThat( support.getDropSchemaCommands( "orm" ) ).containsExactly( "" );
	}

	@Test
	void derbyAndDb2LegacyPreserveRestrictDropCommands() {
		assertThat( new DerbyDialect().getNamespaceSupport().getDropSchemaCommands( "orm" ) )
				.containsExactly( "drop schema orm restrict" );
		assertThat( new DB2LegacyDialect().getNamespaceSupport().getDropSchemaCommands( "orm" ) )
				.containsExactly( "drop schema orm restrict" );
	}

	@Test
	void disabledCommunityProfilesDoNotAdvertiseLifecycleSupport() {
		assertThat( new AltibaseDialect().getNamespaceSupport().canCreateSchema() ).isFalse();
		assertThat( new SybaseLegacyDialect().getNamespaceSupport().canCreateSchema() ).isFalse();
	}
}
