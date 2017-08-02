/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.spi;

/**
 * Unifying contract for things that are selectable at the SQL level.
 *
 * @author Steve Ebersole
 */
public interface SqlSelectable {
	/**
	 * Get the reader capable of reading values of this "selectable"
	 */
	SqlSelectionReader getSqlSelectionReader();
}
