/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.ejb3;

import org.hibernate.boot.internal.Target;
import org.hibernate.models.spi.MemberDetails;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyJoinColumns;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("deprecation")
@JiraKey("HHH-14529")
public class Ejb3XmlElementCollectionTest extends Ejb3XmlTestCase {
	@Test
	public void testNoChildren() {
		final MemberDetails memberDetails = getAttributeMember( Entity2.class, "field1", "element-collection.orm1.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( OrderBy.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( OrderColumn.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKey.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyClass.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyTemporal.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyEnumerated.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyColumn.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyJoinColumns.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyJoinColumn.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( Column.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( Temporal.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( Enumerated.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( Lob.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( AttributeOverrides.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( AttributeOverride.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( AssociationOverride.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( AssociationOverrides.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( CollectionTable.class ) ).isFalse();

		final ElementCollection elementCollectionUsage = memberDetails.getDirectAnnotationUsage( ElementCollection.class );
		assertThat( elementCollectionUsage.fetch() ).isEqualTo( FetchType.LAZY );
		assertThat( elementCollectionUsage.targetClass() ).isEqualTo( void.class );
	}

	@Test
	public void testOrderBy() {
		final MemberDetails memberDetails = getAttributeMember( Entity2.class, "field1", "element-collection.orm2.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( OrderBy.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( OrderColumn.class ) ).isFalse();

		final OrderBy orderByUsage = memberDetails.getDirectAnnotationUsage( OrderBy.class );
		assertThat( orderByUsage.value() ).isEqualTo( "col1 ASC, col2 DESC" );
	}

	@Test
	public void testOrderColumnNoAttributes() {
		final MemberDetails memberDetails = getAttributeMember( Entity2.class, "field1", "element-collection.orm3.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( OrderColumn.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( OrderBy.class ) ).isFalse();

		final OrderColumn orderColumnUsage = memberDetails.getDirectAnnotationUsage( OrderColumn.class );
		assertThat( orderColumnUsage.name() ).isEmpty();
		assertThat( orderColumnUsage.columnDefinition() ).isEmpty();
		assertThat( orderColumnUsage.insertable() ).isTrue();
		assertThat( orderColumnUsage.updatable() ).isTrue();
		assertThat( orderColumnUsage.nullable() ).isTrue();
	}

	@Test
	public void testOrderColumnAllAttributes() {
		final MemberDetails memberDetails = getAttributeMember( Entity2.class, "field1", "element-collection.orm4.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( OrderColumn.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( OrderBy.class ) ).isFalse();

		final OrderColumn orderColumnUsage = memberDetails.getDirectAnnotationUsage( OrderColumn.class );

		assertThat( orderColumnUsage.name() ).isEqualTo( "col1" );
		assertThat( orderColumnUsage.columnDefinition() ).isEqualTo( "int" );
		assertThat( orderColumnUsage.insertable() ).isFalse();
		assertThat( orderColumnUsage.updatable() ).isFalse();
		assertThat( orderColumnUsage.nullable() ).isFalse();
	}

	@Test
	public void testMapKeyNoAttributes() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm5.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKey.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyClass.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyTemporal.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyEnumerated.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyColumn.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyJoinColumns.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyJoinColumn.class ) ).isFalse();

		final MapKey mapKeyUsage = memberDetails.getDirectAnnotationUsage( MapKey.class );
		assertThat( mapKeyUsage.name() ).isEmpty();
	}

	@Test
	public void testMapKeyAllAttributes() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm6.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKey.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyClass.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyTemporal.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyEnumerated.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyColumn.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyJoinColumns.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyJoinColumn.class ) ).isFalse();

		final MapKey mapKeyUsage = memberDetails.getDirectAnnotationUsage( MapKey.class );
		assertThat( mapKeyUsage.name() ).isEqualTo( "field2" );
	}

	@Test
	public void testMapKeyClass() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm7.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyClass.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( MapKey.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyTemporal.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyEnumerated.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyColumn.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyJoinColumns.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyJoinColumn.class ) ).isFalse();

		final MapKeyClass mapKeyClassUsage = memberDetails.getDirectAnnotationUsage( MapKeyClass.class );
		assertThat( mapKeyClassUsage.value() ).isEqualTo( Entity2.class );
	}

	@Test
	public void testMapKeyTemporal() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm8.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyTemporal.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( MapKey.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyClass.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyEnumerated.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyColumn.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyJoinColumns.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyJoinColumn.class ) ).isFalse();

		final MapKeyTemporal mapKeyTemporalUsage = memberDetails.getDirectAnnotationUsage( MapKeyTemporal.class );
		assertThat( mapKeyTemporalUsage.value() ).isEqualTo( TemporalType.DATE );
	}

	@Test
	public void testMapKeyEnumerated() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm9.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyEnumerated.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( MapKey.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyClass.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyTemporal.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyColumn.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyJoinColumns.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyJoinColumn.class ) ).isFalse();

		final MapKeyEnumerated mapKeyEnumeratedUsage = memberDetails.getDirectAnnotationUsage( MapKeyEnumerated.class );
		assertThat( mapKeyEnumeratedUsage.value() ).isEqualTo( EnumType.STRING );
	}

	/**
	 * When there's a single map key attribute override, we still wrap it with
	 * an AttributeOverrides annotation.
	 */
	@Test
	public void testSingleMapKeyAttributeOverride() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm10.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( AttributeOverride.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( AttributeOverrides.class ) ).isTrue();


		final AttributeOverrides overridesUsage = memberDetails.getDirectAnnotationUsage( AttributeOverrides.class );
		final AttributeOverride overrideUsage = overridesUsage.value()[0];
		assertThat( overrideUsage.name() ).isEqualTo( "key.field1" );

		final Column nestedColumnUsage = overrideUsage.column();
		assertThat( nestedColumnUsage.name() ).isEqualTo( "col1" );
	}

	@Test
	public void testMultipleMapKeyAttributeOverrides() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm11.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( AttributeOverrides.class ) ).isTrue();

		final AttributeOverrides attributeOverridesUsage = memberDetails.getDirectAnnotationUsage( AttributeOverrides.class );
		final AttributeOverride[] attributeOverrideUsages = attributeOverridesUsage.value();
		assertThat( attributeOverrideUsages ).hasSize( 2 );

		final AttributeOverride attributeOverrideUsage0 = attributeOverrideUsages[0];
		assertThat( attributeOverrideUsage0.name() ).isEqualTo( "key.field1" );
		final Column nestedColumnUsage0 = attributeOverrideUsage0.column();
		assertThat( nestedColumnUsage0.name() ).isEmpty();
		assertThat( nestedColumnUsage0.insertable() ).isTrue();
		assertThat( nestedColumnUsage0.updatable() ).isTrue();
		assertThat( nestedColumnUsage0.nullable() ).isTrue();
		assertThat( nestedColumnUsage0.unique() ).isFalse();
		assertThat( nestedColumnUsage0.columnDefinition() ).isEmpty();
		assertThat( nestedColumnUsage0.table() ).isEmpty();
		assertThat( nestedColumnUsage0.length() ).isEqualTo( 255 );
		assertThat( nestedColumnUsage0.precision() ).isEqualTo( 0 );
		assertThat( nestedColumnUsage0.scale() ).isEqualTo( 0 );


		final AttributeOverride attributeOverrideUsage1 = attributeOverrideUsages[1];
		assertThat( attributeOverrideUsage1.name() ).isEqualTo( "key.field2" );
		final Column nestedColumnUsage1 = attributeOverrideUsage1.column();
		assertThat( nestedColumnUsage1.name() ).isEqualTo( "col1" );
		assertThat( nestedColumnUsage1.insertable() ).isFalse();
		assertThat( nestedColumnUsage1.updatable() ).isFalse();
		assertThat( nestedColumnUsage1.nullable() ).isFalse();
		assertThat( nestedColumnUsage1.unique() ).isTrue();
		assertThat( nestedColumnUsage1.columnDefinition() ).isEqualTo( "int" );
		assertThat( nestedColumnUsage1.table() ).isEqualTo( "table1" );
		assertThat( nestedColumnUsage1.length() ).isEqualTo( 50 );
		assertThat( nestedColumnUsage1.precision() ).isEqualTo( 2 );
		assertThat( nestedColumnUsage1.scale() ).isEqualTo( 1 );
	}

	@Test
	public void testMapKeyColumnNoAttributes() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm12.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyColumn.class ) ).isTrue();

		final MapKeyColumn mapKeyColumnUsage = memberDetails.getDirectAnnotationUsage( MapKeyColumn.class );
		assertThat( mapKeyColumnUsage.name() ).isEmpty();
		assertThat( mapKeyColumnUsage.table() ).isEmpty();
		assertThat( mapKeyColumnUsage.columnDefinition() ).isEmpty();
		assertThat( mapKeyColumnUsage.nullable() ).isFalse();
		assertThat( mapKeyColumnUsage.insertable() ).isTrue();
		assertThat( mapKeyColumnUsage.updatable() ).isTrue();
		assertThat( mapKeyColumnUsage.unique() ).isFalse();
		assertThat( mapKeyColumnUsage.length() ).isEqualTo( 255 );
		assertThat( mapKeyColumnUsage.precision() ).isEqualTo( 0 );
		assertThat( mapKeyColumnUsage.scale() ).isEqualTo( 0 );
	}

	@Test
	public void testMapKeyColumnAllAttributes() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm13.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyColumn.class ) ).isTrue();

		final MapKeyColumn mapKeyColumnUsage = memberDetails.getDirectAnnotationUsage( MapKeyColumn.class );
		assertThat( mapKeyColumnUsage.name() ).isEqualTo( "col1" );
		assertThat( mapKeyColumnUsage.table() ).isEqualTo( "table1" );
		assertThat( mapKeyColumnUsage.columnDefinition() ).isEqualTo( "int" );
		assertThat( mapKeyColumnUsage.nullable() ).isTrue();
		assertThat( mapKeyColumnUsage.insertable() ).isFalse();
		assertThat( mapKeyColumnUsage.updatable() ).isFalse();
		assertThat( mapKeyColumnUsage.unique() ).isTrue();
		assertThat( mapKeyColumnUsage.length() ).isEqualTo( 50 );
		assertThat( mapKeyColumnUsage.precision() ).isEqualTo( 2 );
		assertThat( mapKeyColumnUsage.scale() ).isEqualTo( 1 );
	}

	/**
	 * When there's a single map key join column, we still wrap it with a
	 * MapKeyJoinColumns annotation.
	 */
	@Test
	public void testSingleMapKeyJoinColumn() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm14.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyJoinColumn.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyJoinColumns.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( MapKey.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyClass.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyTemporal.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyEnumerated.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyColumn.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyColumn.class ) ).isFalse();

		final MapKeyJoinColumns joinColumnsUsage = memberDetails.getDirectAnnotationUsage( MapKeyJoinColumns.class );
		final MapKeyJoinColumn joinColumnUsage = joinColumnsUsage.value()[0];
		assertThat( joinColumnUsage.name() ).isEqualTo( "col1" );
	}

	@Test
	public void testMultipleMapKeyJoinColumns() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm15.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyJoinColumn.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyJoinColumns.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( MapKey.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyClass.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyTemporal.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyEnumerated.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyColumn.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapKeyColumn.class ) ).isFalse();

		final MapKeyJoinColumns joinColumnsUsage = memberDetails.getDirectAnnotationUsage( MapKeyJoinColumns.class );
		final MapKeyJoinColumn[] joinColumnUsages = joinColumnsUsage.value();
		assertThat( joinColumnUsages ).hasSize( 2 );

		final MapKeyJoinColumn joinColumnUsage0 = joinColumnUsages[0];
		assertThat( joinColumnUsage0.name() ).isEmpty();
		assertThat( joinColumnUsage0.referencedColumnName() ).isEmpty();
		assertThat( joinColumnUsage0.unique() ).isFalse();
		assertThat( joinColumnUsage0.nullable() ).isFalse();
		assertThat( joinColumnUsage0.insertable() ).isTrue();
		assertThat( joinColumnUsage0.updatable() ).isTrue();
		assertThat( joinColumnUsage0.columnDefinition() ).isEmpty();
		assertThat( joinColumnUsage0.table() ).isEmpty();

		final MapKeyJoinColumn joinColumnUsage1 = joinColumnUsages[1];
		assertThat( joinColumnUsage1.name() ).isEqualTo( "col1" );
		assertThat( joinColumnUsage1.referencedColumnName() ).isEqualTo( "col2" );
		assertThat( joinColumnUsage1.unique() ).isTrue();
		assertThat( joinColumnUsage1.nullable() ).isTrue();
		assertThat( joinColumnUsage1.insertable() ).isFalse();
		assertThat( joinColumnUsage1.updatable() ).isFalse();
		assertThat( joinColumnUsage1.columnDefinition() ).isEqualTo( "int" );
		assertThat( joinColumnUsage1.table() ).isEqualTo( "table1" );
	}

	@Test
	public void testColumnNoAttributes() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm16.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( Column.class ) ).isTrue();
		final Column columnUsage = memberDetails.getDirectAnnotationUsage( Column.class );
		assertThat( columnUsage.name() ).isEmpty();
		assertThat( columnUsage.unique() ).isFalse();
		assertThat( columnUsage.nullable() ).isTrue();
		assertThat( columnUsage.insertable() ).isTrue();
		assertThat( columnUsage.updatable() ).isTrue();
		assertThat( columnUsage.columnDefinition() ).isEmpty();
		assertThat( columnUsage.table() ).isEmpty();
		assertThat( columnUsage.length() ).isEqualTo( 255 );
		assertThat( columnUsage.precision() ).isEqualTo( 0 );
		assertThat( columnUsage.scale() ).isEqualTo( 0 );
	}

	@Test
	public void testColumnAllAttributes() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm17.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( Column.class ) ).isTrue();
		final Column columnUsage = memberDetails.getDirectAnnotationUsage( Column.class );
		assertThat( columnUsage.name() ).isEqualTo( "col1" );
		assertThat( columnUsage.unique() ).isTrue();
		assertThat( columnUsage.nullable() ).isFalse();
		assertThat( columnUsage.insertable() ).isFalse();
		assertThat( columnUsage.updatable() ).isFalse();
		assertThat( columnUsage.columnDefinition() ).isEqualTo( "int" );
		assertThat( columnUsage.table() ).isEqualTo( "table1" );
		assertThat( columnUsage.length() ).isEqualTo( 50 );
		assertThat( columnUsage.precision() ).isEqualTo( 2 );
		assertThat( columnUsage.scale() ).isEqualTo( 1 );
	}

	@Test
	public void testTemporal() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm18.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( Temporal.class ) ).isTrue();
		final Temporal temporalUsage = memberDetails.getDirectAnnotationUsage( Temporal.class );
		assertThat( temporalUsage.value() ).isEqualTo( TemporalType.DATE );
	}

	@Test
	public void testEnumerated() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm19.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( Enumerated.class ) ).isTrue();
		final Enumerated enumeratedUsage = memberDetails.getDirectAnnotationUsage( Enumerated.class );
		assertThat( enumeratedUsage.value() ).isEqualTo( EnumType.STRING );
	}

	@Test
	public void testLob() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm20.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( Lob.class ) ).isTrue();
	}

	/**
	 * When there's a single attribute override, we still wrap it with an
	 * AttributeOverrides annotation.
	 */
	@Test
	public void testSingleAttributeOverride() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm21.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( AttributeOverride.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( AttributeOverrides.class ) ).isTrue();

		final AttributeOverrides overridesUsage = memberDetails.getDirectAnnotationUsage( AttributeOverrides.class );
		final AttributeOverride overrideUsage = overridesUsage.value()[0];
		assertThat( overrideUsage.name() ).isEqualTo( "value.field1" );

		final Column overrideColumnUsage = overrideUsage.column();
		assertThat( overrideColumnUsage.name() ).isEqualTo( "col1" );
	}

	@Test
	public void testMultipleAttributeOverrides() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm22.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( AttributeOverride.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( AttributeOverrides.class ) ).isTrue();

		final AttributeOverrides overridesUsage = memberDetails.getDirectAnnotationUsage( AttributeOverrides.class );
		final AttributeOverride[] overrideUsages = overridesUsage.value();
		assertThat( overrideUsages ).hasSize( 2 );

		final AttributeOverride overrideUsage0 = overrideUsages[0];
		assertThat( overrideUsage0.name() ).isEqualTo( "value.field1" );
		final Column overrideColumnUsage0 = overrideUsage0.column();
		assertThat( overrideColumnUsage0.name() ).isEmpty();
		assertThat( overrideColumnUsage0.unique() ).isFalse();
		assertThat( overrideColumnUsage0.nullable() ).isTrue();
		assertThat( overrideColumnUsage0.insertable() ).isTrue();
		assertThat( overrideColumnUsage0.updatable() ).isTrue();
		assertThat( overrideColumnUsage0.columnDefinition() ).isEmpty();
		assertThat( overrideColumnUsage0.table() ).isEmpty();
		assertThat( overrideColumnUsage0.length() ).isEqualTo( 255 );
		assertThat( overrideColumnUsage0.precision() ).isEqualTo( 0 );
		assertThat( overrideColumnUsage0.scale() ).isEqualTo( 0 );

		final AttributeOverride overrideUsage1 = overrideUsages[1];
		assertThat( overrideUsage1.name() ).isEqualTo( "value.field2" );
		final Column overrideColumnUsage1 = overrideUsage1.column();
		assertThat( overrideColumnUsage1.name() ).isEqualTo( "col1" );
		assertThat( overrideColumnUsage1.unique() ).isTrue();
		assertThat( overrideColumnUsage1.nullable() ).isFalse();
		assertThat( overrideColumnUsage1.insertable() ).isFalse();
		assertThat( overrideColumnUsage1.updatable() ).isFalse();
		assertThat( overrideColumnUsage1.columnDefinition() ).isEqualTo( "int" );
		assertThat( overrideColumnUsage1.table() ).isEqualTo( "table1" );
		assertThat( overrideColumnUsage1.length() ).isEqualTo( 50 );
		assertThat( overrideColumnUsage1.precision() ).isEqualTo( 2 );
		assertThat( overrideColumnUsage1.scale() ).isEqualTo( 1 );
	}

	/**
	 * Tests that map-key-attribute-override and attribute-override elements
	 * both end up in the AttributeOverrides annotation.
	 */
	@Test
	public void testMixedAttributeOverrides() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm23.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( AttributeOverride.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( AttributeOverrides.class ) ).isTrue();

		final AttributeOverrides overridesUsage = memberDetails.getDirectAnnotationUsage( AttributeOverrides.class );
		final AttributeOverride[] overrideUsages = overridesUsage.value();
		assertThat( overrideUsages ).hasSize( 2 );

		final AttributeOverride overrideUsage0 = overrideUsages[0];
		assertThat( overrideUsage0.name() ).isEqualTo( "key.field1" );
		final Column overrideColumnUsage0 = overrideUsage0.column();
		assertThat( overrideColumnUsage0.name() ).isEqualTo( "col1" );

		final AttributeOverride overrideUsage1 = overrideUsages[1];
		assertThat( overrideUsage1.name() ).isEqualTo( "value.field2" );
		final Column overrideColumnUsage1 = overrideUsage1.column();
		assertThat( overrideColumnUsage1.name() ).isEqualTo( "col2" );
	}

	/**
	 * When there's a single association override, we still wrap it with an
	 * AssociationOverrides annotation.
	 */
	@Test
	public void testSingleAssociationOverride() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm24.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( AssociationOverride.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( AssociationOverrides.class ) ).isTrue();

		final AssociationOverrides overridesUsage = memberDetails.getDirectAnnotationUsage( AssociationOverrides.class );
		final AssociationOverride overrideUsage = overridesUsage.value()[0];
		assertThat( overrideUsage.name() ).isEqualTo( "association1" );
		assertThat( overrideUsage.joinColumns() ).isEmpty();
		final JoinTable joinTableUsage = overrideUsage.joinTable();
		assertThat( joinTableUsage.name() ).isEmpty();
	}

	@Test
	public void testMultipleAssociationOverridesJoinColumns() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm25.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( AssociationOverride.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( AssociationOverrides.class ) ).isTrue();

		final AssociationOverrides overridesUsage = memberDetails.getDirectAnnotationUsage( AssociationOverrides.class );
		final AssociationOverride[] overrideUsages = overridesUsage.value();
		assertThat( overrideUsages ).hasSize( 2 );

		{
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// First, an association using join table
			final AssociationOverride overrideUsage = overrideUsages[0];
			assertThat( overrideUsage.name() ).isEqualTo( "association1" );
			assertThat( overrideUsage.joinColumns() ).isEmpty();

			JoinTable joinTableUsage = overrideUsage.joinTable();
			assertThat( joinTableUsage.name() ).isEqualTo( "table1" );
			assertThat( joinTableUsage.catalog() ).isEqualTo( "catalog1" );
			assertThat( joinTableUsage.schema() ).isEqualTo( "schema1" );

			//JoinColumns
			final JoinColumn[] joinColumUsages = joinTableUsage.joinColumns();
			assertThat( joinColumUsages ).hasSize( 2 );

			final JoinColumn joinColumnUsage0 = joinColumUsages[0];
			assertThat( joinColumnUsage0.name() ).isEmpty();
			assertThat( joinColumnUsage0.referencedColumnName() ).isEmpty();
			assertThat( joinColumnUsage0.table() ).isEmpty();
			assertThat( joinColumnUsage0.columnDefinition() ).isEmpty();
			assertThat( joinColumnUsage0.insertable() ).isTrue();
			assertThat( joinColumnUsage0.updatable() ).isTrue();
			assertThat( joinColumnUsage0.nullable() ).isTrue();
			assertThat( joinColumnUsage0.unique() ).isFalse();

			final JoinColumn joinColumnUsage1 = joinColumUsages[1];
			assertThat( joinColumnUsage1.name() ).isEqualTo( "col1" );
			assertThat( joinColumnUsage1.referencedColumnName() ).isEqualTo( "col2" );
			assertThat( joinColumnUsage1.table() ).isEqualTo( "table2" );
			assertThat( joinColumnUsage1.columnDefinition() ).isEqualTo( "int" );
			assertThat( joinColumnUsage1.insertable() ).isFalse();
			assertThat( joinColumnUsage1.updatable() ).isFalse();
			assertThat( joinColumnUsage1.nullable() ).isFalse();
			assertThat( joinColumnUsage1.unique() ).isTrue();

			//InverseJoinColumns
			final JoinColumn[] inverseJoinColumnUsages = joinTableUsage.inverseJoinColumns();
			assertThat( inverseJoinColumnUsages ).hasSize( 2 );

			final JoinColumn inverseJoinColumnUsage0 = inverseJoinColumnUsages[0];
			assertThat( inverseJoinColumnUsage0.name() ).isEmpty();
			assertThat( inverseJoinColumnUsage0.referencedColumnName() ).isEmpty();
			assertThat( inverseJoinColumnUsage0.table() ).isEmpty();
			assertThat( inverseJoinColumnUsage0.columnDefinition() ).isEmpty();
			assertThat( inverseJoinColumnUsage0.insertable() ).isTrue();
			assertThat( inverseJoinColumnUsage0.updatable() ).isTrue();
			assertThat( inverseJoinColumnUsage0.nullable() ).isTrue();
			assertThat( inverseJoinColumnUsage0.unique() ).isFalse();

			final JoinColumn inverseJoinColumnUsage1 = inverseJoinColumnUsages[1];
			assertThat( inverseJoinColumnUsage1.name() ).isEqualTo( "col3" );
			assertThat( inverseJoinColumnUsage1.referencedColumnName() ).isEqualTo( "col4" );
			assertThat( inverseJoinColumnUsage1.table() ).isEqualTo( "table3" );
			assertThat( inverseJoinColumnUsage1.columnDefinition() ).isEqualTo( "int" );
			assertThat( inverseJoinColumnUsage1.insertable() ).isFalse();
			assertThat( inverseJoinColumnUsage1.updatable() ).isFalse();
			assertThat( inverseJoinColumnUsage1.nullable() ).isFalse();
			assertThat( inverseJoinColumnUsage1.unique() ).isTrue();

			//UniqueConstraints
			final UniqueConstraint[] uniqueConstraintUsages = joinTableUsage.uniqueConstraints();
			assertThat( uniqueConstraintUsages ).hasSize( 2 );

			final UniqueConstraint uniqueConstraintUsage0 = uniqueConstraintUsages[0];
			assertThat( uniqueConstraintUsage0.name() ).isEmpty();
			assertThat( uniqueConstraintUsage0.columnNames() ).containsOnly( "col5" );

			final UniqueConstraint uniqueConstraintUsage1 = uniqueConstraintUsages[1];
			assertThat( uniqueConstraintUsage1.name() ).isEqualTo( "uq1" );
			assertThat( uniqueConstraintUsage1.columnNames() ).containsOnly( "col6", "col7" );
		}

		{
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Second, an association using join columns
			final AssociationOverride overrideUsage = overrideUsages[1];
			assertThat( overrideUsage.name() ).isEqualTo( "association2" );

			//JoinColumns
			final JoinColumn[] joinColumUsages = overrideUsage.joinColumns();
			assertThat( joinColumUsages ).hasSize( 2 );

			final JoinColumn joinColumnUsage0 = joinColumUsages[0];
			assertThat( joinColumnUsage0.name() ).isEmpty();
			assertThat( joinColumnUsage0.referencedColumnName() ).isEmpty();
			assertThat( joinColumnUsage0.table() ).isEmpty();
			assertThat( joinColumnUsage0.columnDefinition() ).isEmpty();
			assertThat( joinColumnUsage0.insertable() ).isTrue();
			assertThat( joinColumnUsage0.updatable() ).isTrue();
			assertThat( joinColumnUsage0.nullable() ).isTrue();
			assertThat( joinColumnUsage0.unique() ).isFalse();

			final JoinColumn joinColumnUsage1 = joinColumUsages[1];
			assertThat( joinColumnUsage1.name() ).isEqualTo( "col8" );
			assertThat( joinColumnUsage1.referencedColumnName() ).isEqualTo( "col9" );
			assertThat( joinColumnUsage1.table() ).isEqualTo( "table4" );
			assertThat( joinColumnUsage1.columnDefinition() ).isEqualTo( "int" );
			assertThat( joinColumnUsage1.insertable() ).isFalse();
			assertThat( joinColumnUsage1.updatable() ).isFalse();
			assertThat( joinColumnUsage1.nullable() ).isFalse();
			assertThat( joinColumnUsage1.unique() ).isTrue();
		}
	}

	@Test
	public void testCollectionTableNoChildren() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm26.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( CollectionTable.class ) ).isTrue();
		final CollectionTable collectionTableUsage = memberDetails.getDirectAnnotationUsage( CollectionTable.class );
		assertThat( collectionTableUsage.name() ).isEmpty();
		assertThat( collectionTableUsage.catalog() ).isEmpty();
		assertThat( collectionTableUsage.schema() ).isEmpty();
		assertThat( collectionTableUsage.joinColumns() ).isEmpty();
		assertThat( collectionTableUsage.uniqueConstraints() ).isEmpty();
	}

	@Test
	public void testCollectionTableAllChildren() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm27.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( CollectionTable.class ) ).isTrue();
		final CollectionTable collectionTableUsage = memberDetails.getDirectAnnotationUsage( CollectionTable.class );
		assertThat( collectionTableUsage.name() ).isEqualTo( "table1" );
		assertThat( collectionTableUsage.catalog() ).isEqualTo( "catalog1" );
		assertThat( collectionTableUsage.schema() ).isEqualTo( "schema1" );

		//JoinColumns
		final JoinColumn[] joinColumnUsages = collectionTableUsage.joinColumns();
		assertThat( joinColumnUsages ).hasSize( 2 );

		final JoinColumn joinColumnUsage0 = joinColumnUsages[0];
		assertThat( joinColumnUsage0.name() ).isEmpty();
		assertThat( joinColumnUsage0.referencedColumnName() ).isEmpty();
		assertThat( joinColumnUsage0.table() ).isEmpty();
		assertThat( joinColumnUsage0.columnDefinition() ).isEmpty();
		assertThat( joinColumnUsage0.insertable() ).isTrue();
		assertThat( joinColumnUsage0.updatable() ).isTrue();
		assertThat( joinColumnUsage0.nullable() ).isTrue();
		assertThat( joinColumnUsage0.unique() ).isFalse();

		final JoinColumn joinColumnUsage1 = joinColumnUsages[1];
		assertThat( joinColumnUsage1.name() ).isEqualTo( "col1" );
		assertThat( joinColumnUsage1.referencedColumnName() ).isEqualTo( "col2" );
		assertThat( joinColumnUsage1.table() ).isEqualTo( "table2" );
		assertThat( joinColumnUsage1.columnDefinition() ).isEqualTo( "int" );
		assertThat( joinColumnUsage1.insertable() ).isFalse();
		assertThat( joinColumnUsage1.updatable() ).isFalse();
		assertThat( joinColumnUsage1.nullable() ).isFalse();
		assertThat( joinColumnUsage1.unique() ).isTrue();

		//UniqueConstraints
		final UniqueConstraint[] uniqueConstraintUsages = collectionTableUsage.uniqueConstraints();
		assertThat( uniqueConstraintUsages ).hasSize( 2 );

		final UniqueConstraint uniqueConstraintUsage0 = uniqueConstraintUsages[0];
		assertThat( uniqueConstraintUsage0.name() ).isEmpty();
		assertThat( uniqueConstraintUsage0.columnNames() ).containsOnly( "col3" );

		final UniqueConstraint uniqueConstraintUsage1 = uniqueConstraintUsages[1];
		assertThat( uniqueConstraintUsage1.name() ).isEqualTo( "uq1" );
		assertThat( uniqueConstraintUsage1.columnNames() ).containsOnly( "col4", "col5" );
	}

	@Test
	public void testAllAttributes() {
		final MemberDetails memberDetails = getAttributeMember( Entity2.class, "field1", "element-collection.orm28.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ElementCollection.class ) ).isTrue();

		final ElementCollection elementCollectionUsage = memberDetails.getDirectAnnotationUsage( ElementCollection.class );
		assertThat( elementCollectionUsage.fetch() ).isEqualTo( FetchType.EAGER );

		final Access accessUsage = memberDetails.getDirectAnnotationUsage( Access.class );
		assertThat( accessUsage.value() ).isEqualTo( AccessType.PROPERTY );

		final Target targetUsage = memberDetails.getDirectAnnotationUsage( Target.class );
		assertThat( targetUsage.value() ).isEqualTo( Entity3.class.getName() );
	}

}
