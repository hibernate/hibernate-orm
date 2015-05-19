/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import java.util.List;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;

/**
 * Service delegate for handling schema creation.
 *
 * @author Steve Ebersole
 */
public interface SchemaCreator {
	/**
	 * Perform the creation to the specified targets
	 *
	 * @param metadata The "compiled" mapping metadata.
	 * @param createSchemas Should the schema(s) actually be created as well ({@code CREATE SCHEMA})?
	 * @param targets The targets for creation
	 *
	 * @throws SchemaManagementException Indicates a problem processing the creation
	 */
	public void doCreation(
			Metadata metadata,
			boolean createSchemas,
			Target... targets) throws SchemaManagementException;

	/**
	 * Perform the creation to the specified targets
	 *
	 * @param metadata The "compiled" mapping metadata.
	 * @param createSchemas Should the schema(s) actually be created as well ({@code CREATE SCHEMA})?
	 * @param dialect Allow explicitly passing the Dialect to use.
	 * @param targets The targets for creation
	 *
	 * @throws SchemaManagementException Indicates a problem processing the creation
	 */
	public void doCreation(
			Metadata metadata,
			boolean createSchemas,
			Dialect dialect,
			Target... targets) throws SchemaManagementException;

	/**
	 * Perform the creation to the specified targets
	 *
	 * @param metadata The "compiled" mapping metadata.
	 * @param createSchemas Should the schema(s) actually be created as well ({@code CREATE SCHEMA})?
	 * @param targets The targets for creation
	 *
	 * @throws SchemaManagementException Indicates a problem processing the creation
	 */
	public void doCreation(
			Metadata metadata,
			boolean createSchemas,
			List<Target> targets) throws SchemaManagementException;

	/**
	 * Perform the creation to the specified targets
	 *
	 * @param metadata The "compiled" mapping metadata.
	 * @param createSchemas Should the schema(s) actually be created as well ({@code CREATE SCHEMA})?
	 * @param dialect Allow explicitly passing the Dialect to use.
	 * @param targets The targets for creation
	 *
	 * @throws SchemaManagementException Indicates a problem processing the creation
	 */
	public void doCreation(
			Metadata metadata,
			boolean createSchemas,
			Dialect dialect,
			List<Target> targets) throws SchemaManagementException;
}
