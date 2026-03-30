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
package org.hibernate.tool.internal.reveng.models.exporter.mapping;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.TemporalType;

import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.BatchSizeAnnotation;
import org.hibernate.boot.models.annotations.internal.OptimisticLockingAnnotation;
import org.hibernate.boot.models.annotations.internal.RowIdAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLRestrictionAnnotation;
import org.hibernate.boot.models.annotations.internal.SubselectAnnotation;
import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.tool.internal.reveng.models.builder.DynamicEntityBuilder;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.CompositeIdMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.EmbeddedFieldMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ForeignKeyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.InheritanceMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ManyToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToOneMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MappingXmlHelper}.
 *
 * @author Koen Aers
 */
public class MappingXmlHelperTest {

	private MappingXmlHelper create(TableMetadata table) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails classDetails = builder.createEntityFromTable(table);
		return new MappingXmlHelper(classDetails);
	}

	private DynamicClassDetails createMinimalEntity(ModelsContext ctx) {
		DynamicClassDetails entity = new DynamicClassDetails(
				"TestEntity", "com.example.TestEntity",
				false, null, null, ctx);
		entity.addAnnotationUsage(JpaAnnotations.ENTITY.createUsage(ctx));
		return entity;
	}

	// --- Entity / class ---

	@Test
	public void testGetClassName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("com.example.Employee", create(table).getClassName());
	}

	// --- Table ---

	@Test
	public void testGetTableName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("EMPLOYEE", create(table).getTableName());
	}

	@Test
	public void testGetSchema() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.setSchema("HR");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("HR", create(table).getSchema());
	}

	@Test
	public void testGetSchemaNull() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertNull(create(table).getSchema());
	}

	@Test
	public void testGetCatalog() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.setCatalog("MYDB");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("MYDB", create(table).getCatalog());
	}

	@Test
	public void testGetCatalogNull() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertNull(create(table).getCatalog());
	}

	// --- Inheritance ---

	@Test
	public void testHasInheritance() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE));
		assertTrue(create(table).hasInheritance());
	}

	@Test
	public void testHasInheritanceFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertFalse(create(table).hasInheritance());
	}

	@Test
	public void testGetInheritanceStrategy() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.inheritance(new InheritanceMetadata(InheritanceType.JOINED));
		assertEquals("JOINED", create(table).getInheritanceStrategy());
	}

	@Test
	public void testGetDiscriminatorColumnName() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
				.discriminatorColumn("DTYPE"));
		assertEquals("DTYPE", create(table).getDiscriminatorColumnName());
	}

	@Test
	public void testGetDiscriminatorColumnNameNull() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertNull(create(table).getDiscriminatorColumnName());
	}

	@Test
	public void testGetDiscriminatorType() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
				.discriminatorColumn("DTYPE")
				.discriminatorType(DiscriminatorType.INTEGER));
		assertEquals("INTEGER", create(table).getDiscriminatorType());
	}

	@Test
	public void testGetDiscriminatorColumnLength() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
				.discriminatorColumn("DTYPE")
				.discriminatorColumnLength(50));
		assertEquals(50, create(table).getDiscriminatorColumnLength());
	}

	@Test
	public void testGetDiscriminatorValue() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.discriminatorValue("CAR");
		assertEquals("CAR", create(table).getDiscriminatorValue());
	}

	@Test
	public void testGetDiscriminatorValueNull() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertNull(create(table).getDiscriminatorValue());
	}

	@Test
	public void testGetPrimaryKeyJoinColumnName() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.primaryKeyJoinColumn("VEHICLE_ID");
		assertEquals("VEHICLE_ID", create(table).getPrimaryKeyJoinColumnName());
	}

	@Test
	public void testGetPrimaryKeyJoinColumnNameNull() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertNull(create(table).getPrimaryKeyJoinColumnName());
	}

	// --- Field categorization ---

	@Test
	public void testGetIdFields() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		MappingXmlHelper helper = create(table);
		List<FieldDetails> idFields = helper.getIdFields();
		assertEquals(1, idFields.size());
		assertEquals("id", idFields.get(0).getName());
	}

	@Test
	public void testGetBasicFields() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		table.addColumn(new ColumnMetadata("AGE", "age", Integer.class));
		MappingXmlHelper helper = create(table);
		List<FieldDetails> basicFields = helper.getBasicFields();
		assertEquals(2, basicFields.size());
		assertEquals("name", basicFields.get(0).getName());
		assertEquals("age", basicFields.get(1).getName());
	}

	@Test
	public void testGetBasicFieldsExcludesIdAndVersion() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("VERSION", "version", Integer.class).version(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		MappingXmlHelper helper = create(table);
		List<FieldDetails> basicFields = helper.getBasicFields();
		assertEquals(1, basicFields.size());
		assertEquals("name", basicFields.get(0).getName());
	}

	@Test
	public void testGetBasicFieldsExcludesForeignKeys() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		MappingXmlHelper helper = create(table);
		List<FieldDetails> basicFields = helper.getBasicFields();
		assertEquals(1, basicFields.size());
		assertEquals("name", basicFields.get(0).getName());
	}

	@Test
	public void testGetVersionFields() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("VERSION", "version", Integer.class).version(true));
		MappingXmlHelper helper = create(table);
		List<FieldDetails> versionFields = helper.getVersionFields();
		assertEquals(1, versionFields.size());
		assertEquals("version", versionFields.get(0).getName());
	}

	@Test
	public void testGetCompositeIdField() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "OrderLineId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID")
				.addAttributeOverride("lineNumber", "LINE_NUMBER"));
		MappingXmlHelper helper = create(table);
		FieldDetails compositeId = helper.getCompositeIdField();
		assertNotNull(compositeId);
		assertEquals("id", compositeId.getName());
		assertTrue(helper.getIdFields().isEmpty());
	}

	@Test
	public void testGetCompositeIdFieldNull() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertNull(create(table).getCompositeIdField());
	}

	// --- Column attributes ---

	@Test
	public void testGetColumnName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("FIRST_NAME", "firstName", String.class));
		MappingXmlHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("FIRST_NAME", helper.getColumnName(field));
	}

	@Test
	public void testIsNullable() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class).nullable(false));
		table.addColumn(new ColumnMetadata("NICK", "nick", String.class));
		MappingXmlHelper helper = create(table);
		List<FieldDetails> fields = helper.getBasicFields();
		assertFalse(helper.isNullable(fields.get(0)));
		assertTrue(helper.isNullable(fields.get(1)));
	}

	@Test
	public void testIsUnique() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("EMAIL", "email", String.class).unique(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		MappingXmlHelper helper = create(table);
		List<FieldDetails> fields = helper.getBasicFields();
		assertTrue(helper.isUnique(fields.get(0)));
		assertFalse(helper.isUnique(fields.get(1)));
	}

	@Test
	public void testGetLength() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class).length(100));
		MappingXmlHelper helper = create(table);
		assertEquals(100, helper.getLength(helper.getBasicFields().get(0)));
	}

	@Test
	public void testGetLengthDefault() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		MappingXmlHelper helper = create(table);
		assertEquals(0, helper.getLength(helper.getBasicFields().get(0)));
	}

	@Test
	public void testGetPrecisionAndScale() {
		TableMetadata table = new TableMetadata("PRODUCT", "Product", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("PRICE", "price", BigDecimal.class)
				.precision(10).scale(2));
		MappingXmlHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals(10, helper.getPrecision(field));
		assertEquals(2, helper.getScale(field));
	}

	@Test
	public void testIsLob() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("BIO", "bio", String.class).lob(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		MappingXmlHelper helper = create(table);
		List<FieldDetails> fields = helper.getBasicFields();
		assertTrue(helper.isLob(fields.get(0)));
		assertFalse(helper.isLob(fields.get(1)));
	}

	@Test
	public void testGetTemporalType() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("HIRE_DATE", "hireDate", Date.class)
				.temporal(TemporalType.TIMESTAMP));
		MappingXmlHelper helper = create(table);
		assertEquals("TIMESTAMP", helper.getTemporalType(helper.getBasicFields().get(0)));
	}

	@Test
	public void testGetTemporalTypeNull() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		MappingXmlHelper helper = create(table);
		assertNull(helper.getTemporalType(helper.getBasicFields().get(0)));
	}

	@Test
	public void testGetGenerationType() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).generationType(GenerationType.IDENTITY));
		MappingXmlHelper helper = create(table);
		assertEquals("IDENTITY", helper.getGenerationType(helper.getIdFields().get(0)));
	}

	@Test
	public void testGetGenerationTypeNull() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		MappingXmlHelper helper = create(table);
		assertNull(helper.getGenerationType(helper.getIdFields().get(0)));
	}

	// --- ManyToOne ---

	@Test
	public void testGetManyToOneFields() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		MappingXmlHelper helper = create(table);
		List<FieldDetails> m2oFields = helper.getManyToOneFields();
		assertEquals(1, m2oFields.size());
		assertEquals("department", m2oFields.get(0).getName());
	}

	@Test
	public void testGetTargetEntityName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		MappingXmlHelper helper = create(table);
		FieldDetails field = helper.getManyToOneFields().get(0);
		assertEquals("com.example.Department", helper.getTargetEntityName(field));
	}

	@Test
	public void testGetManyToOneFetchType() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example")
				.fetchType(FetchType.LAZY));
		MappingXmlHelper helper = create(table);
		assertEquals("LAZY", helper.getManyToOneFetchType(helper.getManyToOneFields().get(0)));
	}

	@Test
	public void testGetManyToOneFetchTypeDefaultNull() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		MappingXmlHelper helper = create(table);
		assertNull(helper.getManyToOneFetchType(helper.getManyToOneFields().get(0)));
	}

	@Test
	public void testIsManyToOneOptional() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example")
				.optional(false));
		MappingXmlHelper helper = create(table);
		assertFalse(helper.isManyToOneOptional(helper.getManyToOneFields().get(0)));
	}

	@Test
	public void testIsManyToOneOptionalDefault() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		MappingXmlHelper helper = create(table);
		assertTrue(helper.isManyToOneOptional(helper.getManyToOneFields().get(0)));
	}

	// --- JoinColumn ---

	@Test
	public void testGetJoinColumnName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		MappingXmlHelper helper = create(table);
		assertEquals("DEPT_ID", helper.getJoinColumnName(helper.getManyToOneFields().get(0)));
	}

	@Test
	public void testGetReferencedColumnName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_CODE", "deptCode", String.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_CODE", "Department", "com.example")
				.referencedColumnName("CODE"));
		MappingXmlHelper helper = create(table);
		assertEquals("CODE", helper.getReferencedColumnName(helper.getManyToOneFields().get(0)));
	}

	@Test
	public void testGetReferencedColumnNameNull() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		MappingXmlHelper helper = create(table);
		assertNull(helper.getReferencedColumnName(helper.getManyToOneFields().get(0)));
	}

	// --- OneToMany ---

	@Test
	public void testGetOneToManyFields() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		MappingXmlHelper helper = create(table);
		List<FieldDetails> o2mFields = helper.getOneToManyFields();
		assertEquals(1, o2mFields.size());
		assertEquals("employees", o2mFields.get(0).getName());
	}

	@Test
	public void testGetOneToManyTargetEntity() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		MappingXmlHelper helper = create(table);
		assertEquals("com.example.Employee",
				helper.getOneToManyTargetEntity(helper.getOneToManyFields().get(0)));
	}

	@Test
	public void testGetOneToManyMappedBy() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		MappingXmlHelper helper = create(table);
		assertEquals("department",
				helper.getOneToManyMappedBy(helper.getOneToManyFields().get(0)));
	}

	@Test
	public void testGetOneToManyFetchType() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example")
				.fetchType(FetchType.EAGER));
		MappingXmlHelper helper = create(table);
		assertEquals("EAGER",
				helper.getOneToManyFetchType(helper.getOneToManyFields().get(0)));
	}

	@Test
	public void testGetOneToManyFetchTypeDefaultNull() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		MappingXmlHelper helper = create(table);
		assertNull(helper.getOneToManyFetchType(helper.getOneToManyFields().get(0)));
	}

	@Test
	public void testIsOneToManyOrphanRemoval() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example")
				.orphanRemoval(true));
		MappingXmlHelper helper = create(table);
		assertTrue(helper.isOneToManyOrphanRemoval(helper.getOneToManyFields().get(0)));
	}

	@Test
	public void testIsOneToManyOrphanRemovalDefault() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		MappingXmlHelper helper = create(table);
		assertFalse(helper.isOneToManyOrphanRemoval(helper.getOneToManyFields().get(0)));
	}

	@Test
	public void testGetOneToManyCascadeTypes() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example")
				.cascade(CascadeType.ALL));
		MappingXmlHelper helper = create(table);
		List<CascadeType> cascades =
				helper.getOneToManyCascadeTypes(helper.getOneToManyFields().get(0));
		assertEquals(1, cascades.size());
		assertEquals(CascadeType.ALL, cascades.get(0));
	}

	@Test
	public void testGetOneToManyCascadeTypesEmpty() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		MappingXmlHelper helper = create(table);
		assertTrue(helper.getOneToManyCascadeTypes(helper.getOneToManyFields().get(0)).isEmpty());
	}

	// --- OneToOne ---

	@Test
	public void testGetOneToOneFields() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID"));
		MappingXmlHelper helper = create(table);
		List<FieldDetails> o2oFields = helper.getOneToOneFields();
		assertEquals(1, o2oFields.size());
		assertEquals("address", o2oFields.get(0).getName());
	}

	@Test
	public void testGetOneToOneMappedBy() {
		TableMetadata table = new TableMetadata("ADDRESS", "Address", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("employee", "Employee", "com.example")
				.mappedBy("address"));
		MappingXmlHelper helper = create(table);
		assertEquals("address",
				helper.getOneToOneMappedBy(helper.getOneToOneFields().get(0)));
	}

	@Test
	public void testGetOneToOneMappedByNull() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID"));
		MappingXmlHelper helper = create(table);
		assertNull(helper.getOneToOneMappedBy(helper.getOneToOneFields().get(0)));
	}

	@Test
	public void testGetOneToOneFetchType() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID")
				.fetchType(FetchType.LAZY));
		MappingXmlHelper helper = create(table);
		assertEquals("LAZY",
				helper.getOneToOneFetchType(helper.getOneToOneFields().get(0)));
	}

	@Test
	public void testGetOneToOneFetchTypeDefaultNull() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID"));
		MappingXmlHelper helper = create(table);
		assertNull(helper.getOneToOneFetchType(helper.getOneToOneFields().get(0)));
	}

	@Test
	public void testIsOneToOneOptional() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID")
				.optional(false));
		MappingXmlHelper helper = create(table);
		assertFalse(helper.isOneToOneOptional(helper.getOneToOneFields().get(0)));
	}

	@Test
	public void testIsOneToOneOptionalDefault() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID"));
		MappingXmlHelper helper = create(table);
		assertTrue(helper.isOneToOneOptional(helper.getOneToOneFields().get(0)));
	}

	@Test
	public void testIsOneToOneOrphanRemoval() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID")
				.orphanRemoval(true));
		MappingXmlHelper helper = create(table);
		assertTrue(helper.isOneToOneOrphanRemoval(helper.getOneToOneFields().get(0)));
	}

	@Test
	public void testGetOneToOneCascadeTypes() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID")
				.cascade(CascadeType.PERSIST, CascadeType.REMOVE));
		MappingXmlHelper helper = create(table);
		List<CascadeType> cascades =
				helper.getOneToOneCascadeTypes(helper.getOneToOneFields().get(0));
		assertEquals(2, cascades.size());
		assertTrue(cascades.contains(CascadeType.PERSIST));
		assertTrue(cascades.contains(CascadeType.REMOVE));
	}

	@Test
	public void testGetOneToOneJoinColumnName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID"));
		MappingXmlHelper helper = create(table);
		assertEquals("ADDRESS_ID",
				helper.getJoinColumnName(helper.getOneToOneFields().get(0)));
	}

	@Test
	public void testGetOneToOneJoinColumnNameNullForInverse() {
		TableMetadata table = new TableMetadata("ADDRESS", "Address", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("employee", "Employee", "com.example")
				.mappedBy("address"));
		MappingXmlHelper helper = create(table);
		assertNull(helper.getJoinColumnName(helper.getOneToOneFields().get(0)));
	}

	// --- ManyToMany ---

	@Test
	public void testGetManyToManyFields() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID"));
		MappingXmlHelper helper = create(table);
		List<FieldDetails> m2mFields = helper.getManyToManyFields();
		assertEquals(1, m2mFields.size());
		assertEquals("projects", m2mFields.get(0).getName());
	}

	@Test
	public void testGetManyToManyTargetEntity() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID"));
		MappingXmlHelper helper = create(table);
		assertEquals("com.example.Project",
				helper.getManyToManyTargetEntity(helper.getManyToManyFields().get(0)));
	}

	@Test
	public void testGetManyToManyMappedBy() {
		TableMetadata table = new TableMetadata("PROJECT", "Project", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("employees", "Employee", "com.example")
				.mappedBy("projects"));
		MappingXmlHelper helper = create(table);
		assertEquals("projects",
				helper.getManyToManyMappedBy(helper.getManyToManyFields().get(0)));
	}

	@Test
	public void testGetManyToManyMappedByNull() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID"));
		MappingXmlHelper helper = create(table);
		assertNull(helper.getManyToManyMappedBy(helper.getManyToManyFields().get(0)));
	}

	@Test
	public void testGetManyToManyFetchType() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID")
				.fetchType(FetchType.EAGER));
		MappingXmlHelper helper = create(table);
		assertEquals("EAGER",
				helper.getManyToManyFetchType(helper.getManyToManyFields().get(0)));
	}

	@Test
	public void testGetManyToManyFetchTypeDefaultNull() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID"));
		MappingXmlHelper helper = create(table);
		assertNull(helper.getManyToManyFetchType(helper.getManyToManyFields().get(0)));
	}

	@Test
	public void testGetManyToManyCascadeTypes() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID")
				.cascade(CascadeType.PERSIST, CascadeType.MERGE));
		MappingXmlHelper helper = create(table);
		List<CascadeType> cascades =
				helper.getManyToManyCascadeTypes(helper.getManyToManyFields().get(0));
		assertEquals(2, cascades.size());
		assertTrue(cascades.contains(CascadeType.PERSIST));
		assertTrue(cascades.contains(CascadeType.MERGE));
	}

	@Test
	public void testGetManyToManyCascadeTypesEmpty() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID"));
		MappingXmlHelper helper = create(table);
		assertTrue(helper.getManyToManyCascadeTypes(helper.getManyToManyFields().get(0)).isEmpty());
	}

	@Test
	public void testGetJoinTableName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID"));
		MappingXmlHelper helper = create(table);
		assertEquals("EMPLOYEE_PROJECT",
				helper.getJoinTableName(helper.getManyToManyFields().get(0)));
	}

	@Test
	public void testGetJoinTableJoinColumnName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID"));
		MappingXmlHelper helper = create(table);
		assertEquals("EMPLOYEE_ID",
				helper.getJoinTableJoinColumnName(helper.getManyToManyFields().get(0)));
	}

	@Test
	public void testGetJoinTableInverseJoinColumnName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID"));
		MappingXmlHelper helper = create(table);
		assertEquals("PROJECT_ID",
				helper.getJoinTableInverseJoinColumnName(helper.getManyToManyFields().get(0)));
	}

	@Test
	public void testGetJoinTableNameNullForInverse() {
		TableMetadata table = new TableMetadata("PROJECT", "Project", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("employees", "Employee", "com.example")
				.mappedBy("projects"));
		MappingXmlHelper helper = create(table);
		assertNull(helper.getJoinTableName(helper.getManyToManyFields().get(0)));
	}

	// --- Embedded / attribute overrides ---

	@Test
	public void testGetEmbeddedFields() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addEmbeddedField(new EmbeddedFieldMetadata("homeAddress", "Address", "com.example")
				.addAttributeOverride("street", "HOME_STREET")
				.addAttributeOverride("city", "HOME_CITY"));
		MappingXmlHelper helper = create(table);
		List<FieldDetails> embeddedFields = helper.getEmbeddedFields();
		assertEquals(1, embeddedFields.size());
		assertEquals("homeAddress", embeddedFields.get(0).getName());
	}

	@Test
	public void testGetAttributeOverrides() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addEmbeddedField(new EmbeddedFieldMetadata("homeAddress", "Address", "com.example")
				.addAttributeOverride("street", "HOME_STREET")
				.addAttributeOverride("city", "HOME_CITY"));
		MappingXmlHelper helper = create(table);
		List<MappingXmlHelper.AttributeOverrideInfo> overrides =
				helper.getAttributeOverrides(helper.getEmbeddedFields().get(0));
		assertEquals(2, overrides.size());
		assertEquals("street", overrides.get(0).fieldName());
		assertEquals("HOME_STREET", overrides.get(0).columnName());
		assertEquals("city", overrides.get(1).fieldName());
		assertEquals("HOME_CITY", overrides.get(1).columnName());
	}

	@Test
	public void testGetAttributeOverridesForCompositeId() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "OrderLineId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID")
				.addAttributeOverride("lineNumber", "LINE_NUMBER"));
		MappingXmlHelper helper = create(table);
		List<MappingXmlHelper.AttributeOverrideInfo> overrides =
				helper.getAttributeOverrides(helper.getCompositeIdField());
		assertEquals(2, overrides.size());
		assertEquals("orderId", overrides.get(0).fieldName());
		assertEquals("ORDER_ID", overrides.get(0).columnName());
		assertEquals("lineNumber", overrides.get(1).fieldName());
		assertEquals("LINE_NUMBER", overrides.get(1).columnName());
	}

	// --- getPackageName ---

	@Test
	public void testGetPackageNameWithPackage() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertEquals("com.example", new MappingXmlHelper(entity).getPackageName());
	}

	@Test
	public void testGetPackageNameNoPackage() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = new DynamicClassDetails(
				"TestEntity", "TestEntity", false, null, null, ctx);
		entity.addAnnotationUsage(JpaAnnotations.ENTITY.createUsage(ctx));
		assertNull(new MappingXmlHelper(entity).getPackageName());
	}

	// --- isMutable ---

	@Test
	public void testIsMutableDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertTrue(new MappingXmlHelper(entity).isMutable());
	}

	@Test
	public void testIsMutableFalseWhenImmutable() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		entity.addAnnotationUsage(HibernateAnnotations.IMMUTABLE.createUsage(ctx));
		assertFalse(new MappingXmlHelper(entity).isMutable());
	}

	// --- isDynamicUpdate ---

	@Test
	public void testIsDynamicUpdateDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertFalse(new MappingXmlHelper(entity).isDynamicUpdate());
	}

	@Test
	public void testIsDynamicUpdateTrue() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		entity.addAnnotationUsage(HibernateAnnotations.DYNAMIC_UPDATE.createUsage(ctx));
		assertTrue(new MappingXmlHelper(entity).isDynamicUpdate());
	}

	// --- isDynamicInsert ---

	@Test
	public void testIsDynamicInsertDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertFalse(new MappingXmlHelper(entity).isDynamicInsert());
	}

	@Test
	public void testIsDynamicInsertTrue() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		entity.addAnnotationUsage(HibernateAnnotations.DYNAMIC_INSERT.createUsage(ctx));
		assertTrue(new MappingXmlHelper(entity).isDynamicInsert());
	}

	// --- getBatchSize ---

	@Test
	public void testGetBatchSizeDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertEquals(0, new MappingXmlHelper(entity).getBatchSize());
	}

	@Test
	public void testGetBatchSizeSet() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		BatchSizeAnnotation bs = HibernateAnnotations.BATCH_SIZE.createUsage(ctx);
		bs.size(25);
		entity.addAnnotationUsage(bs);
		assertEquals(25, new MappingXmlHelper(entity).getBatchSize());
	}

	// --- getSqlRestriction ---

	@Test
	public void testGetSqlRestrictionDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertNull(new MappingXmlHelper(entity).getSqlRestriction());
	}

	@Test
	public void testGetSqlRestrictionSet() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SQLRestrictionAnnotation sr = HibernateAnnotations.SQL_RESTRICTION.createUsage(ctx);
		sr.value("active = true");
		entity.addAnnotationUsage(sr);
		assertEquals("active = true", new MappingXmlHelper(entity).getSqlRestriction());
	}

	// --- getOptimisticLockMode ---

	@Test
	public void testGetOptimisticLockModeDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertNull(new MappingXmlHelper(entity).getOptimisticLockMode());
	}

	@Test
	public void testGetOptimisticLockModeAll() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		OptimisticLockingAnnotation ol = HibernateAnnotations.OPTIMISTIC_LOCKING.createUsage(ctx);
		ol.type(OptimisticLockType.ALL);
		entity.addAnnotationUsage(ol);
		assertEquals("ALL", new MappingXmlHelper(entity).getOptimisticLockMode());
	}

	// --- getRowId ---

	@Test
	public void testGetRowIdDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertNull(new MappingXmlHelper(entity).getRowId());
	}

	@Test
	public void testGetRowIdSet() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		RowIdAnnotation rid = HibernateAnnotations.ROW_ID.createUsage(ctx);
		rid.value("ROWID");
		entity.addAnnotationUsage(rid);
		assertEquals("ROWID", new MappingXmlHelper(entity).getRowId());
	}

	// --- getSubselect ---

	@Test
	public void testGetSubselectDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertNull(new MappingXmlHelper(entity).getSubselect());
	}

	@Test
	public void testGetSubselectSet() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SubselectAnnotation ss = HibernateAnnotations.SUBSELECT.createUsage(ctx);
		ss.value("select * from EMPLOYEE where active = true");
		entity.addAnnotationUsage(ss);
		assertEquals("select * from EMPLOYEE where active = true",
				new MappingXmlHelper(entity).getSubselect());
	}

	// --- isConcreteProxy ---

	@Test
	public void testIsConcreteProxyDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertFalse(new MappingXmlHelper(entity).isConcreteProxy());
	}

	@Test
	public void testIsConcreteProxyTrue() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		entity.addAnnotationUsage(HibernateAnnotations.CONCRETE_PROXY.createUsage(ctx));
		assertTrue(new MappingXmlHelper(entity).isConcreteProxy());
	}
}
