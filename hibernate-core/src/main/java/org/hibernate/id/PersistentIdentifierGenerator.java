/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.id;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.spi.relational.ExportableProducer;

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
public interface PersistentIdentifierGenerator extends IdentifierGenerator, ExportableProducer {

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
	 * The key under whcih to find the {@link org.hibernate.cfg.ObjectNameNormalizer} in the config param map.
	 */
	public static final String IDENTIFIER_NORMALIZER = "identifier_normalizer";

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
}
