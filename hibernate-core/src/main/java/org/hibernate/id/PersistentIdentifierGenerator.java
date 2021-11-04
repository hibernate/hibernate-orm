/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * An <tt>IdentifierGenerator</tt> that requires creation of database objects.
 * <br><br>
 * All <tt>PersistentIdentifierGenerator</tt>s have access to a special mapping parameter
 * in their {@link #configure(Type, Properties, ServiceRegistry)} method: schema
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @see IdentifierGenerator
 */
public interface PersistentIdentifierGenerator extends IdentifierGenerator {

	/**
	 * The configuration parameter holding the schema name
	 */
	String SCHEMA = "schema";

	/**
	 * The configuration parameter holding the table name for the
	 * generated id
	 */
	String TABLE = "target_table";

	/**
	 * The configuration parameter holding the table names for all
	 * tables for which the id must be unique
	 */
	String TABLES = "identity_tables";

	/**
	 * The configuration parameter holding the primary key column
	 * name of the generated id
	 */
	String PK = "target_column";

	/**
	 * The configuration parameter holding the catalog name
	 */
	String CATALOG = "catalog";

	/**
	 * The key under which to find the {@link org.hibernate.boot.model.naming.ObjectNameNormalizer} in the config param map.
	 */
	String IDENTIFIER_NORMALIZER = "identifier_normalizer";
}
