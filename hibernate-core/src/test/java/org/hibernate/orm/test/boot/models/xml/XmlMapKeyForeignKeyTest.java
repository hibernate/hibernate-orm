package org.hibernate.orm.test.boot.models.xml;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ForeignKey;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyJoinColumn;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies that orm.xml `<map-key-foreign-key/>` reaches the boot model, the same way
/// `@MapKeyJoinColumns(foreignKey = ...)` does, for a map keyed by an entity.
@JiraKey("HHH-21254")
public class XmlMapKeyForeignKeyTest {

	@Test
	void mapKeyJoinColumnNameIsApplied() {
		final Collection prices = bindMap( "<map-key-join-column name=\"category_fk\"/>" );

		assertThat( columnNames( prices ) ).contains( "category_fk" );
	}

	@Test
	void nestedForeignKeyNameIsApplied() {
		final Collection prices = bindMap( """
				<map-key-join-column name="category_fk">
					<foreign-key name="fk_prices_category_nested"/>
				</map-key-join-column>
				""" );

		assertThat( foreignKeyNames( prices ) ).contains( "fk_prices_category_nested" );
	}

	@Test
	void mapKeyForeignKeyNameIsApplied() {
		final Collection prices = bindMap( """
				<map-key-join-column name="category_fk"/>
				<map-key-foreign-key name="fk_prices_category"/>
				""" );

		assertThat( foreignKeyNames( prices ) ).contains( "fk_prices_category" );
	}

	@Test
	void mapKeyForeignKeyNameWithoutJoinColumnIsApplied() {
		final Collection prices = bindMap( "<map-key-foreign-key name=\"fk_prices_category\"/>" );

		assertThat( foreignKeyNames( prices ) ).contains( "fk_prices_category" );
	}

	@Test
	void mapKeyForeignKeyOverrideKeepsAnnotatedJoinColumn() {
		final String xml = """
				<entity-mappings xmlns="https://www.hibernate.org/xsd/orm/mapping" version="8.0">
					<entity class="%s" metadata-complete="false">
						<attributes>
							<element-collection name="prices">
								<map-key-foreign-key name="fk_prices_category"/>
							</element-collection>
						</attributes>
					</entity>
				</entity-mappings>
				""".formatted( AnnotatedItem.class.getName() );
		final Collection prices = buildMetadata( xml, AnnotatedCategory.class, AnnotatedItem.class )
				.getCollectionBinding( AnnotatedItem.class.getName() + ".prices" );
		assertThat( prices ).as( "Map declared via annotations" ).isNotNull();

		// from the annotation: must survive the XML override
		assertThat( columnNames( prices ) ).contains( "cat_id" );
		// from the XML
		assertThat( foreignKeyNames( prices ) ).contains( "fk_prices_category" );
	}

	private static Collection bindMap(String mapKeyElements) {
		final String xml = """
				<entity-mappings xmlns="https://www.hibernate.org/xsd/orm/mapping" version="7.0">
					<entity class="%s" name="Category" access="FIELD" metadata-complete="true">
						<table name="category"/>
						<attributes>
							<id name="id"/>
						</attributes>
					</entity>
					<entity class="%s" name="Item" access="FIELD" metadata-complete="true">
						<table name="item"/>
						<attributes>
							<id name="id"/>
							<element-collection name="prices">
								%s
								<column name="price"/>
								<collection-table name="item_prices"/>
							</element-collection>
						</attributes>
					</entity>
				</entity-mappings>
				""".formatted( Category.class.getName(), Item.class.getName(), mapKeyElements );
		final Collection prices = buildMetadata( xml ).getCollectionBinding( Item.class.getName() + ".prices" );
		assertThat( prices ).as( "Map declared in orm.xml" ).isNotNull();
		return prices;
	}

	private static Metadata buildMetadata(String xml, Class<?>... annotatedClasses) {
		try ( var registry = new StandardServiceRegistryBuilder()
				.applySetting( "hibernate.dialect", H2Dialect.class.getName() )
				.applySetting( "hibernate.boot.allow_jdbc_metadata_access", false )
				.applySetting( "hibernate.validate_xml", true )
				.build() ) {
			final MetadataSources sources = new MetadataSources( registry );
			for ( final Class<?> annotatedClass : annotatedClasses ) {
				sources.addAnnotatedClass( annotatedClass );
			}
			return sources
					.addInputStream( new ByteArrayInputStream( xml.getBytes( StandardCharsets.UTF_8 ) ) )
					.buildMetadata();
		}
	}

	private static List<String> columnNames(Collection collection) {
		final List<String> names = new ArrayList<>();
		for ( final var column : collection.getCollectionTable().getColumns() ) {
			names.add( column.getName() );
		}
		return names;
	}

	private static List<String> foreignKeyNames(Collection collection) {
		final List<String> names = new ArrayList<>();
		for ( final ForeignKey foreignKey : collection.getCollectionTable().getForeignKeyCollection() ) {
			names.add( foreignKey.getName() );
		}
		return names;
	}

	public static class Category {
		Long id;
	}

	public static class Item {
		Long id;
		Map<Category, String> prices;
	}

	@Entity(name = "AnnotatedCategory")
	public static class AnnotatedCategory {
		@Id
		Long id;
	}

	@Entity(name = "AnnotatedItem")
	public static class AnnotatedItem {
		@Id
		Long id;

		@ElementCollection
		@CollectionTable(name = "annotated_item_prices")
		@MapKeyJoinColumn(name = "cat_id")
		@Column(name = "price")
		Map<AnnotatedCategory, String> prices;
	}
}
