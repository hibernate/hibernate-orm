/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.sql.ast.spi.SyntheticTableGroupSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies the community Dialects using the focused synthetic-table-group
/// strategy.
///
/// @author Steve Ebersole
public class SyntheticTableGroupSupportTest {
	@Test
	void dialectsUseTheCanonicalLiteralStrategy() {
		assertThat( new InformixDialect().getSyntheticTableGroupSupport() )
				.isSameAs( SyntheticTableGroupSupport.SELECT_ONE_FOR_LITERALS );
		assertThat( new IngresDialect().getSyntheticTableGroupSupport() )
				.isSameAs( SyntheticTableGroupSupport.SELECT_ONE_FOR_LITERALS );
		assertThat( new SybaseLegacyDialect().getSyntheticTableGroupSupport() )
				.isSameAs( SyntheticTableGroupSupport.SELECT_ONE_FOR_LITERALS );
	}
}
