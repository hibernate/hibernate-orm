/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.type.spi.StandardSizeStrategy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies community Dialect size-strategy owners and a representative
/// inherited family.
///
/// @author Steve Ebersole
public class SizeStrategyTest {
	@Test
	void directOwnersReturnStableStandardStrategySubclasses() {
		assertStableStandardStrategy( new MySQLLegacyDialect() );
		assertStableStandardStrategy( new SQLServerLegacyDialect() );
		assertStableStandardStrategy( new SingleStoreDialect() );
		assertStableStandardStrategy( new SybaseLegacyDialect() );
		assertStableStandardStrategy( new SybaseASELegacyDialect() );
	}

	@Test
	void inheritedFamilyUsesItsExistingOwner() {
		final MySQLLegacyDialect owner = new MySQLLegacyDialect();
		final MariaDBLegacyDialect descendant = new MariaDBLegacyDialect();

		assertThat( owner.getSizeStrategy().getClass().getEnclosingClass() )
				.isEqualTo( MySQLLegacyDialect.class );
		assertThat( descendant.getSizeStrategy().getClass().getEnclosingClass() )
				.isEqualTo( MySQLLegacyDialect.class );
	}

	private static void assertStableStandardStrategy(Dialect dialect) {
		assertThat( dialect.getSizeStrategy() ).isInstanceOf( StandardSizeStrategy.class );
		assertThat( dialect.getSizeStrategy() ).isSameAs( dialect.getSizeStrategy() );
	}
}
