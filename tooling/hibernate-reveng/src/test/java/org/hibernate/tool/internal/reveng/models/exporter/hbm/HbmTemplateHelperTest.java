/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.exporter.hbm;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.InheritanceType;

import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.InheritanceMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HbmTemplateHelper}.
 *
 * @author Koen Aers
 */
public class HbmTemplateHelperTest {

	private HbmTemplateHelper create(TableMetadata table) {
		return new HbmTemplateHelper(table);
	}

	// --- getHibernateTypeName ---

	@Test
	public void testGetHibernateTypeNameFromStoredValue() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		ColumnMetadata col = new ColumnMetadata("NAME", "name", String.class)
				.hibernateTypeName("string");
		assertEquals("string", create(table).getHibernateTypeName(col));
	}

	@Test
	public void testGetHibernateTypeNameFallbackFromJavaClass() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		ColumnMetadata col = new ColumnMetadata("NAME", "name", String.class);
		assertEquals("string", create(table).getHibernateTypeName(col));
	}

	@Test
	public void testGetHibernateTypeNameFallbackWrapperType() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		ColumnMetadata col = new ColumnMetadata("ID", "id", Long.class);
		assertEquals("java.lang.Long", create(table).getHibernateTypeName(col));
	}

	@Test
	public void testGetHibernateTypeNameStoredOverridesFallback() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		ColumnMetadata col = new ColumnMetadata("ID", "id", Long.class)
				.hibernateTypeName("long");
		assertEquals("long", create(table).getHibernateTypeName(col));
	}

	// --- getGeneratorClass ---

	@Test
	public void testGetGeneratorClassIdentity() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		assertEquals("identity", create(table).getGeneratorClass(GenerationType.IDENTITY));
	}

	@Test
	public void testGetGeneratorClassSequence() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		assertEquals("sequence", create(table).getGeneratorClass(GenerationType.SEQUENCE));
	}

	@Test
	public void testGetGeneratorClassTable() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		assertEquals("table", create(table).getGeneratorClass(GenerationType.TABLE));
	}

	@Test
	public void testGetGeneratorClassAuto() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		assertEquals("native", create(table).getGeneratorClass(GenerationType.AUTO));
	}

	@Test
	public void testGetGeneratorClassUuid() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		assertEquals("uuid2", create(table).getGeneratorClass(GenerationType.UUID));
	}

	@Test
	public void testGetGeneratorClassNull() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		assertEquals("assigned", create(table).getGeneratorClass(null));
	}

	// --- getClassTag ---

	@Test
	public void testGetClassTagRootEntity() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("class", create(table).getClassTag());
	}

	@Test
	public void testGetClassTagSingleTableSubclass() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.parent("Vehicle", "com.example");
		assertEquals("subclass", create(table).getClassTag());
	}

	@Test
	public void testGetClassTagJoinedSubclass() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.parent("Vehicle", "com.example");
		table.primaryKeyJoinColumn("VEHICLE_ID");
		assertEquals("joined-subclass", create(table).getClassTag());
	}

	@Test
	public void testGetClassTagTablePerClassSubclass() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.parent("Vehicle", "com.example");
		table.inheritance(new InheritanceMetadata(InheritanceType.TABLE_PER_CLASS));
		assertEquals("union-subclass", create(table).getClassTag());
	}

	// --- getFullClassName ---

	@Test
	public void testGetFullClassName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("com.example.Employee", create(table).getFullClassName());
	}

	@Test
	public void testGetFullClassNameNoPackage() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "");
		assertEquals("Employee", create(table).getFullClassName());
	}

	@Test
	public void testGetFullClassNameNullPackage() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", null);
		assertEquals("Employee", create(table).getFullClassName());
	}

	// --- getFullParentClassName ---

	@Test
	public void testGetFullParentClassName() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.parent("Vehicle", "com.example");
		assertEquals("com.example.Vehicle", create(table).getFullParentClassName());
	}

	@Test
	public void testGetFullParentClassNameNoPackage() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.parent("Vehicle", "");
		assertEquals("Vehicle", create(table).getFullParentClassName());
	}

	// --- getColumnAttributes ---

	@Test
	public void testGetColumnAttributesAllDefaults() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		ColumnMetadata col = new ColumnMetadata("NAME", "name", String.class);
		assertEquals("", create(table).getColumnAttributes(col));
	}

	@Test
	public void testGetColumnAttributesNotNull() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		ColumnMetadata col = new ColumnMetadata("NAME", "name", String.class).nullable(false);
		assertTrue(create(table).getColumnAttributes(col).contains("not-null=\"true\""));
	}

	@Test
	public void testGetColumnAttributesUnique() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		ColumnMetadata col = new ColumnMetadata("EMAIL", "email", String.class).unique(true);
		assertTrue(create(table).getColumnAttributes(col).contains("unique=\"true\""));
	}

	@Test
	public void testGetColumnAttributesLength() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		ColumnMetadata col = new ColumnMetadata("NAME", "name", String.class).length(100);
		assertTrue(create(table).getColumnAttributes(col).contains("length=\"100\""));
	}

	@Test
	public void testGetColumnAttributesPrecisionAndScale() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		ColumnMetadata col = new ColumnMetadata("PRICE", "price", BigDecimal.class)
				.precision(10).scale(2);
		String attrs = create(table).getColumnAttributes(col);
		assertTrue(attrs.contains("precision=\"10\""), attrs);
		assertTrue(attrs.contains("scale=\"2\""), attrs);
	}

	@Test
	public void testGetColumnAttributesCombined() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		ColumnMetadata col = new ColumnMetadata("NAME", "name", String.class)
				.nullable(false).unique(true).length(255);
		String attrs = create(table).getColumnAttributes(col);
		assertTrue(attrs.contains("not-null=\"true\""), attrs);
		assertTrue(attrs.contains("unique=\"true\""), attrs);
		assertTrue(attrs.contains("length=\"255\""), attrs);
	}

	// --- getCascadeString ---

	@Test
	public void testGetCascadeStringNone() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		assertEquals("none", create(table).getCascadeString(null));
	}

	@Test
	public void testGetCascadeStringEmpty() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		assertEquals("none", create(table).getCascadeString(new CascadeType[0]));
	}

	@Test
	public void testGetCascadeStringAll() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		assertEquals("all", create(table).getCascadeString(new CascadeType[] { CascadeType.ALL }));
	}

	@Test
	public void testGetCascadeStringPersist() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		assertEquals("persist", create(table).getCascadeString(new CascadeType[] { CascadeType.PERSIST }));
	}

	@Test
	public void testGetCascadeStringMerge() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		assertEquals("merge", create(table).getCascadeString(new CascadeType[] { CascadeType.MERGE }));
	}

	@Test
	public void testGetCascadeStringRemove() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		assertEquals("delete", create(table).getCascadeString(new CascadeType[] { CascadeType.REMOVE }));
	}

	@Test
	public void testGetCascadeStringRefresh() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		assertEquals("refresh", create(table).getCascadeString(new CascadeType[] { CascadeType.REFRESH }));
	}

	@Test
	public void testGetCascadeStringDetach() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		assertEquals("evict", create(table).getCascadeString(new CascadeType[] { CascadeType.DETACH }));
	}

	@Test
	public void testGetCascadeStringMultiple() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		assertEquals("persist, merge", create(table).getCascadeString(
				new CascadeType[] { CascadeType.PERSIST, CascadeType.MERGE }));
	}

	// --- isSubclass ---

	@Test
	public void testIsSubclassFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertFalse(create(table).isSubclass());
	}

	@Test
	public void testIsSubclassTrue() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.parent("Vehicle", "com.example");
		assertTrue(create(table).isSubclass());
	}

	// --- needsDiscriminator ---

	@Test
	public void testNeedsDiscriminatorNoInheritance() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertFalse(create(table).needsDiscriminator());
	}

	@Test
	public void testNeedsDiscriminatorNoColumn() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE));
		assertFalse(create(table).needsDiscriminator());
	}

	@Test
	public void testNeedsDiscriminatorWithColumn() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
				.discriminatorColumn("DTYPE"));
		assertTrue(create(table).needsDiscriminator());
	}

	// --- getDiscriminatorTypeName ---

	@Test
	public void testGetDiscriminatorTypeNameString() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
				.discriminatorType(DiscriminatorType.STRING));
		assertEquals("string", create(table).getDiscriminatorTypeName());
	}

	@Test
	public void testGetDiscriminatorTypeNameChar() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
				.discriminatorType(DiscriminatorType.CHAR));
		assertEquals("character", create(table).getDiscriminatorTypeName());
	}

	@Test
	public void testGetDiscriminatorTypeNameInteger() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
				.discriminatorType(DiscriminatorType.INTEGER));
		assertEquals("integer", create(table).getDiscriminatorTypeName());
	}

	@Test
	public void testGetDiscriminatorTypeNameDefault() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE));
		assertEquals("string", create(table).getDiscriminatorTypeName());
	}

	@Test
	public void testGetDiscriminatorTypeNameNoInheritance() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("string", create(table).getDiscriminatorTypeName());
	}
}
