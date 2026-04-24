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
package org.hibernate.tool.reveng.internal.exporter.ddl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringWriter;
import java.util.List;
import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.reveng.internal.builder.db.DynamicEntityBuilder;
import org.hibernate.tool.reveng.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.TableDescriptor;
import org.junit.jupiter.api.Test;

class DdlSchemaOperationsTest {

	private Properties defaultProperties() {
		Properties props = new Properties();
		props.put(AvailableSettings.DIALECT, "org.hibernate.dialect.H2Dialect");
		return props;
	}

	private List<ClassDetails> buildSimpleEntity() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableDescriptor table = new TableDescriptor(
				"PERSON", "Person", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class)
				.primaryKey(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class)
				.length(100));
		builder.createEntityFromTable(table);
		return List.of(builder.getModelsContext()
				.getClassDetailsRegistry()
				.getClassDetails("com.example.Person"));
	}

	@Test
	void testCreateDdl() {
		DdlSchemaOperations ops = new DdlSchemaOperations(
				buildSimpleEntity(), defaultProperties(), ";", false, false);
		StringWriter writer = new StringWriter();
		ops.createDdl(writer);
		String ddl = writer.toString();
		assertTrue(ddl.contains("create table"),
				"Should contain CREATE TABLE: " + ddl);
		assertTrue(ddl.contains("PERSON"),
				"Should reference PERSON table: " + ddl);
	}

	@Test
	void testDropDdl() {
		DdlSchemaOperations ops = new DdlSchemaOperations(
				buildSimpleEntity(), defaultProperties(), ";", false, false);
		StringWriter writer = new StringWriter();
		ops.dropDdl(writer);
		String ddl = writer.toString();
		assertTrue(ddl.toLowerCase().contains("drop"),
				"Should contain DROP: " + ddl);
	}

	@Test
	void testBothDdl() {
		DdlSchemaOperations ops = new DdlSchemaOperations(
				buildSimpleEntity(), defaultProperties(), ";", false, false);
		StringWriter writer = new StringWriter();
		ops.bothDdl(writer);
		String ddl = writer.toString();
		assertTrue(ddl.toLowerCase().contains("drop"),
				"Should contain DROP: " + ddl);
		assertTrue(ddl.toLowerCase().contains("create table"),
				"Should contain CREATE TABLE: " + ddl);
		int dropPos = ddl.toLowerCase().indexOf("drop");
		int createPos = ddl.toLowerCase().indexOf("create table");
		assertTrue(dropPos < createPos,
				"DROP should come before CREATE");
	}

	@Test
	void testCreateDdlWithCustomDelimiter() {
		DdlSchemaOperations ops = new DdlSchemaOperations(
				buildSimpleEntity(), defaultProperties(), "$$", false, false);
		StringWriter writer = new StringWriter();
		ops.createDdl(writer);
		String ddl = writer.toString();
		assertTrue(ddl.contains("$$"),
				"Should use custom delimiter: " + ddl);
	}
}
