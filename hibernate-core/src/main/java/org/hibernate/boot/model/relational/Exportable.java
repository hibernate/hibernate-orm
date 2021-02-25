/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;

import org.hibernate.mapping.Contributable;

/**
 * Contract for entities (in the ERD sense) which can be exported via {@code CREATE}, {@code ALTER}, etc
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.tool.schema.spi.Exporter
 */
public interface Exportable extends Contributable {
	/**
	 * Get a unique identifier to make sure we are not exporting the same database structure multiple times.
	 *
	 * @return The exporting identifier.
	 */
	String getExportIdentifier();

	/**
	 * The contributor of this exportable.  Usually these come from ORM mappings of the application model.
	 * But other integrations might contribute relational objects to the database model - hibernate-search,
	 * hibernate-envers, etc
	 */
	default String getContributor() {
		return "orm";
	}
}
