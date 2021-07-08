/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import javax.persistence.TableGenerator;

import org.hibernate.HibernateException;
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
public interface PersistentIdentifierGenerator extends OptimizableGenerator {
	/**
	 * The configuration parameter holding the catalog name
	 */
	String CATALOG = "catalog";

	/**
	 * The configuration parameter holding the schema name
	 */
	String SCHEMA = "schema";

	/**
	 * The configuration parameter key for the explicit id table name
	 *
	 * @see TableGenerator#name()
	 *
	 * @implNote The name is used to avoid collision with parameters
	 * for the entity table name which is already registered under `table`
	 */
	String TABLE_NAME_PARAM = "target_table";

	/**
	 * @deprecated (as of 6.0) Use {@link #TABLE_NAME_PARAM} instead
	 */
	@Deprecated
	String TABLE = TABLE_NAME_PARAM;

	/**
	 * The configuration parameter key for the explicit id sequence name
	 *
	 * @see javax.persistence.SequenceGenerator#name()
	 */
	String SEQUENCE_NAME_PARAM = "sequence_name";

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
	 * The key under which to find the {@link org.hibernate.boot.model.naming.ObjectNameNormalizer} in the config param map.
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
