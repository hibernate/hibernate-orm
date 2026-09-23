/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.util.function.Consumer;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

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

/// Attribute fallback preserves column precedence and resolves only single local columns.
///
/// @author Steve Ebersole
@BaseUnitTest
class IndexAttributeReferenceTest {
	@Test
	void basicNestedInheritedAndAssociationAttributes() {
		final var physical = new IndexColumnResolutionTest.Prefix();
		inspect( physical, metadata -> {
			final var entity = metadata.getEntityBinding( Named.class.getName() );
			final var table = (PhysicalTable) entity.getTable();
			final var index = table.getIndexes().get( "attributes_idx" );
			assertThat( index.getSelectables() ).extracting( org.hibernate.mapping.Selectable::getText )
					.containsExactly( "p_renamed", "p_address_city", "p_inherited_column", "p_target_fk" );
			for ( var selectable : index.getSelectables() ) {
				assertThat( selectable ).isSameAs( table.getColumn( (org.hibernate.mapping.Column) selectable ) );
			}
			assertThat( index.getSelectableOrderMap().get( index.getSelectables().get( 1 ) ) ).isEqualTo( "desc" );
			assertThat( table.getUniqueKeys().get( "attribute_uk" ).getColumns() )
					.containsExactly( (org.hibernate.mapping.Column) index.getSelectables().get( 0 ) );
			assertThat( physical.columns ).filteredOn( "renamed"::equals ).hasSize( 1 );
			assertThat( physical.columns ).filteredOn( "target_fk"::equals ).hasSize( 1 );
		}, Named.class, Target.class );
	}

	@Test
	void logicalAndPhysicalColumnsWinOverAttributes() {
		inspect( new IndexColumnResolutionTest.Prefix(), metadata -> {
			final var table = (PhysicalTable) metadata.getEntityBinding( Precedence.class.getName() ).getTable();
			assertThat( table.getIndexes().get( "precedence_idx" ).getSelectables() )
					.extracting( org.hibernate.mapping.Selectable::getText ).containsExactly( "p_code", "p_other" );
		}, Precedence.class );
	}

	@Test
	void secondaryTableAttribute() {
		inspect( new IndexColumnResolutionTest.Prefix(), metadata -> {
			final var table = (PhysicalTable) metadata.getEntityBinding( Secondary.class.getName() ).getJoins().get( 0 ).getTable();
			assertThat( table.getIndexes().get( "secondary_idx" ).getSelectables() )
					.extracting( org.hibernate.mapping.Selectable::getText ).containsExactly( "p_details_text" );
		}, Secondary.class );
	}

	@Test void noLeafGuessing() { rejected( "unknown column 'city'", Leaf.class ); }
	@Test void noCompositeExpansion() { rejected( "exactly one column (found 2)", Composite.class ); }
	@Test void noCompositeAssociationExpansion() { rejected( "exactly one column (found 2)", CompositeAssociation.class, CompositeTarget.class ); }
	@Test void noFormulaExpansion() { rejected( "contains a formula", FormulaEntity.class ); }
	@Test void noOtherTableColumn() { rejected( "actual column on the declaring table", WrongTable.class ); }
	@Test void noAssociationTraversal() { rejected( "only supported through embeddables", Traversal.class, Target.class ); }
	@Test void noInverseAssociation() { rejected( "inverse association", Inverse.class, Owner.class ); }

	private void rejected(String reason, Class<?>... types) {
		assertThatThrownBy( () -> inspect( new IndexColumnResolutionTest.Prefix(), metadata -> {}, types ) )
				.isInstanceOf( AnnotationException.class ).hasMessageContaining( reason ).hasMessageContaining( "@Table.indexes[0]" );
	}

	private void inspect(IndexColumnResolutionTest.Prefix physical, Consumer<Metadata> assertion, Class<?>... types) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var sources = new MappingSources();
			for ( var type : types ) { sources.addManagedClass( type ); }
			assertion.accept( MetadataBuildingTestHelper.buildMetadataWithNaming(
					registry, sources, new StandardImplicitNamingStrategy(), physical ) );
		}
	}

	@MappedSuperclass
	static class Base { @Id long id; @Column(name = "inherited_column") String inherited; }
	@Embeddable
	static class Address { String city; String street; }
	@Entity
	static class Target { @Id long id; String name; }
	@Entity @Table(indexes = {
			@Index(name = "attributes_idx", columnList = "name,address.city desc,inherited,target"),
			@Index(name = "attribute_uk", columnList = "name", unique = true) })
	static class Named extends Base {
		@Column(name = "renamed") String name;
		@Embedded Address address;
		@ManyToOne @JoinColumn(name = "target_fk") Target target;
	}
	@Entity @Table(indexes = @Index(name = "precedence_idx", columnList = "code,p_other"))
	static class Precedence {
		@Id long id;
		@Column(name = "code") String first;
		@Column(name = "other") String second;
		@Column(name = "unselected_one") String code;
		@Column(name = "unselected_two") String p_other;
	}
	@Entity @SecondaryTable(name = "details", indexes = @Index(name = "secondary_idx", columnList = "description"))
	static class Secondary { @Id long id; @Column(name = "details_text", table = "details") String description; }
	@Entity @Table(indexes = @Index(columnList = "city"))
	static class Leaf { @Id long id; @Embedded Address address; }
	@Entity @Table(indexes = @Index(columnList = "address"))
	static class Composite { @Id long id; @Embedded Address address; }
	@Embeddable
	static class CompositeId implements java.io.Serializable { long first; long second; }
	@Entity
	static class CompositeTarget { @EmbeddedId CompositeId id; }
	@Entity @Table(indexes = @Index(columnList = "target"))
	static class CompositeAssociation { @Id long id; @ManyToOne CompositeTarget target; }
	@Entity @Table(indexes = @Index(columnList = "computed"))
	static class FormulaEntity { @Id long id; @Formula("1 + 1") int computed; }
	@Entity @SecondaryTable(name = "details") @Table(indexes = @Index(columnList = "description"))
	static class WrongTable { @Id long id; @Column(name = "details_text", table = "details") String description; }
	@Entity @Table(indexes = @Index(columnList = "target.name"))
	static class Traversal { @Id long id; @ManyToOne @JoinColumn(name = "target_fk") Target target; }
	@Entity @Table(indexes = @Index(columnList = "owner"))
	static class Inverse { @Id long id; @OneToOne(mappedBy = "inverse") Owner owner; }
	@Entity
	static class Owner { @Id long id; @OneToOne @JoinColumn(name = "inverse_fk") Inverse inverse; }
}
