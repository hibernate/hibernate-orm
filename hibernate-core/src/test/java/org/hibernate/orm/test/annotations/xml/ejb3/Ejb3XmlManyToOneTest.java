/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.ejb3;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.boot.internal.Target;
import org.hibernate.models.spi.MemberDetails;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.UniqueConstraint;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-14529")
public class Ejb3XmlManyToOneTest extends Ejb3XmlTestCase {
	@Test
	public void testNoJoins() {
		final MemberDetails memberDetails = getAttributeMember( Entity1.class, "field1", "many-to-one.orm1.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ManyToOne.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( JoinColumn.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( JoinColumns.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( JoinTable.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( Id.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapsId.class ) ).isFalse();

		final ManyToOne manyToOneUsage = memberDetails.getDirectAnnotationUsage( ManyToOne.class );
		assertThat( manyToOneUsage.cascade() ).isEmpty();
		assertThat( manyToOneUsage.fetch() ).isEqualTo( FetchType.EAGER );
		assertThat( manyToOneUsage.optional() ).isTrue();
		assertThat( manyToOneUsage.targetEntity() ).isEqualTo( void.class );
	}

	/**
	 * When there's a single join column, we still wrap it with a JoinColumns
	 * annotation.
	 */
	@Test
	public void testSingleJoinColumn() {
		final MemberDetails memberDetails = getAttributeMember( Entity1.class, "field1", "many-to-one.orm2.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ManyToOne.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( JoinColumnsOrFormulas.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( JoinColumnOrFormula.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( JoinColumns.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( JoinColumn.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( JoinFormula.class ) ).isFalse();

		assertThat( memberDetails.hasDirectAnnotationUsage( JoinTable.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( Id.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapsId.class ) ).isFalse();

		final JoinColumnsOrFormulas joinColumnsOrFormulasUsage = memberDetails.getDirectAnnotationUsage( JoinColumnsOrFormulas.class );
		final JoinColumn joinColumnUsage = joinColumnsOrFormulasUsage.value()[0].column();
		assertThat( joinColumnUsage.name() ).isEqualTo( "col1" );
		assertThat( joinColumnUsage.referencedColumnName() ).isEqualTo( "col2" );
		assertThat( joinColumnUsage.table() ).isEqualTo( "table1" );
	}

	@Test
	public void testMultipleJoinColumns() {
		final MemberDetails memberDetails = getAttributeMember( Entity1.class, "field1", "many-to-one.orm3.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ManyToOne.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( JoinColumnsOrFormulas.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( JoinColumnOrFormula.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( JoinFormula.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( JoinColumn.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( JoinTable.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( Id.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapsId.class ) ).isFalse();

		final JoinColumnsOrFormulas joinColumnsOrFormulasUsage = memberDetails.getDirectAnnotationUsage( JoinColumnsOrFormulas.class );
		final JoinColumnOrFormula[] joinColumnOrFormulaUsages = joinColumnsOrFormulasUsage.value();
		assertThat( joinColumnOrFormulaUsages ).hasSize( 2 );

		final JoinColumn joinColumnUsage0 = joinColumnOrFormulaUsages[0].column();
		assertThat( joinColumnUsage0.name() ).isEmpty();
		assertThat( joinColumnUsage0.referencedColumnName() ).isEmpty();
		assertThat( joinColumnUsage0.table() ).isEmpty();
		assertThat( joinColumnUsage0.columnDefinition() ).isEmpty();
		assertThat( joinColumnUsage0.insertable() ).isTrue();
		assertThat( joinColumnUsage0.updatable() ).isTrue();
		assertThat( joinColumnUsage0.nullable() ).isTrue();
		assertThat( joinColumnUsage0.unique() ).isFalse();

		final JoinColumn joinColumnUsage1 = joinColumnOrFormulaUsages[1].column();
		assertThat( joinColumnUsage1.name() ).isEqualTo( "col1" );
		assertThat( joinColumnUsage1.referencedColumnName() ).isEqualTo( "col2" );
		assertThat( joinColumnUsage1.table() ).isEqualTo( "table1" );
		assertThat( joinColumnUsage1.columnDefinition() ).isEqualTo( "int" );
		assertThat( joinColumnUsage1.insertable() ).isFalse();
		assertThat( joinColumnUsage1.updatable() ).isFalse();
		assertThat( joinColumnUsage1.nullable() ).isFalse();
		assertThat( joinColumnUsage1.unique() ).isTrue();
	}

	@Test
	public void testJoinTableNoChildren() {
		final MemberDetails memberDetails = getAttributeMember( Entity1.class, "field1", "many-to-one.orm4.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ManyToOne.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( JoinTable.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( JoinColumns.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( JoinColumn.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( Id.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapsId.class ) ).isFalse();

		final JoinTable joinTableUsage = memberDetails.getDirectAnnotationUsage( JoinTable.class );
		assertThat( joinTableUsage.catalog() ).isEmpty();
		assertThat( joinTableUsage.schema() ).isEmpty();
		assertThat( joinTableUsage.name() ).isEmpty();
		assertThat( joinTableUsage.joinColumns() ).isEmpty();
		assertThat( joinTableUsage.inverseJoinColumns() ).isEmpty();
		assertThat( joinTableUsage.uniqueConstraints() ).isEmpty();
	}

	@Test
	public void testJoinTableAllChildren() {
		final MemberDetails memberDetails = getAttributeMember( Entity1.class, "field1", "many-to-one.orm5.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ManyToOne.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( JoinTable.class ) ).isTrue();

		assertThat( memberDetails.hasDirectAnnotationUsage( JoinColumns.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( JoinColumn.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( Id.class ) ).isFalse();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapsId.class ) ).isFalse();

		final JoinTable joinTableUsage = memberDetails.getDirectAnnotationUsage( JoinTable.class );
		assertThat( joinTableUsage.catalog() ).isEqualTo( "cat1" );
		assertThat( joinTableUsage.schema() ).isEqualTo( "schema1" );
		assertThat( joinTableUsage.name() ).isEqualTo( "table1" );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// JoinColumns
		final JoinColumn[] joinColumnUsages = joinTableUsage.joinColumns();
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

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// InverseJoinColumns
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

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// UniqueConstraints
		final UniqueConstraint[] uniqueConstraintUsages = joinTableUsage.uniqueConstraints();
		assertThat( uniqueConstraintUsages ).hasSize( 2 );

		final UniqueConstraint uniqueConstraintUsage0 = uniqueConstraintUsages[0];
		assertThat( uniqueConstraintUsage0.name() ).isEmpty();
		assertThat( uniqueConstraintUsage0.columnNames() ).containsExactly( "col5" );

		final UniqueConstraint uniqueConstraintUsage1 = uniqueConstraintUsages[1];
		assertThat( uniqueConstraintUsage1.name() ).isEqualTo( "uq1" );
		assertThat( uniqueConstraintUsage1.columnNames() ).containsExactly( "col6", "col7" );
	}

	@Test
	public void testAllAttributes() {
		final MemberDetails memberDetails = getAttributeMember( Entity1.class, "field1", "many-to-one.orm6.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ManyToOne.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( Id.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( MapsId.class ) ).isTrue();
		assertThat( memberDetails.hasDirectAnnotationUsage( Access.class ) ).isTrue();

		final ManyToOne manyToOneUsage = memberDetails.getDirectAnnotationUsage( ManyToOne.class );
		assertThat( manyToOneUsage.cascade() ).isEmpty();
		assertThat( manyToOneUsage.fetch() ).isEqualTo( FetchType.LAZY );
		assertThat( manyToOneUsage.optional() ).isFalse();
		assertThat( manyToOneUsage.targetEntity() ).isEqualTo( void.class );

		final Target targetUsage = memberDetails.getDirectAnnotationUsage( Target.class );
		assertThat( targetUsage.value() ).isEqualTo( Entity3.class.getName() );

		final MapsId mapsIdUsage = memberDetails.getDirectAnnotationUsage( MapsId.class );
		assertThat( mapsIdUsage.value() ).isEqualTo( "col1" );

		final Access accessUsage = memberDetails.getDirectAnnotationUsage( Access.class );
		assertThat( accessUsage.value() ).isEqualTo( AccessType.PROPERTY );
	}

	@Test
	public void testCascadeAll() {
		final MemberDetails memberDetails = getAttributeMember( Entity1.class, "field1", "many-to-one.orm7.xml" );
		assertThat( memberDetails.hasDirectAnnotationUsage( ManyToOne.class ) ).isTrue();

		final ManyToOne manyToOneUsage = memberDetails.getDirectAnnotationUsage( ManyToOne.class );
		assertThat( manyToOneUsage.cascade() ).isEmpty();

		final Cascade cascadeUsage = memberDetails.getDirectAnnotationUsage( Cascade.class );
		assertThat( cascadeUsage.value() ).containsExactly( CascadeType.ALL );
	}

	@Test
	public void testCascadeSomeWithDefaultPersist() {
		final MemberDetails memberDetails = getAttributeMember( Entity1.class, "field1", "many-to-one.orm8.xml" );
		final ManyToOne manyToOneUsage = memberDetails.getDirectAnnotationUsage( ManyToOne.class );
		assertThat( manyToOneUsage.cascade() ).isEmpty();

		final Cascade cascadeUsage = memberDetails.getDirectAnnotationUsage( Cascade.class );
		assertThat( cascadeUsage.value() ).containsOnly(
				CascadeType.PERSIST,
				CascadeType.REMOVE,
				CascadeType.REFRESH,
				CascadeType.DETACH
		);
	}

	/**
	 * Make sure that it doesn't break the handler when {@link CascadeType#ALL}
	 * is specified in addition to a default cascade-persist or individual
	 * cascade settings.
	 */
	@Test
	public void testCascadeAllPlusMore() {
		final MemberDetails memberDetails = getAttributeMember( Entity1.class, "field1", "many-to-one.orm9.xml" );
		final ManyToOne manyToOneUsage = memberDetails.getDirectAnnotationUsage( ManyToOne.class );
		assertThat( manyToOneUsage.cascade() ).isEmpty();

		final Cascade cascadeUsage = memberDetails.getDirectAnnotationUsage( Cascade.class );
		assertThat( cascadeUsage.value() ).containsOnly(
				CascadeType.ALL,
				CascadeType.PERSIST,
				CascadeType.MERGE,
				CascadeType.REMOVE,
				CascadeType.REFRESH,
				CascadeType.DETACH
		);
	}

}
