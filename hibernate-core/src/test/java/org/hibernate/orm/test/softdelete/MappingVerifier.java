/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete;

import org.hibernate.metamodel.mapping.SoftDeleteMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class MappingVerifier {
	public static void verifyMapping(
			SoftDeleteMapping mapping,
			String expectedColumnName,
			String expectedTableName,
			Object expectedDeletedLiteralValue) {
		assertThat( mapping ).isNotNull();
		assertThat( mapping.getColumnName() ).isEqualTo( expectedColumnName );
		assertThat( mapping.getTableName() ).isEqualTo( expectedTableName );
		assertThat( mapping.getDeletedLiteralValue() ).isEqualTo( expectedDeletedLiteralValue );
	}
}
