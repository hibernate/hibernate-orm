/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.Collection;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ImplicitListAsListProvider;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;

/**
 * Uses the default {@value AvailableSettings#DEFAULT_LIST_SEMANTICS} value of LIST
 * and verifies the outcome
 *
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = DEFAULT_LIST_SEMANTICS,
				provider = ImplicitListAsListProvider.class
		)
)
@DomainModel( annotatedClasses = ImplicitListDefaultSemanticsTests.AnEntity.class )
public class ImplicitListDefaultSemanticsTests {
	@Test
	void verifyModel(DomainModelScope scope) {
		scope.withHierarchy( AnEntity.class, (descriptor) -> {
			final Property implicitList = descriptor.getProperty( "implicitList" );
			// this is the change related to AvailableSettings#DEFAULT_LIST_SEMANTICS
			// previous versions interpreted these as BAG
			assertThat( implicitList.getValue() ).isInstanceOf( org.hibernate.mapping.List.class );

			final Property implicitBag = descriptor.getProperty( "implicitBag" );
			assertThat( implicitBag.getValue() ).isInstanceOf( Bag.class );

			final Property explicitBag = descriptor.getProperty( "explicitBag" );
			assertThat( explicitBag.getValue() ).isInstanceOf( Bag.class );

			final Property explicitList = descriptor.getProperty( "explicitList" );
			assertThat( explicitList.getValue() ).isInstanceOf( org.hibernate.mapping.List.class );
		} );
	}

	@Entity( name = "AnEntity" )
	@Table( name = "t_entity" )
	public static class AnEntity {
		@Id
		private Integer id;
		@Basic
		private String name;

		@ElementCollection
		private List<String> implicitList;

		@ElementCollection
		private Collection<String> implicitBag;

		@ElementCollection
		@org.hibernate.annotations.Bag
		private List<String> explicitBag;

		@ElementCollection
		@OrderColumn( name = "explicit_list_position" )
		private List<String> explicitList;

		private AnEntity() {
			// for use by Hibernate
		}

		public AnEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
