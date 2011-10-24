/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
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
		this(alias, entityName, null, lockMode);
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

	/**
	 * The name of the entity to be returned.
	 *
	 * @return The entity name
	 */
	public String getReturnEntityName() {
		return returnEntityName;
	}

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

		NativeSQLQueryRootReturn that = ( NativeSQLQueryRootReturn ) o;

		if ( returnEntityName != null ? !returnEntityName.equals( that.returnEntityName ) : that.returnEntityName != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return hashCode;
	}

	private int determineHashCode() {
		int result = super.hashCode();
		result = 31 * result + ( returnEntityName != null ? returnEntityName.hashCode() : 0 );
		return result;
	}
}
