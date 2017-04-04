/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
