package org.hibernate.orm.test.mapping.collections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Collection;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The implicit unique key of a {@code Set} collection table must be named from the collection table,
 * not from the owner entity table. Otherwise, all the sets of an entity which use the same column names
 * get the same constraint name.
 */
@BaseUnitTest
@JiraKey("HHH-21269")
public class ElementCollectionSetUniqueKeyNameTest {
	private StandardServiceRegistry ssr;

	@BeforeEach
	public void setUp() {
		ssr = ServiceRegistryUtil.serviceRegistry();
	}

	@AfterEach
	public void tearDown() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	void uniqueKeysOfSetsWithSameColumnNamesHaveDistinctNames() {
		final var metadata = new MetadataSources( ssr )
				.addAnnotatedClass( Owner.class )
				.buildMetadata();

		final Map<String, String> uniqueKeyNames = new HashMap<>();
		for ( Collection collection : metadata.getCollectionBindings() ) {
			final var collectionTable = collection.getCollectionTable();
			for ( var uniqueKey : collectionTable.getUniqueKeys().values() ) {
				uniqueKeyNames.put( collectionTable.getName(), uniqueKey.getName() );
			}
		}

		assertThat( uniqueKeyNames ).containsOnlyKeys( "owner_tags", "owner_labels" );
		assertThat( uniqueKeyNames.get( "owner_tags" ) )
				.isNotEqualTo( uniqueKeyNames.get( "owner_labels" ) );
	}

	@Entity(name = "Owner")
	@Table(name = "owner_tbl")
	public static class Owner {
		@Id
		private Long id;

		@ElementCollection
		@CollectionTable(name = "owner_tags", joinColumns = @JoinColumn(name = "owner_id"))
		@Column(name = "val")
		private Set<String> tags;

		@ElementCollection
		@CollectionTable(name = "owner_labels", joinColumns = @JoinColumn(name = "owner_id"))
		@Column(name = "val")
		private Set<String> labels;
	}
}
