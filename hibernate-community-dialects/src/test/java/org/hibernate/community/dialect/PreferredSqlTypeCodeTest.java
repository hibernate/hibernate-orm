/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.type.SqlTypes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies all community preferred array and Boolean type-code overrides.
///
/// @author Steve Ebersole
public class PreferredSqlTypeCodeTest {
	@Test
	void communityValuesAndVersionThresholdsRemainStable() {
		assertThat( new DB2LegacyDialect().getPreferredSqlTypeCodeForArray() ).isEqualTo( SqlTypes.XML_ARRAY );
		assertThat( new MySQLLegacyDialect( DatabaseVersion.make( 5, 6 ) ).getPreferredSqlTypeCodeForArray() )
				.isEqualTo( SqlTypes.VARBINARY );
		assertThat( new MySQLLegacyDialect( DatabaseVersion.make( 5, 7 ) ).getPreferredSqlTypeCodeForArray() )
				.isEqualTo( SqlTypes.JSON_ARRAY );
		assertThat( new OracleLegacyDialect().getPreferredSqlTypeCodeForArray() ).isEqualTo( SqlTypes.ARRAY );
		assertThat( new DerbyLegacyDialect( DatabaseVersion.make( 10, 6 ) ).getPreferredSqlTypeCodeForBoolean() )
				.isEqualTo( SqlTypes.SMALLINT );
		assertThat( new DerbyLegacyDialect( DatabaseVersion.make( 10, 7 ) ).getPreferredSqlTypeCodeForBoolean() )
				.isEqualTo( SqlTypes.BOOLEAN );
		assertThat( new FirebirdDialect( DatabaseVersion.make( 2, 5 ) ).getPreferredSqlTypeCodeForBoolean() )
				.isEqualTo( SqlTypes.BIT );
		assertThat( new FirebirdDialect( DatabaseVersion.make( 3 ) ).getPreferredSqlTypeCodeForBoolean() )
				.isEqualTo( SqlTypes.BOOLEAN );
		assertThat( new IngresDialect( DatabaseVersion.make( 9 ) ).getPreferredSqlTypeCodeForBoolean() )
				.isEqualTo( SqlTypes.BIT );
		assertThat( new IngresDialect( DatabaseVersion.make( 10 ) ).getPreferredSqlTypeCodeForBoolean() )
				.isEqualTo( SqlTypes.BOOLEAN );
	}
}
