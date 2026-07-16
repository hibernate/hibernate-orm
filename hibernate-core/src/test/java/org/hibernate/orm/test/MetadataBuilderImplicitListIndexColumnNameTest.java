/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitIndexColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.MappingSettings.DEFAULT_LIST_SEMANTICS;
import static org.hibernate.cfg.MappingSettings.IMPLICIT_NAMING_STRATEGY;

@JiraKey("HHH-20696")
public class MetadataBuilderImplicitListIndexColumnNameTest {

	private static final String EXPECTED_INDEX_COLUMN_NAME = "INDEX";

	@Test
	public void generatesCorrectIndexColumnName() {
		StandardServiceRegistryBuilder srb = new StandardServiceRegistryBuilder()
				.applySetting( DEFAULT_LIST_SEMANTICS, "LIST" )
				.applySetting( IMPLICIT_NAMING_STRATEGY, CustomImplicitNamingStrategy.class.getName() );

		Metadata metadata = new MetadataSources( srb.build() )
				.addAnnotatedClasses( Entity1.class, Entity2.class )
				.buildMetadata();

		var collectionBinding = List.copyOf( metadata.getCollectionBindings() ).get(0);

		assertThat( collectionBinding ).isInstanceOf( org.hibernate.mapping.List.class );

		org.hibernate.mapping.List list = (org.hibernate.mapping.List) collectionBinding;
		assertThat( list.getIndex() ).isInstanceOf( BasicValue.class );

		BasicValue listIndex = (BasicValue) list.getIndex();
		assertThat( ((Column) listIndex.getColumn()).getName() ).isEqualTo( EXPECTED_INDEX_COLUMN_NAME );
	}

	public static class CustomImplicitNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl
	{
		@Override
		public Identifier determineListIndexColumnName(ImplicitIndexColumnNameSource source) {
			return Identifier.toIdentifier( EXPECTED_INDEX_COLUMN_NAME );
		}
	}

	@Entity
	private static class Entity1 {
		@Id private long id;
		@ManyToMany private List<Entity2> other;
	}

	@Entity
	private static class Entity2 {
		@Id private long other_id;
	}
}
