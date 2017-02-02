/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	private int determineHashCode() {
		int result = type != null ? type.hashCode() : 0;
		result = 31 * result + ( getClass().getName().hashCode() );
		result = 31 * result + ( columnAlias != null ? columnAlias.hashCode() : 0 );
		return result;
	}

	public String getColumnAlias() {
		return columnAlias;
	}

	public Type getType() {
		return type;
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

		final NativeSQLQueryScalarReturn that = (NativeSQLQueryScalarReturn) o;
		if ( columnAlias != null ? !columnAlias.equals( that.columnAlias ) : that.columnAlias != null ) {
			return false;
		}
		if ( type != null ? !type.equals( that.type ) : that.type != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public void traceLog(TraceLogger logger) {
		logger.writeLine( "Scalar[" );
		logger.writeLine( "    columnAlias=" + columnAlias + "," );
		logger.writeLine( "    type=" + ( type == null ? "<unknown>" : type.getName() ) + "," );
		logger.writeLine( "]" );
	}
}
