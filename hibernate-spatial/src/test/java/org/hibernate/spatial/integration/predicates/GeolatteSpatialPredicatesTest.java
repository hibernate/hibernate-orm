/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.integration.predicates;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import org.hibernate.spatial.integration.Model;
import org.hibernate.spatial.predicate.GeolatteSpatialPredicates;
import org.hibernate.spatial.predicate.JTSSpatialPredicates;
import org.hibernate.spatial.testing.HSReflectionUtil;
import org.hibernate.spatial.testing.IsSupportedBySpatial;
import org.hibernate.spatial.testing.SpatialTestBase;
import org.hibernate.spatial.testing.datareader.TestSupport;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.junit.platform.commons.JUnitException;

import org.geolatte.geom.G2D;
import org.geolatte.geom.Polygon;

import static org.geolatte.geom.builder.DSL.g;
import static org.geolatte.geom.builder.DSL.polygon;
import static org.geolatte.geom.builder.DSL.ring;
import static org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({ "unchecked", "rawtypes" })
@RequiresDialectFeature(feature = IsSupportedBySpatial.class)
@SessionFactory
public class GeolatteSpatialPredicatesTest extends SpatialTestBase {

	public final static TestSupport.TestDataPurpose PURPOSE = TestSupport.TestDataPurpose.SpatialFunctionsData;

	static final Polygon<G2D> filter = polygon(
			WGS84,
			ring( g( 0, 0 ), g( 0, 10 ), g( 10, 10 ), g( 10, 0 ), g( 0, 0 ) )
	);

	@Override
	public TestSupport.TestDataPurpose purpose() {
		return PURPOSE;
	}

	@TestFactory
	public Stream<DynamicTest> testFactory() {
		return
				predicateRegexes.all()
						.flatMap( entry -> Stream.of(
								new Args( entry.getKey(), entry.getValue(), Model.GLMODEL ),
								new Args( entry.getKey(), entry.getValue(), Model.JTSMODEL )
						) )
						.map( args -> DynamicTest.dynamicTest(
								displayName( args ),
								testPredicate(
										args.method,
										args.regex,
										args.model
								)
						) );
	}

	@SuppressWarnings("rawtypes")
	public Executable testPredicate(final String key, final String regex, Model model) {
		return () -> scope.inSession( session -> {
			SQLStatementInspector inspector = SQLStatementInspector.extractFromSession( session );
			inspector.clear();
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery query = getCriteriaQuery(
					key,
					criteriaBuilder,
					model
			);
			List resultList = session.createQuery( query ).getResultList();
			String stmt = inspector.getSqlQueries()
					.get( 0 )
					.toLowerCase( Locale.ROOT );
			//TODO -- can't we use a (hamcrest) matcher here?
			assertTrue( stmt.matches( regex ) );
		} );
	}

	private CriteriaQuery getCriteriaQuery(
			String key,
			CriteriaBuilder criteriaBuilder, Model model) {
		CriteriaQuery query = criteriaBuilder.createQuery( model.entityClass );
		Root root = query.from( model.entityClass );
		Method method = predicateMethod( key, model );
		finalizeQuery( criteriaBuilder, model, query, root, method );
		return query;
	}

	private void finalizeQuery(
			CriteriaBuilder criteriaBuilder,
			Model model,
			CriteriaQuery query,
			Root root,
			Method method) {
		try {
			query.select( root )
					.where( (Expression<Boolean>) method.invoke(
							null, criteriaBuilder, root.get( "geom" ), model.from.apply( filter ) ) );
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			throw new JUnitException( "Failure to invoke Geometry Predicate", e );
		}
	}

	@SuppressWarnings("rawtypes")
	private Method predicateMethod(String key, Model model) {
		Class predicateFactoryClass = model == Model.JTSMODEL ?
				JTSSpatialPredicates.class :
				GeolatteSpatialPredicates.class;
		return HSReflectionUtil.getStaticMethod(
				predicateFactoryClass,
				key,
				CriteriaBuilder.class, Expression.class, model.geometryClass
		);
	}

	private String displayName(Args args) {
		return String.format( "Predicate %s on %s", args.method, args.model.entityClass.getSimpleName() );
	}
}

class Args {
	final String method;
	final String regex;
	final Model model;

	public Args(String method, String regex, Model model) {
		this.method = method;
		this.regex = regex;
		this.model = model;
	}

}