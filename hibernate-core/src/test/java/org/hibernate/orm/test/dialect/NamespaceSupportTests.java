/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;


import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupports;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/// Verifies catalog and schema lifecycle strategy selection and API shape.
///
/// @author Steve Ebersole
public class NamespaceSupportTests {
	@Test
	void standardProfilesRenderOrderedPluralCommands() {
		final NamespaceSupport standard = NamespaceSupports.standard();
		assertThat( standard.canCreateCatalog() ).isFalse();
		assertThat( standard.canCreateSchema() ).isTrue();
		assertThat( standard.getCreateSchemaCommands( "orm" ) )
				.containsExactly( "create schema orm" );
		assertThat( standard.getDropSchemaCommands( "orm" ) )
				.containsExactly( "drop schema orm" );

		final NamespaceSupport guarded = NamespaceSupports.standard( true, true );
		assertThat( guarded.getCreateSchemaCommands( "orm" ) )
				.containsExactly( "create schema if not exists orm" );
		assertThat( guarded.getDropSchemaCommands( "orm" ) )
				.containsExactly( "drop schema if exists orm" );
	}

	@Test
	void stockProfilesPreserveDisabledNamespaceBehavior() {
		final NamespaceSupport none = NamespaceSupports.none();
		assertThat( none.canCreateCatalog() ).isFalse();
		assertThat( none.canCreateSchema() ).isFalse();
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> none.getCreateCatalogCommands( "orm" ) );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> none.getCreateSchemaCommands( "orm" ) );

		final NamespaceSupport catalogs = NamespaceSupports.catalogsAsDatabases();
		assertThat( catalogs.canCreateCatalog() ).isTrue();
		assertThat( catalogs.canCreateSchema() ).isFalse();
		assertThat( catalogs.getCreateCatalogCommands( "orm" ) )
				.containsExactly( "create database orm" );
		assertThat( catalogs.getDropCatalogCommands( "orm" ) )
				.containsExactly( "drop database orm" );
	}

	@Test
	void maintainedDialectsSelectCompleteProfiles() {
		assertThat( new H2Dialect().getNamespaceSupport().getCreateSchemaCommands( "orm" ) )
				.containsExactly( "create schema if not exists orm" );
		assertThat( new MySQLDialect().getNamespaceSupport().getCreateCatalogCommands( "orm" ) )
				.containsExactly( "create database orm" );
		assertThat( new OracleDialect().getNamespaceSupport().canCreateSchema() ).isFalse();
	}

}
