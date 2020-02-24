/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.postgis;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.dialect.SpatialFunctionsRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Functions registered in all Postgis Dialects
 * <p>
 * Created by Karel Maesen, Geovise BVBA on 29/10/16.
 */
class PostgisFunctions extends SpatialFunctionsRegistry {

	PostgisFunctions() {

		put(
				"dimension", new StandardSQLFunction(
						"st_dimension",
						StandardBasicTypes.INTEGER
				)
		);
		put(
				"geometrytype", new StandardSQLFunction(
						"st_geometrytype", StandardBasicTypes.STRING
				)
		);
		put(
				"srid", new StandardSQLFunction(
						"st_srid",
						StandardBasicTypes.INTEGER
				)
		);
		put(
				"envelope", new StandardSQLFunction(
						"st_envelope"
				)
		);
		put(
				"makeenvelope", new StandardSQLFunction(
						"st_makeenvelope"
				)
		);
		put(
				"astext", new StandardSQLFunction(
						"st_astext",
						StandardBasicTypes.STRING
				)
		);
		put(
				"asbinary", new StandardSQLFunction(
						"st_asbinary",
						StandardBasicTypes.BINARY
				)
		);
		put(
				"isempty", new StandardSQLFunction(
						"st_isempty",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"issimple", new StandardSQLFunction(
						"st_issimple",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"boundary", new StandardSQLFunction(
						"st_boundary"
				)
		);

		// Register functions for spatial relation constructs
		put(
				"overlaps", new StandardSQLFunction(
						"st_overlaps",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"intersects", new StandardSQLFunction(
						"st_intersects",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"equals", new StandardSQLFunction(
						"st_equals",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"contains", new StandardSQLFunction(
						"st_contains",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"crosses", new StandardSQLFunction(
						"st_crosses",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"disjoint", new StandardSQLFunction(
						"st_disjoint",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"touches", new StandardSQLFunction(
						"st_touches",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"within", new StandardSQLFunction(
						"st_within",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"relate", new StandardSQLFunction(
						"st_relate",
						StandardBasicTypes.BOOLEAN
				)
		);

		// register the spatial analysis functions
		put(
				"distance", new StandardSQLFunction(
						"st_distance",
						StandardBasicTypes.DOUBLE
				)
		);
		put(
				"buffer", new StandardSQLFunction( "st_buffer"
				)
		);
		put(
				"convexhull", new StandardSQLFunction(
						"st_convexhull"
				)
		);
		put(
				"difference", new StandardSQLFunction(
						"st_difference"
				)
		);
		put(
				"intersection", new StandardSQLFunction(
						"st_intersection"
				)
		);
		put(
				"symdifference",
				new StandardSQLFunction( "st_symdifference" )
		);
		put(
				"geomunion", new StandardSQLFunction(
						"st_union"
				)
		);

		//register Spatial Aggregate function
		put(
				"extent", new ExtentFunction()
		);

		//register Spatial Filter function
		put(
				SpatialFunction.filter.name(), new FilterFunction()
		);

		//other common functions
		put(
				"dwithin", new StandardSQLFunction(
						"st_dwithin",
						StandardBasicTypes.BOOLEAN
				)
		);
		put(
				"transform", new StandardSQLFunction(
						"st_transform"
				)
		);
	}

	private static class ExtentFunction extends StandardSQLFunction {

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

	private static class FilterFunction extends StandardSQLFunction {

		public FilterFunction() {
			super( "&&" );
		}

		@Override
		public String render(
				Type firstArgumentType, List arguments, SessionFactoryImplementor sessionFactory) {
			int argumentCount = arguments.size();
			if ( argumentCount != 2 ) {
				throw new QueryException( String.format( "2 arguments expected, received %d", argumentCount ) );
			}

			return Stream.of(
					String.valueOf( arguments.get( 0 ) ),
					getRenderedName( arguments ),
					String.valueOf( arguments.get( 1 ) )
			).collect( Collectors.joining( " ", "(", ")" ) );
		}
	}

}
