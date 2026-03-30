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

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.tool.internal.reveng.models.builder.DynamicEntityBuilder;
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
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails classDetails = builder.createEntityFromTable(table);
		return new HbmTemplateHelper(classDetails);
	}

	// --- getHibernateTypeName ---

	@Test
	public void testGetHibernateTypeNameString() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		HbmTemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("string", helper.getHibernateTypeName(field));
	}

	@Test
	public void testGetHibernateTypeNameWrapperType() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		HbmTemplateHelper helper = create(table);
		FieldDetails field = helper.getIdFields().get(0);
		assertEquals("java.lang.Long", helper.getHibernateTypeName(field));
	}

	@Test
	public void testGetHibernateTypeNameBigDecimal() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("PRICE", "price", BigDecimal.class));
		HbmTemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("big_decimal", helper.getHibernateTypeName(field));
	}

	// --- toGeneratorClass ---

	@Test
	public void testGetGeneratorClassIdentity() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("identity", create(table).toGeneratorClass(GenerationType.IDENTITY));
	}

	@Test
	public void testGetGeneratorClassSequence() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("sequence", create(table).toGeneratorClass(GenerationType.SEQUENCE));
	}

	@Test
	public void testGetGeneratorClassTable() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("table", create(table).toGeneratorClass(GenerationType.TABLE));
	}

	@Test
	public void testGetGeneratorClassAuto() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("native", create(table).toGeneratorClass(GenerationType.AUTO));
	}

	@Test
	public void testGetGeneratorClassUuid() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("uuid2", create(table).toGeneratorClass(GenerationType.UUID));
	}

	@Test
	public void testGetGeneratorClassNull() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("assigned", create(table).toGeneratorClass(null));
	}

	@Test
	public void testGetGeneratorClassFromField() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).generationType(GenerationType.IDENTITY));
		HbmTemplateHelper helper = create(table);
		assertEquals("identity", helper.getGeneratorClass(helper.getIdFields().get(0)));
	}

	@Test
	public void testGetGeneratorClassFromFieldNoGeneration() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		HbmTemplateHelper helper = create(table);
		assertEquals("assigned", helper.getGeneratorClass(helper.getIdFields().get(0)));
	}

	// --- getClassTag ---

	@Test
	public void testGetClassTagRootEntity() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("class", create(table).getClassTag());
	}

	@Test
	public void testGetClassTagSingleTableSubclass() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.parent("Vehicle", "com.example");
		assertEquals("subclass", create(table).getClassTag());
	}

	@Test
	public void testGetClassTagJoinedSubclass() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.parent("Vehicle", "com.example");
		table.primaryKeyJoinColumn("VEHICLE_ID");
		assertEquals("joined-subclass", create(table).getClassTag());
	}

	@Test
	public void testGetClassTagTablePerClassSubclass() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.parent("Vehicle", "com.example");
		table.inheritance(new InheritanceMetadata(InheritanceType.TABLE_PER_CLASS));
		assertEquals("union-subclass", create(table).getClassTag());
	}

	// --- getClassName ---

	@Test
	public void testGetClassName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("com.example.Employee", create(table).getClassName());
	}

	@Test
	public void testGetClassNameNoPackage() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("Employee", create(table).getClassName());
	}

	// --- getParentClassName ---

	@Test
	public void testGetParentClassName() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.parent("Vehicle", "com.example");
		assertEquals("com.example.Vehicle", create(table).getParentClassName());
	}

	@Test
	public void testGetParentClassNameNoParent() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertNull(create(table).getParentClassName());
	}

	// --- getColumnAttributes ---

	@Test
	public void testGetColumnAttributesAllDefaults() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		HbmTemplateHelper helper = create(table);
		assertEquals("", helper.getColumnAttributes(helper.getBasicFields().get(0)));
	}

	@Test
	public void testGetColumnAttributesNotNull() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class).nullable(false));
		HbmTemplateHelper helper = create(table);
		assertTrue(helper.getColumnAttributes(helper.getBasicFields().get(0)).contains("not-null=\"true\""));
	}

	@Test
	public void testGetColumnAttributesUnique() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("EMAIL", "email", String.class).unique(true));
		HbmTemplateHelper helper = create(table);
		assertTrue(helper.getColumnAttributes(helper.getBasicFields().get(0)).contains("unique=\"true\""));
	}

	@Test
	public void testGetColumnAttributesLength() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class).length(100));
		HbmTemplateHelper helper = create(table);
		assertTrue(helper.getColumnAttributes(helper.getBasicFields().get(0)).contains("length=\"100\""));
	}

	@Test
	public void testGetColumnAttributesPrecisionAndScale() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("PRICE", "price", BigDecimal.class)
				.precision(10).scale(2));
		HbmTemplateHelper helper = create(table);
		String attrs = helper.getColumnAttributes(helper.getBasicFields().get(0));
		assertTrue(attrs.contains("precision=\"10\""), attrs);
		assertTrue(attrs.contains("scale=\"2\""), attrs);
	}

	@Test
	public void testGetColumnAttributesCombined() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class)
				.nullable(false).unique(true).length(100));
		HbmTemplateHelper helper = create(table);
		String attrs = helper.getColumnAttributes(helper.getBasicFields().get(0));
		assertTrue(attrs.contains("not-null=\"true\""), attrs);
		assertTrue(attrs.contains("unique=\"true\""), attrs);
		assertTrue(attrs.contains("length=\"100\""), attrs);
	}

	// --- getCascadeString ---

	@Test
	public void testGetCascadeStringNone() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("none", create(table).getCascadeString(null));
	}

	@Test
	public void testGetCascadeStringEmpty() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("none", create(table).getCascadeString(new CascadeType[0]));
	}

	@Test
	public void testGetCascadeStringAll() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("all", create(table).getCascadeString(new CascadeType[] { CascadeType.ALL }));
	}

	@Test
	public void testGetCascadeStringPersist() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("persist", create(table).getCascadeString(new CascadeType[] { CascadeType.PERSIST }));
	}

	@Test
	public void testGetCascadeStringMerge() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("merge", create(table).getCascadeString(new CascadeType[] { CascadeType.MERGE }));
	}

	@Test
	public void testGetCascadeStringRemove() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("delete", create(table).getCascadeString(new CascadeType[] { CascadeType.REMOVE }));
	}

	@Test
	public void testGetCascadeStringRefresh() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("refresh", create(table).getCascadeString(new CascadeType[] { CascadeType.REFRESH }));
	}

	@Test
	public void testGetCascadeStringDetach() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("evict", create(table).getCascadeString(new CascadeType[] { CascadeType.DETACH }));
	}

	@Test
	public void testGetCascadeStringMultiple() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("persist, merge", create(table).getCascadeString(
				new CascadeType[] { CascadeType.PERSIST, CascadeType.MERGE }));
	}

	// --- isSubclass ---

	@Test
	public void testIsSubclassFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertFalse(create(table).isSubclass());
	}

	@Test
	public void testIsSubclassTrue() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.parent("Vehicle", "com.example");
		assertTrue(create(table).isSubclass());
	}

	// --- needsDiscriminator ---

	@Test
	public void testNeedsDiscriminatorNoInheritance() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertFalse(create(table).needsDiscriminator());
	}

	@Test
	public void testNeedsDiscriminatorNoColumn() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE));
		assertFalse(create(table).needsDiscriminator());
	}

	@Test
	public void testNeedsDiscriminatorWithColumn() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
				.discriminatorColumn("DTYPE"));
		assertTrue(create(table).needsDiscriminator());
	}

	// --- getDiscriminatorTypeName ---

	@Test
	public void testGetDiscriminatorTypeNameChar() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
				.discriminatorColumn("DTYPE")
				.discriminatorType(DiscriminatorType.CHAR));
		assertEquals("character", create(table).getDiscriminatorTypeName());
	}

	@Test
	public void testGetDiscriminatorTypeNameInteger() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
				.discriminatorColumn("DTYPE")
				.discriminatorType(DiscriminatorType.INTEGER));
		assertEquals("integer", create(table).getDiscriminatorTypeName());
	}

	@Test
	public void testGetDiscriminatorTypeNameNoInheritance() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("string", create(table).getDiscriminatorTypeName());
	}
}
