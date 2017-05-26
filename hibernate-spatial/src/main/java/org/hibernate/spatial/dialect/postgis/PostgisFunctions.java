/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.postgis;

import java.util.List;

import org.hibernate.query.sqm.produce.function.spi.StandardSqmFunctionTemplate;
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
				"dimension", new StandardSqmFunctionTemplate(
						"st_dimension",
						StandardBasicTypes.INTEGER
				)
		);
		put(
				"geometrytype", new StandardSqmFunctionTemplate(
						"st_geometrytype", StandardBasicTypes.STRING
				)
		);
		put(
				"srid", new StandardSqmFunctionTemplate(
						"st_srid",
						StandardBasicTypes.INTEGER
				)
		);
		put(
				"envelope", new StandardSqmFunctionTemplate(
						"st_envelope"
				)
		);
		put(
				"astext", new StandardSqmFunctionTemplate(
						"st_astext",
						StandardBasicTypes.STRING
				)
		);
		put(
				"asbinary", new StandardSqmFunctionTemplate(
						"st_asbinary",
						StandardBasicTypes.BINARY
				)
		);
		put(
				"isempty", new StandardSqmFunctionTemplate(
						"st_isempty",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"issimple", new StandardSqmFunctionTemplate(
						"st_issimple",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"boundary", new StandardSqmFunctionTemplate(
						"st_boundary"
				)
		);

		// Register functions for spatial relation constructs
		put(
				"overlaps", new StandardSqmFunctionTemplate(
						"st_overlaps",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"intersects", new StandardSqmFunctionTemplate(
						"st_intersects",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"equals", new StandardSqmFunctionTemplate(
						"st_equals",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"contains", new StandardSqmFunctionTemplate(
						"st_contains",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"crosses", new StandardSqmFunctionTemplate(
						"st_crosses",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"disjoint", new StandardSqmFunctionTemplate(
						"st_disjoint",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"touches", new StandardSqmFunctionTemplate(
						"st_touches",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"within", new StandardSqmFunctionTemplate(
						"st_within",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"relate", new StandardSqmFunctionTemplate(
						"st_relate",
						StandardBasicTypes.BOOLEAN
				)
		);

		// register the spatial analysis functions
		put(
				"distance", new StandardSqmFunctionTemplate(
						"st_distance",
						StandardBasicTypes.DOUBLE
				)
		);
		put(
				"buffer", new StandardSqmFunctionTemplate(
						"st_buffer"
				)
		);
		put(
				"convexhull", new StandardSqmFunctionTemplate(
						"st_convexhull"
				)
		);
		put(
				"difference", new StandardSqmFunctionTemplate(
						"st_difference"
				)
		);
		put(
				"intersection", new StandardSqmFunctionTemplate(
						"st_intersection"
				)
		);
		put(
				"symdifference",
				new StandardSqmFunctionTemplate( "st_symdifference" )
		);
		put(
				"geomunion", new StandardSqmFunctionTemplate(
						"st_union"
				)
		);

		//register Spatial Aggregate function
		put(
				"extent", new ExtentFunction()
		);

		//other common functions
		put(
				"dwithin", new StandardSqmFunctionTemplate(
						"st_dwithin",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"transform", new StandardSqmFunctionTemplate(
						"st_transform"
				)
		);
	}

	private static class ExtentFunction extends StandardSqmFunctionTemplate {

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
