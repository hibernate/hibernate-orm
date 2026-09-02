/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.type.spi.NationalizationSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies the complete set of community nationalization supply points.
///
/// @author Steve Ebersole
public class NationalizationSupportTest {
	@Test
	void communityProfilesPreserveTheirEffectiveValues() {
		assertThat( new DerbyDialect().getNationalizationSupport() ).isEqualTo( NationalizationSupport.IMPLICIT );
		assertThat( new DerbyLegacyDialect().getNationalizationSupport() ).isEqualTo( NationalizationSupport.IMPLICIT );
		assertThat( new PostgreSQLLegacyDialect().getNationalizationSupport() ).isEqualTo( NationalizationSupport.IMPLICIT );
		assertThat( new InterSystemsIRISDialect().getNationalizationSupport() ).isEqualTo( NationalizationSupport.IMPLICIT );
		assertThat( new MariaDBLegacyDialect().getNationalizationSupport() ).isEqualTo( NationalizationSupport.IMPLICIT );
		assertThat( new FirebirdDialect().getNationalizationSupport() ).isEqualTo( NationalizationSupport.IMPLICIT );
		assertThat( new GaussDBDialect().getNationalizationSupport() ).isEqualTo( NationalizationSupport.IMPLICIT );
		assertThat( new AltibaseDialect().getNationalizationSupport() ).isEqualTo( NationalizationSupport.IMPLICIT );
		assertThat( new CockroachLegacyDialect().getNationalizationSupport() ).isEqualTo( NationalizationSupport.IMPLICIT );
		assertThat( new SQLiteDialect().getNationalizationSupport() ).isEqualTo( NationalizationSupport.IMPLICIT );
		assertThat( new InformixDialect().supportsNationalizedMethods() ).isFalse();
		assertThat( new DB2LegacyDialect().supportsNationalizedMethods() ).isFalse();
	}
}
