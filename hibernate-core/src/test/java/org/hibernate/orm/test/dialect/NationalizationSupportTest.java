/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.sql.Types;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.type.spi.NationalizationSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Verifies the independent database and JDBC-driver nationalization contracts.
///
/// @author Steve Ebersole
public class NationalizationSupportTest {
	@Test
	void profilesPreserveTheirJdbcVariantCodes() {
		assertVariantCodes(
				NationalizationSupport.IMPLICIT,
				Types.CHAR,
				Types.VARCHAR,
				Types.LONGVARCHAR,
				Types.CLOB
		);
		assertVariantCodes(
				NationalizationSupport.EXPLICIT,
				Types.NCHAR,
				Types.NVARCHAR,
				Types.LONGNVARCHAR,
				Types.NCLOB
		);
		assertThatThrownBy( NationalizationSupport.UNSUPPORTED::getCharVariantCode )
				.isInstanceOf( UnsupportedOperationException.class );
		assertThatThrownBy( NationalizationSupport.UNSUPPORTED::getVarcharVariantCode )
				.isInstanceOf( UnsupportedOperationException.class );
		assertThatThrownBy( NationalizationSupport.UNSUPPORTED::getLongVarcharVariantCode )
				.isInstanceOf( UnsupportedOperationException.class );
		assertThatThrownBy( NationalizationSupport.UNSUPPORTED::getClobVariantCode )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	void databaseAndDriverCapabilitiesAreIndependent() {
		final Dialect dialect = new Dialect( DatabaseVersion.make( 1 ) ) {
			@Override
			public NationalizationSupport getNationalizationSupport() {
				return NationalizationSupport.IMPLICIT;
			}

			@Override
			public boolean supportsNationalizedMethods() {
				return false;
			}
		};

		assertThat( dialect.getNationalizationSupport() ).isEqualTo( NationalizationSupport.IMPLICIT );
		assertThat( dialect.supportsNationalizedMethods() ).isFalse();
		assertThat( new Dialect( DatabaseVersion.make( 1 ) ) {}.getNationalizationSupport() )
				.isEqualTo( NationalizationSupport.EXPLICIT );
		assertThat( new Dialect( DatabaseVersion.make( 1 ) ) {}.supportsNationalizedMethods() ).isTrue();
		assertThat( new DB2Dialect().supportsNationalizedMethods() ).isFalse();
		assertThat( new PostgreSQLDialect().getNationalizationSupport() )
				.isEqualTo( NationalizationSupport.IMPLICIT );
	}

	private static void assertVariantCodes(
			NationalizationSupport support,
			int charCode,
			int varcharCode,
			int longVarcharCode,
			int clobCode) {
		assertThat( support.getCharVariantCode() ).isEqualTo( charCode );
		assertThat( support.getVarcharVariantCode() ).isEqualTo( varcharCode );
		assertThat( support.getLongVarcharVariantCode() ).isEqualTo( longVarcharCode );
		assertThat( support.getClobVariantCode() ).isEqualTo( clobCode );
	}
}
