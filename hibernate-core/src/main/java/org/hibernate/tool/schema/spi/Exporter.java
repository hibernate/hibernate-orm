/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.tool.schema.spi;

import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.spi.relational.Exportable;

/**
 * Defines a contract for exporting of database objects (tables, sequences, etc) for use in SQL {@code CREATE} and
 * {@code DROP} scripts
 *
 * @author Steve Ebersole
 */
public interface Exporter<T extends Exportable> {
	public static final String[] NO_COMMANDS = new String[0];

	/**
	 * Get the commands needed for creation.
	 *
	 * @return The commands needed for creation scripting.
	 */
	public String[] getSqlCreateStrings(T exportable, JdbcEnvironment jdbcEnvironment);

	/**
	 * Get the commands needed for dropping.
	 *
	 * @return The commands needed for drop scripting.
	 */
	public String[] getSqlDropStrings(T exportable, JdbcEnvironment jdbcEnvironment);
}
