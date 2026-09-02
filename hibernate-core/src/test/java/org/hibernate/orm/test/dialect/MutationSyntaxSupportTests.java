/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hibernate.dialect.sql.ast.spi.MutationKind.DELETE;
import static org.hibernate.dialect.sql.ast.spi.MutationKind.INSERT;
import static org.hibernate.dialect.sql.ast.spi.MutationKind.UPDATE;
import static org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability.FROM_CLAUSE;
import static org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability.JOIN;
import static org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability.REQUIRES_WHERE;

/// Tests the immutable, mutation-kind-specific syntax capability profile.
///
/// @author Steve Ebersole
public class MutationSyntaxSupportTests {
	@Test
	void capabilitiesAreMultiValuedAndMutationSpecific() {
		final MutationSyntaxSupport support = MutationSyntaxSupport.builder()
				.capabilities( UPDATE, FROM_CLAUSE, JOIN )
				.capability( DELETE, JOIN )
				.build();

		assertThat( support.capabilities( UPDATE ) ).containsExactlyInAnyOrder( FROM_CLAUSE, JOIN );
		assertThat( support.capabilities( DELETE ) ).containsExactly( JOIN );
		assertThat( support.capabilities( INSERT ) ).isEmpty();
	}

	@Test
	void returnedCapabilitySetsAreImmutable() {
		final MutationSyntaxSupport support = MutationSyntaxSupport.of( UPDATE, FROM_CLAUSE );

		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> support.capabilities( UPDATE ).add( JOIN ) );
	}

	@Test
	void conditionalCapabilitySelectionCanDisableACapability() {
		final MutationSyntaxSupport support = MutationSyntaxSupport.builder()
				.capability( UPDATE, FROM_CLAUSE )
				.capability( UPDATE, FROM_CLAUSE, false )
				.build();

		assertThat( support.capabilities( UPDATE ) ).isEmpty();
	}

	@Test
	void spannerExpressesMandatoryWhereWithoutTranslatorOverrides() {
		final MutationSyntaxSupport support = new SpannerDialect().getMutationSyntaxSupport();

		assertThat( support.supports( UPDATE, REQUIRES_WHERE ) ).isTrue();
		assertThat( support.supports( DELETE, REQUIRES_WHERE ) ).isTrue();
		assertThat( support.supports( UPDATE, FROM_CLAUSE ) ).isFalse();
	}

	@Test
	void hanaPlatformAndCloudExposeDifferentNativeUpdateSyntax() {
		final MutationSyntaxSupport platform =
				new HANADialect( DatabaseVersion.make( 3 ) ).getMutationSyntaxSupport();
		final MutationSyntaxSupport cloud =
				new HANADialect( DatabaseVersion.make( 4 ) ).getMutationSyntaxSupport();

		assertThat( platform.supports( UPDATE, FROM_CLAUSE ) ).isTrue();
		assertThat( cloud.supports( UPDATE, FROM_CLAUSE ) ).isFalse();
	}
}
