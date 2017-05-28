/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.postgis;

import java.util.List;

import org.hibernate.query.sqm.produce.function.spi.NamedSqmFunctionTemplate;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.spatial.dialect.SpatialFunctionsRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Functions registered in all Postgis Dialects
 *
 * Created by Karel Maesen, Geovise BVBA on 29/10/16.
 */
class PostgisFunctions extends SpatialFunctionsRegistry {

	PostgisFunctions() {

		put(
				"dimension", new NamedSqmFunctionTemplate(
						"st_dimension",
						StandardBasicTypes.INTEGER
				)
		);
		put(
				"geometrytype", new NamedSqmFunctionTemplate(
						"st_geometrytype", StandardBasicTypes.STRING
				)
		);
		put(
				"srid", new NamedSqmFunctionTemplate(
						"st_srid",
						StandardBasicTypes.INTEGER
				)
		);
		put(
				"envelope", new NamedSqmFunctionTemplate(
						"st_envelope"
				)
		);
		put(
				"astext", new NamedSqmFunctionTemplate(
						"st_astext",
						StandardBasicTypes.STRING
				)
		);
		put(
				"asbinary", new NamedSqmFunctionTemplate(
						"st_asbinary",
						StandardBasicTypes.BINARY
				)
		);
		put(
				"isempty", new NamedSqmFunctionTemplate(
						"st_isempty",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"issimple", new NamedSqmFunctionTemplate(
						"st_issimple",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"boundary", new NamedSqmFunctionTemplate(
						"st_boundary"
				)
		);

		// Register functions for spatial relation constructs
		put(
				"overlaps", new NamedSqmFunctionTemplate(
						"st_overlaps",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"intersects", new NamedSqmFunctionTemplate(
						"st_intersects",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"equals", new NamedSqmFunctionTemplate(
						"st_equals",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"contains", new NamedSqmFunctionTemplate(
						"st_contains",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"crosses", new NamedSqmFunctionTemplate(
						"st_crosses",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"disjoint", new NamedSqmFunctionTemplate(
						"st_disjoint",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"touches", new NamedSqmFunctionTemplate(
						"st_touches",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"within", new NamedSqmFunctionTemplate(
						"st_within",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"relate", new NamedSqmFunctionTemplate(
						"st_relate",
						StandardBasicTypes.BOOLEAN
				)
		);

		// register the spatial analysis functions
		put(
				"distance", new NamedSqmFunctionTemplate(
						"st_distance",
						StandardBasicTypes.DOUBLE
				)
		);
		put(
				"buffer", new NamedSqmFunctionTemplate(
						"st_buffer"
				)
		);
		put(
				"convexhull", new NamedSqmFunctionTemplate(
						"st_convexhull"
				)
		);
		put(
				"difference", new NamedSqmFunctionTemplate(
						"st_difference"
				)
		);
		put(
				"intersection", new NamedSqmFunctionTemplate(
						"st_intersection"
				)
		);
		put(
				"symdifference",
				new NamedSqmFunctionTemplate( "st_symdifference" )
		);
		put(
				"geomunion", new NamedSqmFunctionTemplate(
						"st_union"
				)
		);

		//register Spatial Aggregate function
		put(
				"extent", new ExtentFunction()
		);

		//other common functions
		put(
				"dwithin", new NamedSqmFunctionTemplate(
						"st_dwithin",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"transform", new NamedSqmFunctionTemplate(
						"st_transform"
				)
		);
	}

	private static class ExtentFunction extends NamedSqmFunctionTemplate {

		public ExtentFunction() {
			super( "st_extent" );
		}

		@Override
		public String render(
				Type firstArgumentType, List arguments, SessionFactoryImplementor sessionFactory) {
			String rendered = super.render( firstArgumentType, arguments, sessionFactory );
			//add cast
			return rendered + "::geometry";
		}
	}
}
