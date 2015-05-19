/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;


import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Exportable;

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
	public String[] getSqlCreateStrings(T exportable, Metadata metadata);

	/**
	 * Get the commands needed for dropping.
	 *
	 * @return The commands needed for drop scripting.
	 */
	public String[] getSqlDropStrings(T exportable, Metadata metadata);
}
