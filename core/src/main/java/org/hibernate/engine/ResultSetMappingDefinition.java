//$Id: ResultSetMappingDefinition.java 10018 2006-06-15 05:21:06Z steve.ebersole@jboss.com $
package org.hibernate.engine;

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

import org.hibernate.engine.query.sql.NativeSQLQueryReturn;

/**
 * Keep a description of the resultset mapping
 *
 * @author Emmanuel Bernard
 */
public class ResultSetMappingDefinition implements Serializable {

	private final String name;
	private final List /*NativeSQLQueryReturn*/ queryReturns = new ArrayList();

	public ResultSetMappingDefinition(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

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
		return ( NativeSQLQueryReturn[] ) queryReturns.toArray( new NativeSQLQueryReturn[0] );
	}

}
