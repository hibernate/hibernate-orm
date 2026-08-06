/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitIndexColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ImplicitListAsListProvider;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.MappingSettings.DEFAULT_LIST_SEMANTICS;
import static org.hibernate.cfg.MappingSettings.IMPLICIT_NAMING_STRATEGY;

@JiraKey("HHH-20696")
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = DEFAULT_LIST_SEMANTICS,
				provider = ImplicitListAsListProvider.class
		),
		settings = @Setting(
				name = IMPLICIT_NAMING_STRATEGY,
				value = "org.hibernate.orm.test.MetadataBuilderImplicitListIndexColumnNameTest$CustomImplicitNamingStrategy"
		)
)
@DomainModel(
		annotatedClasses = {
				MetadataBuilderImplicitListIndexColumnNameTest.Entity1.class,
				MetadataBuilderImplicitListIndexColumnNameTest.Entity2.class
		}
)
public class MetadataBuilderImplicitListIndexColumnNameTest {

	private static final String EXPECTED_INDEX_COLUMN_NAME = "INDEX";

	@Test
	public void generatesCorrectIndexColumnName(final DomainModelScope scope) {
		Metadata metadata = scope.getDomainModel();

		var collectionBinding = List.copyOf( metadata.getCollectionBindings() ).get(0);
		assertThat( collectionBinding ).isInstanceOf( org.hibernate.mapping.List.class );

		org.hibernate.mapping.List list = (org.hibernate.mapping.List) collectionBinding;
		assertThat( list.getIndex() ).isInstanceOf( BasicValue.class );

		BasicValue listIndex = (BasicValue) list.getIndex();
		assertThat( ((Column) listIndex.getColumn()).getName() ).isEqualTo( EXPECTED_INDEX_COLUMN_NAME );
	}

	@SuppressWarnings("unused")
	public static class CustomImplicitNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl
	{
		@Override
		public Identifier determineListIndexColumnName(ImplicitIndexColumnNameSource source) {
			return Identifier.toIdentifier( EXPECTED_INDEX_COLUMN_NAME );
		}
	}

	@SuppressWarnings("unused")
	@Entity
	static class Entity1 {
		@Id private long id;
		@ManyToMany private List<Entity2> other;
	}

	@SuppressWarnings("unused")
	@Entity
	static class Entity2 {
		@Id private long other_id;
	}
}
