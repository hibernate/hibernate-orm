/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.persister.entity;

import java.util.List;

import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableColumn;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The entity temporary table ({@value TemporaryTable#ENTITY_TABLE_PREFIX}) must mirror the physical
 * column names of the entity, not the names of the Java fields the columns are mapped to.
 *
 * @author Marek Hajdík
 */
@JiraKey("HHH-20343")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTable.class)
@DomainModel(annotatedClasses = {
		TemporaryTableColumnNamingTest.Document.class,
		TemporaryTableColumnNamingTest.Animal.class,
		TemporaryTableColumnNamingTest.Dog.class
})
@SessionFactory
public class TemporaryTableColumnNamingTest {

	@Test
	public void testSingleTableEntity(SessionFactoryScope scope) {
		assertThat( entityTableColumnNames( scope, Document.class ) )
				.containsExactlyInAnyOrder( "ID_", "DESCRIPTION_", "STREET_", "ZIP_", "PARENT_ID_" );
	}

	@Test
	public void testJoinedInheritance(SessionFactoryScope scope) {
		assertThat( entityTableColumnNames( scope, Animal.class ) )
				.containsExactlyInAnyOrder( "ANIMAL_ID_", "NAME_" );
		// ANIMAL.NAME_ and DOG.NAME_ are distinct columns of the same entity,
		// so the second one is qualified with the name of the table it belongs to
		assertThat( entityTableColumnNames( scope, Dog.class ) )
				.containsExactlyInAnyOrder( "ANIMAL_ID_", "ANIMAL0_NAME_", "BREED_", "DOG0_NAME_" );
	}

	private static List<String> entityTableColumnNames(SessionFactoryScope scope, Class<?> entityType) {
		final SqmMultiTableInsertStrategy insertStrategy = scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( entityType )
				.getSqmMultiTableInsertStrategy();
		assertThat( insertStrategy )
				.as( "No multi-table insert strategy for " + entityType.getSimpleName() )
				.isNotNull();
		final TemporaryTable entityTable = entityTable( insertStrategy );
		assumeTrue( entityTable != null, "Dialect does not use a temporary table based insert strategy" );
		return entityTable.getColumns().stream()
				// Skip the synthetic columns, they are not mapped to any entity attribute
				.filter( column -> column != entityTable.getSessionUidColumn() )
				.map( TemporaryTableColumn::getColumnName )
				.filter( columnName -> !TemporaryTable.ENTITY_TABLE_IDENTITY_COLUMN.equals( columnName )
						&& !TemporaryTable.ENTITY_ROW_NUMBER_COLUMN.equals( columnName ) )
				.toList();
	}

	private static TemporaryTable entityTable(SqmMultiTableInsertStrategy insertStrategy) {
		if ( insertStrategy instanceof LocalTemporaryTableInsertStrategy strategy ) {
			return strategy.getTemporaryTable();
		}
		else if ( insertStrategy instanceof GlobalTemporaryTableInsertStrategy strategy ) {
			return strategy.getTemporaryTable();
		}
		else if ( insertStrategy instanceof PersistentTableInsertStrategy strategy ) {
			return strategy.getTemporaryTable();
		}
		else {
			return null;
		}
	}

	/**
	 * A pooled sequence generator forces the use of an entity temporary table,
	 * even though the entity itself is mapped to a single table.
	 */
	@Entity
	@Table(name = "DOCUMENT_TABLE")
	public static class Document {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "document_generator")
		@SequenceGenerator(name = "document_generator", sequenceName = "DOCUMENT_SEQ", allocationSize = 50)
		@Column(name = "ID_")
		private Long id;

		@Column(name = "DESCRIPTION_")
		private String description;

		@Embedded
		private Address address;

		@ManyToOne
		@JoinColumn(name = "PARENT_ID_")
		private Document parent;
	}

	@Embeddable
	public static class Address {

		@Column(name = "STREET_")
		private String street;

		@Column(name = "ZIP_")
		private String zipCode;
	}

	@Entity
	@Table(name = "ANIMAL")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Animal {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "animal_generator")
		@SequenceGenerator(name = "animal_generator", sequenceName = "ANIMAL_SEQ", allocationSize = 50)
		@Column(name = "ANIMAL_ID_")
		private Long id;

		@Column(name = "NAME_")
		private String name;
	}

	@Entity
	@Table(name = "DOG")
	public static class Dog extends Animal {

		@Column(name = "BREED_")
		private String breed;

		@Column(name = "NAME_")
		private String ownerName;
	}
}
