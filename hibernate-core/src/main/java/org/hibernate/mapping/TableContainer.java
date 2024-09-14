/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

/**
 * Container for Table and Join reference
 *
 * @author Steve Ebersole
 */
public interface TableContainer {
	Table findTable(String name);
	Table getTable(String name);
	Join findSecondaryTable(String name);
	Join getSecondaryTable(String name);
}
