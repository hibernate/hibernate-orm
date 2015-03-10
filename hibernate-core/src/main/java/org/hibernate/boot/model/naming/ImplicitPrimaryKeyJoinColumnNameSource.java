/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
