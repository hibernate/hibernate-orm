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
 * names a "root" entity.  A root entity means it is explicitly a
 * "column" in the result, as opposed to a fetched relationship or role.
 *
 * @author Steve Ebersole
 */
public class NativeSQLQueryRootReturn extends NativeSQLQueryNonScalarReturn {
	private final String returnEntityName;
	private final int hashCode;

	/**
	 * Construct a return representing an entity returned at the root
	 * of the result.
	 *
	 * @param alias The result alias
	 * @param entityName The entity name.
	 * @param lockMode The lock mode to apply
	 */
	public NativeSQLQueryRootReturn(String alias, String entityName, LockMode lockMode) {
		this( alias, entityName, null, lockMode );
	}

	/**
	 *
	 * @param alias The result alias
	 * @param entityName The entity name.
	 * @param propertyResults Any user-supplied column->property mappings
	 * @param lockMode The lock mode to apply
	 */
	public NativeSQLQueryRootReturn(String alias, String entityName, Map<String,String[]> propertyResults, LockMode lockMode) {
		super( alias, propertyResults, lockMode );
		this.returnEntityName = entityName;
		this.hashCode = determineHashCode();
	}

	private int determineHashCode() {
		int result = super.hashCode();
		result = 31 * result + ( returnEntityName != null ? returnEntityName.hashCode() : 0 );
		return result;
	}

	/**
	 * The name of the entity to be returned.
	 *
	 * @return The entity name
	 */
	public String getReturnEntityName() {
		return returnEntityName;
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
		if ( ! super.equals( o ) ) {
			return false;
		}

		final NativeSQLQueryRootReturn that = (NativeSQLQueryRootReturn) o;

		if ( returnEntityName != null ? !returnEntityName.equals( that.returnEntityName ) : that.returnEntityName != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}
}
