/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi.sql;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.internal.util.collections.ArrayHelper;

/**
 * Defines the specification or blue-print for a native-sql query.
 * Essentially a simple struct containing the information needed to "translate"
 * a native-sql query and cache that translated representation.  Also used as
 * the key by which the native-sql query plans are cached.
 *
 * @author Steve Ebersole
 */
public class NativeSQLQuerySpecification {
	private final String queryString;
	private final NativeSQLQueryReturn[] queryReturns;
	private final Set querySpaces;
	private final int hashCode;

	public NativeSQLQuerySpecification(
			String queryString,
			NativeSQLQueryReturn[] queryReturns,
			Collection querySpaces) {
		this.queryString = queryString;
		this.queryReturns = queryReturns;
		if ( querySpaces == null ) {
			this.querySpaces = Collections.EMPTY_SET;
		}
		else {
			Set tmp = new HashSet( querySpaces );
			this.querySpaces = Collections.unmodifiableSet( tmp );
		}

		// pre-determine and cache the hashcode
		int hashCode = queryString.hashCode();
		hashCode = 29 * hashCode + this.querySpaces.hashCode();
		if ( this.queryReturns != null ) {
			hashCode = 29 * hashCode + ArrayHelper.toList( this.queryReturns ).hashCode();
		}
		this.hashCode = hashCode;
	}

	public String getQueryString() {
		return queryString;
	}

	public NativeSQLQueryReturn[] getQueryReturns() {
		return queryReturns;
	}

	public Set getQuerySpaces() {
		return querySpaces;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final NativeSQLQuerySpecification that = (NativeSQLQuerySpecification) o;

		return querySpaces.equals( that.querySpaces )
				&& queryString.equals( that.queryString )
				&& Arrays.equals( queryReturns, that.queryReturns );
	}

	@Override
	public int hashCode() {
		return hashCode;
	}
}
