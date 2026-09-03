/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schema.pkg;

import org.hibernate.mapping.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Jira("https://hibernate.atlassian.net/browse/HHH-18977")
@DomainModel(annotatedClasses = PackageLevelDefaultSchemaEntity.class)
public class PackageLevelDefaultSchemaTest {
	@Test
	void verifyPackageLevelDefaultSchema(DomainModelScope scope) {
		scope.withHierarchy( PackageLevelDefaultSchemaEntity.class, (descriptor) -> {
			final Table table = descriptor.getTable();
			assertThat( table.getSchema() )
					.as( "@DefaultSchema on package-info.java should set the schema" )
					.isEqualTo( "pkg_schema" );
		} );
	}
}
