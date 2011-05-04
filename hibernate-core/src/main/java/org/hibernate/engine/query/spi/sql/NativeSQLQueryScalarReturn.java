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
import org.hibernate.type.Type;

/**
 * Describes a scalar return in a native SQL query.
 *
 * @author gloegl
 */
public class NativeSQLQueryScalarReturn implements NativeSQLQueryReturn {
	private final Type type;
	private final String columnAlias;
	private final int hashCode;

	public NativeSQLQueryScalarReturn(String alias, Type type) {
		this.type = type;
		this.columnAlias = alias;
		this.hashCode = determineHashCode();
	}

	public String getColumnAlias() {
		return columnAlias;
	}

	public Type getType() {
		return type;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		NativeSQLQueryScalarReturn that = ( NativeSQLQueryScalarReturn ) o;

		if ( columnAlias != null ? !columnAlias.equals( that.columnAlias ) : that.columnAlias != null ) {
			return false;
		}
		if ( type != null ? !type.equals( that.type ) : that.type != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return hashCode;
	}

	private int determineHashCode() {
		int result = type != null ? type.hashCode() : 0;
		result = 31 * result + ( getClass().getName().hashCode() );
		result = 31 * result + ( columnAlias != null ? columnAlias.hashCode() : 0 );
		return result;
	}
}
