/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.relational;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.tool.schema.extract.spi.TableInformation;

/**
 * Models any mapped "table reference" (e.g. a physical table, an in-lined
 * view, etc).
 *
 * @author Steve Ebersole
 */
public interface MappedTable extends Exportable, Loggable {
	/**
	 * Get an identifier for this MappedTable that is unique across all
	 * MappedTable references in a given {@link Database}.
	 * <p/>
	 * Think "surrogate primary key" relative to Database.
	 */
	String getUid();

	List<String> getCheckConstraints();

	/**
	 * Will this MappedTable physically be exported as per
	 * {@link Exportable}?  Or is it "virtual"?
	 */
	boolean isExportable();

	List<InitCommand> getInitCommands();

	/**
	 * Retrieve all columns defined for this table.  The returned Set has
	 * an iteration order defined as the order that columns are encountered
	 * while processing the application's mappings.
	 */
	Set<MappedColumn> getMappedColumns();

	// todo (6.0) : others as deemed appropriate - see o.h.mapping.Table

	void setName(String name);

	/**
	 * Get the name of the mapped table.
	 */
	String getName();

	Identifier getNameIdentifier();

	String getQuotedName();

	QualifiedTableName getQualifiedTableName();

	/**
	 * Get the schema associated to the mapped table.
	 */
	String getSchema();

	boolean isSchemaQuoted();

	/**
	 * Get the catalog associated to the mapped table.
	 */
	String getCatalog();

	Index getOrCreateIndex(String indexName);

	UniqueKey getOrCreateUniqueKey(String keyName);

	void createForeignKeys();

	/**
	 * Create a foreign key associated to the mapped table.
	 *
	 * @param keyName the foreign key name.
	 * @param keyColumns the columns to be associated with the foreign key.
	 * @param referencedEntityName the referenced entity name.
	 * @param keyDefinition foreign key definition
	 * @return the constructed foreign key.
	 */
	ForeignKey createForeignKey(String keyName, List keyColumns, String referencedEntityName, String keyDefinition);

	// This must be done outside of Table, rather than statically, to ensure
	// deterministic alias names.  See HHH-2448.
	void setUniqueInteger(int uniqueInteger);

	KeyValue getIdentifierValue();

	void addCheckConstraint(String constraint);

	boolean containsColumn(Column column);

	String getRowId();

	void setRowId(String rowId);

	String getSubselect();

	void setSubselect(String subselect);

	void setHasDenormalizedTables();

	/**
	 * Set whether the mapped table is abstract.
	 * @param isAbstract
	 */
	void setAbstract(boolean isAbstract);

	/**
	 * Get whether the mapped table is abstract.
	 */
	boolean isAbstract();

	boolean isPhysicalTable();

	/**
	 * Get the mapped table comme.t
	 */
	String getComment();

	/**
	 * Set a comment on the mapped table.
	 * @param comment
	 */
	void setComment(String comment);

	boolean isCatalogQuoted();

	Column getColumn(Column column);

	Column getColumn(Identifier name);

	Column getColumn(int n);

	void addColumn(Column column);

	Iterator getColumnIterator();

	Iterator<Index> getIndexIterator();

	Iterator getForeignKeyIterator();

	Iterator<UniqueKey> getUniqueKeyIterator();

	Iterator sqlAlterStrings(
			Dialect dialect,
			Mapping p,
			TableInformation tableInfo,
			String defaultCatalog,
			String defaultSchema) throws HibernateException;

	boolean hasPrimaryKey();

	PrimaryKey getPrimaryKey();

	void setPrimaryKey(PrimaryKey primaryKey);

	void addInitCommand(InitCommand command);
}
