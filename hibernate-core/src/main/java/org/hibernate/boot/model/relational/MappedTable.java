/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.relational;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.mapping.KeyValue;
import org.hibernate.metamodel.model.relational.internal.InflightTable;
import org.hibernate.metamodel.model.relational.spi.Exportable;
import org.hibernate.metamodel.model.relational.spi.PhysicalNamingStrategy;
import org.hibernate.metamodel.model.relational.spi.RuntimeDatabaseModelProducer;
import org.hibernate.naming.Identifier;
import org.hibernate.naming.QualifiedTableName;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Models any mapped "table reference" (e.g. a physical table, an in-lined
 * view, etc).
 *
 * @author Steve Ebersole
 */
public interface MappedTable<T extends MappedColumn> extends Loggable {
	/**
	 * Get an identifier for this MappedTable that is unique across all
	 * MappedTable references in a given {@link Database}.
	 */
	UUID getUid();

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
	Set<T> getMappedColumns();

	void setName(String name);

	/**
	 * Get the name of the mapped table.
	 */
	String getName();

	Identifier getNameIdentifier();

	String getQuotedName();

	String getQuotedName(Dialect dialect);

	QualifiedTableName getQualifiedTableName();

	/**
	 * Get the schema associated to the mapped table.
	 */
	String getSchema();

	/**
	 * Get the catalog associated to the mapped table.
	 */
	String getCatalog();

	MappedIndex getOrCreateIndex(String indexName);

	MappedUniqueKey getOrCreateUniqueKey(String keyName);

	default MappedUniqueKey createUniqueKey(List<T> columns){
		return null;
	}

	void addUniqueKey(MappedUniqueKey uk);

	void createForeignKeys();

	/**
	 * Create a foreign key associated to the mapped table.
	 *
	 * @param keyName the foreign key name.
	 * @param keyColumns the columns to be associated with the foreign key.
	 * @param referencedEntityName the referenced entity name.
	 * @param keyDefinition foreign key definition
	 *
	 * @return the constructed foreign key.
	 */
	MappedForeignKey createForeignKey(String keyName, List<T> keyColumns, String referencedEntityName, String keyDefinition);

	MappedForeignKey createForeignKey(
			String keyName,
			List keyColumns,
			String referencedEntityName,
			String keyDefinition,
			List referencedColumns);


	// This must be done outside of Table, rather than statically, to ensure
	// deterministic alias names.  See HHH-2448.
	void setUniqueInteger(int uniqueInteger);

	KeyValue getIdentifierValue();

	void addCheckConstraint(String constraint);

	boolean containsColumn(T column);

	String getRowId();

	void setRowId(String rowId);

	String getSubselect();

	void setSubselect(String subselect);

	boolean isSubselect();

	void setHasDenormalizedTables();

	/**
	 * Set whether the mapped table is abstract.
	 *
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
	 *
	 * @param comment
	 */
	void setComment(String comment);

	T getColumn(T column);

	T getColumn(Identifier name);

	T getColumn(int n);

	void addColumn(T column);

	Collection<MappedIndex> getIndexes();

	Collection<MappedForeignKey> getForeignKeys();

	Collection<MappedUniqueKey> getUniqueKeys();

	MappedPrimaryKey getPrimaryKey();

	void setPrimaryKey(MappedPrimaryKey primaryKey);

	void addInitCommand(InitCommand command);

	default boolean hasDenormalizedTables() {
		return false;
	}

	default void setIdentifierValue(KeyValue idValue) {
	}

	InflightTable generateRuntimeTable(
			PhysicalNamingStrategy namingStrategy,
			JdbcEnvironment jdbcEnvironment,
			IdentifierGeneratorFactory identifierGeneratorFactory,
			RuntimeDatabaseModelProducer.Callback callback,
			TypeConfiguration typeConfiguration);
}
