/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.dialect.Dialect;

/**
 * An <tt>IdentifierGenerator</tt> that requires creation of database objects.
 * <br><br>
 * All <tt>PersistentIdentifierGenerator</tt>s that also implement
 * <tt>Configurable</tt> have access to a special mapping parameter: schema
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @see IdentifierGenerator
 * @see Configurable
 */
public interface PersistentIdentifierGenerator extends IdentifierGenerator, ExportableProducer {

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
	 * The key under whcih to find the {@link org.hibernate.boot.model.naming.ObjectNameNormalizer} in the config param map.
	 */
	String IDENTIFIER_NORMALIZER = "identifier_normalizer";

	/**
	 * The SQL required to create the underlying database objects.
	 *
	 * @param dialect The dialect against which to generate the create command(s)
	 *
	 * @return The create command(s)
	 *
	 * @throws HibernateException problem creating the create command(s)
	 * @deprecated Utilize the ExportableProducer contract instead
	 */
	@Deprecated
	String[] sqlCreateStrings(Dialect dialect) throws HibernateException;

	/**
	 * The SQL required to remove the underlying database objects.
	 *
	 * @param dialect The dialect against which to generate the drop command(s)
	 *
	 * @return The drop command(s)
	 *
	 * @throws HibernateException problem creating the drop command(s)
	 * @deprecated Utilize the ExportableProducer contract instead
	 */
	@Deprecated
	String[] sqlDropStrings(Dialect dialect) throws HibernateException;

	/**
	 * Return a key unique to the underlying database objects. Prevents us from
	 * trying to create/remove them multiple times.
	 *
	 * @return Object an identifying key for this generator
	 */
	Object generatorKey();
}
