/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import java.util.Collection;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface DatabaseModel {
	Collection<Namespace> getNamespaces();

	Namespace getDefaultNamespace();

	JdbcEnvironment getJdbcEnvironment();

	Collection<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjects();

	Collection<InitCommand> getInitCommands();

	void addInitCommand(InitCommand initCommand);

	default Namespace getNamespace(String catalogName, String schemaName) {
		if ( catalogName == null && schemaName == null ) {
			return getDefaultNamespace();
		}

		for ( Namespace namespace : getNamespaces() ) {
			if ( catalogName != null ) {
				// we need to match the catalog name
				if ( namespace.getCatalogName() == null ) {
					continue;
				}

				if ( ! catalogName.equals( namespace.getCatalogName().getCanonicalName() ) ) {
					continue;
				}
			}

			if ( schemaName != null ) {
				// we need to match the schema name
				if ( namespace.getSchemaName() == null ) {
					continue;
				}

				if ( ! schemaName.equals( namespace.getSchemaName().getCanonicalName() ) ) {
					continue;
				}

				// if we get here we have a match.. return it
				return namespace;
			}
		}

		throw new HibernateException(
				"Could not locate database namespace [catalog=" + catalogName + ", schema=" + schemaName + "]"
		);
	}
}
