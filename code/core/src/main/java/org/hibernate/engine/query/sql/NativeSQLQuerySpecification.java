package org.hibernate.engine.query.sql;

import org.hibernate.util.ArrayHelper;

import java.util.Set;
import java.util.Collection;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collections;

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
			Set tmp = new HashSet();
			tmp.addAll( querySpaces );
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

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final NativeSQLQuerySpecification that = ( NativeSQLQuerySpecification ) o;

		return querySpaces.equals( that.querySpaces ) &&
		       queryString.equals( that.queryString ) &&
		       Arrays.equals( queryReturns, that.queryReturns );
	}


	public int hashCode() {
		return hashCode;
	}
}
