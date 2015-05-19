/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

/**
 * Used to help determine the implicit name of columns which are part of a primary-key,
 * well simultaneously being part of a foreign-key (join).  Generally, this happens in:<ul>
 *     <li>secondary tables</li>
 *     <li>joined inheritance tables</li>
 *     <li>one-to-one associations</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface ImplicitPrimaryKeyJoinColumnNameSource extends ImplicitNameSource {
	/**
	 * Access the name of the table referenced by the foreign-key described here.
	 *
	 * @return The referenced table name.
	 */
	Identifier getReferencedTableName();

	/**
	 * Access the name of the column that is a primary key column in the
	 * referenced-table that is referenced by the foreign-key described here.
	 *
	 * @return The referenced primary key column name.
	 */
	Identifier getReferencedPrimaryKeyColumnName();
}
