/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.relational;

import org.hibernate.dialect.Dialect;

/**
 * Contract for entities (in the ERD sense) which can be exported via {@code CREATE}, {@code ALTER}, etc
 *
 * @author Steve Ebersole
 */
public interface Exportable {
	/**
	 * Get a unique identifier to make sure we are not exporting the same database structure multiple times.
	 *
	 * @return The exporting identifier.
	 */
	public String getExportIdentifier();

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
