/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */

package org.hibernate.boot.models.annotations.spi;

/**
 * Information which is common across all table annotations
 *
 * @author Steve Ebersole
 */
public interface CommonTableDetails extends DatabaseObjectDetails, UniqueConstraintCollector, IndexCollector {
	/**
	 * The table name
	 */
	String name();

	/**
	 * Setter for {@linkplain #name()}
	 */
	void name(String name);
}
