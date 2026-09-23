/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Formula;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.mapping.PhysicalTable;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Explicit collection roles resolve local columns independently of literal column names.
///
/// @author Steve Ebersole
@BaseUnitTest
class CollectionRoleColumnReferenceTest {
	@Test
	void basicMapRolesAndOwnerColumn() {
		inspect( BasicMap.class, table -> {
			assertThat( table.getIndexes().get( "roles_idx" ).getSelectables() )
					.extracting( org.hibernate.mapping.Selectable::getText ).containsExactly( "p_map_key", "p_map_value", "p_owner_fk" );
			assertThat( table.getUniqueKeys().get( "roles_uk" ).getColumns() )
					.extracting( org.hibernate.mapping.Column::getName ).containsExactly( "p_map_key", "p_map_value" );
			assertThat( table.getIndexes().get( "roles_idx" ).getSelectableOrderMap().values() ).contains( "desc" );
		} );
	}

	@Test
	void embeddedMapPaths() {
		inspect( EmbeddedMap.class, table -> {
			assertThat( table.getIndexes().get( "paths_idx" ).getSelectables() )
					.extracting( org.hibernate.mapping.Selectable::getText ).containsExactly( "p_key_code", "p_city_col" );
			assertThat( table.getUniqueKeys().get( "paths_uk" ).getColumns() )
					.extracting( org.hibernate.mapping.Column::getName ).containsExactly( "p_key_code", "p_city_col" );
		} );
	}

	@Test
	void roleSyntaxWinsAndQuotingEscapesIt() {
		inspect( Escaped.class, table -> {
			assertThat( table.getIndexes().get( "role_idx" ).getSelectables() )
					.extracting( org.hibernate.mapping.Selectable::getText ).containsExactly( "p_city_col" );
			final var literal = (org.hibernate.mapping.Column) table.getIndexes().get( "literal_idx" ).getSelectables().get( 0 );
			assertThat( literal.getName() ).isEqualTo( "p_{element}.city" );
			assertThat( literal.isQuoted() ).isTrue();
			assertThat( table.getUniqueKeys().get( "literal_uk" ).getColumns() ).containsExactly( literal );
			assertThat( table.getUniqueKeys().get( "role_uk" ).getColumns() )
					.extracting( org.hibernate.mapping.Column::getName ).containsExactly( "p_city_col" );
		} );
	}

	@Test
	void entityMapRolesUseLocalForeignKeys() {
		inspect( EntityMap.class, table -> {
			assertThat( table.getIndexes().get( "entity_roles_idx" ).getSelectables() )
					.extracting( org.hibernate.mapping.Selectable::getText ).containsExactly( "p_key_fk", "p_element_fk" );
			assertThat( table.getUniqueKeys().get( "entity_roles_uk" ).getColumns() )
					.extracting( org.hibernate.mapping.Column::getName ).containsExactly( "p_key_fk", "p_element_fk" );
		}, KeyEntity.class, Target.class );
	}

	@Test
	void unnamedJoinTableRetainsItsIndexDeclaration() {
		inspect( UnnamedJoin.class, table -> {
			assertThat( table.getIndexes().get( "join_idx" ).getSelectables() ).hasSize( 1 );
		}, Target.class );
	}

	@Test void keyRequiresMap() { rejected( InvalidKey.class, "{key} requires a map", ".indexes[0]" ); }
	@Test void noAssociationTraversal() { rejected( Traversal.class, "only supported through embeddables", ".uniqueConstraints[0]", Target.class ); }
	@Test void noCompositeExpansion() { rejected( Composite.class, "exactly one column", ".indexes[0]" ); }
	@Test void malformedPath() { rejected( Malformed.class, "malformed collection role path", ".uniqueConstraints[0]" ); }
	@Test void noUnprefixedAttributeGuessing() { rejected( Unprefixed.class, "unknown column 'city'", ".indexes[0]" ); }
	@Test void unknownRoleAttribute() { rejected( Unknown.class, "unknown collection attribute", ".indexes[0]" ); }
	@Test void formulasAreRejected() { rejected( FormulaCollection.class, "contains a formula", ".uniqueConstraints[0]" ); }
	@Test
	void borrowedMapKeyCannotSelectTargetTableColumn() {
		assertThatThrownBy( () -> inspect( BorrowedKey.class, table -> {}, Target.class ) )
				.isInstanceOf( AnnotationException.class )
				.hasMessageContaining( "{key}" ).hasMessageContaining( ".indexes[0]" )
				.hasMessageMatching( "(?s).*(formula|actual column on the declaring table).*" );
	}
	@Test void markersRequireCollectionContext() { rejected( EntityMarker.class, "requires a collection-table", ".uniqueConstraints[0]" ); }

	private void rejected(Class<?> owner, String reason, String location, Class<?>... related) {
		assertThatThrownBy( () -> inspect( owner, table -> {}, related ) )
				.isInstanceOf( AnnotationException.class ).hasMessageContaining( reason ).hasMessageContaining( location );
	}

