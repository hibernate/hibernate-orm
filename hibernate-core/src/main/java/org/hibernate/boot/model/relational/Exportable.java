/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;

/**
 * Contract for entities (in the ERD sense) which can be exported via {@code CREATE}, {@code ALTER}, etc
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.tool.schema.spi.Exporter
 */
public interface Exportable {
	/**
	 * Get a unique identifier to make sure we are not exporting the same database structure multiple times.
	 *
	 * @return The exporting identifier.
	 */
	public String getExportIdentifier();
}
