/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;

import java.io.Serializable;

import org.hibernate.dialect.Dialect;

/**
 * Auxiliary database objects (i.e., triggers, stored procedures, etc) defined
 * in the mappings.  Allows Hibernate to manage their lifecycle as part of
 * creating/dropping the schema.
 *
 * @author Steve Ebersole
 */
public interface AuxiliaryDatabaseObject extends Exportable, Serializable {
	/**
	 * Does this database object apply to the given dialect?
	 *
	 * @param dialect The dialect to check against.
	 * @return True if this database object does apply to the given dialect.
	 */
	public boolean appliesToDialect(Dialect dialect);

	/**
	 * Defines a simple precedence.  Should creation of this auxiliary object happen before creation of
	 * tables?  If {@code true}, the auxiliary object creation will happen after any explicit schema creations
	 * but before table/sequence creations; if {@code false}, the auxiliary object creation will happen after
	 * explicit schema creations and after table/sequence creations.
	 *
	 * This precedence is automatically inverted for dropping.
	 *
	 * @return {@code true} indicates this object should be created before tables; {@code false} indicates
	 * it should be created after.
	 */
	public boolean beforeTablesOnCreation();

	/**
	 * Gets the SQL strings for creating the database object.
	 *
	 * @param dialect The dialect for which to generate the SQL creation strings
	 *
	 * @return the SQL strings for creating the database object.
	 */
	public String[] sqlCreateStrings(Dialect dialect);

	/**
	 * Gets the SQL strings for dropping the database object.
	 *
	 * @param dialect The dialect for which to generate the SQL drop strings
	 *
	 * @return the SQL strings for dropping the database object.
	 */
	public String[] sqlDropStrings(Dialect dialect);

	/**
	 * Additional, optional interface for AuxiliaryDatabaseObject that want to allow
	 * expansion of allowable dialects via mapping.
	 */
	public static interface Expandable {
		public void addDialectScope(String dialectName);
	}
}
