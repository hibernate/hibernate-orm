/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete;

import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.metamodel.mapping.internal.SoftDeleteMappingImpl;

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
		assertThat( ( (SoftDeleteMappingImpl) mapping ).getDeletionIndicator() ).isEqualTo( expectedDeletedLiteralValue );
	}

	public static void verifyTimestampMapping(
			SoftDeleteMapping mapping,
			String expectedColumnName,
			String expectedTableName) {
		assertThat( mapping ).isNotNull();
		assertThat( mapping.getSoftDeleteStrategy() ).isEqualTo( SoftDeleteType.TIMESTAMP );
		assertThat( mapping.getColumnName() ).isEqualTo( expectedColumnName );
		assertThat( mapping.getTableName() ).isEqualTo( expectedTableName );
	}
}
