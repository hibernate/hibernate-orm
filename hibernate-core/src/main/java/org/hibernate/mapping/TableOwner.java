/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.boot.model.relational.MappedTable;

/**
 * Additional, optional contract as part pf the {@link org.hibernate.mapping.PersistentClass}
 * hierarchy used to differentiate entity bindings for entities that map to their own table
 * (root, union-subclass, joined-subclass) versus those that do not (discriminator-subclass).
 * 
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public interface TableOwner {
	/**
	 * @deprecated since 6.0, use {@link #setMappedTable(MappedTable)}.
	 */
	@Deprecated
	default void setTable(Table table) {
		setMappedTable( table );
	}

	void setMappedTable(MappedTable mappedTable);
}
