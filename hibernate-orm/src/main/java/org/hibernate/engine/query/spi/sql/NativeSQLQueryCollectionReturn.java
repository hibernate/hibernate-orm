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
 * names a collection role in the form {className}.{collectionRole}; it
 * is used in defining a custom sql query for loading an entity's
 * collection in non-fetching scenarios (i.e., loading the collection
 * itself as the "root" of the result).
 *
 * @author Steve Ebersole
 */
public class NativeSQLQueryCollectionReturn extends NativeSQLQueryNonScalarReturn {
	private final String ownerEntityName;
	private final String ownerProperty;
	private final int hashCode;

	/**
	 * Construct a native-sql return representing a collection initializer
	 *
	 * @param alias The result alias
	 * @param ownerEntityName The entity-name of the entity owning the collection
	 * to be initialized.
	 * @param ownerProperty The property name (on the owner) which represents
	 * the collection to be initialized.
	 * @param propertyResults Any user-supplied column->property mappings
	 * @param lockMode The lock mode to apply to the collection.
	 */
	@SuppressWarnings("unchecked")
	public NativeSQLQueryCollectionReturn(
			String alias,
			String ownerEntityName,
			String ownerProperty,
			Map propertyResults,
			LockMode lockMode) {
		super( alias, propertyResults, lockMode );
		this.ownerEntityName = ownerEntityName;
		this.ownerProperty = ownerProperty;
		this.hashCode = determineHashCode();
	}

	private int determineHashCode() {
		int result = super.hashCode();
		result = 31 * result + ( ownerEntityName != null ? ownerEntityName.hashCode() : 0 );
		result = 31 * result + ( ownerProperty != null ? ownerProperty.hashCode() : 0 );
		return result;
	}

	/**
	 * Returns the class owning the collection.
	 *
	 * @return The class owning the collection.
	 */
	public String getOwnerEntityName() {
		return ownerEntityName;
	}

	/**
	 * Returns the name of the property representing the collection from the {@link #getOwnerEntityName}.
	 *
	 * @return The name of the property representing the collection on the owner class.
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

		final NativeSQLQueryCollectionReturn that = (NativeSQLQueryCollectionReturn) o;

		if ( ownerEntityName != null ? !ownerEntityName.equals( that.ownerEntityName ) : that.ownerEntityName != null ) {
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
