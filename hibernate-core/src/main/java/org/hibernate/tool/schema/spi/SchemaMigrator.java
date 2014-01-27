/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import org.hibernate.metamodel.spi.relational.Database;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;

/**
 * Service delegate for handling schema migration.
 *
 * @author Steve Ebersole
 */
public interface SchemaMigrator {
	/**
	 * Perform a migration to the specified targets.
	 *
	 * @param database The current Hibernate relational model
	 * @param existingDatabase Access to the information about the existing database.
	 * @param createSchemas Should the schema(s) actually be created as well ({@code CREATE SCHEMA})?
	 * @param targets The migration targets
	 *
	 * @throws SchemaManagementException
	 */
	public void doMigration(
			Database database,
			DatabaseInformation existingDatabase,
			boolean createSchemas,
			List<Target> targets) throws SchemaManagementException;
}
