/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi.sql;

import java.util.Map;

import org.hibernate.LockMode;

/**
 * Represents a return defined as part of a native sql query which
 * names a fetched role.
 *
 * @author Steve Ebersole
 */
public class NativeSQLQueryJoinReturn extends NativeSQLQueryNonScalarReturn {
	private final String ownerAlias;
	private final String ownerProperty;
	private final int hashCode;
	/**
	 * Construct a return descriptor representing some form of fetch.
	 *
	 * @param alias The result alias
	 * @param ownerAlias The owner's result alias
	 * @param ownerProperty The owner's property representing the thing to be fetched
	 * @param propertyResults Any user-supplied column->property mappings
	 * @param lockMode The lock mode to apply
	 */
	@SuppressWarnings("unchecked")
	public NativeSQLQueryJoinReturn(
			String alias,
			String ownerAlias,
			String ownerProperty,
			Map propertyResults,
			LockMode lockMode) {
		super( alias, propertyResults, lockMode );
		this.ownerAlias = ownerAlias;
		this.ownerProperty = ownerProperty;
		this.hashCode = determineHashCode();
	}

	private int determineHashCode() {
		int result = super.hashCode();
		result = 31 * result + ( ownerAlias != null ? ownerAlias.hashCode() : 0 );
		result = 31 * result + ( ownerProperty != null ? ownerProperty.hashCode() : 0 );
		return result;
	}

	/**
	 * Retrieve the alias of the owner of this fetched association.
	 *
	 * @return The owner's alias.
	 */
	public String getOwnerAlias() {
		return ownerAlias;
	}

	/**
	 * Retrieve the property name (relative to the owner) which maps to
	 * the association to be fetched.
	 *
	 * @return The property name.
	 */
	public String getOwnerProperty() {
		return ownerProperty;
	}

	@Override
	@SuppressWarnings("RedundantIfStatement")
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		final NativeSQLQueryJoinReturn that = (NativeSQLQueryJoinReturn) o;

		if ( ownerAlias != null ? !ownerAlias.equals( that.ownerAlias ) : that.ownerAlias != null ) {
			return false;
		}
		if ( ownerProperty != null ? !ownerProperty.equals( that.ownerProperty ) : that.ownerProperty != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}
}
