/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;
import java.io.Serializable;

import org.hibernate.dialect.Dialect;

/**
 * Auxiliary database objects (i.e., triggers, stored procedures, etc) defined
 * in the mappings.  Allows Hibernate to manage their lifecycle as part of
 * creating/dropping the schema.
 *
 * @author Steve Ebersole
 */
public interface AuxiliaryDatabaseObject extends RelationalModel, Serializable {
	/**
	 * Add the given dialect name to the scope of dialects to which
	 * this database object applies.
	 *
	 * @param dialectName The name of a dialect.
	 */
	void addDialectScope(String dialectName);

	/**
	 * Does this database object apply to the given dialect?
	 *
	 * @param dialect The dialect to check against.
	 * @return True if this database object does apply to the given dialect.
	 */
	boolean appliesToDialect(Dialect dialect);
}
