/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;

/**
 * Keep a description of the resultset mapping
 *
 * @author Emmanuel Bernard
 */
public class ResultSetMappingDefinition implements Serializable {
	private final String name;
	private final List<NativeSQLQueryReturn> queryReturns = new ArrayList<NativeSQLQueryReturn>();

	/**
	 * Constructs a ResultSetMappingDefinition
	 *
	 * @param name The mapping name
	 */
	public ResultSetMappingDefinition(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	/**
	 * Adds a return.
	 *
	 * @param queryReturn The return
	 */
	public void addQueryReturn(NativeSQLQueryReturn queryReturn) {
		queryReturns.add( queryReturn );
	}

// We could also keep these if needed for binary compatibility with annotations, provided
// it only uses the addXXX() methods...
//	public void addEntityQueryReturn(NativeSQLQueryNonScalarReturn entityQueryReturn) {
//		entityQueryReturns.add(entityQueryReturn);
//	}
//
//	public void addScalarQueryReturn(NativeSQLQueryScalarReturn scalarQueryReturn) {
//		scalarQueryReturns.add(scalarQueryReturn);
//	}

	public NativeSQLQueryReturn[] getQueryReturns() {
		return queryReturns.toArray( new NativeSQLQueryReturn[queryReturns.size()] );
	}

	public String traceLoggableFormat() {
		final StringBuilder buffer = new StringBuilder()
				.append( "ResultSetMappingDefinition[\n" )
				.append( "    name=" ).append( name ).append( "\n" )
				.append( "    returns=[\n" );

		for ( NativeSQLQueryReturn rtn : queryReturns ) {
			rtn.traceLog(
					new NativeSQLQueryReturn.TraceLogger() {
						@Override
						public void writeLine(String traceLine) {
							buffer.append( "        " ).append( traceLine ).append( "\n" );
						}
					}
			);
		}

		buffer.append( "    ]\n" ).append( "]" );

		return buffer.toString();
	}
}
