/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.bag.pkg;

import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ImplicitListAsListProvider;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;

@Jira("https://hibernate.atlassian.net/browse/HHH-15066")
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = DEFAULT_LIST_SEMANTICS,
				provider = ImplicitListAsListProvider.class
		)
)
@DomainModel(annotatedClasses = PackageLevelBagEntity.class)
public class PackageLevelBagTest {
	@Test
	void verifyPackageLevelBag(DomainModelScope scope) {
		scope.withHierarchy( PackageLevelBagEntity.class, (descriptor) -> {
			final Property names = descriptor.getProperty( "names" );
			assertThat( names.getValue() )
					.as( "@Bag on package-info.java should force BAG semantics even with LIST default" )
					.isInstanceOf( Bag.class );
		} );
	}
}
