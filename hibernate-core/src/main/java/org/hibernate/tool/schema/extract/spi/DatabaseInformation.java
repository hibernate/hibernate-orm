/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.tool.schema.extract.spi;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.Schema;

/**
 * Provides access to information about existing schema objects (tables, sequences etc) of existing database.
 *
 * @author Christoph Sturm
 * @author Teodor Danciu
 * @author Steve Ebersole
 */
public interface DatabaseInformation {
	/**
	 * Check to see if the given schema already exists.
	 *
	 * @param schema The schema name
	 *
	 * @return {@code true} indicates a schema with the given name already exists
	 */
	boolean schemaExists(Schema.Name schema);

	/**
	 * Obtain reference to the named TableInformation
	 *
	 * @param catalogName The name of the catalog which contains the schema which the table belongs to
	 * @param schemaName The name of the schema the table belongs to
	 * @param tableName The table name
	 *
	 * @return The table information.  May return {@code null} if not found.
	 */
	public TableInformation getTableInformation(Identifier catalogName, Identifier schemaName, Identifier tableName);

	/**
	 * Obtain reference to the named TableInformation
	 *
	 * @param schemaName The name of the schema the table belongs to
	 * @param tableName The table name
	 *
	 * @return The table information.  May return {@code null} if not found.
	 */
	public TableInformation getTableInformation(Schema.Name schemaName, Identifier tableName);

	/**
	 * Obtain reference to the named TableInformation
	 *
	 * @param tableName The qualfied table name
	 *
	 * @return The table information.  May return {@code null} if not found.
	 */
	public TableInformation getTableInformation(QualifiedTableName tableName);

	public void registerTable(TableInformation tableInformation);

	/**
	 * Obtain reference to the named SequenceInformation
	 *
	 * @param catalogName The name of the catalog which contains the schema which the sequence belongs to
	 * @param schemaName The name of the schema the sequence belongs to
	 * @param sequenceName The sequence name
	 *
	 * @return The sequence information.  May return {@code null} if not found.
	 */
	public SequenceInformation getSequenceInformation(
			Identifier catalogName,
			Identifier schemaName,
			Identifier sequenceName);

	/**
	 * Obtain reference to the named SequenceInformation
	 *
	 * @param schemaName The name of the schema the table belongs to
	 * @param sequenceName The sequence name
	 *
	 * @return The sequence information.  May return {@code null} if not found.
	 */
	public SequenceInformation getSequenceInformation(Schema.Name schemaName, Identifier sequenceName);

	/**
	 * Obtain reference to the named SequenceInformation
	 *
	 * @param sequenceName The qualified sequence name
	 *
	 * @return The sequence information.  May return {@code null} if not found.
	 */
	public SequenceInformation getSequenceInformation(QualifiedSequenceName sequenceName);
}
