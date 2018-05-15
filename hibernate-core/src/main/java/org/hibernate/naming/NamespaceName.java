/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.naming;

import java.util.Objects;

/**
 * @author Steve Ebersole
 */
public class NamespaceName implements Comparable<NamespaceName> {
	private final Identifier catalog;
	private final Identifier schema;

	public NamespaceName(Identifier catalog, Identifier schema) {
		this.catalog = catalog;
		this.schema = schema;
	}

	public Identifier getCatalog() {
		return catalog;
	}

	public Identifier getSchema() {
		return schema;
	}

	@Override
	public String toString() {
		return "Name" + "{catalog=" + catalog + ", schema=" + schema + '}';
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final NamespaceName that = (NamespaceName) o;

		return Objects.equals( this.catalog, that.catalog )
				&& Objects.equals( this.schema, that.schema );
	}

	@Override
	public int hashCode() {
		int result = catalog != null ? catalog.hashCode() : 0;
		result = 31 * result + ( schema != null ? schema.hashCode() : 0 );
		return result;
	}

	@Override
	public int compareTo(NamespaceName that) {
		// per Comparable, the incoming Name cannot be null.  However, its catalog/schema might be
		// so we need to account for that.
		int catalogCheck = compare( this.catalog, that.catalog );
		if ( catalogCheck != 0 ) {
			return catalogCheck;
		}

		return compare( this.schema, that.schema );
	}


	public static <T extends Comparable<T>> int compare(T first, T second) {
		if ( first == null ) {
			if ( second == null ) {
				return 0;
			}
			else {
				return 1;
			}
		}
		else {
			if ( second == null ) {
				return -1;
			}
			else {
				return first.compareTo( second );
			}
		}
	}
}
