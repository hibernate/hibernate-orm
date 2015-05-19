/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;

/**
 * Identifies metamodel objects that can produce {@link Exportable} relational stuff.
 *
 * @author Steve Ebersole
 */
public interface ExportableProducer {
	/**
	 * Register the contained exportable things to the {@link Database}
	 *
	 * @param database The database instance
	 */
	public void registerExportables(Database database);
}
