/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.Incubating;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.mapping.Table;

/**
 * Defines a filter for Hibernate's schema tooling.
 *
 * @since 5.1
 */
@Incubating
public interface SchemaFilter {
	/**
	 * Should the given namespace (catalog+schema) be included?  If {@code true}, the
	 * namespace will be further processed; if {@code false}, processing will skip this
	 * namespace.
	 *
	 * @param namespace The namespace to check for inclusion.
	 *
	 * @return {@code true} to include the namespace; {@code false} otherwise
	 */
	boolean includeNamespace(Namespace namespace);

	/**
	 * Should the given table be included?  If {@code true}, the
	 * table will be further processed; if {@code false}, processing will skip this
	 * table.
	 *
	 * @param table The table to check for inclusion
	 *
	 * @return {@code true} to include the table; {@code false} otherwise
	 */
	boolean includeTable(Table table);

	/**
	 * Should the given sequence be included?  If {@code true}, the
	 * sequence will be further processed; if {@code false}, processing will skip this
	 * sequence.
	 *
	 * @param sequence The sequence to check for inclusion
	 *
	 * @return {@code true} to include the sequence; {@code false} otherwise
	 */
	boolean includeSequence(Sequence sequence);

}
