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
package org.hibernate.tool.reveng.internal.descriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents metadata for a database table.
 *
 * @author Koen Aers
 */
public class TableDescriptor {
	private String tableName;
	private String schema;
	private String catalog;
	private String entityClassName;
	private String entityPackage;
	private List<ColumnDescriptor> columns = new ArrayList<>();
	private List<ForeignKeyDescriptor> foreignKeys = new ArrayList<>();
	private List<OneToManyDescriptor> oneToManys = new ArrayList<>();
	private List<OneToOneDescriptor> oneToOnes = new ArrayList<>();
	private List<ManyToManyDescriptor> manyToManys = new ArrayList<>();
	private List<EmbeddedFieldDescriptor> embeddedFields = new ArrayList<>();
	private InheritanceDescriptor inheritance;
	private String discriminatorValue;
	private String parentEntityClassName;
	private String parentEntityPackage;
	private String primaryKeyJoinColumnName;
	private CompositeIdDescriptor compositeId;
	private String comment;
	private List<IndexDescriptor> indexes = new ArrayList<>();
	private Map<String, List<String>> metaAttributes = new LinkedHashMap<>();

	public TableDescriptor(String tableName, String entityClassName, String entityPackage) {
		this.tableName = tableName;
		this.entityClassName = entityClassName;
		this.entityPackage = entityPackage;
	}

	public TableDescriptor addColumn(ColumnDescriptor column) {
		this.columns.add(column);
		return this;
	}

	public TableDescriptor addForeignKey(ForeignKeyDescriptor foreignKey) {
		this.foreignKeys.add(foreignKey);
		return this;
	}

	public TableDescriptor addOneToMany(OneToManyDescriptor oneToMany) {
		this.oneToManys.add(oneToMany);
		return this;
	}

	public TableDescriptor addOneToOne(OneToOneDescriptor oneToOne) {
		this.oneToOnes.add(oneToOne);
		return this;
	}

	public TableDescriptor addManyToMany(ManyToManyDescriptor manyToMany) {
		this.manyToManys.add(manyToMany);
		return this;
	}

	public TableDescriptor addEmbeddedField(EmbeddedFieldDescriptor embeddedField) {
		this.embeddedFields.add(embeddedField);
		return this;
	}

	public boolean isForeignKeyColumn(String columnName) {
		return foreignKeys.stream()
			.anyMatch(fk -> fk.getForeignKeyColumnNames().contains(columnName))
			|| oneToOnes.stream()
			.anyMatch(o2o -> o2o.getForeignKeyColumnNames().contains(columnName));
	}

	// Getters and setters
	public String getTableName() { return tableName; }
	public void setTableName(String tableName) { this.tableName = tableName; }

	public String getSchema() { return schema; }
	public void setSchema(String schema) { this.schema = schema; }

	public String getCatalog() { return catalog; }
	public void setCatalog(String catalog) { this.catalog = catalog; }

	public String getEntityClassName() { return entityClassName; }
	public void setEntityClassName(String entityClassName) { this.entityClassName = entityClassName; }

	public String getEntityPackage() { return entityPackage; }
	public void setEntityPackage(String entityPackage) { this.entityPackage = entityPackage; }

	public List<ColumnDescriptor> getColumns() { return columns; }
	public void setColumns(List<ColumnDescriptor> columns) { this.columns = columns; }

	public List<ForeignKeyDescriptor> getForeignKeys() { return foreignKeys; }
	public void setForeignKeys(List<ForeignKeyDescriptor> foreignKeys) { this.foreignKeys = foreignKeys; }

	public List<OneToManyDescriptor> getOneToManys() { return oneToManys; }
	public void setOneToManys(List<OneToManyDescriptor> oneToManys) { this.oneToManys = oneToManys; }

	public List<OneToOneDescriptor> getOneToOnes() { return oneToOnes; }
	public void setOneToOnes(List<OneToOneDescriptor> oneToOnes) { this.oneToOnes = oneToOnes; }

	public List<ManyToManyDescriptor> getManyToManys() { return manyToManys; }
	public void setManyToManys(List<ManyToManyDescriptor> manyToManys) { this.manyToManys = manyToManys; }

	public List<EmbeddedFieldDescriptor> getEmbeddedFields() { return embeddedFields; }
	public void setEmbeddedFields(List<EmbeddedFieldDescriptor> embeddedFields) { this.embeddedFields = embeddedFields; }

	public InheritanceDescriptor getInheritance() { return inheritance; }
	public TableDescriptor inheritance(InheritanceDescriptor inheritance) {
		this.inheritance = inheritance;
		return this;
	}

	public String getDiscriminatorValue() { return discriminatorValue; }
	public TableDescriptor discriminatorValue(String discriminatorValue) {
		this.discriminatorValue = discriminatorValue;
		return this;
	}

	public String getParentEntityClassName() { return parentEntityClassName; }
	public String getParentEntityPackage() { return parentEntityPackage; }
	public TableDescriptor parent(String parentEntityClassName, String parentEntityPackage) {
		this.parentEntityClassName = parentEntityClassName;
		this.parentEntityPackage = parentEntityPackage;
		return this;
	}

	public String getPrimaryKeyJoinColumnName() { return primaryKeyJoinColumnName; }
	public TableDescriptor primaryKeyJoinColumn(String primaryKeyJoinColumnName) {
		this.primaryKeyJoinColumnName = primaryKeyJoinColumnName;
		return this;
	}

	public CompositeIdDescriptor getCompositeId() { return compositeId; }
	public TableDescriptor compositeId(CompositeIdDescriptor compositeId) {
		this.compositeId = compositeId;
		return this;
	}

	public String getComment() { return comment; }
	public TableDescriptor comment(String comment) {
		this.comment = comment;
		return this;
	}

	public List<IndexDescriptor> getIndexes() { return indexes; }
	public TableDescriptor addIndex(IndexDescriptor index) {
		this.indexes.add(index);
		return this;
	}

	public Map<String, List<String>> getMetaAttributes() { return metaAttributes; }
	public TableDescriptor metaAttributes(Map<String, List<String>> metaAttributes) {
		this.metaAttributes = metaAttributes;
		return this;
	}
	public TableDescriptor addMetaAttribute(String name, String value) {
		this.metaAttributes.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
		return this;
	}
	public List<String> getMetaAttribute(String name) {
		return metaAttributes.getOrDefault(name, Collections.emptyList());
	}
	public boolean hasMetaAttribute(String name) {
		return metaAttributes.containsKey(name);
	}
}
