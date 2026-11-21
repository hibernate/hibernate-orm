package org.hibernate.orm.test.collection.map;

import org.hibernate.boot.pipeline.internal.MetadataBuildingHelper;
import org.hibernate.boot.pipeline.internal.source.MappingSources;

import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.MapKeyFormula;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyJoinColumns;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ServiceRegistry
@JiraKey( "HHH-21053" )
public class MapKeyFormulaConflictTest {

	@Test
	void testMapKeyFormulaWithMapKeyColumn(ServiceRegistryScope scope) {
		final MappingSources sources = new MappingSources()
				.addManagedClass( FormulaAndColumnParent.class )
				.addManagedClass( Item.class );

		assertThatThrownBy( () -> MetadataBuildingHelper.buildMetadata( scope.getRegistry(), sources ) )
				.isInstanceOf( AnnotationException.class )
				.hasMessageContaining( "is annotated both '@MapKeyFormula' and '@MapKeyColumn'" );
	}

	@Test
	void testMapKeyFormulaWithMapKey(ServiceRegistryScope scope) {
		final MappingSources sources = new MappingSources()
				.addManagedClass( FormulaAndMapKeyParent.class )
				.addManagedClass( Item.class );

		assertThatThrownBy( () -> MetadataBuildingHelper.buildMetadata( scope.getRegistry(), sources ) )
				.isInstanceOf( AnnotationException.class )
				.hasMessageContaining( "is annotated both '@MapKey' and '@MapKeyFormula'" );
	}

	@Test
	void testMapKeyFormulaWithMapKeyJoinColumn(ServiceRegistryScope scope) {
		final MappingSources sources = new MappingSources()
				.addManagedClass( FormulaAndJoinColumnParent.class )
				.addManagedClass( Item.class );

		assertThatThrownBy( () -> MetadataBuildingHelper.buildMetadata( scope.getRegistry(), sources ) )
				.isInstanceOf( AnnotationException.class )
				.hasMessageContaining( "is annotated both '@MapKeyFormula' and '@MapKeyJoinColumn'" );
	}

	@Test
	void testMapKeyFormulaWithMapKeyJoinColumns(ServiceRegistryScope scope) {
		final MappingSources sources = new MappingSources()
				.addManagedClass( FormulaAndJoinColumnsParent.class )
				.addManagedClass( Item.class );

		assertThatThrownBy( () -> MetadataBuildingHelper.buildMetadata( scope.getRegistry(), sources ) )
				.isInstanceOf( AnnotationException.class )
				.hasMessageContaining( "is annotated both '@MapKeyFormula' and '@MapKeyJoinColumn'" );
	}

	@Test
	void testMapKeyFormulaWithEntityKey(ServiceRegistryScope scope) {
		final MappingSources sources = new MappingSources()
				.addManagedClass( FormulaAndEntityKeyParent.class )
				.addManagedClass( Item.class );

		assertThatThrownBy( () -> MetadataBuildingHelper.buildMetadata( scope.getRegistry(), sources ) )
				.isInstanceOf( AnnotationException.class )
				.hasMessageContaining( "is annotated '@MapKeyFormula' but its key is an entity" );
	}

	@Entity(name = "FormulaAndColumnParent")
	@Table(name = "formula_column_parent")
	public static class FormulaAndColumnParent {
		@Id
		private Long id;

		@OneToMany
		@JoinColumn(name = "parent_fk")
		@MapKeyFormula("upper(code)")
		@MapKeyColumn(name = "key_col")
		private Map<String, Item> items;
	}

	@Entity(name = "FormulaAndMapKeyParent")
	@Table(name = "formula_map_key_parent")
	public static class FormulaAndMapKeyParent {
		@Id
		private Long id;

		@OneToMany
		@JoinColumn(name = "parent_fk")
		@MapKeyFormula("upper(code)")
		@MapKey(name = "code")
		private Map<String, Item> items;
	}

	@Entity(name = "FormulaAndJoinColumnParent")
	@Table(name = "formula_join_column_parent")
	public static class FormulaAndJoinColumnParent {
		@Id
		private Long id;

		@OneToMany
		@JoinColumn(name = "parent_fk")
		@MapKeyFormula("key_fk")
		@MapKeyJoinColumn(name = "key_fk")
		private Map<Item, Item> items;
	}

	@Entity(name = "FormulaAndJoinColumnsParent")
	@Table(name = "formula_join_columns_parent")
	public static class FormulaAndJoinColumnsParent {
		@Id
		private Long id;

		@OneToMany
		@JoinColumn(name = "parent_fk")
		@MapKeyFormula("key_fk")
		@MapKeyJoinColumns(@MapKeyJoinColumn(name = "key_fk"))
		private Map<Item, Item> items;
	}

	@Entity(name = "FormulaAndEntityKeyParent")
	@Table(name = "formula_entity_key_parent")
	public static class FormulaAndEntityKeyParent {
		@Id
		private Long id;

		@OneToMany
		@JoinColumn(name = "parent_fk")
		@MapKeyFormula("key_fk")
		private Map<Item, Item> items;
	}

	@Entity(name = "Item")
	@Table(name = "map_formula_item")
	public static class Item {
		@Id
		private Long id;

		private String code;
	}
}
