/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.integration;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.spatial.HSMessageLogger;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.dialect.hana.HANASpatialDialect;
import org.hibernate.spatial.integration.jts.JtsGeomEntity;
import org.hibernate.spatial.predicate.SpatialPredicates;
import org.hibernate.spatial.testing.SpatialDialectMatcher;
import org.hibernate.spatial.testing.SpatialFunctionalTestCase;

import org.hibernate.testing.Skip;
import org.hibernate.testing.SkipForDialect;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @see TestSpatialRestrictions
 */
@Skip(condition = SpatialDialectMatcher.class, message = "No Spatial Dialect")
@SkipForDialect(value = HANASpatialDialect.class, comment = "The HANA dialect is tested via org.hibernate.spatial.dialect.hana.TestHANASpatialFunctions", jiraKey = "HHH-12426")
public class TestSpatialPredicates extends SpatialFunctionalTestCase {

	private static HSMessageLogger LOG = Logger.getMessageLogger(
			HSMessageLogger.class,
			TestSpatialPredicates.class.getName()
	);

	protected HSMessageLogger getLogger() {
		return LOG;
	}

	@Test
	public void within() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.within ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getWithin( expectationsFactory.getTestPolygon() );
		BiFunction<CriteriaBuilder, Root<JtsGeomEntity>, Predicate> predicateFactory = (criteriaBuilder, root) ->
				SpatialPredicates.within( criteriaBuilder, root.get( "geom" ), expectationsFactory.getTestPolygon() );
		retrieveAndCompare( dbexpected, predicateFactory );
	}

	@Test
	public void filter() throws SQLException {
		if ( !dialectSupportsFiltering() ) {
			LOG.info( "Filtering is not supported by Dialect" );
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getFilter( expectationsFactory.getTestPolygon() );
		BiFunction<CriteriaBuilder, Root<JtsGeomEntity>, Predicate> predicateFactory = (criteriaBuilder, root) ->
				SpatialPredicates.filter( criteriaBuilder, root.get( "geom" ), expectationsFactory.getTestPolygon() );
		retrieveAndCompare( dbexpected, predicateFactory );
	}

	@Test
	public void contains() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.contains ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getContains( expectationsFactory.getTestPolygon() );
		BiFunction<CriteriaBuilder, Root<JtsGeomEntity>, Predicate> predicateFactory = (criteriaBuilder, root) ->
				SpatialPredicates.contains(
						criteriaBuilder,
						root.get( "geom" ),
						expectationsFactory.getTestPolygon()
				);
		retrieveAndCompare( dbexpected, predicateFactory );
	}

	@Test
	public void crosses() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.crosses ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getCrosses( expectationsFactory.getTestPolygon() );
		BiFunction<CriteriaBuilder, Root<JtsGeomEntity>, Predicate> predicateFactory = (criteriaBuilder, root) ->
				SpatialPredicates.crosses( criteriaBuilder, root.get( "geom" ), expectationsFactory.getTestPolygon() );
		retrieveAndCompare( dbexpected, predicateFactory );
	}

	@Test
	public void touches() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.touches ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getTouches( expectationsFactory.getTestPolygon() );
		BiFunction<CriteriaBuilder, Root<JtsGeomEntity>, Predicate> predicateFactory = (criteriaBuilder, root) ->
				SpatialPredicates.touches( criteriaBuilder, root.get( "geom" ), expectationsFactory.getTestPolygon() );
		retrieveAndCompare( dbexpected, predicateFactory );
	}

	@Test
	public void disjoint() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.disjoint ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getDisjoint( expectationsFactory.getTestPolygon() );
		BiFunction<CriteriaBuilder, Root<JtsGeomEntity>, Predicate> predicateFactory = (criteriaBuilder, root) ->
				SpatialPredicates.disjoint(
						criteriaBuilder,
						root.get( "geom" ),
						expectationsFactory.getTestPolygon()
				);
		retrieveAndCompare( dbexpected, predicateFactory );
	}

	@Test
	public void eq() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.equals ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getEquals( expectationsFactory.getTestPolygon() );
		BiFunction<CriteriaBuilder, Root<JtsGeomEntity>, Predicate> predicateFactory = (criteriaBuilder, root) ->
				SpatialPredicates.eq( criteriaBuilder, root.get( "geom" ), expectationsFactory.getTestPolygon() );
		retrieveAndCompare( dbexpected, predicateFactory );
	}

	@Test
	public void intersects() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.intersects ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getIntersects( expectationsFactory.getTestPolygon() );
		BiFunction<CriteriaBuilder, Root<JtsGeomEntity>, Predicate> predicateFactory = (criteriaBuilder, root) ->
				SpatialPredicates.intersects(
						criteriaBuilder,
						root.get( "geom" ),
						expectationsFactory.getTestPolygon()
				);
		retrieveAndCompare( dbexpected, predicateFactory );
	}

	@Test
	public void overlaps() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.overlaps ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getOverlaps( expectationsFactory.getTestPolygon() );
		BiFunction<CriteriaBuilder, Root<JtsGeomEntity>, Predicate> predicateFactory = (criteriaBuilder, root) ->
				SpatialPredicates.overlaps(
						criteriaBuilder,
						root.get( "geom" ),
						expectationsFactory.getTestPolygon()
				);
		retrieveAndCompare( dbexpected, predicateFactory );
	}

	@Test
	public void dwithin() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.dwithin ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getDwithin( expectationsFactory.getTestPoint(), 30.0 );
		BiFunction<CriteriaBuilder, Root<JtsGeomEntity>, Predicate> predicateFactory = (criteriaBuilder, root) ->
				SpatialPredicates.distanceWithin(
						criteriaBuilder,
						root.get( "geom" ),
						expectationsFactory.getTestPoint(),
						30.0
				);
		retrieveAndCompare( dbexpected, predicateFactory );
	}

	@Test
	public void isEmpty() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.isempty ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getIsEmpty();
		BiFunction<CriteriaBuilder, Root<JtsGeomEntity>, Predicate> predicateFactory = (criteriaBuilder, root) ->
				SpatialPredicates.isEmpty( criteriaBuilder, root.get( "geom" ) );
		retrieveAndCompare( dbexpected, predicateFactory );
	}

	@Test
	public void isNotEmpty() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.isempty ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.getIsNotEmpty();
		BiFunction<CriteriaBuilder, Root<JtsGeomEntity>, Predicate> predicateFactory = (criteriaBuilder, root) ->
				SpatialPredicates.isNotEmpty( criteriaBuilder, root.get( "geom" ) );
		retrieveAndCompare( dbexpected, predicateFactory );
	}

	@Test
	public void havingSRID() throws SQLException {
		if ( !isSupportedByDialect( SpatialFunction.srid ) ) {
			return;
		}
		Map<Integer, Boolean> dbexpected = expectationsFactory.havingSRID( 4326 );
		BiFunction<CriteriaBuilder, Root<JtsGeomEntity>, Predicate> predicateFactory = (criteriaBuilder, root) ->
				SpatialPredicates.havingSRID( criteriaBuilder, root.get( "geom" ), 4326 );
		retrieveAndCompare( dbexpected, predicateFactory );
		dbexpected = expectationsFactory.havingSRID( 31370 );
		predicateFactory = (criteriaBuilder, root) ->
				SpatialPredicates.havingSRID( criteriaBuilder, root.get( "geom" ), 31370 );
		retrieveAndCompare( dbexpected, predicateFactory );
	}

	private void retrieveAndCompare(
			Map<Integer, Boolean> dbexpected,
			BiFunction<CriteriaBuilder, Root<JtsGeomEntity>, Predicate> predicateFactory) {
		try (Session session = openSession()) {
			Transaction tx = null;
			try {
				tx = session.beginTransaction();
				CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
				CriteriaQuery<JtsGeomEntity> criteriaQuery = criteriaBuilder.createQuery( JtsGeomEntity.class );
				Root<JtsGeomEntity> root = criteriaQuery.from( JtsGeomEntity.class );
				criteriaQuery.select( root )
						.where( predicateFactory.apply( criteriaBuilder, root ) );
				List<JtsGeomEntity> list = session.createQuery( criteriaQuery )
						.getResultList();
				compare( dbexpected, list );
			}
			finally {
				if ( tx != null ) {
					tx.rollback();
				}
			}
		}
	}

	private void compare(Map<Integer, Boolean> dbexpected, List<JtsGeomEntity> list) {
		int cnt = dbexpected.entrySet()
				.stream()
				.filter( Map.Entry::getValue )
				.reduce( 0, (accumulator, entry) -> {
					if ( !findInList( entry.getKey(), list ) ) {
						fail( String.format( "Expected object with id= %d, but not found in result", entry.getKey() ) );
					}
					return accumulator + 1;
				}, Integer::sum );
		assertEquals( cnt, list.size() );
		LOG.infof( "Found %d objects within testsuite-suite polygon.", cnt );
	}

	private boolean findInList(Integer id, List<JtsGeomEntity> list) {
		return list.stream()
				.anyMatch( entity -> entity.getId().equals( id ) );
	}
}
