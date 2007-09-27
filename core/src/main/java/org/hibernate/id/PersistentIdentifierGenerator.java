//$Id: PersistentIdentifierGenerator.java 6514 2005-04-26 06:37:54Z oneovthafew $
package org.hibernate.id;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.jdbc.util.SQLStatementLogger;

/**
 * An <tt>IdentifierGenerator</tt> that requires creation of database objects.
 * <br><br>
 * All <tt>PersistentIdentifierGenerator</tt>s that also implement
 * <tt>Configurable</tt> have access to a special mapping parameter: schema
 *
 * @see IdentifierGenerator
 * @see Configurable
 * @author Gavin King
 */
public interface PersistentIdentifierGenerator extends IdentifierGenerator {

	/**
	 * The configuration parameter holding the schema name
	 */
	public static final String SCHEMA = "schema";

	/**
	 * The configuration parameter holding the table name for the
	 * generated id
	 */
	public static final String TABLE = "target_table";

	/**
	 * The configuration parameter holding the table names for all
	 * tables for which the id must be unique
	 */
	public static final String TABLES = "identity_tables";

	/**
	 * The configuration parameter holding the primary key column
	 * name of the generated id
	 */
	public static final String PK = "target_column";

    /**
     * The configuration parameter holding the catalog name
     */
    public static final String CATALOG = "catalog";
    
	/**
	 * The SQL required to create the underlying database objects.
	 *
	 * @param dialect The dialect against which to generate the create command(s)
	 * @return The create command(s)
	 * @throws HibernateException problem creating the create command(s)
	 */
	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException;

	/**
	 * The SQL required to remove the underlying database objects.
	 *
	 * @param dialect The dialect against which to generate the drop command(s)
	 * @return The drop command(s)
	 * @throws HibernateException problem creating the drop command(s)
	 */
	public String[] sqlDropStrings(Dialect dialect) throws HibernateException;

	/**
	 * Return a key unique to the underlying database objects. Prevents us from
	 * trying to create/remove them multiple times.
	 * 
	 * @return Object an identifying key for this generator
	 */
	public Object generatorKey();

	static final SQLStatementLogger SQL_STATEMENT_LOGGER = new SQLStatementLogger( false, false );

}






