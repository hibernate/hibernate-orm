/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.cockroachdb;

import org.hibernate.spatial.dialect.SpatialFunctionsRegistry;
import org.hibernate.spatial.dialect.postgis.PostgisFunctions;
import org.hibernate.spatial.dialect.postgis.PostgisSupport;

public class CockroachDBSpatialSupport extends PostgisSupport {

	CockroachDBSpatialSupport(){
		super(new CockroachDBSpatialFunctions() );
	}


}

class CockroachDBSpatialFunctions extends PostgisFunctions {

	CockroachDBSpatialFunctions(){
		super();
		this.functionMap.remove( "geomunion" );
	}

}

