/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.id.enhanced;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.dialect.Dialect;

/**
 * Encapsulates definition of the underlying data structure backing a
 * sequence-style generator.
 *
 * @author Steve Ebersole
 */
public interface DatabaseStructure {
	/**
	 * The name of the database structure (table or sequence).
	 * @return The structure name.
	 */
	public String getName();

	/**
	 * How many times has this structure been accessed through this reference?
	 * @return The number of accesses.
	 */
	public int getTimesAccessed();

	/**
	 * The configured initial value
	 * @return The configured initial value
	 */
	public int getInitialValue();

	/**
	 * The configured increment size
	 * @return The configured increment size
	 */
	public int getIncrementSize();

	/**
	 * A callback to be able to get the next value from the underlying
	 * structure as needed.
	 *
	 * @param session The session.
	 * @return The next value.
	 */
	public AccessCallback buildCallback(SessionImplementor session);

	/**
	 * Prepare this structure for use.  Called sometime after instantiation,
	 * but before first use.
	 *
	 * @param optimizer The optimizer being applied to the generator.
	 */
	public void prepare(Optimizer optimizer);

	/**
	 * Commands needed to create the underlying structures.
	 * @param dialect The database dialect being used.
	 * @return The creation commands.
	 */
	public String[] sqlCreateStrings(Dialect dialect);

	/**
	 * Commands needed to drop the underlying structures.
	 * @param dialect The database dialect being used.
	 * @return The drop commands.
	 */
	public String[] sqlDropStrings(Dialect dialect);
}