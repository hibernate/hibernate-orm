/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.walking.spi;

import java.util.Arrays;


/**
 * Used to uniquely identify a foreign key, so that we don't join it more than once creating circularities.  Note
 * that the table+columns refers to the association owner.  These are used to help detect bi-directional associations
 * since the Hibernate runtime metamodel (persisters) do not inherently know this information.  For example, consider
 * the Order -> Customer and Customer -> Order(s) bi-directional association; both would be mapped to the
 * {@code ORDER_TABLE.CUST_ID} column.  That is the purpose of this struct.
 * <p/>
 * Bit of a misnomer to call this an association attribute.  But this follows the legacy use of AssociationKey
 * from old JoinWalkers to denote circular join detection
 *
 * @author Steve Ebersole
 * @author Gail Badner
 * @author Gavin King
 */
public class AssociationKey {
	private final String table;
	private final String[] columns;

	/**
	 * Create the AssociationKey.
	 *
	 * @param table The table part of the association key
	 * @param columns The columns that define the association key
	 */
	public AssociationKey(String table, String[] columns) {
		this.table = table;
		this.columns = columns;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final AssociationKey that = (AssociationKey) o;
		return table.equals( that.table ) && Arrays.equals( columns, that.columns );

	}

	@Override
	public int hashCode() {
		return table.hashCode();
	}

	private String str;

	@Override
	public String toString() {
		if ( str == null ) {
			str = "AssociationKey(table=" + table + ", columns={" + String.join( ",", columns ) + "})";
		}
		return str;
	}
}
