/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityStrategies;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/// Verifies that community Dialects without a row-level-security override use
/// the supported stock strategy.
///
/// @author Steve Ebersole
/// @since 8.0
public class RowLevelSecuritySupportTest {
	@Test
	void communityDialectsUseTheUnsupportedStockStrategy() {
		for ( var dialect : List.of(
				new SQLiteDialect(),
				new TiDBDialect(),
				new InformixDialect(),
				new FirebirdDialect() ) ) {
			assertSame( RowLevelSecurityStrategies.none(), dialect.getRowLevelSecurity() );
		}
	}
}
