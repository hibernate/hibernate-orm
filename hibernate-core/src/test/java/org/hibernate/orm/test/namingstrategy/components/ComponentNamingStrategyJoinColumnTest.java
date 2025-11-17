/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy.components;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Column;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@Jira("https://hibernate.atlassian.net/browse/HHH-11826")
public class ComponentNamingStrategyJoinColumnTest {
	@Test
	public void testNamingComponentPath() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();

		try {
			final MetadataSources ms = new MetadataSources( ssr )
					.addAnnotatedClass( BaseEntity.class )
					.addAnnotatedClass( CollectionWrapper.class )
					.addAnnotatedClass( CollectionItem.class )
					.addAnnotatedClass( ToOneEntity.class );
			final Metadata metadata = ms.getMetadataBuilder()
					.applyImplicitNamingStrategy( ImplicitNamingStrategyComponentPathImpl.INSTANCE )
					.build();

			final org.hibernate.mapping.Collection collection = metadata.getCollectionBinding(
					BaseEntity.class.getName() + '.' + "collectionWrapper.items"
			);

			final org.hibernate.mapping.Table table = collection.getCollectionTable();
			assertThat( table.getName() ).isEqualTo( "BaseEntity_collectionWrapper_items" );
			assertThat( collection.getOwner().getKey().getColumnSpan() ).isEqualTo( 1 );
			assertThat( collection.getKey().getColumns().get( 0 ).getName() ).isEqualTo( "BaseEntity_id" );
			assertThat( table.getColumns().stream().map( Column::getName ) ).contains(
					"BaseEntity_id",
					"collectionWrapper_items_name",
					"collectionWrapper_items_toOne_id"
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity(name = "BaseEntity")
	static class BaseEntity {
		@Id
		private Long id;

		@Embedded
		private CollectionWrapper collectionWrapper;
	}

	@Embeddable
	static class CollectionWrapper {
		@ElementCollection
		private List<CollectionItem> items;
	}

	@Embeddable
	static class CollectionItem {
		private String name;

		@ManyToOne
		private ToOneEntity toOne;
	}

	@Entity(name = "ToOneEntity")
	static class ToOneEntity {
		@Id
		private Long id;
	}
}
