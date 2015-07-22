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
 * Service delegate for handling schema dropping.
 *
 * @author Steve Ebersole
 */
public interface SchemaDropper {
	/**
	 * Perform the drop to the specified targets
	 *
	 * @param metadata The "compiled" mapping metadata.
	 * @param dropNamespaces Should the schema(s)/catalog(s) actually be dropped also ({@code DROP SCHEMA})?
	 * @param targets The targets for drop
	 *
	 * @throws SchemaManagementException Indicates a problem processing the creation
	 */
	public void doDrop(Metadata metadata, boolean dropNamespaces, Target... targets) throws SchemaManagementException;
	/**
	 * Perform the drop to the specified targets
	 *
	 * @param metadata The "compiled" mapping metadata.
	 * @param dropNamespaces Should the schema(s)/catalog(s) actually be dropped also ({@code DROP SCHEMA})?
	 * @param targets The targets for drop
	 *
	 * @throws SchemaManagementException Indicates a problem processing the creation
	 */
	public void doDrop(Metadata metadata, boolean dropNamespaces, Dialect dialect, Target... targets) throws SchemaManagementException;

	/**
	 * Perform the drop to the specified targets
	 *
	 * @param metadata The "compiled" mapping metadata.
	 * @param dropNamespaces Should the schema(s)/catalog(s) actually be dropped also ({@code DROP SCHEMA})?
	 * @param targets The targets for drop
	 *
	 * @throws SchemaManagementException Indicates a problem processing the creation
	 */
	public void doDrop(Metadata metadata, boolean dropNamespaces, List<Target> targets) throws SchemaManagementException;

	/**
	 * Perform the drop to the specified targets
	 *
	 * @param metadata The "compiled" mapping metadata.
	 * @param dropNamespaces Should the schema(s)/catalog(s) actually be dropped also ({@code DROP SCHEMA})?
	 * @param targets The targets for drop
	 *
	 * @throws SchemaManagementException Indicates a problem processing the creation
	 */
	public void doDrop(Metadata metadata, boolean dropNamespaces, Dialect dialect, List<Target> targets) throws SchemaManagementException;
}
