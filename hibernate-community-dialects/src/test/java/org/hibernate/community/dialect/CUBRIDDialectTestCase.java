/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@BaseUnitTest
public class CUBRIDDialectTestCase {

	private final Dialect dialect = new CUBRIDDialect();

	@Test
	public void testAlterColumnTypeUsesModifyWithFullDefinition() {
		assertThat( dialect.getAlterColumnTypeString( "age", "integer", "integer not null" ) )
				.isEqualTo( "modify column age integer not null" );
	}
}
