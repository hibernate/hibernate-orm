/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.annotations.xml.ejb3;

import java.lang.annotation.Annotation;
import java.util.List;

import org.hibernate.boot.internal.Target;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
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
import jakarta.persistence.JoinColumns;
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
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( OrderBy.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( OrderColumn.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKey.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyClass.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyTemporal.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyEnumerated.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyColumn.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyJoinColumns.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyJoinColumn.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( Column.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( Temporal.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( Enumerated.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( Lob.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( AttributeOverrides.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( AttributeOverride.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( AssociationOverride.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( AssociationOverrides.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( CollectionTable.class ) ).isFalse();

		final AnnotationUsage<ElementCollection> elementCollectionUsage = memberDetails.getAnnotationUsage( ElementCollection.class );
		assertThat( elementCollectionUsage.getEnum( "fetch", FetchType.class ) ).isEqualTo( FetchType.LAZY );
		assertThat( elementCollectionUsage.getClassDetails( "targetClass" ) ).isEqualTo( ClassDetails.VOID_CLASS_DETAILS );
	}

	@Test
	public void testOrderBy() {
		final MemberDetails memberDetails = getAttributeMember( Entity2.class, "field1", "element-collection.orm2.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasAnnotationUsage( OrderBy.class ) ).isTrue();
		assertThat( memberDetails.hasAnnotationUsage( OrderColumn.class ) ).isFalse();

		final AnnotationUsage<OrderBy> orderByUsage = memberDetails.getAnnotationUsage( OrderBy.class );
		assertThat( orderByUsage.getString( "value" ) ).isEqualTo( "col1 ASC, col2 DESC" );
	}

	@Test
	public void testOrderColumnNoAttributes() {
		final MemberDetails memberDetails = getAttributeMember( Entity2.class, "field1", "element-collection.orm3.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasAnnotationUsage( OrderColumn.class ) ).isTrue();
		assertThat( memberDetails.hasAnnotationUsage( OrderBy.class ) ).isFalse();

		final AnnotationUsage<OrderColumn> orderColumnUsage = memberDetails.getAnnotationUsage( OrderColumn.class );
		assertThat( orderColumnUsage.getString( "name" ) ).isEmpty();
		assertThat( orderColumnUsage.getString( "columnDefinition" ) ).isEmpty();
		assertThat( orderColumnUsage.getBoolean( "insertable" ) ).isTrue();
		assertThat( orderColumnUsage.getBoolean( "updatable" ) ).isTrue();
		assertThat( orderColumnUsage.getBoolean( "nullable" ) ).isTrue();
	}

	@Test
	public void testOrderColumnAllAttributes() {
		final MemberDetails memberDetails = getAttributeMember( Entity2.class, "field1", "element-collection.orm4.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasAnnotationUsage( OrderColumn.class ) ).isTrue();
		assertThat( memberDetails.hasAnnotationUsage( OrderBy.class ) ).isFalse();

		final AnnotationUsage<OrderColumn> orderColumnUsage = memberDetails.getAnnotationUsage( OrderColumn.class );

		assertThat( orderColumnUsage.getString( "name" ) ).isEqualTo( "col1" );
		assertThat( orderColumnUsage.getString( "columnDefinition" ) ).isEqualTo( "int" );
		assertThat( orderColumnUsage.getBoolean( "insertable" ) ).isFalse();
		assertThat( orderColumnUsage.getBoolean( "updatable" ) ).isFalse();
		assertThat( orderColumnUsage.getBoolean( "nullable" ) ).isFalse();
	}

	@Test
	public void testMapKeyNoAttributes() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm5.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasAnnotationUsage( MapKey.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( MapKeyClass.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyTemporal.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyEnumerated.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyColumn.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyJoinColumns.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyJoinColumn.class ) ).isFalse();

		final AnnotationUsage<MapKey> mapKeyUsage = memberDetails.getAnnotationUsage( MapKey.class );
		assertThat( mapKeyUsage.getString( "name" ) ).isEmpty();
	}

	@Test
	public void testMapKeyAllAttributes() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm6.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasAnnotationUsage( MapKey.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( MapKeyClass.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyTemporal.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyEnumerated.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyColumn.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyJoinColumns.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyJoinColumn.class ) ).isFalse();

		final AnnotationUsage<MapKey> mapKeyUsage = memberDetails.getAnnotationUsage( MapKey.class );
		assertThat( mapKeyUsage.getString( "name" ) ).isEqualTo( "field2" );
	}

	@Test
	public void testMapKeyClass() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm7.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyClass.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( MapKey.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyTemporal.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyEnumerated.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyColumn.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyJoinColumns.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyJoinColumn.class ) ).isFalse();

		final AnnotationUsage<MapKeyClass> mapKeyClassUsage = memberDetails.getAnnotationUsage( MapKeyClass.class );
		assertThat( mapKeyClassUsage.getClassDetails( "value" ).toJavaClass() ).isEqualTo( Entity2.class );
	}

	@Test
	public void testMapKeyTemporal() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm8.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyTemporal.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( MapKey.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyClass.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyEnumerated.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyColumn.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyJoinColumns.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyJoinColumn.class ) ).isFalse();

		final AnnotationUsage<MapKeyTemporal> mapKeyTemporalUsage = memberDetails.getAnnotationUsage( MapKeyTemporal.class );
		assertThat( mapKeyTemporalUsage.getEnum( "value", TemporalType.class ) ).isEqualTo( TemporalType.DATE );
	}

	@Test
	public void testMapKeyEnumerated() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm9.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyEnumerated.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( MapKey.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyClass.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyTemporal.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyColumn.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyJoinColumns.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyJoinColumn.class ) ).isFalse();

		final AnnotationUsage<MapKeyEnumerated> mapKeyEnumeratedUsage = memberDetails.getAnnotationUsage( MapKeyEnumerated.class );
		assertThat( mapKeyEnumeratedUsage.getEnum( "value", EnumType.class ) ).isEqualTo( EnumType.STRING );
	}

	/**
	 * When there's a single map key attribute override, we still wrap it with
	 * an AttributeOverrides annotation.
	 */
	@Test
	public void testSingleMapKeyAttributeOverride() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm10.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasAnnotationUsage( AttributeOverride.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( AttributeOverrides.class ) ).isTrue();

		final AnnotationUsage<AttributeOverride> attributeOverrideUsage = memberDetails.getAnnotationUsage( AttributeOverride.class );
		assertThat( attributeOverrideUsage.getString( "name" ) ).isEqualTo( "key.field1" );

		final AnnotationUsage<Column> nestedColumnUsage = attributeOverrideUsage.getNestedUsage( "column" );
		assertThat( nestedColumnUsage.getString( "name" ) ).isEqualTo( "col1" );
	}

	@Test
	public void testMultipleMapKeyAttributeOverrides() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm11.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasAnnotationUsage( AttributeOverrides.class ) ).isTrue();

		final AnnotationUsage<AttributeOverrides> attributeOverridesUsage = memberDetails.getAnnotationUsage( AttributeOverrides.class );
		final List<AnnotationUsage<AttributeOverride>> attributeOverrideUsages = attributeOverridesUsage.getList( "value" );
		assertThat( attributeOverrideUsages ).hasSize( 2 );

		final AnnotationUsage<AttributeOverride> attributeOverrideUsage0 = attributeOverrideUsages.get( 0 );
		assertThat( attributeOverrideUsage0.getString( "name" ) ).isEqualTo( "key.field1" );
		final AnnotationUsage<Column> nestedColumnUsage0 = attributeOverrideUsage0.getNestedUsage( "column" );
		assertThat( nestedColumnUsage0.getString( "name" ) ).isEmpty();
		assertThat( nestedColumnUsage0.getBoolean(  "insertable" ) ).isTrue();
		assertThat( nestedColumnUsage0.getBoolean(  "updatable" ) ).isTrue();
		assertThat( nestedColumnUsage0.getBoolean(  "nullable" ) ).isTrue();
		assertThat( nestedColumnUsage0.getBoolean(  "unique" ) ).isFalse();
		assertThat( nestedColumnUsage0.getString( "columnDefinition" ) ).isEmpty();
		assertThat( nestedColumnUsage0.getString( "table" ) ).isEmpty();
		assertThat( nestedColumnUsage0.getInteger( "length" ) ).isEqualTo( 255 );
		assertThat( nestedColumnUsage0.getInteger( "precision" ) ).isEqualTo( 0 );
		assertThat( nestedColumnUsage0.getInteger( "scale" ) ).isEqualTo( 0 );


		final AnnotationUsage<AttributeOverride> attributeOverrideUsage1 = attributeOverrideUsages.get( 1 );
		assertThat( attributeOverrideUsage1.getString( "name" ) ).isEqualTo( "key.field2" );
		final AnnotationUsage<Column> nestedColumnUsage1 = attributeOverrideUsage1.getNestedUsage( "column" );
		assertThat( nestedColumnUsage1.getString( "name" ) ).isEqualTo( "col1" );
		assertThat( nestedColumnUsage1.getBoolean(  "insertable" ) ).isFalse();
		assertThat( nestedColumnUsage1.getBoolean(  "updatable" ) ).isFalse();
		assertThat( nestedColumnUsage1.getBoolean(  "nullable" ) ).isFalse();
		assertThat( nestedColumnUsage1.getBoolean(  "unique" ) ).isTrue();
		assertThat( nestedColumnUsage1.getString( "columnDefinition" ) ).isEqualTo( "int" );
		assertThat( nestedColumnUsage1.getString( "table" ) ).isEqualTo( "table1" );
		assertThat( nestedColumnUsage1.getInteger( "length" ) ).isEqualTo( 50 );
		assertThat( nestedColumnUsage1.getInteger( "precision" ) ).isEqualTo( 2 );
		assertThat( nestedColumnUsage1.getInteger( "scale" ) ).isEqualTo( 1 );
	}

	@Test
	public void testMapKeyColumnNoAttributes() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm12.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyColumn.class ) ).isTrue();

		final AnnotationUsage<MapKeyColumn> mapKeyColumnUsage = memberDetails.getAnnotationUsage( MapKeyColumn.class );
		assertThat( mapKeyColumnUsage.getString( "name" ) ).isEmpty();
		assertThat( mapKeyColumnUsage.getString( "table" ) ).isEmpty();
		assertThat( mapKeyColumnUsage.getString( "columnDefinition" ) ).isEmpty();
		assertThat( mapKeyColumnUsage.getBoolean( "nullable" ) ).isFalse();
		assertThat( mapKeyColumnUsage.getBoolean( "insertable" ) ).isTrue();
		assertThat( mapKeyColumnUsage.getBoolean( "updatable" ) ).isTrue();
		assertThat( mapKeyColumnUsage.getBoolean( "unique" ) ).isFalse();
		assertThat( mapKeyColumnUsage.getInteger( "length" ) ).isEqualTo( 255 );
		assertThat( mapKeyColumnUsage.getInteger( "precision" ) ).isEqualTo( 0 );
		assertThat( mapKeyColumnUsage.getInteger( "scale" ) ).isEqualTo( 0 );
	}

	@Test
	public void testMapKeyColumnAllAttributes() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm13.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyColumn.class ) ).isTrue();

		final AnnotationUsage<MapKeyColumn> mapKeyColumnUsage = memberDetails.getAnnotationUsage( MapKeyColumn.class );
		assertThat( mapKeyColumnUsage.getString( "name" ) ).isEqualTo( "col1" );
		assertThat( mapKeyColumnUsage.getString( "table" ) ).isEqualTo( "table1" );
		assertThat( mapKeyColumnUsage.getString( "columnDefinition" ) ).isEqualTo( "int" );
		assertThat( mapKeyColumnUsage.getBoolean( "nullable" ) ).isTrue();
		assertThat( mapKeyColumnUsage.getBoolean( "insertable" ) ).isFalse();
		assertThat( mapKeyColumnUsage.getBoolean( "updatable" ) ).isFalse();
		assertThat( mapKeyColumnUsage.getBoolean( "unique" ) ).isTrue();
		assertThat( mapKeyColumnUsage.getInteger( "length" ) ).isEqualTo( 50 );
		assertThat( mapKeyColumnUsage.getInteger( "precision" ) ).isEqualTo( 2 );
		assertThat( mapKeyColumnUsage.getInteger( "scale" ) ).isEqualTo( 1 );
	}

	/**
	 * When there's a single map key join column, we still wrap it with a
	 * MapKeyJoinColumns annotation.
	 */
	@Test
	public void testSingleMapKeyJoinColumn() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm14.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( MapKeyJoinColumn.class ) ).isTrue();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyJoinColumns.class ) ).isFalse();

		assertThat( memberDetails.hasAnnotationUsage( MapKey.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyClass.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyTemporal.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyEnumerated.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyColumn.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyColumn.class ) ).isFalse();

		final AnnotationUsage<MapKeyJoinColumn> joinColumnUsage = memberDetails.getAnnotationUsage( MapKeyJoinColumn.class );
		assertThat( joinColumnUsage.getString( "name" ) ).isEqualTo( "col1" );
	}

	@Test
	public void testMultipleMapKeyJoinColumns() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm15.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( MapKeyJoinColumn.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyJoinColumns.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( MapKey.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyClass.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyTemporal.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyEnumerated.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyColumn.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( MapKeyColumn.class ) ).isFalse();

		final AnnotationUsage<MapKeyJoinColumns> joinColumnsUsage = memberDetails.getAnnotationUsage( MapKeyJoinColumns.class );
		final List<AnnotationUsage<MapKeyJoinColumn>> joinColumnUsages = joinColumnsUsage.getList( "value" );
		assertThat( joinColumnUsages ).hasSize( 2 );

		final AnnotationUsage<MapKeyJoinColumn> joinColumnUsage0 = joinColumnUsages.get( 0 );
		assertThat( joinColumnUsage0.getString( "name" ) ).isEmpty();
		assertThat( joinColumnUsage0.getString( "referencedColumnName" ) ).isEmpty();
		assertThat( joinColumnUsage0.getBoolean( "unique" ) ).isFalse();
		assertThat( joinColumnUsage0.getBoolean( "nullable" ) ).isFalse();
		assertThat( joinColumnUsage0.getBoolean( "insertable" ) ).isTrue();
		assertThat( joinColumnUsage0.getBoolean( "updatable" ) ).isTrue();
		assertThat( joinColumnUsage0.getString( "columnDefinition" ) ).isEmpty();
		assertThat( joinColumnUsage0.getString( "table" ) ).isEmpty();

		final AnnotationUsage<MapKeyJoinColumn> joinColumnUsage1 = joinColumnUsages.get( 1 );
		assertThat( joinColumnUsage1.getString( "name" ) ).isEqualTo( "col1" );
		assertThat( joinColumnUsage1.getString( "referencedColumnName" ) ).isEqualTo( "col2" );
		assertThat( joinColumnUsage1.getBoolean( "unique" ) ).isTrue();
		assertThat( joinColumnUsage1.getBoolean( "nullable" ) ).isTrue();
		assertThat( joinColumnUsage1.getBoolean( "insertable" ) ).isFalse();
		assertThat( joinColumnUsage1.getBoolean( "updatable" ) ).isFalse();
		assertThat( joinColumnUsage1.getString( "columnDefinition" ) ).isEqualTo( "int" );
		assertThat( joinColumnUsage1.getString( "table" ) ).isEqualTo( "table1" );
	}

	@Test
	public void testColumnNoAttributes() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm16.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( Column.class ) ).isTrue();
		final AnnotationUsage<Column> columnUsage = memberDetails.getAnnotationUsage( Column.class );
		assertThat( columnUsage.getString( "name" ) ).isEmpty();
		assertThat( columnUsage.getBoolean( "unique" ) ).isFalse();
		assertThat( columnUsage.getBoolean( "nullable" ) ).isTrue();
		assertThat( columnUsage.getBoolean( "insertable" ) ).isTrue();
		assertThat( columnUsage.getBoolean( "updatable" ) ).isTrue();
		assertThat( columnUsage.getString( "columnDefinition" ) ).isEmpty();
		assertThat( columnUsage.getString( "table" ) ).isEmpty();
		assertThat( columnUsage.getInteger( "length" ) ).isEqualTo( 255 );
		assertThat( columnUsage.getInteger( "precision" ) ).isEqualTo( 0 );
		assertThat( columnUsage.getInteger( "scale" ) ).isEqualTo( 0 );
	}

	@Test
	public void testColumnAllAttributes() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm17.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( Column.class ) ).isTrue();
		final AnnotationUsage<Column> columnUsage = memberDetails.getAnnotationUsage( Column.class );
		assertThat( columnUsage.getString( "name" ) ).isEqualTo( "col1" );
		assertThat( columnUsage.getBoolean( "unique" ) ).isTrue();
		assertThat( columnUsage.getBoolean( "nullable" ) ).isFalse();
		assertThat( columnUsage.getBoolean( "insertable" ) ).isFalse();
		assertThat( columnUsage.getBoolean( "updatable" ) ).isFalse();
		assertThat( columnUsage.getString( "columnDefinition" ) ).isEqualTo( "int" );
		assertThat( columnUsage.getString( "table" ) ).isEqualTo( "table1" );
		assertThat( columnUsage.getInteger( "length" ) ).isEqualTo( 50 );
		assertThat( columnUsage.getInteger( "precision" ) ).isEqualTo( 2 );
		assertThat( columnUsage.getInteger( "scale" ) ).isEqualTo( 1 );
	}

	@Test
	public void testTemporal() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm18.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( Temporal.class ) ).isTrue();
		final AnnotationUsage<Temporal> temporalUsage = memberDetails.getAnnotationUsage( Temporal.class );
		assertThat( temporalUsage.getEnum( "value", TemporalType.class ) ).isEqualTo( TemporalType.DATE );
	}

	@Test
	public void testEnumerated() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm19.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( Enumerated.class ) ).isTrue();
		final AnnotationUsage<Enumerated> enumeratedUsage = memberDetails.getAnnotationUsage( Enumerated.class );
		assertThat( enumeratedUsage.getEnum( "value", EnumType.class ) ).isEqualTo( EnumType.STRING );
	}

	@Test
	public void testLob() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm20.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( Lob.class ) ).isTrue();
	}

	/**
	 * When there's a single attribute override, we still wrap it with an
	 * AttributeOverrides annotation.
	 */
	@Test
	public void testSingleAttributeOverride() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm21.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( AttributeOverride.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( AttributeOverrides.class ) ).isTrue();

		final AnnotationUsage<AttributeOverride> overrideUsage = memberDetails.getAnnotationUsage( AttributeOverride.class );
		assertThat( overrideUsage.getString( "name" ) ).isEqualTo( "value.field1" );

		final AnnotationUsage<Annotation> overrideColumnUsage = overrideUsage.getNestedUsage( "column" );
		assertThat( overrideColumnUsage.getString( "name" ) ).isEqualTo( "col1" );
	}

	@Test
	public void testMultipleAttributeOverrides() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm22.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( AttributeOverride.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( AttributeOverrides.class ) ).isTrue();

		final AnnotationUsage<AttributeOverrides> overridesUsage = memberDetails.getAnnotationUsage( AttributeOverrides.class );
		final List<AnnotationUsage<AttributeOverride>> overrideUsages = overridesUsage.getList( "value" );
		assertThat( overrideUsages ).hasSize( 2 );

		final AnnotationUsage<AttributeOverride> overrideUsage0 = overrideUsages.get( 0 );
		assertThat( overrideUsage0.getString( "name" ) ).isEqualTo( "value.field1" );
		final AnnotationUsage<Column> overrideColumnUsage0 = overrideUsage0.getNestedUsage( "column" );
		assertThat( overrideColumnUsage0.getString( "name" ) ).isEmpty();
		assertThat( overrideColumnUsage0.getBoolean( "unique" ) ).isFalse();
		assertThat( overrideColumnUsage0.getBoolean( "nullable" ) ).isTrue();
		assertThat( overrideColumnUsage0.getBoolean( "insertable" ) ).isTrue();
		assertThat( overrideColumnUsage0.getBoolean( "updatable" ) ).isTrue();
		assertThat( overrideColumnUsage0.getString( "columnDefinition" ) ).isEmpty();
		assertThat( overrideColumnUsage0.getString( "table" ) ).isEmpty();
		assertThat( overrideColumnUsage0.getInteger( "length" ) ).isEqualTo( 255 );
		assertThat( overrideColumnUsage0.getInteger( "precision" ) ).isEqualTo( 0 );
		assertThat( overrideColumnUsage0.getInteger( "scale" ) ).isEqualTo( 0 );

		final AnnotationUsage<AttributeOverride> overrideUsage1 = overrideUsages.get( 1 );
		assertThat( overrideUsage1.getString( "name" ) ).isEqualTo( "value.field2" );
		final AnnotationUsage<Column> overrideColumnUsage1 = overrideUsage1.getNestedUsage( "column" );
		assertThat( overrideColumnUsage1.getString( "name" ) ).isEqualTo( "col1" );
		assertThat( overrideColumnUsage1.getBoolean( "unique" ) ).isTrue();
		assertThat( overrideColumnUsage1.getBoolean( "nullable" ) ).isFalse();
		assertThat( overrideColumnUsage1.getBoolean( "insertable" ) ).isFalse();
		assertThat( overrideColumnUsage1.getBoolean( "updatable" ) ).isFalse();
		assertThat( overrideColumnUsage1.getString( "columnDefinition" ) ).isEqualTo( "int" );
		assertThat( overrideColumnUsage1.getString( "table" ) ).isEqualTo( "table1" );
		assertThat( overrideColumnUsage1.getInteger( "length" ) ).isEqualTo( 50 );
		assertThat( overrideColumnUsage1.getInteger( "precision" ) ).isEqualTo( 2 );
		assertThat( overrideColumnUsage1.getInteger( "scale" ) ).isEqualTo( 1 );
	}

	/**
	 * Tests that map-key-attribute-override and attribute-override elements
	 * both end up in the AttributeOverrides annotation.
	 */
	@Test
	public void testMixedAttributeOverrides() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm23.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( AttributeOverride.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( AttributeOverrides.class ) ).isTrue();

		final AnnotationUsage<AttributeOverrides> overridesUsage = memberDetails.getAnnotationUsage( AttributeOverrides.class );
		final List<AnnotationUsage<AttributeOverride>> overrideUsages = overridesUsage.getList( "value" );
		assertThat( overrideUsages ).hasSize( 2 );

		final AnnotationUsage<AttributeOverride> overrideUsage0 = overrideUsages.get( 0 );
		assertThat( overrideUsage0.getString( "name" ) ).isEqualTo( "key.field1" );
		final AnnotationUsage<Column> overrideColumnUsage0 = overrideUsage0.getNestedUsage( "column" );
		assertThat( overrideColumnUsage0.getString( "name" ) ).isEqualTo( "col1" );

		final AnnotationUsage<AttributeOverride> overrideUsage1 = overrideUsages.get( 1 );
		assertThat( overrideUsage1.getString( "name" ) ).isEqualTo( "value.field2" );
		final AnnotationUsage<Column> overrideColumnUsage1 = overrideUsage1.getNestedUsage( "column" );
		assertThat( overrideColumnUsage1.getString( "name" ) ).isEqualTo( "col2" );
	}

	/**
	 * When there's a single association override, we still wrap it with an
	 * AssociationOverrides annotation.
	 */
	@Test
	public void testSingleAssociationOverride() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm24.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( AssociationOverride.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( AssociationOverrides.class ) ).isTrue();

		final AnnotationUsage<AssociationOverride> overrideUsage = memberDetails.getAnnotationUsage( AssociationOverride.class );
		assertThat( overrideUsage.getString( "name" ) ).isEqualTo( "association1" );
		assertThat( overrideUsage.getList( "joinColumns" ) ).isEmpty();
		final AnnotationUsage<Annotation> joinTableUsage = overrideUsage.getNestedUsage( "joinTable" );
		assertThat( joinTableUsage.getString( "name" ) ).isEmpty();
	}

	@Test
	public void testMultipleAssociationOverridesJoinColumns() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm25.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( AssociationOverride.class ) ).isFalse();
		assertThat( memberDetails.hasAnnotationUsage( AssociationOverrides.class ) ).isTrue();

		final AnnotationUsage<AssociationOverrides> overridesUsage = memberDetails.getAnnotationUsage( AssociationOverrides.class );
		final List<AnnotationUsage<AssociationOverrides>> overrideUsages = overridesUsage.getList( "value" );
		assertThat( overrideUsages ).hasSize( 2 );

		{
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// First, an association using join table
			final AnnotationUsage<AssociationOverrides> overrideUsage = overrideUsages.get( 0 );
			assertThat( overrideUsage.getString( "name" ) ).isEqualTo( "association1" );
			assertThat( overrideUsage.getList( "joinColumns" ) ).isEmpty();

			AnnotationUsage<Annotation> joinTableUsage = overrideUsage.getNestedUsage( "joinTable" );
			assertThat( joinTableUsage.getString( "catalog" ) ).isEqualTo( "catalog1" );
			assertThat( joinTableUsage.getString( "name" ) ).isEqualTo( "table1" );
			assertThat( joinTableUsage.getString( "schema" ) ).isEqualTo( "schema1" );

			//JoinColumns
			final List<AnnotationUsage<JoinColumn>> joinColumUsages = joinTableUsage.getList( "joinColumns" );
			assertThat( joinColumUsages ).hasSize( 2 );

			final AnnotationUsage<JoinColumn> joinColumnUsage0 = joinColumUsages.get( 0 );
			assertThat( joinColumnUsage0.getString( "name" ) ).isEmpty();
			assertThat( joinColumnUsage0.getString( "referencedColumnName" ) ).isEmpty();
			assertThat( joinColumnUsage0.getString( "table" ) ).isEmpty();
			assertThat( joinColumnUsage0.getString( "columnDefinition" ) ).isEmpty();
			assertThat( joinColumnUsage0.getBoolean( "insertable" ) ).isTrue();
			assertThat( joinColumnUsage0.getBoolean( "updatable" ) ).isTrue();
			assertThat( joinColumnUsage0.getBoolean( "nullable" ) ).isTrue();
			assertThat( joinColumnUsage0.getBoolean( "unique" ) ).isFalse();

			final AnnotationUsage<JoinColumn> joinColumnUsage1 = joinColumUsages.get( 1 );
			assertThat( joinColumnUsage1.getString( "name" ) ).isEqualTo( "col1" );
			assertThat( joinColumnUsage1.getString( "referencedColumnName" ) ).isEqualTo( "col2" );
			assertThat( joinColumnUsage1.getString( "table" ) ).isEqualTo( "table2" );
			assertThat( joinColumnUsage1.getString( "columnDefinition" ) ).isEqualTo( "int" );
			assertThat( joinColumnUsage1.getBoolean( "insertable" ) ).isFalse();
			assertThat( joinColumnUsage1.getBoolean( "updatable" ) ).isFalse();
			assertThat( joinColumnUsage1.getBoolean( "nullable" ) ).isFalse();
			assertThat( joinColumnUsage1.getBoolean( "unique" ) ).isTrue();

			//InverseJoinColumns
			final List<AnnotationUsage<JoinColumn>> inverseJoinColumnUsages = joinTableUsage.getList( "inverseJoinColumns" );
			assertThat( inverseJoinColumnUsages ).hasSize( 2 );

			final AnnotationUsage<JoinColumn> inverseJoinColumnUsage0 = inverseJoinColumnUsages.get( 0 );
			assertThat( inverseJoinColumnUsage0.getString( "name" ) ).isEmpty();
			assertThat( inverseJoinColumnUsage0.getString( "referencedColumnName" ) ).isEmpty();
			assertThat( inverseJoinColumnUsage0.getString( "table" ) ).isEmpty();
			assertThat( inverseJoinColumnUsage0.getString( "columnDefinition" ) ).isEmpty();
			assertThat( inverseJoinColumnUsage0.getBoolean( "insertable" ) ).isTrue();
			assertThat( inverseJoinColumnUsage0.getBoolean( "updatable" ) ).isTrue();
			assertThat( inverseJoinColumnUsage0.getBoolean( "nullable" ) ).isTrue();
			assertThat( inverseJoinColumnUsage0.getBoolean( "unique" ) ).isFalse();

			final AnnotationUsage<JoinColumn> inverseJoinColumnUsage1 = inverseJoinColumnUsages.get( 1 );
			assertThat( inverseJoinColumnUsage1.getString( "name" ) ).isEqualTo( "col3" );
			assertThat( inverseJoinColumnUsage1.getString( "referencedColumnName" ) ).isEqualTo( "col4" );
			assertThat( inverseJoinColumnUsage1.getString( "table" ) ).isEqualTo( "table3" );
			assertThat( inverseJoinColumnUsage1.getString( "columnDefinition" ) ).isEqualTo( "int" );
			assertThat( inverseJoinColumnUsage1.getBoolean( "insertable" ) ).isFalse();
			assertThat( inverseJoinColumnUsage1.getBoolean( "updatable" ) ).isFalse();
			assertThat( inverseJoinColumnUsage1.getBoolean( "nullable" ) ).isFalse();
			assertThat( inverseJoinColumnUsage1.getBoolean( "unique" ) ).isTrue();

			//UniqueConstraints
			final List<AnnotationUsage<UniqueConstraint>> uniqueConstraintUsages = joinTableUsage.getList( "uniqueConstraints" );
			assertThat( uniqueConstraintUsages ).hasSize( 2 );

			final AnnotationUsage<UniqueConstraint> uniqueConstraintUsage0 = uniqueConstraintUsages.get( 0 );
			assertThat( uniqueConstraintUsage0.getString( "name" ) ).isEmpty();
			assertThat( uniqueConstraintUsage0.getList( "columnNames" ) ).containsOnly( "col5" );

			final AnnotationUsage<UniqueConstraint> uniqueConstraintUsage1 = uniqueConstraintUsages.get( 1 );
			assertThat( uniqueConstraintUsage1.getString( "name" ) ).isEqualTo( "uq1" );
			assertThat( uniqueConstraintUsage1.getList( "columnNames" ) ).containsOnly( "col6", "col7" );
		}

		{
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Second, an association using join columns
			final AnnotationUsage<AssociationOverrides> overrideUsage = overrideUsages.get( 1 );
			assertThat( overrideUsage.getString( "name" ) ).isEqualTo( "association2" );

			//JoinColumns
			final List<AnnotationUsage<JoinColumn>> joinColumUsages = overrideUsage.getList( "joinColumns" );
			assertThat( joinColumUsages ).hasSize( 2 );

			final AnnotationUsage<JoinColumn> joinColumnUsage0 = joinColumUsages.get( 0 );
			assertThat( joinColumnUsage0.getString( "name" ) ).isEmpty();
			assertThat( joinColumnUsage0.getString( "referencedColumnName" ) ).isEmpty();
			assertThat( joinColumnUsage0.getString( "table" ) ).isEmpty();
			assertThat( joinColumnUsage0.getString( "columnDefinition" ) ).isEmpty();
			assertThat( joinColumnUsage0.getBoolean( "insertable" ) ).isTrue();
			assertThat( joinColumnUsage0.getBoolean( "updatable" ) ).isTrue();
			assertThat( joinColumnUsage0.getBoolean( "nullable" ) ).isTrue();
			assertThat( joinColumnUsage0.getBoolean( "unique" ) ).isFalse();

			final AnnotationUsage<JoinColumn> joinColumnUsage1 = joinColumUsages.get( 1 );
			assertThat( joinColumnUsage1.getString( "name" ) ).isEqualTo( "col8" );
			assertThat( joinColumnUsage1.getString( "referencedColumnName" ) ).isEqualTo( "col9" );
			assertThat( joinColumnUsage1.getString( "table" ) ).isEqualTo( "table4" );
			assertThat( joinColumnUsage1.getString( "columnDefinition" ) ).isEqualTo( "int" );
			assertThat( joinColumnUsage1.getBoolean( "insertable" ) ).isFalse();
			assertThat( joinColumnUsage1.getBoolean( "updatable" ) ).isFalse();
			assertThat( joinColumnUsage1.getBoolean( "nullable" ) ).isFalse();
			assertThat( joinColumnUsage1.getBoolean( "unique" ) ).isTrue();
		}
	}

	@Test
	public void testCollectionTableNoChildren() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm26.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( CollectionTable.class ) ).isTrue();
		final AnnotationUsage<CollectionTable> collectionTableUsage = memberDetails.getAnnotationUsage( CollectionTable.class );
		assertThat( collectionTableUsage.getString( "name" ) ).isEmpty();
		assertThat( collectionTableUsage.getString( "catalog" ) ).isEmpty();
		assertThat( collectionTableUsage.getString( "schema" ) ).isEmpty();
		assertThat( collectionTableUsage.getList( "joinColumns" ) ).isEmpty();
		assertThat( collectionTableUsage.getList( "uniqueConstraints" ) ).isEmpty();
	}

	@Test
	public void testCollectionTableAllChildren() {
		final MemberDetails memberDetails = getAttributeMember( Entity3.class, "field1", "element-collection.orm27.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();

		assertThat( memberDetails.hasAnnotationUsage( CollectionTable.class ) ).isTrue();
		final AnnotationUsage<CollectionTable> collectionTableUsage = memberDetails.getAnnotationUsage( CollectionTable.class );
		assertThat( collectionTableUsage.getString( "name" ) ).isEqualTo( "table1" );
		assertThat( collectionTableUsage.getString( "catalog" ) ).isEqualTo( "catalog1" );
		assertThat( collectionTableUsage.getString( "schema" ) ).isEqualTo( "schema1" );

		//JoinColumns
		final List<AnnotationUsage<JoinColumn>> joinColumnUsages = collectionTableUsage.getList( "joinColumns" );
		assertThat( joinColumnUsages ).hasSize( 2 );

		final AnnotationUsage<JoinColumn> joinColumnUsage0 = joinColumnUsages.get( 0 );
		assertThat( joinColumnUsage0.getString( "name" ) ).isEmpty();
		assertThat( joinColumnUsage0.getString( "referencedColumnName" ) ).isEmpty();
		assertThat( joinColumnUsage0.getString( "table" ) ).isEmpty();
		assertThat( joinColumnUsage0.getString( "columnDefinition" ) ).isEmpty();
		assertThat( joinColumnUsage0.getBoolean( "insertable" ) ).isTrue();
		assertThat( joinColumnUsage0.getBoolean( "updatable" ) ).isTrue();
		assertThat( joinColumnUsage0.getBoolean( "nullable" ) ).isTrue();
		assertThat( joinColumnUsage0.getBoolean( "unique" ) ).isFalse();

		final AnnotationUsage<JoinColumn> joinColumnUsage1 = joinColumnUsages.get( 1 );
		assertThat( joinColumnUsage1.getString( "name" ) ).isEqualTo( "col1" );
		assertThat( joinColumnUsage1.getString( "referencedColumnName" ) ).isEqualTo( "col2" );
		assertThat( joinColumnUsage1.getString( "table" ) ).isEqualTo( "table2" );
		assertThat( joinColumnUsage1.getString( "columnDefinition" ) ).isEqualTo( "int" );
		assertThat( joinColumnUsage1.getBoolean( "insertable" ) ).isFalse();
		assertThat( joinColumnUsage1.getBoolean( "updatable" ) ).isFalse();
		assertThat( joinColumnUsage1.getBoolean( "nullable" ) ).isFalse();
		assertThat( joinColumnUsage1.getBoolean( "unique" ) ).isTrue();

		//UniqueConstraints
		final List<AnnotationUsage<UniqueConstraint>> uniqueConstraintUsages = collectionTableUsage.getList( "uniqueConstraints" );
		assertThat( uniqueConstraintUsages ).hasSize( 2 );

		final AnnotationUsage<UniqueConstraint> uniqueConstraintUsage0 = uniqueConstraintUsages.get( 0 );
		assertThat( uniqueConstraintUsage0.getString( "name" ) ).isEmpty();
		assertThat( uniqueConstraintUsage0.getList( "columnNames" ) ).containsOnly( "col3" );

		final AnnotationUsage<UniqueConstraint> uniqueConstraintUsage1 = uniqueConstraintUsages.get( 1 );
		assertThat( uniqueConstraintUsage1.getString( "name" ) ).isEqualTo( "uq1" );
		assertThat( uniqueConstraintUsage1.getList( "columnNames" ) ).containsOnly( "col4", "col5" );
	}

	@Test
	public void testAllAttributes() {
		final MemberDetails memberDetails = getAttributeMember( Entity2.class, "field1", "element-collection.orm28.xml" );
		assertThat( memberDetails.hasAnnotationUsage( ElementCollection.class ) ).isTrue();

		final AnnotationUsage<ElementCollection> elementCollectionUsage = memberDetails.getAnnotationUsage( ElementCollection.class );
		assertThat( elementCollectionUsage.getEnum( "fetch", FetchType.class ) ).isEqualTo( FetchType.EAGER );

		final AnnotationUsage<Access> accessUsage = memberDetails.getAnnotationUsage( Access.class );
		assertThat( accessUsage.getEnum( "value", AccessType.class ) ).isEqualTo( AccessType.PROPERTY );

		final AnnotationUsage<Target> targetUsage = memberDetails.getAnnotationUsage( Target.class );
		assertThat( targetUsage.getString( "value" ) ).isEqualTo( Entity3.class.getName() );
	}

}
