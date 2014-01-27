/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.relational;

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
}
