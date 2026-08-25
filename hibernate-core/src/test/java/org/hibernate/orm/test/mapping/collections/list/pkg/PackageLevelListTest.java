/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.list.pkg;

import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Jira("https://hibernate.atlassian.net/browse/HHH-15066")
@DomainModel(annotatedClasses = PackageLevelListEntity.class)
public class PackageLevelListTest {
	@Test
	void verifyPackageLevelList(DomainModelScope scope) {
		scope.withHierarchy( PackageLevelListEntity.class, (descriptor) -> {
			final Property names = descriptor.getProperty( "names" );
			assertThat( names.getValue() )
					.as( "@List on package-info.java should force LIST semantics" )
					.isInstanceOf( org.hibernate.mapping.List.class );
		} );
	}
}
