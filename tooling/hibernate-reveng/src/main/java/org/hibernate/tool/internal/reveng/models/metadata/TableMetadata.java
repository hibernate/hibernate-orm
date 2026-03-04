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
package org.hibernate.tool.internal.reveng.models.metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents metadata for a database table.
 *
 * @author Koen Aers
 */
public class TableMetadata {
	private String tableName;
	private String schema;
	private String catalog;
	private String entityClassName;
	private String entityPackage;
	private List<ColumnMetadata> columns = new ArrayList<>();
	private List<ForeignKeyMetadata> foreignKeys = new ArrayList<>();
	private List<OneToManyMetadata> oneToManys = new ArrayList<>();
	private List<OneToOneMetadata> oneToOnes = new ArrayList<>();
	private List<ManyToManyMetadata> manyToManys = new ArrayList<>();
	private List<EmbeddedFieldMetadata> embeddedFields = new ArrayList<>();
	private InheritanceMetadata inheritance;
	private String discriminatorValue;
	private String parentEntityClassName;
	private String parentEntityPackage;
	private String primaryKeyJoinColumnName;
	private CompositeIdMetadata compositeId;

	public TableMetadata(String tableName, String entityClassName, String entityPackage) {
		this.tableName = tableName;
		this.entityClassName = entityClassName;
		this.entityPackage = entityPackage;
	}

	public TableMetadata addColumn(ColumnMetadata column) {
		this.columns.add(column);
		return this;
	}

	public TableMetadata addForeignKey(ForeignKeyMetadata foreignKey) {
		this.foreignKeys.add(foreignKey);
		return this;
	}

	public TableMetadata addOneToMany(OneToManyMetadata oneToMany) {
		this.oneToManys.add(oneToMany);
		return this;
	}

	public TableMetadata addOneToOne(OneToOneMetadata oneToOne) {
		this.oneToOnes.add(oneToOne);
		return this;
	}

	public TableMetadata addManyToMany(ManyToManyMetadata manyToMany) {
		this.manyToManys.add(manyToMany);
		return this;
	}

	public TableMetadata addEmbeddedField(EmbeddedFieldMetadata embeddedField) {
		this.embeddedFields.add(embeddedField);
		return this;
	}

	public boolean isForeignKeyColumn(String columnName) {
		return foreignKeys.stream()
			.anyMatch(fk -> fk.getForeignKeyColumnName().equals(columnName))
			|| oneToOnes.stream()
			.filter(o2o -> o2o.getForeignKeyColumnName() != null)
			.anyMatch(o2o -> o2o.getForeignKeyColumnName().equals(columnName));
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

	public List<ColumnMetadata> getColumns() { return columns; }
	public void setColumns(List<ColumnMetadata> columns) { this.columns = columns; }

	public List<ForeignKeyMetadata> getForeignKeys() { return foreignKeys; }
	public void setForeignKeys(List<ForeignKeyMetadata> foreignKeys) { this.foreignKeys = foreignKeys; }

	public List<OneToManyMetadata> getOneToManys() { return oneToManys; }
	public void setOneToManys(List<OneToManyMetadata> oneToManys) { this.oneToManys = oneToManys; }

	public List<OneToOneMetadata> getOneToOnes() { return oneToOnes; }
	public void setOneToOnes(List<OneToOneMetadata> oneToOnes) { this.oneToOnes = oneToOnes; }

	public List<ManyToManyMetadata> getManyToManys() { return manyToManys; }
	public void setManyToManys(List<ManyToManyMetadata> manyToManys) { this.manyToManys = manyToManys; }

	public List<EmbeddedFieldMetadata> getEmbeddedFields() { return embeddedFields; }
	public void setEmbeddedFields(List<EmbeddedFieldMetadata> embeddedFields) { this.embeddedFields = embeddedFields; }

	public InheritanceMetadata getInheritance() { return inheritance; }
	public TableMetadata inheritance(InheritanceMetadata inheritance) {
		this.inheritance = inheritance;
		return this;
	}

	public String getDiscriminatorValue() { return discriminatorValue; }
	public TableMetadata discriminatorValue(String discriminatorValue) {
		this.discriminatorValue = discriminatorValue;
		return this;
	}

	public String getParentEntityClassName() { return parentEntityClassName; }
	public String getParentEntityPackage() { return parentEntityPackage; }
	public TableMetadata parent(String parentEntityClassName, String parentEntityPackage) {
		this.parentEntityClassName = parentEntityClassName;
		this.parentEntityPackage = parentEntityPackage;
		return this;
	}

	public String getPrimaryKeyJoinColumnName() { return primaryKeyJoinColumnName; }
	public TableMetadata primaryKeyJoinColumn(String primaryKeyJoinColumnName) {
		this.primaryKeyJoinColumnName = primaryKeyJoinColumnName;
		return this;
	}

	public CompositeIdMetadata getCompositeId() { return compositeId; }
	public TableMetadata compositeId(CompositeIdMetadata compositeId) {
		this.compositeId = compositeId;
		return this;
	}
}
