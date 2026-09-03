/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.List;

import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ImplicitListAsListProvider;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;

@Jira("https://hibernate.atlassian.net/browse/HHH-15066")
public class TypeLevelBagAndListTests {

	@ServiceRegistry(
			settingProviders = @SettingProvider(
					settingName = DEFAULT_LIST_SEMANTICS,
					provider = ImplicitListAsListProvider.class
			)
	)
	@DomainModel(annotatedClasses = BagEntity.class)
	@Test
	void verifyTypeLevelBag(DomainModelScope scope) {
		scope.withHierarchy( BagEntity.class, (descriptor) -> {
			final Property names = descriptor.getProperty( "names" );
			assertThat( names.getValue() ).isInstanceOf( Bag.class );
		} );
	}

	@DomainModel(annotatedClasses = ListEntity.class)
	@Test
	void verifyTypeLevelList(DomainModelScope scope) {
		scope.withHierarchy( ListEntity.class, (descriptor) -> {
			final Property names = descriptor.getProperty( "names" );
			assertThat( names.getValue() ).isInstanceOf( org.hibernate.mapping.List.class );
		} );
	}

	@org.hibernate.annotations.Bag
	@Entity(name = "BagEntity")
	@Table(name = "bag_entity")
	public static class BagEntity {
		@Id
		private Integer id;

		@ElementCollection
		private List<String> names;
	}

	@org.hibernate.annotations.List
	@Entity(name = "ListEntity")
	@Table(name = "list_entity")
	public static class ListEntity {
		@Id
		private Integer id;

		@ElementCollection
		private List<String> names;
	}
}
