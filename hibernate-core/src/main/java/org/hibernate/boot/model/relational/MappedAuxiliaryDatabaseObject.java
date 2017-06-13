/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;

import java.io.Serializable;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.relational.spi.AuxiliaryDatabaseObject;

/**
 * Auxiliary database objects (i.e., triggers, stored procedures, etc) defined
 * in the mappings.  Allows Hibernate to manage their lifecycle as part of
 * creating/dropping the schema.
 *
 * @author Steve Ebersole
 */
public interface MappedAuxiliaryDatabaseObject extends Serializable {

	/**
	 * Get a unique identifier to make sure we are not adding the same database structure multiple times.
	 *
	 * @return The identifier.
	 */
	String getIdentifier();

	/**
	 * Does this database object apply to the given dialect?
	 *
	 * @param dialect The dialect to check against.
	 * @return True if this database object does apply to the given dialect.
	 */
	boolean appliesToDialect(Dialect dialect);

	/**
	 * Defines a simple precedence.  Should creation of this auxiliary object happen beforeQuery creation of
	 * tables?  If {@code true}, the auxiliary object creation will happen afterQuery any explicit schema creations
	 * but beforeQuery table/sequence creations; if {@code false}, the auxiliary object creation will happen afterQuery
	 * explicit schema creations and afterQuery table/sequence creations.
	 *
	 * This precedence is automatically inverted for dropping.
	 *
	 * @return {@code true} indicates this object should be created beforeQuery tables; {@code false} indicates
	 * it should be created afterQuery.
	 */
	boolean beforeTablesOnCreation();

	/**
	 * Gets the SQL strings for creating the database object.
	 *
	 * @param dialect The dialect for which to generate the SQL creation strings
	 *
	 * @return the SQL strings for creating the database object.
	 */
	String[] sqlCreateStrings(Dialect dialect);

	/**
	 * Gets the SQL strings for dropping the database object.
	 *
	 * @param dialect The dialect for which to generate the SQL drop strings
	 *
	 * @return the SQL strings for dropping the database object.
	 */
	String[] sqlDropStrings(Dialect dialect);

	/**
	 * Additional, optional interface for AuxiliaryDatabaseObject that want to allow
	 * expansion of allowable dialects via mapping.
	 */
	interface Expandable {
		void addDialectScope(String dialectName);
	}

	default AuxiliaryDatabaseObject generateRuntimeAuxiliaryDatabaseObject(Dialect dialect) {
		return new AuxiliaryDatabaseObject(
				getIdentifier(),
				beforeTablesOnCreation(),
				sqlCreateStrings( dialect ),
				sqlDropStrings( dialect )
		);
	}
}