	private void inspect(Class<?> owner, Consumer<PhysicalTable> assertion, Class<?>... related) {
		final var physical = new IndexColumnResolutionTest.Prefix();
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var sources = new MappingSources().addManagedClass( owner );
			for ( var type : related ) { sources.addManagedClass( type ); }
			final Metadata metadata = MetadataBuildingTestHelper.buildMetadataWithNaming(
					registry, sources, new StandardImplicitNamingStrategy(), physical );
			final var table = (PhysicalTable) metadata.getCollectionBinding( owner.getName() + ".values" ).getCollectionTable();
			assertion.accept( table );
			for ( var index : table.getIndexes().values() ) {
				for ( var selectable : index.getSelectables() ) {
					final var column = (org.hibernate.mapping.Column) selectable;
					assertThat( column ).isSameAs( table.getColumn( column ) );
				}
			}
			assertThat( physical.columns.stream().filter( "map_value"::equals ).count() ).isLessThanOrEqualTo( 1 );
		}
	}

	@Entity
	static class BasicMap {
		@Id long id;
		@ElementCollection @MapKeyColumn(name = "map_key") @Column(name = "map_value")
		@CollectionTable(joinColumns = @JoinColumn(name = "owner_fk"),
				indexes = @Index(name = "roles_idx", columnList = "{key}, {element} desc, owner_fk"),
				uniqueConstraints = @UniqueConstraint(name = "roles_uk", columnNames = {"{key}", "{element}"}))
		Map<String, String> values;
	}
	@Embeddable static class Key { @Column(name = "key_code") String code; }
	@Embeddable static class Address { @Column(name = "city_col") String city; String street; }
	@Embeddable static class Details { @Embedded Address address; }
	@Entity
	static class EmbeddedMap {
		@Id long id;
		@ElementCollection @CollectionTable(
				indexes = @Index(name = "paths_idx", columnList = "{key}.code, {element}.address.city"),
				uniqueConstraints = @UniqueConstraint(name = "paths_uk", columnNames = {"{key}.code", "{element}.address.city"}))
		Map<Key, Details> values;
	}
	@Embeddable static class EscapedValue {
		@Column(name = "city_col") String city;
		@Column(name = "`{element}.city`") String literal;
	}
	@Entity
	static class Escaped {
		@Id long id;
		@ElementCollection @CollectionTable(indexes = {
				@Index(name = "role_idx", columnList = "{element}.city"),
				@Index(name = "literal_idx", columnList = "`{element}.city`")}, uniqueConstraints = {
				@UniqueConstraint(name = "role_uk", columnNames = "{element}.city"),
				@UniqueConstraint(name = "literal_uk", columnNames = "`{element}.city`")})
		Set<EscapedValue> values;
	}
	@Entity static class KeyEntity { @Id long id; }
	@Entity static class Target { @Id long id; String name; }
	@Entity
	static class EntityMap {
		@Id long id;
		@ManyToMany @MapKeyJoinColumn(name = "key_fk")
		@JoinTable(inverseJoinColumns = @JoinColumn(name = "element_fk"),
				indexes = @Index(name = "entity_roles_idx", columnList = "{key}, {element}"),
				uniqueConstraints = @UniqueConstraint(name = "entity_roles_uk", columnNames = {"{key}", "{element}"}))
		Map<KeyEntity, Target> values;
	}
	@Entity static class UnnamedJoin {
		@Id long id;
		@ManyToMany @JoinTable(indexes = @Index(name = "join_idx", columnList = "{element}")) Set<Target> values;
	}
	@Entity static class InvalidKey {
		@Id long id;
		@ElementCollection @CollectionTable(indexes = @Index(columnList = "{key}")) Set<String> values;
	}
	@Entity static class Traversal {
		@Id long id;
		@ManyToMany @JoinTable(uniqueConstraints = @UniqueConstraint(columnNames = "{element}.name")) Set<Target> values;
	}
	@Entity static class Composite {
		@Id long id;
		@ElementCollection @CollectionTable(indexes = @Index(columnList = "{element}")) Set<Address> values;
	}
	@Entity static class Malformed {
		@Id long id;
		@ElementCollection @CollectionTable(uniqueConstraints = @UniqueConstraint(columnNames = "{element}.")) Set<Address> values;
	}
	@Entity static class Unprefixed {
		@Id long id;
		@ElementCollection @CollectionTable(indexes = @Index(columnList = "city")) Set<Address> values;
	}
	@Entity static class Unknown {
		@Id long id;
		@ElementCollection @CollectionTable(indexes = @Index(columnList = "{element}.missing")) Set<Address> values;
	}
	@Embeddable static class Calculated {
		String value;
		@Formula("1 + 1") int computed;
	}
	@Entity static class FormulaCollection {
		@Id long id;
		@ElementCollection @CollectionTable(uniqueConstraints = @UniqueConstraint(columnNames = "{element}.computed")) Set<Calculated> values;
	}
	@Entity static class BorrowedKey {
		@Id long id;
		@ManyToMany @MapKey(name = "name") @JoinTable(indexes = @Index(columnList = "{key}")) Map<String, Target> values;
	}
	@Entity @Table(uniqueConstraints = @UniqueConstraint(columnNames = "{element}"))
	static class EntityMarker { @Id long id; String value; }
}
