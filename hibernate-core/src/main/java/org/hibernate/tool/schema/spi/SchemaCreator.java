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
