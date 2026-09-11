/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.bag.typelevel;

import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Bag;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ImplicitListAsListProvider;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;

@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = DEFAULT_LIST_SEMANTICS,
				provider = ImplicitListAsListProvider.class
		)
)
@DomainModel( annotatedClasses = TypeLevelBagTests.TypeLevelBagEntity.class )
class TypeLevelBagTests {
	@Test
	void typeLevelBagActsAsDefault(DomainModelScope scope) {
		scope.withHierarchy( TypeLevelBagEntity.class, (descriptor) -> {
			final Property implicitBag = descriptor.getProperty( "implicitBag" );
			assertThat( implicitBag.getValue() ).isInstanceOf( org.hibernate.mapping.Bag.class );

			final Property explicitList = descriptor.getProperty( "explicitList" );
			assertThat( explicitList.getValue() ).isInstanceOf( org.hibernate.mapping.List.class );

			final Property set = descriptor.getProperty( "set" );
			assertThat( set.getValue() ).isInstanceOf( org.hibernate.mapping.Set.class );
		} );
	}

	@Bag
	@Entity( name = "TypeLevelBagEntity" )
	static class TypeLevelBagEntity {
		@Id
		private Integer id;

		@ElementCollection
		private List<String> implicitBag;

		@ElementCollection
		@OrderColumn( name = "explicit_list_position" )
		private List<String> explicitList;

		@ElementCollection
		private Set<String> set;
	}
}
